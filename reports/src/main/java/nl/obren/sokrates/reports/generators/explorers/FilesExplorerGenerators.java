package nl.obren.sokrates.reports.generators.explorers;

import nl.obren.sokrates.common.renderingutils.ExplorerTemplate;
import nl.obren.sokrates.reports.utils.DataImageUtils;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.filehistory.FileModificationHistory;
import nl.obren.sokrates.sourcecode.landscape.analysis.FileExport;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class FilesExplorerGenerators {

    private File reportsFolder;

    public FilesExplorerGenerators(File reportsFolder) {
        this.reportsFolder = reportsFolder;
    }

    // Package-private for testing the git-history field population.
    List<FileExport> getFiles(NamedSourceCodeAspect aspect, String scope) {
        List<FileExport> files = new ArrayList<>();

        aspect.getSourceFiles().forEach(file -> {
            if (!files.contains(file) && !file.getRelativePath().startsWith("- -")) {
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
                }
                files.add(fileExport);
            }
        });

        return files;
    }

    public void exportJson(CodeAnalysisResults codeAnalysisResults) {
        try {
            List<FileExport> files = new ArrayList<>();

            files.addAll(getFiles(codeAnalysisResults.getMainAspectAnalysisResults().getAspect(), "main"));
            files.addAll(getFiles(codeAnalysisResults.getTestAspectAnalysisResults().getAspect(), "test"));
            files.addAll(getFiles(codeAnalysisResults.getGeneratedAspectAnalysisResults().getAspect(), "generated"));
            files.addAll(getFiles(codeAnalysisResults.getBuildAndDeployAspectAnalysisResults().getAspect(), "build"));
            files.addAll(getFiles(codeAnalysisResults.getOtherAspectAnalysisResults().getAspect(), "other"));

            ExplorerTemplate explorerTemplate = new ExplorerTemplate();

            List<String> fileLangs = files.stream().map(FileExport::getMainLang).collect(Collectors.toList());
            String fileLangIcons = DataImageUtils.getLangDataImageMapJson(fileLangs);
            String filesExplorer = explorerTemplate.render("files-explorer.html", files, fileLangIcons);
            File folder = new File(reportsFolder, "explorers");
            folder.mkdirs();
            FileUtils.write(new File(folder, "files-explorer.html"), filesExplorer, UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
