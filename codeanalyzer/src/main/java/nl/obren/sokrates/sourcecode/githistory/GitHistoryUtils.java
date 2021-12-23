package nl.obren.sokrates.sourcecode.githistory;

/*
 * Assumes that you have generated the text file being read using the following git command:
 * git ls-files -z | xargs -0 -n1 -I{} -- git log --date=short --format="%ad %ae %H {}" {} > git-history.txt
 * git log --merges --first-parent --date=short --format="%ad %ae" > git-merges.txt
 */

import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.sourcecode.analysis.FileHistoryAnalysisConfig;
import nl.obren.sokrates.sourcecode.operations.ComplexOperation;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GitHistoryUtils {
    public static final String GIT_HISTORY_FILE_NAME = "git-history.txt";
    private static List<FileUpdate> updates = null;
    private static Map<String, String> anonymizeEmails = new HashMap<>();

    public static String printContributorsCommand() {
        return "git ls-files -z | xargs -0 -n1 -I{} -- git log --date=short --format=\"%ad %ae %H {}\" {} > " + GIT_HISTORY_FILE_NAME;
    }

    public static List<AuthorCommit> getAuthorCommits(File file, FileHistoryAnalysisConfig config) {
        List<AuthorCommit> commits = new ArrayList<>();
        List<String> commitIds = new ArrayList<>();

        getHistoryFromFile(file, config).forEach(fileUpdate -> {
            String commitId = fileUpdate.getCommitId();
            if (!commitIds.contains(commitId)) {
                commitIds.add(commitId);
                commits.add(new AuthorCommit(fileUpdate.getDate(), fileUpdate.getAuthorEmail()));
            }
        });

        return commits;
    }

    public static boolean shouldIgnore(String email, List<String> ignoreContributors) {
        for (String ignorePattern : ignoreContributors) {
            if (RegexUtils.matchesEntirely(ignorePattern, email)) {
                return true;
            }
        }
        return false;
    }

    public static List<FileUpdate> getHistoryFromFile(File file, FileHistoryAnalysisConfig config) {
        if (updates != null) {
            return updates;
        }
        updates = new ArrayList<>();
        System.out.println("Reading history from file");
        List<String> lines;
        try {
            lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return updates;
        }

        lines.forEach(line -> {
            System.out.println(line);
            FileUpdate fileUpdate = GitHistoryUtils.parseLine(line, config);
            if (fileUpdate != null) {
                updates.add(fileUpdate);
            }
        });

        return updates;
    }

    public static FileUpdate parseLine(String line, FileHistoryAnalysisConfig config) {
        List<String> ignoreContributors = config.getIgnoreContributors();
        boolean anonymize = config.isAnonymizeContributors();

        int index1 = line.indexOf(" ");
        if (index1 >= 10) {
            int index2 = line.indexOf(" ", index1 + 1);
            if (index2 > 0) {
                int index3 = line.indexOf(" ", index2 + 1);
                if (index3 > 0) {
                    String date = line.substring(0, 10).trim();
                    String author = line.substring(index1 + 1, index2).trim();
                    if (anonymize) {
                        if (shouldIgnore(author, ignoreContributors)) {
                            return null;
                        }
                        String anonymizedAuthor = anonymizeEmails.get(author);
                        if (anonymizedAuthor == null) {
                            anonymizedAuthor = "Contributor " + (anonymizeEmails.keySet().size() + 1);
                            anonymizeEmails.put(author, anonymizedAuthor);
                        }
                        author = anonymizedAuthor;
                        System.out.println(author + " -> " + anonymizedAuthor);
                    } else if (config.getTransformContributorEmails().size() > 0) {
                        ComplexOperation operation = new ComplexOperation(config.getTransformContributorEmails());
                        String original = author;
                        author = operation.exec(author);
                        System.out.println(original + " -> " + author);
                        if (shouldIgnore(author, ignoreContributors)) {
                            return null;
                        }
                    }

                    String commitId = line.substring(index2 + 1, index3).trim();
                    String path = line.substring(index3 + 1).trim();

                    return new FileUpdate(date, author, commitId, path);
                }
            }
        }

        return null;
    }
}
