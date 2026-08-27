package nl.obren.sokrates.sourcecode.githistory;

/*
 * Reads the git-history.txt produced by GitHistoryExtractor. Each line is:
 *   <date> <email> <commitId> <path> <name> [<linesAdded> <linesDeleted>]
 * The two churn columns are optional - older history files omit them, in which case
 * linesAdded/linesDeleted default to 0.
 *
 * Equivalent git command (without churn columns):
 * git ls-files -z | xargs -0 -n1 -I{} -- git log --date=short --format="%ad %ae %H {}" {} > git-history.txt
 * git log --merges --first-parent --date=short --format="%ad %ae" > git-merges.txt
 */

import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.sourcecode.analysis.CoAuthorsConfig;
import nl.obren.sokrates.sourcecode.analysis.FileHistoryAnalysisConfig;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.landscape.PersonConfig;
import nl.obren.sokrates.sourcecode.operations.ComplexOperation;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;

public class GitHistoryUtils {
    public static final String GIT_HISTORY_FILE_NAME = "git-history.txt";
    // Optional sidecar written by GitHistoryExtractor next to git-history.txt: one line per
    // commit, "<sha> <first line of the commit message>". A sidecar (rather than extra columns
    // on git-history.txt lines) keeps older parsers of the main file working unchanged.
    public static final String GIT_COMMITS_FILE_NAME = "git-commits.txt";
    // Optional sidecar written by GitHistoryExtractor next to git-history.txt: one line per
    // (commit, trailer), "<sha> <Key>: <value>" — the commit message's trailers (Co-authored-by,
    // Signed-off-by, ...) verbatim. The committer identity rides along as the pseudo-trailer
    // "Committer: Name <email>", written only when it differs from the author. Sha-keyed like
    // git-commits.txt, so sub-history splits copy it unchanged; older extractions don't have it.
    public static final String GIT_COMMIT_TRAILERS_FILE_NAME = "git-commit-trailers.txt";
    public static final String COMMITTER_TRAILER_KEY = "Committer";
    // Pseudo-trailer for tool signature lines in the message body that are not trailers, e.g.
    // "🤖 Generated with [Claude Code](https://claude.com/claude-code)" (older Claude Code versions
    // wrote only this line, without a Co-authored-by). Captured by extractMessageSignatures with a
    // generic "<generated|made|...> with ..." line-start heuristic; the analysis config decides which
    // tool it names (values of this key only ever resolve to AI agents, never to people).
    public static final String MESSAGE_SIGNATURE_TRAILER_KEY = "Message-Signature";
    private static final java.util.regex.Pattern MESSAGE_SIGNATURE_LINE = java.util.regex.Pattern.compile(
            "^\\W*(?:generated|made|created|built)\\s+(?:with|by|using)\\s+\\S.*$",
            java.util.regex.Pattern.CASE_INSENSITIVE);
    public static final String EARLIEST_DATE = "1980-01-01";
    private static final Log LOG = LogFactory.getLog(GitHistoryUtils.class);
    private static List<FileUpdate> updates = null;
    // Co-authors per sha for the history file last resolved (getAuthorCommits is called once per
    // scope; the sidecar is read once per file). Keyed by path, unlike the JVM-wide updates cache.
    private static String coAuthorsCacheFile = null;
    private static Map<String, List<CoAuthor>> coAuthorsCache = null;
    private static Map<String, String> anonymizeEmails = new HashMap<>();

    /**
     * Reads the optional git-commits.txt sidecar into a sha -> first-message-line map. Returns an
     * empty map when the file is absent (older extractions) or unreadable, so consumers degrade
     * gracefully to message-less behavior.
     */
    public static Map<String, String> getCommitMessagesFromFile(File file) {
        Map<String, String> messages = new HashMap<>();
        if (file == null || !file.exists()) {
            return messages;
        }
        try {
            FileUtils.readLines(file, StandardCharsets.UTF_8).forEach(line -> {
                int index = line.indexOf(' ');
                if (index > 0 && index < line.length() - 1) {
                    messages.putIfAbsent(line.substring(0, index), line.substring(index + 1).trim());
                }
            });
        } catch (IOException e) {
            LOG.error("Could not read " + file.getPath(), e);
        }
        return messages;
    }

    /**
     * Reads the optional git-commit-trailers.txt sidecar into a sha -> trailers map (trailer order
     * preserved). Returns an empty map when the file is absent (older extractions) or unreadable.
     */
    public static Map<String, List<CommitTrailer>> getCommitTrailersFromFile(File file) {
        Map<String, List<CommitTrailer>> trailers = new HashMap<>();
        if (file == null || !file.exists()) {
            return trailers;
        }
        try {
            FileUtils.readLines(file, StandardCharsets.UTF_8).forEach(line -> {
                int index = line.indexOf(' ');
                if (index > 0 && index < line.length() - 1) {
                    CommitTrailer trailer = CommitTrailer.parse(line.substring(index + 1));
                    if (trailer != null) {
                        trailers.computeIfAbsent(line.substring(0, index), k -> new ArrayList<>()).add(trailer);
                    }
                }
            });
        } catch (IOException e) {
            LOG.error("Could not read " + file.getPath(), e);
        }
        return trailers;
    }

    /**
     * Extracts the trailers of a full commit message: the "Key: value" lines of the last paragraph
     * (git's own leniency applies — a final paragraph that is only partly trailers still yields
     * its trailer lines; an indented continuation line is folded into the previous trailer).
     * Pure string logic (no JGit) so it is testable and reusable outside the extractor.
     */
    public static List<CommitTrailer> extractTrailers(String fullMessage) {
        List<CommitTrailer> trailers = new ArrayList<>();
        if (StringUtils.isBlank(fullMessage)) {
            return trailers;
        }
        String[] lines = fullMessage.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        int end = lines.length;
        while (end > 0 && StringUtils.isBlank(lines[end - 1])) {
            end--;
        }
        int start = end;
        while (start > 0 && !StringUtils.isBlank(lines[start - 1])) {
            start--;
        }
        // A one-paragraph message has no trailer block (the paragraph is the subject).
        if (start == 0) {
            return trailers;
        }
        CommitTrailer last = null;
        for (int i = start; i < end; i++) {
            String line = lines[i];
            if (last != null && Character.isWhitespace(line.charAt(0))) {
                last.setValue((last.getValue() + " " + line.trim()).trim());
                continue;
            }
            CommitTrailer trailer = CommitTrailer.parse(line);
            if (trailer != null) {
                trailers.add(trailer);
            }
            last = trailer;
        }
        return trailers;
    }

    /**
     * Tool signature lines in a commit message body (every line after the subject line) such as
     * "🤖 Generated with [Claude Code](...)": lines that, after optional leading non-word characters
     * (emoji, bullets), start with "generated/made/created/built with/by/using".
     * Anchored at line start so prose like "regenerated with -Dsokrates.updateGolden" is not matched.
     */
    public static List<String> extractMessageSignatures(String fullMessage) {
        List<String> signatures = new ArrayList<>();
        if (StringUtils.isBlank(fullMessage)) {
            return signatures;
        }
        String[] lines = fullMessage.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (MESSAGE_SIGNATURE_LINE.matcher(line).matches() && !signatures.contains(line)) {
                signatures.add(line);
            }
        }
        return signatures;
    }

    public static String printContributorsCommand() {
        return "git ls-files -z | xargs -0 -n1 -I{} -- git log --date=short --format=\"%ad %ae %H {}\" {} > " + GIT_HISTORY_FILE_NAME;
    }

    public static List<AuthorCommit> getAuthorCommits(File file, FileHistoryAnalysisConfig config) {
        return getAuthorCommits(file, config, null);
    }

    /**
     * Builds the per-commit list, optionally restricted to file updates matching {@code pathFilter}
     * (e.g. only files in the main scope). A commit whose files are all filtered out produces no
     * AuthorCommit, so commit/contributor counts reflect only the selected scope. When the filter is
     * null every file update is included (the all-scope behaviour).
     */
    public static List<AuthorCommit> getAuthorCommits(File file, FileHistoryAnalysisConfig config, Predicate<FileUpdate> pathFilter) {
        List<AuthorCommit> commits = new ArrayList<>();
        Map<String, AuthorCommit> commitsMap = new HashMap<>();

        int index[] = {0};

        List<FileUpdate> historyFromFile = getHistoryFromFile(file, config);
        Map<String, List<CoAuthor>> coAuthorsBySha = getCachedCoAuthorsBySha(file, config);
        historyFromFile.forEach(fileUpdate -> {
            index[0] += 1;
            if (index[0] % 1000 == 1 || index[0] == historyFromFile.size()) {
                LOG.info("Importing " + fileUpdate.getAuthorEmail() + " " + fileUpdate.getDate() + " (" + index[0] + " / " + historyFromFile.size() + ")");
            }
            if (pathFilter != null && !pathFilter.test(fileUpdate)) {
                return;
            }
            String commitId = fileUpdate.getCommitId();
            AuthorCommit existing = commitsMap.get(commitId);
            if (existing == null) {
                AuthorCommit authorCommit = new AuthorCommit(fileUpdate.getDate(), fileUpdate.getAuthorEmail(), fileUpdate.getUserName(), fileUpdate.isBot());
                authorCommit.addChurn(fileUpdate.getLinesAdded(), fileUpdate.getLinesDeleted());
                List<CoAuthor> coAuthors = coAuthorsBySha.get(commitId);
                if (coAuthors != null) {
                    authorCommit.setCoAuthors(coAuthors);
                }
                commits.add(authorCommit);
                commitsMap.put(commitId, authorCommit);
            } else {
                existing.incrementFileUpdatesCount();
                existing.addChurn(fileUpdate.getLinesAdded(), fileUpdate.getLinesDeleted());
            }
        });

        return commits;
    }

    public static boolean shouldIgnore(String email, List<String> ignoreContributors) {
        for (String ignorePattern : ignoreContributors) {
            if (RegexUtils.matchesEntirely(ignorePattern.toLowerCase(), email.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, List<CoAuthor>> getCachedCoAuthorsBySha(File file, FileHistoryAnalysisConfig config) {
        String key = file == null ? "" : file.getAbsolutePath();
        if (coAuthorsCache == null || !key.equals(coAuthorsCacheFile)) {
            coAuthorsCache = getCoAuthorsBySha(file, config);
            coAuthorsCacheFile = key;
        }
        return coAuthorsCache;
    }

    public static List<FileUpdate> getHistoryFromFile(File file, FileHistoryAnalysisConfig config) {
        if (updates != null) {
            return updates;
        }
        updates = new ArrayList<>();
        LOG.info("Reading history from file");
        List<String> lines;
        try {
            lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.info(e.getMessage());
            return updates;
        }

        int displayCounter[] = {0};
        lines.forEach(line -> {
            displayCounter[0] += 1;
            boolean lastLine = displayCounter[0] == lines.size();
            if (displayCounter[0] % 1000 == 1 || lastLine) {
                LOG.info("Reading commit line " + displayCounter[0] + "/" + lines.size() +
                        ": " + StringUtils.abbreviate(line, 64));
            }
            FileUpdate fileUpdate = GitHistoryUtils.parseLine(line, config);
            if (fileUpdate != null) {
                // parseLine already set the bot flag (testing the original and any transformed
                // email); no need to recompute it here.
                updates.add(fileUpdate);
            }
        });

        return updates;
    }

    /**
     * Normalises a (lowercased) commit identity email the way the reports see it: null when it
     * matches ignoreContributors (before or after transformation), else anonymised (when enabled)
     * or run through transformContributorEmails. Shared by commit authors and co-authors so both
     * collapse to the same identities.
     */
    static String normalizeEmail(String email, FileHistoryAnalysisConfig config) {
        List<String> ignoreContributors = config.getIgnoreContributors();
        if (shouldIgnore(email, ignoreContributors)) {
            return null;
        }
        if (config.isAnonymizeContributors()) {
            String anonymized = anonymizeEmails.get(email);
            if (anonymized == null) {
                anonymized = "Contributor " + (anonymizeEmails.keySet().size() + 1);
                anonymizeEmails.put(email, anonymized);
            }
            return anonymized;
        }
        if (config.getTransformContributorEmails().size() > 0) {
            ComplexOperation operation = new ComplexOperation(config.getTransformContributorEmails());
            email = operation.exec(email);
            if (shouldIgnore(email, ignoreContributors)) {
                return null;
            }
        }
        return email;
    }

    /**
     * Applies the optional people config (config-people.json): if the email matches a person's
     * emailPatterns OR the userName matches a userNamePattern, remap the email to that person's
     * canonical email (so a person's multiple identities collapse into one in the reports) and
     * override the userName with the configured display name when one is set. Matches on the raw
     * userName before overriding it. Returns {email, userName}.
     */
    static String[] applyPeopleConfig(String email, String userName, FileHistoryAnalysisConfig config) {
        if (config.getPeopleConfig() != null) {
            PersonConfig personConfig = config.getPeopleConfig().getPerson(email, userName);
            if (StringUtils.isNotBlank(personConfig.getEmail())) {
                email = personConfig.getEmail();
            }
            if (StringUtils.isNotBlank(personConfig.getUserName())) {
                userName = personConfig.getUserName();
            }
        }
        return new String[]{email, userName};
    }

    /**
     * Resolves the co-authors of every commit from the optional git-commit-trailers.txt sidecar
     * next to {@code historyFile}: trailers whose key is one of config.coAuthors.trailerKeys become
     * a CoAuthor — an AI agent when the value matches an aiAgents pattern (or the email matches the
     * bots list, agent = "bot"), else a person whose email/name are normalised exactly like commit
     * authors (ignored identities are dropped). Deduped per commit by CoAuthor.getKey(). Empty map
     * when there is no sidecar (older extractions) or when config.coAuthors.enabled is false.
     */
    public static Map<String, List<CoAuthor>> getCoAuthorsBySha(File historyFile, FileHistoryAnalysisConfig config) {
        Map<String, List<CoAuthor>> result = new HashMap<>();
        if (historyFile == null || config.getCoAuthors() == null || !config.getCoAuthors().isEnabled()) {
            return result;
        }
        File trailersFile = new File(historyFile.getParentFile(), GIT_COMMIT_TRAILERS_FILE_NAME);
        Map<String, List<CommitTrailer>> trailersBySha = getCommitTrailersFromFile(trailersFile);
        trailersBySha.forEach((sha, trailers) -> {
            List<CoAuthor> coAuthors = resolveCoAuthors(trailers, config);
            if (!coAuthors.isEmpty()) {
                result.put(sha, coAuthors);
            }
        });
        return result;
    }

    static List<CoAuthor> resolveCoAuthors(List<CommitTrailer> trailers, FileHistoryAnalysisConfig config) {
        List<CoAuthor> coAuthors = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        CoAuthorsConfig coAuthorsConfig = config.getCoAuthors();
        for (CommitTrailer trailer : trailers) {
            if (!coAuthorsConfig.isCoAuthorKey(trailer.getKey())) {
                continue;
            }
            CoAuthor coAuthor = resolveCoAuthor(trailer, config);
            if (coAuthor != null && keys.add(coAuthor.getKey())) {
                coAuthors.add(coAuthor);
            }
        }
        return coAuthors;
    }

    private static CoAuthor resolveCoAuthor(CommitTrailer trailer, FileHistoryAnalysisConfig config) {
        String name = trailer.getName();
        String email = trailer.getEmail();
        String agent = config.getCoAuthors().classify(trailer.getValue(), email);
        if (agent == null && StringUtils.isNotBlank(email) && isBot(email, config.getBots())) {
            agent = CoAuthor.BOT_AGENT;
        }
        // A message signature line names a tool or nothing — never a person.
        if (agent == null && trailer.hasKey(MESSAGE_SIGNATURE_TRAILER_KEY)) {
            return null;
        }
        if (agent != null) {
            return new CoAuthor(name, email, agent);
        }
        if (StringUtils.isBlank(email)) {
            return StringUtils.isBlank(name) ? null : new CoAuthor(name, "", null);
        }
        String normalized = normalizeEmail(email, config);
        if (normalized == null) {
            return null;
        }
        String[] identity = applyPeopleConfig(normalized, name, config);
        return new CoAuthor(identity[1], identity[0], null);
    }

    public static FileUpdate parseLine(String line, FileHistoryAnalysisConfig config) {

        int index1 = line.indexOf(" ");
        if (index1 >= 10) {
            int index2 = line.indexOf(" ", index1 + 1);
            if (index2 > 0) {
                int index3 = line.indexOf(" ", index2 + 1);
                if (index3 > 0) {
                    String date = line.substring(0, 10).trim();
                    if (ignoreCommitByDate(line, date)) {
                        return null;
                    }
                    String rawEmail = line.substring(index1 + 1, index2).trim().toLowerCase();
                    String authorEmail = normalizeEmail(rawEmail, config);
                    if (authorEmail == null) {
                        return null;
                    }

                    // Bot detection on the final (possibly transformed/anonymized) email - computed
                    // once here. getHistoryFromFile previously recomputed this; that is now redundant.
                    boolean bot = isBot(authorEmail, config.getBots());

                    String commitId = line.substring(index2 + 1, index3).trim();
                    String path = line.substring(index3 + 1).replaceAll(" .*", "").replaceAll("[&]nbsp[;]", " ").trim();

                    int index4 = line.indexOf(" ", index3 + 1);

                    String userName = "";
                    if (index4 > index3) {
                        userName = line.substring(index4 + 1).replaceAll(" .*", "").replaceAll("[&]nbsp[;]", " ").trim();
                    }

                    String[] identity = applyPeopleConfig(authorEmail, userName, config);
                    authorEmail = identity[0];
                    userName = identity[1];

                    FileUpdate fileUpdate = new FileUpdate(date, authorEmail, userName, commitId, path, bot);

                    // Optional trailing churn columns: "... <name> <linesAdded> <linesDeleted>".
                    // Older git-history.txt files don't have them, so absence is fine (defaults stay 0).
                    if (index4 > index3) {
                        String[] tokens = line.substring(index4 + 1).trim().split(" ");
                        if (tokens.length >= 3) {
                            fileUpdate.setLinesAdded(parseChurn(tokens[tokens.length - 2]));
                            fileUpdate.setLinesDeleted(parseChurn(tokens[tokens.length - 1]));
                        }
                    }

                    return fileUpdate;
                }
            }
        }

        return null;
    }

    private static boolean ignoreCommitByDate(String line, String date) {
        if (date.compareTo(DateUtils.getAnalysisDate()) > 0) {
            LOG.info("Ignoring future date: " + line);
            return true;
        }
        if (date.compareTo(EARLIEST_DATE) < 0) {
            LOG.info("Ignoring dates before the initial git release: " + line);
            return true;
        }
        return false;
    }

    private static int parseChurn(String token) {
        try {
            return Integer.parseInt(token.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static boolean isBot(String email, List<String> bots) {
        // Case-insensitive: bot identity is matched against the email regardless of case.
        return RegexUtils.matchesAnyPatternIgnoreCase(email, bots);
    }
}
