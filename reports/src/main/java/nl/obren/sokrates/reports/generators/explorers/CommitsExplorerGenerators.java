package nl.obren.sokrates.reports.generators.explorers;

import nl.obren.sokrates.common.renderingutils.ExplorerTemplate;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.FileHistoryAnalysisConfig;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.githistory.CoAuthor;
import nl.obren.sokrates.sourcecode.githistory.FileUpdate;
import nl.obren.sokrates.sourcecode.githistory.GitHistoryUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Generates the client-rendered commits explorer (commits-explorer.html) for an individual
 * repository report — the commit-level counterpart of {@link FilesExplorerGenerators} /
 * {@link UnitsExplorerGenerators}. No per-commit file list survives the analysis (the per-file and
 * per-commit pivots each drop the other side), so commits are reconstructed by re-reading
 * git-history.txt line-by-line via {@link GitHistoryUtils#parseLine} — deliberately NOT via
 * GitHistoryUtils.getHistoryFromFile, whose JVM-wide static cache is keyed on nothing and would
 * return another repository's history in a multi-repo process. parseLine applies the same
 * ignore/bots/anonymize/people-config rules as every other report, so counts reconcile.
 */
public class CommitsExplorerGenerators {
    /**
     * Newest commits kept in the embedded payload (matches the MAX_EXPORT_LIST_SIZE precedent);
     * the page shows "newest N of M" when the history is longer.
     */
    static final int MAX_COMMITS = 10000;

    private File reportsFolder;

    public CommitsExplorerGenerators(File reportsFolder) {
        this.reportsFolder = reportsFolder;
    }

    public void exportJson(CodeAnalysisResults codeAnalysisResults, File sokratesConfigFolder) {
        try {
            List<CommitFileExport> currentFiles = collectCurrentFiles(codeAnalysisResults);
            List<FileUpdate> fileUpdates = readFileUpdates(codeAnalysisResults, sokratesConfigFolder);
            Map<String, String> messagesBySha = readCommitMessages(codeAnalysisResults, sokratesConfigFolder);
            Map<String, List<CoAuthor>> coAuthorsBySha = readCoAuthors(codeAnalysisResults, sokratesConfigFolder);

            CommitsExplorerData data = buildData(currentFiles, fileUpdates, messagesBySha, coAuthorsBySha, MAX_COMMITS);

            String commitsExplorer = new ExplorerTemplate().render("commits-explorer.html", data);
            File folder = new File(reportsFolder, "explorers");
            folder.mkdirs();
            FileUtils.write(new File(folder, "commits-explorer.html"), commitsExplorer, UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * All current files across the five scopes (same scope keys as the files explorer), deduped by
     * source file. These form the base of the explorer's circle-packing visualization.
     */
    private List<CommitFileExport> collectCurrentFiles(CodeAnalysisResults results) {
        List<CommitFileExport> files = new ArrayList<>();
        Set<SourceFile> seen = new HashSet<>();
        collectAspectFiles(files, seen, results.getMainAspectAnalysisResults().getAspect(), "main");
        collectAspectFiles(files, seen, results.getTestAspectAnalysisResults().getAspect(), "test");
        collectAspectFiles(files, seen, results.getGeneratedAspectAnalysisResults().getAspect(), "generated");
        collectAspectFiles(files, seen, results.getBuildAndDeployAspectAnalysisResults().getAspect(), "build");
        collectAspectFiles(files, seen, results.getOtherAspectAnalysisResults().getAspect(), "other");
        return files;
    }

    private void collectAspectFiles(List<CommitFileExport> files, Set<SourceFile> seen, NamedSourceCodeAspect aspect, String scope) {
        aspect.getSourceFiles().forEach(file -> {
            if (!file.getRelativePath().startsWith("- -") && seen.add(file)) {
                files.add(new CommitFileExport(file.getRelativePath(), scope, file.getLinesOfCode()));
            }
        });
    }

    private List<FileUpdate> readFileUpdates(CodeAnalysisResults results, File sokratesConfigFolder) {
        List<FileUpdate> updates = new ArrayList<>();
        if (sokratesConfigFolder == null) {
            return updates;
        }
        FileHistoryAnalysisConfig historyConfig = results.getCodeConfiguration().getFileHistoryAnalysis();
        File historyFile = historyConfig.getFilesHistoryFile(sokratesConfigFolder);
        if (!historyFile.exists()) {
            return updates;
        }
        List<String> lines;
        try {
            lines = FileUtils.readLines(historyFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return updates;
        }
        lines.forEach(line -> {
            FileUpdate fileUpdate = GitHistoryUtils.parseLine(line, historyConfig);
            if (fileUpdate != null) {
                updates.add(fileUpdate);
            }
        });
        return updates;
    }

    /**
     * The first-message-line map from the optional git-commits.txt sidecar next to
     * git-history.txt; empty (message-less explorer) for histories extracted by older versions.
     */
    private Map<String, String> readCommitMessages(CodeAnalysisResults results, File sokratesConfigFolder) {
        if (sokratesConfigFolder == null) {
            return new HashMap<>();
        }
        File historyFile = results.getCodeConfiguration().getFileHistoryAnalysis().getFilesHistoryFile(sokratesConfigFolder);
        return GitHistoryUtils.getCommitMessagesFromFile(
                new File(historyFile.getParentFile(), GitHistoryUtils.GIT_COMMITS_FILE_NAME));
    }

    /**
     * The co-authors per sha resolved from the optional git-commit-trailers.txt sidecar next to
     * git-history.txt through fileHistoryAnalysis.coAuthors; empty for older extractions.
     */
    private Map<String, List<CoAuthor>> readCoAuthors(CodeAnalysisResults results, File sokratesConfigFolder) {
        if (sokratesConfigFolder == null) {
            return new HashMap<>();
        }
        FileHistoryAnalysisConfig historyConfig = results.getCodeConfiguration().getFileHistoryAnalysis();
        return GitHistoryUtils.getCoAuthorsBySha(historyConfig.getFilesHistoryFile(sokratesConfigFolder), historyConfig);
    }

    static CommitsExplorerData buildData(List<CommitFileExport> currentFiles, List<FileUpdate> fileUpdates,
                                         Map<String, String> messagesBySha, int maxCommits) {
        return buildData(currentFiles, fileUpdates, messagesBySha, new HashMap<>(), maxCommits);
    }

    /**
     * Groups file updates by commit sha, sorts commits newest first, caps them, and resolves each
     * commit's paths against the current files (unmatched paths become "deleted" file entries,
     * added only when a kept commit references them). Package-private for testing.
     */
    static CommitsExplorerData buildData(List<CommitFileExport> currentFiles, List<FileUpdate> fileUpdates,
                                         Map<String, String> messagesBySha, Map<String, List<CoAuthor>> coAuthorsBySha,
                                         int maxCommits) {
        // Size proxy for files no longer in the codebase (their LOC is unknowable): the largest
        // single-commit lines-added/deleted seen for the path — the deletion commit removes the
        // whole file, so its linesDeleted is roughly the file's final size. Keeps the deleted
        // files visible at a plausible scale in the circle packing instead of size-1 specks.
        Map<String, Integer> sizeProxyByPath = new HashMap<>();

        // Group by sha, preserving first-seen order (git log order: newest first within a date).
        Map<String, CommitExport> commitsBySha = new LinkedHashMap<>();
        Map<String, Set<String>> pathsBySha = new LinkedHashMap<>();
        fileUpdates.forEach(update -> {
            sizeProxyByPath.merge(update.getPath().toLowerCase(),
                    Math.max(update.getLinesAdded(), update.getLinesDeleted()), Math::max);
            String sha = update.getCommitId();
            CommitExport commit = commitsBySha.get(sha);
            if (commit == null) {
                commit = new CommitExport();
                commit.setSha(sha.length() > 10 ? sha.substring(0, 10) : sha);
                commit.setDate(update.getDate());
                commit.setEmail(update.getAuthorEmail());
                commit.setUserName(update.getUserName());
                commit.setBot(update.isBot());
                commit.setMessage(messagesBySha.getOrDefault(sha, ""));
                List<CoAuthor> coAuthors = coAuthorsBySha.get(sha);
                if (coAuthors != null) {
                    for (CoAuthor coAuthor : coAuthors) {
                        commit.getCoAuthors().add(new CoAuthorExport(coAuthor));
                    }
                }
                commitsBySha.put(sha, commit);
                pathsBySha.put(sha, new LinkedHashSet<>());
            }
            commit.setLinesAdded(commit.getLinesAdded() + update.getLinesAdded());
            commit.setLinesDeleted(commit.getLinesDeleted() + update.getLinesDeleted());
            pathsBySha.get(sha).add(update.getPath());
        });

        List<String> shas = new ArrayList<>(commitsBySha.keySet());
        // Stable sort by date descending: within the same date the file's git-log order (newest
        // first) is preserved, which is the best intra-day ordering available (no time of day).
        shas.sort(Comparator.comparing((String sha) -> commitsBySha.get(sha).getDate()).reversed());

        CommitsExplorerData data = new CommitsExplorerData();
        data.setTotalCommitsCount(shas.size());
        if (shas.size() > maxCommits) {
            shas = shas.subList(0, maxCommits);
        }

        // File index: current files first; history-only paths are appended lazily so only paths
        // referenced by a kept commit end up in the payload. Case-insensitive match, mirroring
        // FileHistoryAnalyzer's path lookup.
        List<CommitFileExport> files = new ArrayList<>(currentFiles);
        Map<String, Integer> fileIdsByPath = new HashMap<>();
        for (int i = 0; i < files.size(); i++) {
            fileIdsByPath.putIfAbsent(files.get(i).getPath().toLowerCase(), i);
        }

        for (String sha : shas) {
            CommitExport commit = commitsBySha.get(sha);
            for (String path : pathsBySha.get(sha)) {
                Integer fileId = fileIdsByPath.get(path.toLowerCase());
                if (fileId == null) {
                    fileId = files.size();
                    int sizeProxy = sizeProxyByPath.getOrDefault(path.toLowerCase(), 0);
                    files.add(new CommitFileExport(path, CommitFileExport.SCOPE_DELETED, sizeProxy));
                    fileIdsByPath.put(path.toLowerCase(), fileId);
                }
                commit.getFileIds().add(fileId);
            }
            data.getCommits().add(commit);
        }
        data.setFiles(files);

        return data;
    }
}
