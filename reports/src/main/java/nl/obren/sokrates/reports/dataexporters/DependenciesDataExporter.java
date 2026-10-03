package nl.obren.sokrates.reports.dataexporters;

import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FilesHistoryAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.TemporalDependenciesWindow;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.filehistory.FilePairChangedTogether;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.File;
import java.io.IOException;
import java.util.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.function.Consumer;

/**
 * The dependency text exports of data/text/: the logical component dependencies (all, per decomposition, per pair) and
 * the temporal file-pair dependencies per window. Moved out of {@link DataExporter}.
 */
class DependenciesDataExporter {
    private static final Log LOG = LogFactory.getLog(DependenciesDataExporter.class);
    private final CodeAnalysisResults analysisResults;
    private final File textDataFolder;
    private final Consumer<String> info;

    DependenciesDataExporter(CodeAnalysisResults analysisResults, File textDataFolder, Consumer<String> info) {
        this.analysisResults = analysisResults;
        this.textDataFolder = textDataFolder;
        this.info = info;
    }

    void exportDependencies(CodeAnalysisResults analysisResults) {
        exportDependencies("", "", "");
        analysisResults.getLogicalDecompositionsAnalysisResults().forEach(logicalDecompositionAnalysisResults -> {
            logicalDecompositionAnalysisResults.getComponentDependencies().forEach(componentDependency -> {
                exportDependencies(logicalDecompositionAnalysisResults.getKey(), componentDependency.getFromComponent(), componentDependency.getToComponent());
            });
        });
    }

    // Exports every co-change window the analyzer computed, and only those: a window that was not
    // analyzed (because the configured depth does not reach it, or because there is no commit
    // history at all) gets no file, so its absence cannot be misread as "analyzed, nothing found" -
    // which a header-only file is indistinguishable from.
    void saveTemporalDependencies(CodeAnalysisResults analysisResults) {
        saveTemporalDependencies(analysisResults, textDataFolder);
    }

    // Package-private so the set of exported windows can be asserted without a full analysis run.
    void saveTemporalDependencies(CodeAnalysisResults analysisResults, File targetFolder) {
        FilesHistoryAnalysisResults historyResults = analysisResults.getFilesHistoryAnalysisResults();
        int maxDays = analysisResults.getCodeConfiguration().getAnalysis().getMaxTemporalDependenciesDepthDays();
        List<TemporalDependenciesWindow> analyzed = TemporalDependenciesWindow.analyzedWindows(historyResults, maxDays);

        Arrays.stream(TemporalDependenciesWindow.values())
                .filter(window -> !analyzed.contains(window))
                .forEach(window -> LOG.info("Not exporting " + window.getDataFileName()
                        + ": co-change window not analyzed ("
                        + (historyResults.hasHistory()
                        ? "maxTemporalDependenciesDepthDays=" + maxDays
                        : "no commit history") + ")"));

        analyzed.forEach(window -> {
            List<FilePairChangedTogether> filePairs = window.getFilePairs(historyResults);
            exportFilesChangedTogether(filePairs, window.getDataFileName(), targetFolder);
            exportFilesChangedTogether(historyResults.getFilePairsChangedTogetherInDifferentFolders(filePairs),
                    window.getDifferentFoldersDataFileName(), targetFolder);
        });
    }

    private void exportFilesChangedTogether(List<FilePairChangedTogether> filePairsChangedTogether, String fileName, File targetFolder) {
        StringBuilder content = new StringBuilder();
        content.append("file 1\tfile 2\t# same commits\t# commits file 1\t# commits file 2\n");
        if (filePairsChangedTogether.size() > 0) {
            filePairsChangedTogether.sort((a, b) -> b.getCommits().size() - a.getCommits().size());

            int limit = Math.min(10000, filePairsChangedTogether.size());
            List<FilePairChangedTogether> limitedList = filePairsChangedTogether.subList(0, limit);

            limitedList.forEach(pair -> {
                content.append(pair.getSourceFile1().getRelativePath()).append("\t");
                content.append(pair.getSourceFile2().getRelativePath()).append("\t");
                content.append(pair.getCommits().size()).append("\t");
                content.append(pair.getCommitsCountFile1()).append("\t");
                content.append(pair.getCommitsCountFile2()).append("\n");
            });
        }
        try {
            FileUtils.write(new File(targetFolder, fileName), content.toString(), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exportDependencies(String filterLogicalDecomposition, String filterFrom, String filterTo) {
        analysisResults.getLogicalDecompositionsAnalysisResults().forEach(logicalDecomposition -> {
            String logicalDecompositionName = logicalDecomposition.getKey();
            if (shouldProcessLogicalDecomposition(filterLogicalDecomposition, logicalDecompositionName)) {
                StringBuilder content = new StringBuilder();
                String fileNamePrefix = DataExporter.dependenciesFileNamePrefix(filterFrom, filterTo, logicalDecompositionName);
                logicalDecomposition.getComponentDependencies().forEach(dependency -> {
                    content.append(appendDependency(filterFrom, filterTo, dependency));
                });
                try {
                    FileUtils.write(new File(textDataFolder, fileNamePrefix + ".txt"), content.toString(), UTF_8);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private String appendDependency(String filterFrom, String filterTo, ComponentDependency dependency) {
        StringBuilder content = new StringBuilder();

        String from = dependency.getFromComponent();
        String to = dependency.getToComponent();
        if (shouldAppendDependency(filterFrom, filterTo, from, to)) {
            dependency.getEvidence().forEach(evidence -> {
                content.append("from: " + from);
                content.append("\n");
                content.append("to: " + to);
                content.append("\nevidence:\n");
                content.append(" - file: \"");
                content.append(evidence.getPathFrom());
                content.append("\"\n");
                content.append("   contains \"");
                content.append(evidence.getEvidence());
                content.append("\"\n\n");
            });
        }

        return content.toString();
    }

    private boolean shouldProcessLogicalDecomposition(String filterLogicalDecomposition, String logicalDecompositionName) {
        return StringUtils.isBlank(filterLogicalDecomposition) || logicalDecompositionName.equalsIgnoreCase(filterLogicalDecomposition);
    }

    private boolean shouldAppendDependency(String filterFrom, String filterTo, String fromComponent, String toComponent) {
        return StringUtils.isBlank(filterFrom) || StringUtils.isBlank(filterTo) || (fromComponent.equalsIgnoreCase(filterFrom) && toComponent.equalsIgnoreCase(filterTo));
    }
}
