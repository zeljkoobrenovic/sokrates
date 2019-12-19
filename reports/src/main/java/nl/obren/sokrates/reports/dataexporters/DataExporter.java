/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.dataexporters;

import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.common.utils.SystemUtils;
import nl.obren.sokrates.reports.dataexporters.dependencies.DependenciesExporter;
import nl.obren.sokrates.reports.dataexporters.duplication.DuplicateFileBlockExportInfo;
import nl.obren.sokrates.reports.dataexporters.duplication.DuplicationExportInfo;
import nl.obren.sokrates.reports.dataexporters.duplication.DuplicationExporter;
import nl.obren.sokrates.reports.dataexporters.files.FileListExporter;
import nl.obren.sokrates.reports.dataexporters.units.UnitListExporter;
import nl.obren.sokrates.reports.generators.explorers.DependenciesExplorerGenerator;
import nl.obren.sokrates.reports.generators.explorers.DuplicationExplorerGenerator;
import nl.obren.sokrates.reports.generators.explorers.FilesExplorerGenerator;
import nl.obren.sokrates.reports.generators.explorers.UnitsExplorerGenerator;
import nl.obren.sokrates.reports.utils.HtmlTemplateUtils;
import nl.obren.sokrates.reports.utils.ZipUtils;
import nl.obren.sokrates.sourcecode.IgnoredFilesGroup;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.DuplicationAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.UnitsAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.dependencies.Dependency;
import nl.obren.sokrates.sourcecode.duplication.DuplicatedFileBlock;
import nl.obren.sokrates.sourcecode.duplication.DuplicationInstance;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.StringEscapeUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class DataExporter {
    public static final String INTERACTIVE_HTML_FOLDER_NAME = "explorers";
    public static final String SRC_CACHE_FOLDER_NAME = "src";
    public static final String DATA_FOLDER_NAME = "data";
    public static final String HISTORY_FOLDER_NAME = "history";
    public static final String SEPARATOR = "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -\n";
    private static final Log LOG = LogFactory.getLog(DataExporter.class);
    private ProgressFeedback progressFeedback;
    private CodeConfiguration codeConfiguration;
    private File reportsFolder;
    private CodeAnalysisResults analysisResults;
    private File dataFolder;
    private File historyFolder;
    private File codeCacheFolder;

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

    public void saveData(CodeConfiguration codeConfiguration, File reportsFolder, CodeAnalysisResults analysisResults) throws IOException {
        this.codeConfiguration = codeConfiguration;
        this.reportsFolder = reportsFolder;
        this.analysisResults = analysisResults;
        this.dataFolder = getDataFolder();
        this.historyFolder = getDataHistoryFolder();

        exportFileLists();
        exportMetrics();
        exportControls();
        exportJson();
        exportDuplicates();
        exportUnits();
        exportInteractiveExplorers();
        exportSourceFile();
        exportDependencies(analysisResults);
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
            FileUtils.write(new File(dataFolder, "metrics.txt"), content.toString(), UTF_8);
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
            FileUtils.write(new File(dataFolder, "controls.txt"), content.toString(), UTF_8);
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
            FileUtils.write(new File(dataFolder, "duplicates.txt"), content.toString(), UTF_8);
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
            FileUtils.write(new File(dataFolder, "units.txt"), content.toString(), UTF_8);
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

        analysisResults.getCrossCuttingConcernsAnalysisResults().forEach(group -> {
            group.getCrossCuttingConcerns().forEach(concern -> {
                saveSourceCodeAspect(concern.getAspect(), DataExportUtils.getCrossCuttingConcernFilePrefix(group.getKey()));
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
            FileUtils.write(new File(dataFolder, "excluded_files_ignored_extensions.txt"), content.toString(), UTF_8);
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
                logicalDecomposition.getAllDependencies().forEach(dependency -> {
                    content.append(appendDependency(filterFrom, filterTo, logicalDecompositionName, dependency));
                });
                try {
                    FileUtils.write(new File(dataFolder, fileNamePrefix + ".txt"), content.toString(), UTF_8);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private String appendDependency(String filterFrom, String filterTo, String logicalDecompositionName, Dependency dependency) {
        StringBuilder content = new StringBuilder();
        dependency.getFromFiles().forEach(sourceFileDependency -> {
            List<NamedSourceCodeAspect> fromComponents = dependency.getFromComponents(logicalDecompositionName);
            List<NamedSourceCodeAspect> toComponents = dependency.getToComponents(logicalDecompositionName);

            String fromComponent = dependency.getFromComponentName() != null
                    ? dependency.getFromComponentName()
                    : fromComponents.size() > 0 ? fromComponents.get(0).getName() : "UNKNOWN";

            String toComponent = dependency.getToComponentName() != null
                    ? dependency.getToComponentName()
                    : toComponents.size() > 0 ? toComponents.get(0).getName() : "UNKNOWN";

            if (shouldAppendDependency(filterFrom, filterTo, fromComponent, toComponent)) {
                content.append("from: " + fromComponent);
                content.append("\n");
                content.append("to: " + toComponent);
                content.append("\nevidence:\n");
                content.append(" - file: \"");
                content.append(sourceFileDependency.getSourceFile().getRelativePath());
                content.append("\"\n");
                content.append("   contains \"");
                content.append(sourceFileDependency.getCodeFragment());
                content.append("\"\n\n");
            }
        });

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
            FileUtils.write(new File(dataFolder, "excluded_files_ignored_rules.txt"), content.toString(), UTF_8);
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
            FileUtils.write(new File(dataFolder, DataExportUtils.getAspectFileListFileName(aspect, prefix)), content.toString(), UTF_8);
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

        if (codeConfiguration.getAnalysis().isSaveDailyHistory()) {
            ZipUtils.stringToZipFile(new File(getTodayHistoryFolder(), "analysisResults.zip"), "analysisResults.json", analysisResultsJson);
        }

        FileUtils.write(new File(dataFolder, "mainFiles.json"), new JsonGenerator().generate(analysisResults.getMainAspectAnalysisResults().getAspect().getSourceFiles()), UTF_8);
        FileUtils.write(new File(dataFolder, "testFiles.json"), new JsonGenerator().generate(analysisResults.getTestAspectAnalysisResults().getAspect().getSourceFiles()), UTF_8);
        FileUtils.write(new File(dataFolder, "units.json"), new JsonGenerator().generate(new UnitListExporter(analysisResults.getUnitsAnalysisResults().getAllUnits()).getAllUnitsData()), UTF_8);
        FileUtils.write(new File(dataFolder, "files.json"), new FileListExporter(analysisResults.getFilesAnalysisResults().getAllFiles()).getJson(), UTF_8);
        FileUtils.write(new File(dataFolder, "duplicates.json"), new JsonGenerator().generate(new DuplicationExporter(
                analysisResults.getDuplicationAnalysisResults().getAllDuplicates()).getDuplicationExportInfo()), UTF_8);
        FileUtils.write(new File(dataFolder, "logical_decompositions.json"), new JsonGenerator().generate(
                analysisResults.getLogicalDecompositionsAnalysisResults()), UTF_8);
        FileUtils.write(new File(dataFolder, "dependencies.json"), new JsonGenerator().generate(
                new DependenciesExporter(analysisResults.getAllDependencies()).getDependenciesExportInfo()), UTF_8);
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
            html = html.replace("${lang}", unit.getSourceFile().getExtension());
            html = html.replace("${code}", StringEscapeUtils.escapeHtml4(unit.getBody().replace("\n", "\n ")));
            html = html.replace("${lines-of-code}", FormattingUtils.getFormattedCount(unit.getLinesOfCode()));
            html = html.replace("${mccabe-index}", FormattingUtils.getFormattedCount(unit.getMcCabeIndex()));

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
            html = html.replace("${lang}", sourceFile.getExtension());
            html = html.replace("${code}", StringEscapeUtils.escapeHtml4(sourceFile.getContent().replace("\n", "\n ")));
            html = html.replace("${lines-of-code}", FormattingUtils.getFormattedCount(sourceFile.getLinesOfCode()));

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
        String pattern = "yyyy-MM-dd";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

        String date = simpleDateFormat.format(new Date());

        File folder = new File(getDataHistoryFolder(), date);
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
