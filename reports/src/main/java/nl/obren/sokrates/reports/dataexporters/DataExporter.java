/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.dataexporters;

import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.common.utils.SystemUtils;
import nl.obren.sokrates.reports.dataexporters.dependencies.DependenciesExporter;
import nl.obren.sokrates.reports.dataexporters.duplication.DuplicateFileBlockExportInfo;
import nl.obren.sokrates.reports.dataexporters.duplication.DuplicationExportInfo;
import nl.obren.sokrates.reports.dataexporters.duplication.DuplicationExporter;
import nl.obren.sokrates.reports.dataexporters.files.FileListExporter;
import nl.obren.sokrates.reports.dataexporters.trends.MetricsTrendExporter;
import nl.obren.sokrates.reports.dataexporters.units.UnitListExporter;
import nl.obren.sokrates.reports.generators.explorers.DependenciesExplorerGenerator;
import nl.obren.sokrates.reports.generators.explorers.DuplicationExplorerGenerator;
import nl.obren.sokrates.reports.generators.explorers.FilesExplorerGenerator;
import nl.obren.sokrates.reports.generators.explorers.UnitsExplorerGenerator;
import nl.obren.sokrates.reports.utils.HtmlTemplateUtils;
import nl.obren.sokrates.reports.utils.ZipUtils;
import nl.obren.sokrates.sourcecode.IgnoredFilesGroup;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.SourceFileWithSearchData;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.DuplicationAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.UnitsAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.duplication.DuplicatedFileBlock;
import nl.obren.sokrates.sourcecode.duplication.DuplicationInstance;
import nl.obren.sokrates.sourcecode.filehistory.FileHistoryScopingUtils;
import nl.obren.sokrates.sourcecode.filehistory.FileModificationHistory;
import nl.obren.sokrates.sourcecode.filehistory.FilePairChangedTogether;
import nl.obren.sokrates.sourcecode.lang.DefaultLanguageAnalyzer;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzerFactory;
import nl.obren.sokrates.sourcecode.search.FoundLine;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.StringEscapeUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class DataExporter {
    public static final String INTERACTIVE_HTML_FOLDER_NAME = "explorers";
    public static final String SRC_CACHE_FOLDER_NAME = "src";
    public static final String DATA_FOLDER_NAME = "data";
    public static final String HISTORY_FOLDER_NAME = "history";
    public static final String SEPARATOR = "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -\n";
    public static final String FOUND_TEXT_PER_FILE_SIFFIX = "_found_text_per_file";
    public static final String FOUND_TEXT_SUFFIX = "_found_text";
    private static final Log LOG = LogFactory.getLog(DataExporter.class);
    private ProgressFeedback progressFeedback;
    private File sokratesConfigFile;
    private CodeConfiguration codeConfiguration;
    private File reportsFolder;
    private CodeAnalysisResults analysisResults;
    private File dataFolder;
    private File historyFolder;
    private File codeCacheFolder;
    private File textDataFolder;
    private File extraAnalysisDataFolder;

    public DataExporter(ProgressFeedback progressFeedback) {
        this.progressFeedback = progressFeedback;
    }

    public static String dependenciesFileNamePrefix(String fromComponent, String toComponent, String logicalDecompositionName) {
        String fileNamePrefix = "dependencies_" + SystemUtils.getFileSystemFriendlyName(logicalDecompositionName);
        if (StringUtils.isNotBlank(fromComponent) && StringUtils.isNotBlank(toComponent)) {
            fileNamePrefix += "_" + SystemUtils.getFileSystemFriendlyName(fromComponent + "_" + toComponent);
        }
        return fileNamePrefix;
    }

    public void saveData(File sokratesConfigFile, CodeConfiguration codeConfiguration, File reportsFolder, CodeAnalysisResults analysisResults) throws IOException {
        this.sokratesConfigFile = sokratesConfigFile;
        this.codeConfiguration = codeConfiguration;
        this.reportsFolder = reportsFolder;
        this.analysisResults = analysisResults;
        this.dataFolder = getDataFolder();
        this.textDataFolder = getTextDataFolder();
        this.extraAnalysisDataFolder = getExtraAnalysisDataFolder();
        this.historyFolder = getDataHistoryFolder();

        exportFileLists();
        exportMetrics();
        exportTrends();
        exportControls();
        exportContributors();
        exportJson();
        exportDuplicates();
        exportUnits();
        exportInteractiveExplorers();
        exportSourceFile();
        exportDependencies(analysisResults);
        saveTemporalDependencies(analysisResults);
    }

    private void exportTrends() {
        MetricsTrendExporter exporter = new MetricsTrendExporter(sokratesConfigFile, analysisResults);

        try {
            FileUtils.write(new File(textDataFolder, "metrics_trend.txt"), exporter.getText(), UTF_8);
            FileUtils.write(new File(textDataFolder, "metrics_trend_loc_per_extension.txt"), exporter.getText("LINES_OF_CODE_MAIN_.*"), UTF_8);
            FileUtils.write(new File(textDataFolder, "metrics_trend_loc_duplication.txt"), exporter.getText("(DUPLICATION_NUMBER_OF_CLEANED_LINES|DUPLICATION_NUMBER_OF_DUPLICATED_LINES)"), UTF_8);
            FileUtils.write(new File(textDataFolder, "metrics_trend_unit_size_loc.txt"), exporter.getText("UNIT_SIZE_DISTRIBUTION_.*_LOC"), UTF_8);
            FileUtils.write(new File(textDataFolder, "metrics_trend_conditional_complexity_loc.txt"), exporter.getText("CONDITIONAL_COMPLEXITY_DISTRIBUTION_.*_LOC"), UTF_8);
            FileUtils.write(new File(textDataFolder, "metrics_trend_loc_logical_decompositions.txt"), exporter.getText("LINES_OF_CODE_DECOMPOSITION_.*", ".*_EXT_.*"), UTF_8);
            FileUtils.write(new File(textDataFolder, "metrics_trend_loc_file_size.txt"), exporter.getText("LINES_OF_CODE_FILE_SIZE_.*", ".*_EXT_.*"), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exportMetrics() {
        StringBuilder content = new StringBuilder();

        analysisResults.getMetricsList().getMetrics().forEach(metric -> {
            content.append(metric.getId());
            content.append(": ");
            content.append(metric.getValue());
            content.append("\n");
        });
        try {
            FileUtils.write(new File(textDataFolder, "metrics.txt"), content.toString(), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exportControls() {
        StringBuilder content = new StringBuilder();

        analysisResults.getControlResults().getGoalsAnalysisResults().forEach(goalsAnalysisResults -> {
            goalsAnalysisResults.getControlStatuses().forEach(status -> {
                content.append("goal: " + goalsAnalysisResults.getMetricsWithGoal().getGoal() + "\n");
                content.append("control metric: " + status.getMetric().getId() + "\n");
                content.append("status: " + status.getStatus() + "\n");
                content.append("desired range: " + status.getControl().getDesiredRange().getTextDescription() + "\n");
                content.append("value: " + status.getMetric().getValue() + "\n");
                content.append("description: " + status.getControl().getDescription() + "\n");
                content.append("\n");
            });
        });
        try {
            FileUtils.write(new File(textDataFolder, "controls.txt"), content.toString(), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void exportContributors() {
        StringBuilder content = new StringBuilder();

        List<Contributor> contributors = analysisResults.getContributorsAnalysisResults().getContributors();
        int total = contributors.stream().mapToInt(c -> c.getCommitsCount()).sum();

        content.append("Contributor\t#commits\t#commits (30 days)\t#commits (90 days)\tfirst commit\tlast commit\n");

        contributors.forEach(contributor -> {
            content.append(contributor.getEmail() + "\t");
            content.append(contributor.getCommitsCount() + "\t");
            content.append(contributor.getCommitsCount30Days() + "\t");
            content.append(contributor.getCommitsCount90Days() + "\t");
            content.append(contributor.getFirstCommitDate() + "\t");
            content.append(contributor.getLatestCommitDate() + "\t");
            double percentage = 100.0 * contributor.getCommitsCount() / total;
            content.append(FormattingUtils.getFormattedPercentage(percentage) + "%\n");
        });
        try {
            FileUtils.write(new File(textDataFolder, "contributors.txt"), content.toString(), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exportDuplicates() {
        DuplicationExportInfo duplicationExportInfo = new DuplicationExporter(analysisResults.getDuplicationAnalysisResults().getAllDuplicates()).getDuplicationExportInfo();

        StringBuilder content = new StringBuilder();

        int id[] = {1};
        duplicationExportInfo.getDuplicates().forEach(duplicate -> {
            List<DuplicateFileBlockExportInfo> duplicatedFileBlocks = duplicate.getDuplicatedFileBlocks();
            content.append("duplicated block id: " + id[0] + "\n");
            content.append("size: " + duplicate.getBlockSize() + " cleaned lines of code\n");
            content.append("in " + duplicatedFileBlocks.size() + " files:\n");
            duplicatedFileBlocks.forEach(duplicateFileBlock -> {
                content.append(" - " + duplicateFileBlock.getFile().getRelativePath());
                content.append(" (" + duplicateFileBlock.getStartLine() + ":" + duplicateFileBlock.getEndLine() + ")\n");
            });

            content.append("\n");

            id[0]++;
        });
        try {
            FileUtils.write(new File(textDataFolder, "duplicates.txt"), content.toString(), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exportUnits() {
        UnitListExporter units = new UnitListExporter(analysisResults.getUnitsAnalysisResults().getAllUnits());
        int id[] = {1};
        StringBuilder content = new StringBuilder();
        units.getAllUnitsData().forEach(unit -> {
            content.append("id: " + id[0] + "\n");
            content.append("unit: " + unit.getShortName() + "\n");
            content.append("file: " + unit.getRelativeFileName() + "\n");
            content.append("start line: " + unit.getStartLine() + "\n");
            content.append("end line: " + unit.getEndLine() + "\n");
            content.append("size: " + unit.getLinesOfCode() + " LOC\n");
            content.append("McCabe index: " + unit.getMcCabeIndex() + "\n");
            content.append("number of parameters: " + unit.getNumberOfParameters() + "\n");
            content.append("\n");

            id[0]++;
        });
        try {
            FileUtils.write(new File(textDataFolder, "units.txt"), content.toString(), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exportDependencies(CodeAnalysisResults analysisResults) {
        exportDependencies("", "", "");
        analysisResults.getLogicalDecompositionsAnalysisResults().forEach(logicalDecompositionAnalysisResults -> {
            logicalDecompositionAnalysisResults.getComponentDependencies().forEach(componentDependency -> {
                exportDependencies(logicalDecompositionAnalysisResults.getKey(), componentDependency.getFromComponent(), componentDependency.getToComponent());
            });
        });
    }

    private void saveTemporalDependencies(CodeAnalysisResults analysisResults) {
        List<FilePairChangedTogether> filePairsChangedTogether = analysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether();
        exportFilesChangedTogether(filePairsChangedTogether,
                "temporal_dependencies.txt");
        exportFilesChangedTogether(analysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogetherInDifferentFolders(filePairsChangedTogether),
                "temporal_dependencies_different_folders.txt");
        List<FilePairChangedTogether> filePairsChangedTogether30Days = analysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogether30Days();
        exportFilesChangedTogether(filePairsChangedTogether30Days,
                "temporal_dependencies_30_days.txt");
        exportFilesChangedTogether(analysisResults.getFilesHistoryAnalysisResults().getFilePairsChangedTogetherInDifferentFolders(filePairsChangedTogether30Days),
                "temporal_dependencies_different_folders_30_days.txt");
    }

    private void exportFilesChangedTogether(List<FilePairChangedTogether> filePairsChangedTogether, String fileName) {
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
            FileUtils.write(new File(textDataFolder, fileName), content.toString(), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exportFileLists() {
        saveExcludedByExtensionFiles();
        saveExplicitlyIgnoredFiles();
        saveSourceCodeAspect(analysisResults.getMainAspectAnalysisResults().getAspect(), "");
        saveSourceCodeAspect(analysisResults.getTestAspectAnalysisResults().getAspect(), "");
        saveSourceCodeAspect(analysisResults.getGeneratedAspectAnalysisResults().getAspect(), "");
        saveSourceCodeAspect(analysisResults.getBuildAndDeployAspectAnalysisResults().getAspect(), "");
        saveSourceCodeAspect(analysisResults.getOtherAspectAnalysisResults().getAspect(), "");

        analysisResults.getLogicalDecompositionsAnalysisResults().forEach(logicalDecomposition -> {
            logicalDecomposition.getComponents().forEach(component -> {
                saveSourceCodeAspect(component.getAspect(), DataExportUtils.getComponentFilePrefix(logicalDecomposition.getKey()));
            });
        });

        analysisResults.getConcernsAnalysisResults().forEach(group -> {
            group.getConcerns().forEach(concern -> {
                saveSourceCodeAspect(concern.getAspect(), DataExportUtils.getConcernFilePrefix(group.getKey()));
                saveFoundText(concern, DataExportUtils.getConcernFilePrefix(group.getKey()));
                saveFoundTextPerFile(concern, DataExportUtils.getConcernFilePrefix(group.getKey()));
            });
        });
    }

    private void saveExcludedByExtensionFiles() {
        StringBuilder content = new StringBuilder();

        Map<String, List<SourceFile>> extensionsMap = new HashMap<>();

        analysisResults.getFilesExcludedByExtension().forEach(sourceFile -> {
            String extension = FilenameUtils.getExtension(sourceFile.getRelativePath());
            List<SourceFile> files = extensionsMap.get(extension);
            if (files == null) {
                files = new ArrayList<>();
                extensionsMap.put(extension, files);
            }
            files.add(sourceFile);
        });

        List<String> extensions = new ArrayList<>(extensionsMap.keySet());
        Collections.sort(extensions, (o1, o2) -> extensionsMap.get(o2).size() - extensionsMap.get(o1).size());

        extensions.forEach(extension -> {
            List<SourceFile> sourceFiles = extensionsMap.get(extension);
            content.append(SEPARATOR);
            content.append("*." + extension + " files (" + sourceFiles.size() + ")");
            content.append(":\n\n");
            sourceFiles.forEach(sourceFile -> {
                content.append(sourceFile.getRelativePath());
                content.append("\n");
            });
            content.append(SEPARATOR);
            content.append("\n\n\n");
        });

        try {
            FileUtils.write(new File(textDataFolder, "excluded_files_ignored_extensions.txt"), content.toString(), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exportDependencies(String filterLogicalDecomposition, String filterFrom, String filterTo) {
        analysisResults.getLogicalDecompositionsAnalysisResults().forEach(logicalDecomposition -> {
            String logicalDecompositionName = logicalDecomposition.getKey();
            if (shouldProcessLogicalDecomposition(filterLogicalDecomposition, logicalDecompositionName)) {
                StringBuilder content = new StringBuilder();
                String fileNamePrefix = dependenciesFileNamePrefix(filterFrom, filterTo, logicalDecompositionName);
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

    private void saveExplicitlyIgnoredFiles() {
        StringBuilder content = new StringBuilder();

        Map<String, IgnoredFilesGroup> ignoredFilesGroups = analysisResults.getIgnoredFilesGroups();
        List<String> keys = new ArrayList<>(ignoredFilesGroups.keySet());
        Collections.sort(keys, (o1, o2) -> ignoredFilesGroups.get(o2).getSourceFiles().size() - ignoredFilesGroups.get(o1).getSourceFiles().size());
        keys.forEach(key -> {
            IgnoredFilesGroup ignoredFilesGroup = ignoredFilesGroups.get(key);
            content.append(SEPARATOR);
            content.append(ignoredFilesGroup.getFilter().getNote());
            content.append("\n");
            content.append(key);
            content.append("\n");
            List<SourceFile> sourceFiles = ignoredFilesGroup.getSourceFiles();
            content.append(sourceFiles.size() + " files");
            content.append(":\n\n");
            sourceFiles.forEach(sourceFile -> {
                content.append(sourceFile.getRelativePath());
                content.append("\n");
            });
            content.append(SEPARATOR);
            content.append("\n\n\n");
        });

        try {
            FileUtils.write(new File(textDataFolder, "excluded_files_ignored_rules.txt"), content.toString(), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveSourceCodeAspect(NamedSourceCodeAspect aspect, String prefix) {
        StringBuilder content = new StringBuilder();

        List<SourceFile> files = new ArrayList<>(aspect.getSourceFiles());
        Collections.sort(files, Comparator.comparing(SourceFile::getRelativePath));

        content.append("Path\tLines of Code\n");
        files.forEach(sourceFile -> {
            content.append(sourceFile.getRelativePath());
            content.append("\t");
            content.append(sourceFile.getLinesOfCode());
            content.append("\n");
        });

        try {
            FileUtils.write(new File(textDataFolder, DataExportUtils.getAspectFileListFileName(aspect, prefix)), content.toString(), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveFoundText(AspectAnalysisResults aspectAnalysisResults, String prefix) {
        if (aspectAnalysisResults.getFoundTextList().size() == 0) {
            return;
        }

        StringBuilder content = new StringBuilder();

        content.append("Text\tCount\n");
        int total[] = {0};
        int unique[] = {0};
        aspectAnalysisResults.getFoundTextList().forEach(foundText -> {
            content.append(foundText.getText().trim());
            content.append("\t");
            content.append(foundText.getCount());
            content.append("\n");

            unique[0] += 1;
            total[0] += foundText.getCount();
        });

        try {
            String fileName = DataExportUtils.getAspectFileListFileName(aspectAnalysisResults.getAspect(), prefix, FOUND_TEXT_SUFFIX);
            String data = "Summary: " + total[0] + " " + (total[0] == 1 ? "instance" : "instances") + ", " + unique[0] + " unique\n\n";
            data += content.toString();
            FileUtils.write(new File(textDataFolder, fileName), data, UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveFoundTextPerFile(AspectAnalysisResults aspectAnalysisResults, String prefix) {
        Map<File, SourceFileWithSearchData> foundFiles = aspectAnalysisResults.getFoundFiles();
        if (foundFiles.size() == 0) {
            return;
        }

        StringBuilder content = new StringBuilder();

        List<SourceFileWithSearchData> list = new ArrayList<>(foundFiles.values());
        Collections.sort(list, (a, b) -> b.getFoundInstancesCount() - a.getFoundInstancesCount());
        list.forEach(data -> {
            if (content.length() > 0) {
                content.append("\n\n");
            }
            List<FoundLine> lines = data.getLinesWithSearchedContent();
            content.append(data.getSourceFile().getRelativePath() + " (" + lines.size() + " " + (lines.size() == 1 ? "line" : "lines") + "):\n");
            data.getLinesWithSearchedContent().forEach(line -> {
                content.append("\t");
                content.append("- line " + line.getLineNumber() + ": ");
                content.append(line.getFoundText().trim());
                content.append("\n");
            });
        });

        try {
            String fileName = DataExportUtils.getAspectFileListFileName(aspectAnalysisResults.getAspect(), prefix, FOUND_TEXT_PER_FILE_SIFFIX);
            FileUtils.write(new File(textDataFolder, fileName), content.toString(), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exportSourceFile() throws IOException {
        this.codeCacheFolder = getCodeCacheFolder();

        detailedInfo("Saving details and source code cache:");
        if (codeConfiguration.getAnalysis().isCacheSourceFiles()) {
            saveAspectJsonFiles(codeConfiguration.getMain(), "main");
            saveAspectJsonFiles(codeConfiguration.getTest(), "test");
            saveAspectJsonFiles(codeConfiguration.getGenerated(), "generated");
            saveAspectJsonFiles(codeConfiguration.getBuildAndDeployment(), "buildAndDeployment");
            saveAspectJsonFiles(codeConfiguration.getOther(), "other");
        }

        UnitsAnalysisResults unitsAnalysisResults = analysisResults.getUnitsAnalysisResults();
        saveUnitFragmentFiles(unitsAnalysisResults.getLongestUnits(), "longest_unit");
        saveUnitFragmentFiles(unitsAnalysisResults.getMostComplexUnits(), "most_complex_unit");

        if (codeConfiguration.getAnalysis().isCacheSourceFiles()) {
            //    saveAllUnitFragmentFiles(unitsAnalysisResults.getAllUnits(), "all_units");
        }

        DuplicationAnalysisResults duplicationAnalysisResults = analysisResults.getDuplicationAnalysisResults();
        saveDuplicateFragmentFiles(duplicationAnalysisResults.getLongestDuplicates(), "longest_duplicates");
        saveDuplicateFragmentFiles(duplicationAnalysisResults.getMostFrequentDuplicates(), "most_frequent_duplicates");
    }

    private void exportInteractiveExplorers() throws IOException {
        File interactiveHtmlFolder = getInteractiveHtmlFolder();
        FileUtils.write(new File(interactiveHtmlFolder, "MainFiles.html"),
                new FilesExplorerGenerator(analysisResults).generateExplorer(), UTF_8);
        FileUtils.write(new File(interactiveHtmlFolder, "Units.html"),
                new UnitsExplorerGenerator(analysisResults).generateExplorer(), UTF_8);
        FileUtils.write(new File(interactiveHtmlFolder, "Duplicates.html"),
                new DuplicationExplorerGenerator(analysisResults).generateExplorer(), UTF_8);
        FileUtils.write(new File(interactiveHtmlFolder, "Dependencies.html"),
                new DependenciesExplorerGenerator(analysisResults).generateExplorer(), UTF_8);
    }

    private void exportJson() throws IOException {
        String analysisResultsJson = new JsonGenerator().generate(analysisResults);
        FileUtils.write(new File(dataFolder, "analysisResults.json"), analysisResultsJson, UTF_8);

        String configJson = FileUtils.readFileToString(sokratesConfigFile, UTF_8);
        FileUtils.write(new File(dataFolder, "config.json"), configJson, UTF_8);

        if (codeConfiguration.getTrendAnalysis().isSaveHistory()) {
            ZipUtils.stringToZipFile(new File(getTodayHistoryFolder(), "analysisResults.zip"),
                    new String[][]{{"config.json", configJson},
                            {"analysisResults.json", analysisResultsJson}});
            ZipUtils.stringToZipFile(new File(getLatestHistoryFolder(), "analysisResults.zip"),
                    new String[][]{{"config.json", configJson},
                            {"analysisResults.json", analysisResultsJson}});
        }

        FileUtils.write(new File(dataFolder, "mainFiles.json"), new JsonGenerator().generate(analysisResults.getMainAspectAnalysisResults().getAspect().getSourceFiles()), UTF_8);

        if (codeConfiguration.getFileHistoryAnalysis().filesHistoryImportPathExists(sokratesConfigFile.getParentFile())) {
            saveExtraAnalysesConfig();
        }

        FileUtils.write(new File(textDataFolder, "mainFiles.txt"), getFilesAsTxt(analysisResults.getMainAspectAnalysisResults().getAspect().getSourceFiles()), UTF_8);
        FileUtils.write(new File(textDataFolder, "mainFilesWithHistory.txt"), getFilesWithHistoryAsTxt(analysisResults.getMainAspectAnalysisResults().getAspect().getSourceFiles()), UTF_8);
        FileUtils.write(new File(dataFolder, "testFiles.json"), new JsonGenerator().generate(analysisResults.getTestAspectAnalysisResults().getAspect().getSourceFiles()), UTF_8);
        FileUtils.write(new File(dataFolder, "units.json"), new JsonGenerator().generate(new UnitListExporter(analysisResults.getUnitsAnalysisResults().getAllUnits()).getAllUnitsData()), UTF_8);
        FileUtils.write(new File(dataFolder, "files.json"), new FileListExporter(analysisResults.getFilesAnalysisResults().getAllFiles()).getJson(), UTF_8);
        FileUtils.write(new File(dataFolder, "duplicates.json"), new JsonGenerator().generate(new DuplicationExporter(
                analysisResults.getDuplicationAnalysisResults().getAllDuplicates()).getDuplicationExportInfo()), UTF_8);
        FileUtils.write(new File(dataFolder, "logical_decompositions.json"), new JsonGenerator().generate(
                analysisResults.getLogicalDecompositionsAnalysisResults()), UTF_8);
        FileUtils.write(new File(dataFolder, "dependencies.json"), new JsonGenerator().generate(
                new DependenciesExporter(analysisResults.getAllDependencies()).getDependenciesExportInfo()), UTF_8);
        FileUtils.write(new File(dataFolder, "contributors.json"), new JsonGenerator().generate(analysisResults.getContributorsAnalysisResults().getContributors()), UTF_8);
    }

    public File getTextDataFolder() {
        File textDataFolder = new File(dataFolder, "text");
        textDataFolder.mkdirs();
        return textDataFolder;
    }

    public File getExtraAnalysisDataFolder() {
        File extraAnalysisDataFolder = new File(dataFolder, "extra_analysis");
        extraAnalysisDataFolder.mkdirs();
        return extraAnalysisDataFolder;
    }

    private void saveExtraAnalysesConfig() {
        try {
            String jsonContent = FileUtils.readFileToString(sokratesConfigFile, UTF_8);

            FileUtils.write(new File(extraAnalysisDataFolder, "config_original.json"), new JsonGenerator().generate(codeConfiguration), UTF_8);

            saveConfigByFileChangeFrequency(jsonContent);
            saveConfigByFileAge(jsonContent);
            saveConfigByFileFreshness(jsonContent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveConfigByFileChangeFrequency(String jsonContent) throws IOException {
        CodeConfiguration codeConfiguration = (CodeConfiguration) new JsonMapper().getObject(jsonContent, CodeConfiguration.class);
        codeConfiguration.setLogicalDecompositions(FileHistoryScopingUtils.getLogicalDecompositionsFileUpdateFrequency(analysisResults.getMainAspectAnalysisResults().getAspect().getSourceFiles()));

        codeConfiguration.getFileHistoryAnalysis().setImportPath("");

        FileUtils.write(new File(extraAnalysisDataFolder, "config_by_file_change_frequency.json"), new JsonGenerator().generate(codeConfiguration), UTF_8);
    }

    private void saveConfigByFileAge(String jsonContent) throws IOException {
        CodeConfiguration codeConfiguration = (CodeConfiguration) new JsonMapper().getObject(jsonContent, CodeConfiguration.class);
        codeConfiguration.setLogicalDecompositions(FileHistoryScopingUtils.getLogicalDecompositionsByAge(analysisResults.getMainAspectAnalysisResults().getAspect().getSourceFiles()));

        codeConfiguration.getFileHistoryAnalysis().setImportPath("");

        FileUtils.write(new File(extraAnalysisDataFolder, "config_by_file_age.json"), new JsonGenerator().generate(codeConfiguration), UTF_8);
    }

    private void saveConfigByFileFreshness(String jsonContent) throws IOException {
        CodeConfiguration codeConfiguration = (CodeConfiguration) new JsonMapper().getObject(jsonContent, CodeConfiguration.class);
        codeConfiguration.setLogicalDecompositions(FileHistoryScopingUtils.getLogicalDecompositionsByFreshness(analysisResults.getMainAspectAnalysisResults().getAspect().getSourceFiles()));

        codeConfiguration.getFileHistoryAnalysis().setImportPath("");

        FileUtils.write(new File(extraAnalysisDataFolder, "config_by_file_freshness.json"), new JsonGenerator().generate(codeConfiguration), UTF_8);
    }

    private String getFilesAsTxt(List<SourceFile> sourceFiles) {
        StringBuilder builder = new StringBuilder();

        builder.append("path\t# lines of code").append("\n");

        sourceFiles.forEach(sourceFile -> {
            builder.append(sourceFile.getRelativePath())
                    .append("\t")
                    .append(sourceFile.getLinesOfCode())
                    .append("\n");
        });

        return builder.toString();
    }

    private String getFilesWithHistoryAsTxt(List<SourceFile> sourceFiles) {
        StringBuilder builder = new StringBuilder();

        builder
                .append("path\t")
                .append("# lines of code\t")
                .append("number of updates\tdays since first update\tdays since last update\t")
                .append("first updated\tlast updated\t")
                .append("\n");

        sourceFiles.forEach(sourceFile -> {
            FileModificationHistory history = sourceFile.getFileModificationHistory();
            if (history != null) {
                builder.append(sourceFile.getRelativePath())
                        .append("\t")
                        .append(sourceFile.getLinesOfCode())
                        .append("\t")
                        .append(history.getDates().size())
                        .append("\t")
                        .append(history.daysSinceFirstUpdate())
                        .append("\t")
                        .append(history.daysSinceLatestUpdate())
                        .append("\t")
                        .append(history.getOldestDate())
                        .append("\t")
                        .append(history.getLatestDate())
                        .append("\n");
            }
        });

        return builder.toString();
    }

    private void saveUnitFragmentFiles(List<UnitInfo> units, String fragmentType) throws IOException {

        File fragmentsFolder = recreateFolder("fragments/" + fragmentType);

        detailedInfo(" - saving source code cache for the " + fragmentType + "fragments");
        int count[] = {0};
        units.forEach(unit -> {
            count[0]++;
            saveUnitAsHtml(fragmentType, fragmentsFolder, count, unit);
        });
    }

    private void saveUnitAsHtml(String fragmentType, File fragmentsFolder, int[] count, UnitInfo unit) {
        try {
            String fileName = fragmentType + "_" + count[0] + "." + unit.getSourceFile().getExtension();
            String fileAndLines = unit.getSourceFile().getRelativePath() + " [" + unit.getStartLine() + ":" + unit.getEndLine() + "]";

            String htmlTemplate = HtmlTemplateUtils.getResource("/templates/CodeFragmentUnit.html");
            String html = htmlTemplate.replace("${title}", unit.getShortName());
            html = html.replace("${unit-name}", unit.getShortName());
            html = html.replace("${file-and-lines}", fileAndLines);
            html = html.replace("${language}", unit.getSourceFile().getExtension());
            html = html.replace("${code}", StringEscapeUtils.escapeHtml4(unit.getBody()));
            html = html.replace("${lines-of-code}", FormattingUtils.formatCount(unit.getLinesOfCode()));
            html = html.replace("${mccabe-index}", FormattingUtils.formatCount(unit.getMcCabeIndex()));

            File htmlFile = new File(fragmentsFolder, fileName + ".html");
            FileUtils.write(htmlFile, html, UTF_8);

        } catch (IOException e) {
            LOG.warn(e);
        }
    }

    private void saveFileAsHtml(File htmlFile, SourceFile sourceFile) {
        try {

            String htmlTemplate = HtmlTemplateUtils.getResource("/templates/CodeFragmentFile.html");
            String html = htmlTemplate.replace("${title}", sourceFile.getRelativePath());
            html = html.replace("${file-path}", sourceFile.getRelativePath());
            html = html.replace("${file-name}", sourceFile.getFile().getName());
            String langName = LanguageAnalyzerFactory.getInstance().getLanguageAnalyzer(sourceFile).getClass().getSimpleName().replace("Analyzer", "").toLowerCase();
            String defaultLangName = DefaultLanguageAnalyzer.class.getSimpleName().replace("Analyzer", "");
            html = html.replace("${language}", langName.equalsIgnoreCase(defaultLangName) ? sourceFile.getExtension() : langName);
            html = html.replace("${code}", StringEscapeUtils.escapeHtml4(sourceFile.getContent()));
            html = html.replace("${lines-of-code}", FormattingUtils.formatCount(sourceFile.getLinesOfCode()));

            FileUtils.write(htmlFile, html, UTF_8);

        } catch (IOException e) {
            LOG.warn(e);
        }
    }

    private void saveDuplicateFragmentFiles(List<DuplicationInstance> duplicates, String fragmentType) throws IOException {
        File fragmentsFolder = recreateFolder("fragments/" + fragmentType);

        detailedInfo(" - saving source code cache for the " + fragmentType + "fragments");
        int count[] = {0};
        duplicates.forEach(duplicate -> {
            count[0]++;
            try {
                DuplicatedFileBlock firstFileBlock = duplicate.getDuplicatedFileBlocks().get(0);
                String extension = firstFileBlock.getSourceFile().getExtension();
                String fileName = fragmentType + "_" + count[0] + "." + extension;
                File file = new File(fragmentsFolder, fileName);

                StringBuilder body = new StringBuilder();

                duplicate.getDuplicatedFileBlocks().forEach(block -> {
                    body.append(block.getSourceFile().getRelativePath() + " [" + block.getStartLine() + ":" + block.getEndLine() + "]:\n");
                    body.append(SEPARATOR);
                    body.append(block.getSourceFile().getLines().subList(block.getStartLine() - 1, block.getEndLine()).stream().collect(Collectors.joining("\n")) + "\n" + SEPARATOR + "\n\n\n");
                });

                FileUtils.write(file, body.toString(), UTF_8);
            } catch (IOException e) {
                LOG.warn(e);
            }
        });
    }

    private File recreateFolder(String folderName) throws IOException {
        File fragmentsFolder = new File(codeCacheFolder, folderName);
        if (fragmentsFolder.exists()) {
            FileUtils.deleteDirectory(fragmentsFolder);
        }
        fragmentsFolder.mkdirs();
        return fragmentsFolder;
    }

    private void saveAllUnitFragmentFiles(List<UnitInfo> units, String fragmentType) throws IOException {
        File fragmentsFolder = recreateFolder("fragments/" + fragmentType);

        detailedInfo(" - saving source code cache for the " + fragmentType + "fragments");
        int count[] = {0};
        units.forEach(unit -> {
            count[0]++;
            try {
                SourceFile sourceFile = unit.getSourceFile();
                File fragmentsSubFolder = new File(fragmentsFolder, sourceFile.getRelativePath());
                fragmentsSubFolder.mkdirs();
                String fileName = fragmentType + "_" + count[0] + "." + sourceFile.getExtension();
                File file = new File(fragmentsSubFolder, fileName);
                String body = sourceFile.getRelativePath() + " ["
                        + unit.getStartLine() + ":" + unit.getEndLine() + "]:\n\n"
                        + unit.getBody();
                FileUtils.write(file, body, UTF_8);
            } catch (IOException e) {
                LOG.warn(e);
            }
        });
    }


    private void saveAspectJsonFiles(NamedSourceCodeAspect aspect, String aspectName) throws IOException {
        File filesListFile = new File(dataFolder, aspectName + "FilesPaths.json");
        detailedInfo(" - storing the file list for the <b>" + aspectName + "</b> aspect in <a href='" + filesListFile.getPath() + "'>" + filesListFile.getPath() + "</a>");
        List<String> files = new ArrayList<>();
        aspect.getSourceFiles().forEach(sourceFile -> {
            files.add(sourceFile.getRelativePath());
        });
        FileUtils.write(filesListFile, new JsonGenerator().generate(files), UTF_8);

        File aspectCodeCacheFolder = recreateFolder(aspectName);

        Map<String, List<String>> contents = new HashMap<>();
        detailedInfo(" - saving source code cache for the <b>" + aspectName + "</b> aspect in <a href='" + aspectCodeCacheFolder.getPath() + "'>" + aspectCodeCacheFolder.getPath() + "</a>");
        aspect.getSourceFiles().forEach(sourceFile -> {
            contents.put(sourceFile.getRelativePath(), sourceFile.getLines());
            try {
                FileUtils.write(new File(aspectCodeCacheFolder, sourceFile.getRelativePath()), sourceFile.getContent(), UTF_8);
                saveFileAsHtml(new File(aspectCodeCacheFolder, sourceFile.getRelativePath() + ".html"), sourceFile);
            } catch (IOException e) {
                LOG.warn(e);
            }
        });
    }

    public File getCodeCacheFolder() {
        File codeCacheFolder = new File(reportsFolder, SRC_CACHE_FOLDER_NAME);
        codeCacheFolder.mkdirs();
        return codeCacheFolder;
    }

    public File getInteractiveHtmlFolder() {
        File codeCacheFolder = new File(reportsFolder, INTERACTIVE_HTML_FOLDER_NAME);
        codeCacheFolder.mkdirs();
        return codeCacheFolder;
    }

    public File getDataFolder() {
        File dataFolder = new File(reportsFolder, DATA_FOLDER_NAME);
        dataFolder.mkdirs();
        return dataFolder;
    }

    public File getDataHistoryFolder() {
        File folder = new File(reportsFolder, HISTORY_FOLDER_NAME);
        folder.mkdirs();
        return folder;
    }

    public File getTodayHistoryFolder() {
        return codeConfiguration.getTrendAnalysis().getSnapshotFolder(sokratesConfigFile.getParentFile());
    }

    public File getLatestHistoryFolder() {
        File folder = new File(getDataHistoryFolder(), "LATEST");
        folder.mkdirs();
        return folder;
    }

    private void info(String text) {
        LOG.info(text);
        if (progressFeedback != null) {
            progressFeedback.setText(text);
        }
    }

    public void detailedInfo(String text) {
        LOG.info(text);
        if (progressFeedback != null) {
            progressFeedback.setDetailedText(text);
        }
    }

}
