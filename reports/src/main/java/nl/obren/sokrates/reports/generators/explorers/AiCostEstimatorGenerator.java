package nl.obren.sokrates.reports.generators.explorers;

import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.renderingutils.ExplorerTemplate;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.githistory.FileUpdate;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Generates the client-rendered AI cost estimator (explorers/ai-cost-estimator.html): what the
 * repository's history would have cost if an AI coding agent had written it. The git history is
 * reduced here to tasks and sessions (deterministic, from the same file updates as the commits
 * explorer); the page applies the token and price priors and simulates their uncertainty.
 * <ul>
 * <li>Only the analyzed scopes count (main, test, build and deployment, other): changes to files
 * that exist but are in no scope (unanalyzed extensions, ignored files) are dropped, while paths no
 * longer in the codebase are kept when their extension is analyzed (deleted or renamed code).</li>
 * <li>Noise is dropped first: bot commits, commits touching more than {@link #MAX_FILES_PER_COMMIT}
 * files, lockfiles, vendored and generated paths, and single file changes over
 * {@link #MAX_LINES_PER_FILE_CHANGE} lines. Merge commits are not in git-history.txt at all.</li>
 * <li>A task groups the commits that share a ticket id in the message (a prefix such as ABC-123
 * seen with at least {@link #MIN_TICKET_NUMBERS} numbers), otherwise consecutive commits of one
 * author at most {@link #TASK_MAX_GAP_DAYS} day apart that touch overlapping files (the history
 * has dates, no times of day).</li>
 * <li>A task is split into sessions of at most {@link #SESSION_MAX_FILES} files or
 * {@link #SESSION_MAX_CHURN} churned lines; a session reads the edited files (their current total
 * lines, comments and blank lines included, as an agent reads the whole file; at most
 * {@link #MAX_READ_LINES} lines each), new files are written, deleted files cost nothing.</li>
 * <li>The task type comes from the shape of its changes: new code, refactoring, fix or feature.</li>
 * </ul>
 */
public class AiCostEstimatorGenerator {
    public static final String DATA_FILE_NAME = "aiCostEstimator.json";
    public static final String PAGE_FILE_NAME = "ai-cost-estimator.html";
    static final int MAX_COMMITS = 20000;
    static final int MAX_FILES_PER_COMMIT = 300;
    static final int MAX_LINES_PER_FILE_CHANGE = 3000;
    static final int MAX_READ_LINES = 2000;
    static final int SESSION_MAX_FILES = 10;
    static final int SESSION_MAX_CHURN = 400;
    static final int TASK_MAX_GAP_DAYS = 1;
    static final int MIN_TICKET_NUMBERS = 5;

    static final String TYPE_NEW = "new";
    static final String TYPE_REFACTOR = "refactor";
    static final String TYPE_FIX = "fix";
    static final String TYPE_FEATURE = "feature";

    private static final int EDIT = 0;
    private static final int NEW = 1;
    private static final int DELETE = 2;

    private static final Pattern TICKET = Pattern.compile("\\b([A-Z][A-Z0-9]{1,9})-(\\d{1,7})\\b");
    // Standard names that look like ticket ids (UTF-8, SHA-256, CVE-2024-…, PEP-8, …).
    private static final Set<String> NOT_TICKETS = new HashSet<>(Arrays.asList(
            "UTF", "ISO", "SHA", "RFC", "CVE", "CWE", "HTTP", "TLS", "SSL", "MD", "ES", "ECMA", "PEP", "JSR", "JEP",
            "GHSA", "WIP", "TODO", "FIXME", "COVID", "IPV", "X", "AES", "RSA", "BASE"));

    private static final Set<String> LOCK_FILES = new HashSet<>(Arrays.asList(
            "package-lock.json", "npm-shrinkwrap.json", "yarn.lock", "pnpm-lock.yaml", "bun.lockb", "cargo.lock",
            "poetry.lock", "pipfile.lock", "uv.lock", "pdm.lock", "gemfile.lock", "composer.lock", "go.sum",
            "podfile.lock", "pubspec.lock", "mix.lock", "packages.lock.json", "gradle.lockfile", "flake.lock",
            "deno.lock", "paket.lock", "cartfile.resolved", "package.resolved"));
    private static final Pattern VENDORED = Pattern.compile(
            "(^|.*/)(vendor|vendors|third_party|third-party|thirdparty|node_modules|bower_components|external|externals|deps)/.*");
    private static final Pattern GENERATED = Pattern.compile(
            "(^|.*/)(dist|generated|gen|__generated__)/.*|.*\\.(min\\.js|min\\.css|map|pb\\.go|g\\.dart|designer\\.cs)$|.*_pb2\\.py$");

    private final File reportsFolder;

    public AiCostEstimatorGenerator(File reportsFolder) {
        this.reportsFolder = reportsFolder;
    }

    /** The repository's estimator data: the history (git-history.txt next to the config) reduced to tasks and sessions. */
    public static AiCostEstimatorData build(CodeAnalysisResults results, File sokratesConfigFolder) {
        List<FileUpdate> fileUpdates = CommitsExplorerGenerators.readFileUpdates(results, sokratesConfigFolder);
        Map<String, String> messagesBySha = CommitsExplorerGenerators.readCommitMessages(results, sokratesConfigFolder);
        Set<String> extensions = new HashSet<>();
        results.getCodeConfiguration().getExtensions().forEach(extension -> extensions.add(extension.toLowerCase()));
        List<CommitFileExport> currentFiles = new ArrayList<>();
        File[] sourceRoot = {null};
        Set<SourceFile> seen = new HashSet<>();
        collect(currentFiles, seen, sourceRoot, results.getMainAspectAnalysisResults().getAspect(), "main");
        collect(currentFiles, seen, sourceRoot, results.getTestAspectAnalysisResults().getAspect(), "test");
        collect(currentFiles, seen, sourceRoot, results.getGeneratedAspectAnalysisResults().getAspect(), "generated");
        collect(currentFiles, seen, sourceRoot, results.getBuildAndDeployAspectAnalysisResults().getAspect(), "build");
        collect(currentFiles, seen, sourceRoot, results.getOtherAspectAnalysisResults().getAspect(), "other");
        // A path in no scope counts only when it is gone from the disk (deleted or renamed) and its extension is analyzed.
        Predicate<String> keepUnscoped = path -> extensions.contains(extensionOf(path))
                && (sourceRoot[0] == null || !new File(sourceRoot[0], path).exists());
        AiCostEstimatorData data = buildData(currentFiles, fileUpdates, messagesBySha, keepUnscoped, MAX_COMMITS);
        data.setMainLinesOfCode(Math.max(0, results.getMainAspectAnalysisResults().getLinesOfCode()));
        data.setLinesInScopes(currentFiles.stream().filter(file -> !"generated".equals(file.getScope()))
                .mapToLong(file -> Math.max(0, file.getLinesOfCode())).sum());
        return data;
    }

    /** Writes the data into the data folder (data/aiCostEstimator.json, packaged into data.zip; the landscape reads it). */
    public static void saveData(AiCostEstimatorData data, File dataFolder) {
        try {
            FileUtils.write(new File(dataFolder, DATA_FILE_NAME), new JsonGenerator().generate(data), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Renders explorers/ai-cost-estimator.html (the repository report) or the landscape's page. */
    public void exportPage(AiCostEstimatorData data, File folder) {
        exportPage(data, folder, "{}");
    }

    /** {@code assumptionsJson}: the configured default assumptions ({@code EstimateAssumptions}), {@code {}} for none. */
    public void exportPage(AiCostEstimatorData data, File folder, String assumptionsJson) {
        try {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("assumptions", assumptionsJson != null ? assumptionsJson : "{}");
            String page = new ExplorerTemplate().render("ai-cost-estimator.html", data, placeholders);
            folder.mkdirs();
            FileUtils.write(new File(folder, PAGE_FILE_NAME), page, UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void exportPage(AiCostEstimatorData data, String assumptionsJson) {
        exportPage(data, new File(reportsFolder, "explorers"), assumptionsJson);
    }

    /** The scope's files with their total lines (what an agent reads); remembers the source root on the way. */
    private static void collect(List<CommitFileExport> files, Set<SourceFile> seen, File[] sourceRoot, NamedSourceCodeAspect aspect, String scope) {
        aspect.getSourceFiles().forEach(file -> {
            if (file.getRelativePath().startsWith("- -") || !seen.add(file)) {
                return;
            }
            File onDisk = file.getFile();
            int lines = onDisk != null && onDisk.exists() ? file.getLines().size() : 0;
            files.add(new CommitFileExport(file.getRelativePath(), scope, Math.max(lines, file.getLinesOfCode())));
            if (sourceRoot[0] == null && onDisk != null) {
                String absolute = onDisk.getAbsolutePath().replace('\\', '/');
                String relative = file.getRelativePath().replace('\\', '/');
                if (absolute.endsWith("/" + relative)) {
                    sourceRoot[0] = new File(absolute.substring(0, absolute.length() - relative.length() - 1));
                }
            }
        });
    }

    static String extensionOf(String path) {
        String name = path.substring(path.lastIndexOf('/') + 1);
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(dot + 1).toLowerCase() : "";
    }

    private static class Change {
        final String path;
        final int added;
        final int deleted;
        int kind = EDIT;

        Change(String path, int added, int deleted) {
            this.path = path;
            this.added = added;
            this.deleted = deleted;
        }
    }

    private static class Commit {
        final String sha;
        final String date;
        final String email;
        final String name;
        final boolean bot;
        final List<Change> changes = new ArrayList<>();
        String ticket = "";

        Commit(FileUpdate update) {
            this.sha = update.getCommitId();
            this.date = update.getDate();
            this.email = update.getAuthorEmail();
            this.name = update.getUserName();
            this.bot = update.isBot();
        }
    }

    private static class FileWork {
        final String path;
        int kind;
        int added = 0;
        int deleted = 0;

        FileWork(String path, int kind) {
            this.path = path;
            this.kind = kind;
        }
    }

    private static class TaskBuilder {
        final AiCostEstimatorData.Task task = new AiCostEstimatorData.Task();
        final Set<String> paths = new HashSet<>();
        final Map<String, FileWork> work = new LinkedHashMap<>();
        String end = "";
    }

    static AiCostEstimatorData buildData(List<CommitFileExport> currentFiles, List<FileUpdate> fileUpdates,
                                         Map<String, String> messagesBySha, Predicate<String> keepUnscoped, int maxCommits) {
        AiCostEstimatorData data = new AiCostEstimatorData();

        Map<String, Integer> currentSize = new HashMap<>();
        Set<String> generatedPaths = new HashSet<>();
        currentFiles.forEach(file -> {
            String key = file.getPath().toLowerCase();
            currentSize.putIfAbsent(key, file.getLinesOfCode());
            if ("generated".equals(file.getScope())) {
                generatedPaths.add(key);
            }
        });
        // A file no longer in the codebase: its largest single change stands in for its size.
        Map<String, Integer> sizeProxy = new HashMap<>();

        Map<String, Commit> bySha = new LinkedHashMap<>();
        fileUpdates.forEach(update -> {
            bySha.computeIfAbsent(update.getCommitId(), sha -> new Commit(update))
                    .changes.add(new Change(update.getPath(), update.getLinesAdded(), update.getLinesDeleted()));
            sizeProxy.merge(update.getPath().toLowerCase(), Math.max(update.getLinesAdded(), update.getLinesDeleted()), Math::max);
        });

        // git log order is newest first; reversed and stably sorted by date, oldest first.
        List<Commit> chronological = new ArrayList<>(bySha.values());
        Collections.reverse(chronological);
        chronological.sort(Comparator.comparing(commit -> commit.date));
        data.setTotalCommitsCount(chronological.size());

        markNewAndDeletedFiles(chronological, currentSize.keySet());

        List<Commit> kept = new ArrayList<>();
        AiCostEstimatorData.Noise noise = data.getNoise();
        for (Commit commit : chronological) {
            if (commit.bot) {
                noise.setBotCommits(noise.getBotCommits() + 1);
                continue;
            }
            if (commit.changes.size() > MAX_FILES_PER_COMMIT) {
                noise.setMassCommits(noise.getMassCommits() + 1);
                noise.setDroppedLines(noise.getDroppedLines() + commit.changes.stream().mapToLong(c -> c.added + c.deleted).sum());
                continue;
            }
            List<Change> changes = new ArrayList<>();
            for (Change change : commit.changes) {
                String lower = change.path.toLowerCase();
                String name = lower.substring(lower.lastIndexOf('/') + 1);
                boolean dropped = true;
                if (!currentSize.containsKey(lower) && !keepUnscoped.test(change.path)) {
                    noise.setUnscopedChanges(noise.getUnscopedChanges() + 1);
                } else if (LOCK_FILES.contains(name)) {
                    noise.setLockFileChanges(noise.getLockFileChanges() + 1);
                } else if (VENDORED.matcher(lower).matches()) {
                    noise.setVendoredChanges(noise.getVendoredChanges() + 1);
                } else if (generatedPaths.contains(lower) || GENERATED.matcher(lower).matches()) {
                    noise.setGeneratedChanges(noise.getGeneratedChanges() + 1);
                } else if (change.added + change.deleted > MAX_LINES_PER_FILE_CHANGE) {
                    noise.setOversizedChanges(noise.getOversizedChanges() + 1);
                } else {
                    dropped = false;
                    changes.add(change);
                }
                if (dropped) {
                    noise.setDroppedLines(noise.getDroppedLines() + change.added + change.deleted);
                }
            }
            if (changes.isEmpty()) {
                noise.setEmptiedCommits(noise.getEmptiedCommits() + 1);
                continue;
            }
            commit.changes.clear();
            commit.changes.addAll(changes);
            kept.add(commit);
        }
        data.setKeptCommitsCount(kept.size());
        if (kept.size() > maxCommits) {
            kept = new ArrayList<>(kept.subList(kept.size() - maxCommits, kept.size()));
        }
        data.setAnalyzedCommitsCount(kept.size());

        Set<String> prefixes = ticketPrefixes(kept, messagesBySha);
        data.setTicketPrefixes(new ArrayList<>(prefixes));
        kept.forEach(commit -> commit.ticket = ticketOf(messagesBySha.getOrDefault(commit.sha, ""), prefixes));

        Map<String, Integer> authorIndex = new HashMap<>();
        List<TaskBuilder> tasks = new ArrayList<>();
        Map<String, TaskBuilder> byTicket = new HashMap<>();
        Map<String, TaskBuilder> openByAuthor = new HashMap<>();
        for (Commit commit : kept) {
            int author = authorIndex.computeIfAbsent(commit.email, email -> {
                data.getAuthors().add(new AiCostEstimatorData.Author(commit.email, commit.name));
                return data.getAuthors().size() - 1;
            });
            TaskBuilder task;
            if (!commit.ticket.isEmpty()) {
                task = byTicket.get(commit.ticket);
                if (task == null) {
                    task = newTask(tasks);
                    task.task.setTicket(commit.ticket);
                    byTicket.put(commit.ticket, task);
                }
            } else {
                task = openByAuthor.get(commit.email);
                if (task == null || daysBetween(task.end, commit.date) > TASK_MAX_GAP_DAYS || !overlaps(task.paths, commit)) {
                    task = newTask(tasks);
                }
                openByAuthor.put(commit.email, task);
            }
            addCommit(task, commit, author);
        }

        tasks.forEach(builder -> {
            finish(builder, currentSize, sizeProxy);
            if (builder.task.churn() > 0) {
                data.getTasks().add(builder.task);
            }
        });
        return data;
    }

    /** A path's first change (oldest first) that only adds lines creates it; a last change that only deletes lines of a path not in the codebase now deletes it. */
    private static void markNewAndDeletedFiles(List<Commit> chronological, Set<String> currentPaths) {
        Set<String> seen = new HashSet<>();
        Map<String, Change> last = new HashMap<>();
        for (Commit commit : chronological) {
            for (Change change : commit.changes) {
                String lower = change.path.toLowerCase();
                if (seen.add(lower) && change.deleted == 0 && change.added > 0) {
                    change.kind = NEW;
                }
                last.put(lower, change);
            }
        }
        last.forEach((path, change) -> {
            if (!currentPaths.contains(path) && change.added == 0 && change.deleted > 0) {
                change.kind = DELETE;
            }
        });
    }

    /** Ticket-like prefixes (ABC in ABC-123) seen with at least MIN_TICKET_NUMBERS different numbers. */
    private static Set<String> ticketPrefixes(List<Commit> commits, Map<String, String> messagesBySha) {
        Map<String, Set<String>> numbers = new TreeMap<>();
        commits.forEach(commit -> {
            Matcher matcher = TICKET.matcher(messagesBySha.getOrDefault(commit.sha, ""));
            while (matcher.find()) {
                if (!NOT_TICKETS.contains(matcher.group(1))) {
                    numbers.computeIfAbsent(matcher.group(1), prefix -> new HashSet<>()).add(matcher.group(2));
                }
            }
        });
        Set<String> prefixes = new TreeSet<>();
        numbers.forEach((prefix, set) -> {
            if (set.size() >= MIN_TICKET_NUMBERS) {
                prefixes.add(prefix);
            }
        });
        return prefixes;
    }

    private static String ticketOf(String message, Set<String> prefixes) {
        if (prefixes.isEmpty()) {
            return "";
        }
        Matcher matcher = TICKET.matcher(message);
        while (matcher.find()) {
            if (prefixes.contains(matcher.group(1))) {
                return matcher.group(1) + "-" + matcher.group(2);
            }
        }
        return "";
    }

    private static TaskBuilder newTask(List<TaskBuilder> tasks) {
        TaskBuilder task = new TaskBuilder();
        tasks.add(task);
        return task;
    }

    static long daysBetween(String from, String to) {
        try {
            return Math.abs(ChronoUnit.DAYS.between(LocalDate.parse(from.substring(0, 10)), LocalDate.parse(to.substring(0, 10))));
        } catch (DateTimeParseException | StringIndexOutOfBoundsException e) {
            return Long.MAX_VALUE;
        }
    }

    private static boolean overlaps(Set<String> paths, Commit commit) {
        return commit.changes.stream().anyMatch(change -> paths.contains(change.path.toLowerCase()));
    }

    private static void addCommit(TaskBuilder builder, Commit commit, int author) {
        AiCostEstimatorData.Task task = builder.task;
        if (task.getCommits().isEmpty()) {
            task.setStart(commit.date);
        }
        builder.end = commit.date;
        task.setEnd(commit.date);
        task.getCommits().add(new AiCostEstimatorData.TaskCommit(commit.sha.length() > 10 ? commit.sha.substring(0, 10) : commit.sha, commit.date, author));
        for (Change change : commit.changes) {
            String lower = change.path.toLowerCase();
            builder.paths.add(lower);
            FileWork work = builder.work.get(lower);
            if (work == null) {
                work = new FileWork(change.path, change.kind);
                builder.work.put(lower, work);
            } else if (change.kind == DELETE) {
                // Deleted by the end of the task: deleting a file costs nothing.
                work.kind = DELETE;
            }
            work.added += change.added;
            work.deleted += change.deleted;
        }
    }

    /** Totals, sessions and type of a task from its per-file work. */
    private static void finish(TaskBuilder builder, Map<String, Integer> currentSize, Map<String, Integer> sizeProxy) {
        AiCostEstimatorData.Task task = builder.task;
        int[] session = null;
        List<int[]> sessions = new ArrayList<>();
        int files = 0;
        for (FileWork work : builder.work.values()) {
            if (work.kind == DELETE) {
                task.setDeletedFiles(task.getDeletedFiles() + 1);
                continue;
            }
            int churn = work.kind == NEW ? work.added : work.added + work.deleted;
            if (churn == 0) {
                continue;
            }
            files++;
            String lower = work.path.toLowerCase();
            int readLines = work.kind == NEW ? 0
                    : Math.min(MAX_READ_LINES, currentSize.getOrDefault(lower, sizeProxy.getOrDefault(lower, 0)));
            if (work.kind == NEW) {
                task.setNewLines(task.getNewLines() + work.added);
            } else {
                task.setEditAdded(task.getEditAdded() + work.added);
                task.setEditDeleted(task.getEditDeleted() + work.deleted);
            }
            // A change bigger than a session is split; every part re-reads the file.
            int parts = (churn + SESSION_MAX_CHURN - 1) / SESSION_MAX_CHURN;
            for (int i = 0; i < parts; i++) {
                int added = part(work.added, i, parts);
                int deleted = work.kind == NEW ? 0 : part(work.deleted, i, parts);
                int partChurn = added + deleted;
                if (session == null || session[0] + 1 > SESSION_MAX_FILES || session[2] + session[3] + session[4] + partChurn > SESSION_MAX_CHURN) {
                    session = new int[5];
                    sessions.add(session);
                }
                session[0]++;
                session[1] += readLines;
                if (work.kind == NEW) {
                    session[4] += added;
                } else {
                    session[2] += added;
                    session[3] += deleted;
                }
            }
        }
        // [files, readLines, editAdded, editDeleted, newLines]
        task.getSessions().addAll(sessions);
        task.setFiles(files);
        task.setType(typeOf(task));
    }

    private static int part(int total, int index, int parts) {
        return (int) ((long) total * (index + 1) / parts - (long) total * index / parts);
    }

    /** New code (mostly new files), refactoring (broad, deletes about as much as it adds), fix (small) or feature. */
    static String typeOf(AiCostEstimatorData.Task task) {
        int churn = task.churn();
        if (churn > 0 && task.getNewLines() >= 0.6 * churn) {
            return TYPE_NEW;
        }
        if (task.getFiles() >= 4 && task.getEditDeleted() >= 0.8 * (task.getEditAdded() + task.getNewLines())) {
            return TYPE_REFACTOR;
        }
        if (churn <= 60 && task.getFiles() <= 3) {
            return TYPE_FIX;
        }
        return TYPE_FEATURE;
    }
}
