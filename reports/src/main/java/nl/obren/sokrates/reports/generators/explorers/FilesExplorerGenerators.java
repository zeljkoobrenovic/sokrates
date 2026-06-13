package nl.obren.sokrates.reports.generators.explorers;

import nl.obren.sokrates.common.renderingutils.ExplorerTemplate;
import nl.obren.sokrates.reports.utils.DataImageUtils;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.filehistory.FileModificationHistory;
import nl.obren.sokrates.sourcecode.landscape.analysis.FileExport;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class FilesExplorerGenerators {

    private File reportsFolder;

    public FilesExplorerGenerators(File reportsFolder) {
        this.reportsFolder = reportsFolder;
    }

    // Package-private for testing the git-history field population.
    List<FileExport> getFiles(NamedSourceCodeAspect aspect, String scope) {
        return getFiles(aspect, scope, scope, new HashSet<>());
    }

    // Package-private for testing the source-file viewer link.
    List<FileExport> getFiles(NamedSourceCodeAspect aspect, String scope, Set<SourceFile> referencedFiles) {
        return getFiles(aspect, scope, scope, referencedFiles);
    }

    /**
     * Builds file exports for an aspect. {@code cacheFolder} is the source-cache subfolder under
     * {@code src/} for this scope (e.g. "main", "buildAndDeployment"); {@code referencedFiles} are the
     * files whose source page was cached — only those get a {@code sourceFileLink}.
     */
    private List<FileExport> getFiles(NamedSourceCodeAspect aspect, String scope, String cacheFolder, Set<SourceFile> referencedFiles) {
        List<FileExport> files = new ArrayList<>();
        // Dedup by source file. The previous `files.contains(file)` compared the FileExport list
        // against a SourceFile, so it was always false (a no-op); use a SourceFile set so a repeated
        // file in the aspect is exported once, and the check is O(1).
        Set<SourceFile> seen = new HashSet<>();

        aspect.getSourceFiles().forEach(file -> {
            if (!file.getRelativePath().startsWith("- -") && seen.add(file)) {
                FileExport fileExport = new FileExport("", file.getRelativePath(), scope, file.getLinesOfCode());
                // Populate git-history columns when file history was analysed; otherwise leave the
                // defaults (0 commits, blank date).
                FileModificationHistory history = file.getFileModificationHistory();
                if (history != null && history.getDates() != null && !history.getDates().isEmpty()) {
                    List<String> dates = history.getDates();
                    fileExport.setCommitsCount(dates.size());
                    fileExport.setRecentCommitsCount30Days((int) dates.stream()
                            .filter(date -> DateUtils.isCommittedLessThanDaysAgo(date, 30)).count());
                    fileExport.setRecentCommitsCount90Days((int) dates.stream()
                            .filter(date -> DateUtils.isCommittedLessThanDaysAgo(date, 90)).count());
                    fileExport.setLatestCommitDate(history.getLatestDate());
                    fileExport.setAgeDays(history.daysSinceFirstUpdate());
                    fileExport.setFreshnessDays(history.daysSinceLatestUpdate());
                    fileExport.setContributorsCount(history.countContributors());
                }
                // Link to the cached source page only for files that actually have one.
                if (referencedFiles.contains(file)) {
                    fileExport.setSourceFileLink("../src/viewer.html#aspect=" + cacheFolder + "&file=" + file.getRelativePath());
                }
                files.add(fileExport);
            }
        });

        return files;
    }

    /**
     * The files whose source is cached into {@code src/<aspect>.zip}, mirroring
     * {@code DataExporter.getReferencedFiles} (only a subset of files is cached).
     */
    private Set<SourceFile> getReferencedFiles(CodeAnalysisResults results) {
        Set<SourceFile> referenced = new HashSet<>();
        referenced.addAll(results.getFilesAnalysisResults().getLongestFiles());
        referenced.addAll(results.getFilesAnalysisResults().getFilesWithMostUnits());
        referenced.addAll(results.getFilesHistoryAnalysisResults().getFilesWithLeastContributors());
        referenced.addAll(results.getFilesHistoryAnalysisResults().getFilesWithMostContributors());
        referenced.addAll(results.getFilesHistoryAnalysisResults().getMostChangedFiles());
        referenced.addAll(results.getFilesHistoryAnalysisResults().getOldestFiles());
        referenced.addAll(results.getFilesHistoryAnalysisResults().getMostPreviouslyChangedFiles());
        referenced.addAll(results.getFilesHistoryAnalysisResults().getMostRecentlyChangedFiles());
        referenced.addAll(results.getFilesHistoryAnalysisResults().getYoungestFiles());
        results.getDuplicationAnalysisResults().getLongestDuplicates().forEach(d ->
                d.getDuplicatedFileBlocks().forEach(b -> referenced.add(b.getSourceFile())));
        return referenced;
    }

    public void exportJson(CodeAnalysisResults codeAnalysisResults) {
        try {
            boolean saveSourceFiles = codeAnalysisResults.getCodeConfiguration().getAnalysis().isSaveSourceFiles();
            Set<SourceFile> referenced = saveSourceFiles ? getReferencedFiles(codeAnalysisResults) : new HashSet<>();

            List<FileExport> files = new ArrayList<>();

            // scope key (explorer/template) -> source-cache subfolder under src/.
            files.addAll(getFiles(codeAnalysisResults.getMainAspectAnalysisResults().getAspect(), "main", "main", referenced));
            files.addAll(getFiles(codeAnalysisResults.getTestAspectAnalysisResults().getAspect(), "test", "test", referenced));
            files.addAll(getFiles(codeAnalysisResults.getGeneratedAspectAnalysisResults().getAspect(), "generated", "generated", referenced));
            files.addAll(getFiles(codeAnalysisResults.getBuildAndDeployAspectAnalysisResults().getAspect(), "build", "buildAndDeployment", referenced));
            files.addAll(getFiles(codeAnalysisResults.getOtherAspectAnalysisResults().getAspect(), "other", "other", referenced));

            ExplorerTemplate explorerTemplate = new ExplorerTemplate();

            List<String> fileLangs = files.stream().map(FileExport::getMainLang).collect(Collectors.toList());
            String fileLangIcons = DataImageUtils.getLangDataImageMapJson(fileLangs);

            nl.obren.sokrates.sourcecode.core.AnalysisConfig analysis =
                    codeAnalysisResults.getCodeConfiguration().getAnalysis();
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("langIcons", fileLangIcons);
            placeholders.put("fileSizeThresholds", FilesExportUtils.thresholdsJson(analysis.getFileSizeThresholds()));
            placeholders.put("fileAgeThresholds", FilesExportUtils.thresholdsJson(analysis.getFileAgeThresholds()));
            placeholders.put("fileUpdateFrequencyThresholds", FilesExportUtils.thresholdsJson(analysis.getFileUpdateFrequencyThresholds()));
            placeholders.put("fileContributorsCountThresholds", FilesExportUtils.thresholdsJson(analysis.getFileContributorsCountThresholds()));
            // From explorers/ the per-repository HTML reports live in ../html/.
            placeholders.put("reportLinkBase", "../html/");
            placeholders.put("saveSourceFiles", Boolean.toString(saveSourceFiles));

            String filesExplorer = explorerTemplate.render("files-explorer.html", files, placeholders);
            File folder = new File(reportsFolder, "explorers");
            folder.mkdirs();
            FileUtils.write(new File(folder, "files-explorer.html"), filesExplorer, UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
