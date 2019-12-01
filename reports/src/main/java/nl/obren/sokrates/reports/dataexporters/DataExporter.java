package nl.obren.sokrates.reports.dataexporters;

import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.reports.dataexporters.dependencies.DependenciesExporter;
import nl.obren.sokrates.reports.dataexporters.duplication.DuplicationExporter;
import nl.obren.sokrates.reports.dataexporters.files.FileListExporter;
import nl.obren.sokrates.reports.dataexporters.units.UnitListExporter;
import nl.obren.sokrates.reports.generators.explorers.DependenciesExplorerGenerator;
import nl.obren.sokrates.reports.generators.explorers.DuplicationExplorerGenerator;
import nl.obren.sokrates.reports.generators.explorers.FilesExplorerGenerator;
import nl.obren.sokrates.reports.generators.explorers.UnitsExplorerGenerator;
import nl.obren.sokrates.sourcecode.IgnoredFilesGroup;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.DuplicationAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.UnitsAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.duplication.DuplicatedFileBlock;
import nl.obren.sokrates.sourcecode.duplication.DuplicationInstance;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class DataExporter {
    public static final String INTERACTIVE_HTML_FOLDER_NAME = "explorers";
    public static final String SRC_CACHE_FOLDER_NAME = "src";
    public static final String DATA_FOLDER_NAME = "data";
    public static final String SEPARATOR = "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -\n";
    private static final Log LOG = LogFactory.getLog(DataExporter.class);
    private ProgressFeedback progressFeedback;
    private CodeConfiguration codeConfiguration;
    private File reportsFolder;
    private CodeAnalysisResults analysisResults;
    private File dataFolder;
    private File codeCacheFolder;

    public DataExporter(ProgressFeedback progressFeedback) {
        this.progressFeedback = progressFeedback;
    }

    public void saveData(CodeConfiguration codeConfiguration, File reportsFolder, CodeAnalysisResults analysisResults) throws IOException {
        this.codeConfiguration = codeConfiguration;
        this.reportsFolder = reportsFolder;
        this.analysisResults = analysisResults;
        this.dataFolder = getDataFolder();

        exportFileLists();
        exportJson();
        exportInteractiveExplorers();
        exportSourceFile();
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
            logicalDecomposition.getComponents().forEach(component  -> {
                saveSourceCodeAspect(component.getAspect(), DataExportUtils.getComponentFilePrefix(logicalDecomposition.getKey()));
            });
        });

        analysisResults.getCrossCuttingConcernsAnalysisResults().forEach(group -> {
            group.getCrossCuttingConcerns().forEach(concern  -> {
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
        saveAspectJsonFiles(codeConfiguration.getMain(), "main");
        saveAspectJsonFiles(codeConfiguration.getTest(), "test");
        saveAspectJsonFiles(codeConfiguration.getGenerated(), "generated");
        saveAspectJsonFiles(codeConfiguration.getBuildAndDeployment(), "buildAndDeployment");
        saveAspectJsonFiles(codeConfiguration.getOther(), "other");

        UnitsAnalysisResults unitsAnalysisResults = analysisResults.getUnitsAnalysisResults();
        saveUnitFragmentFiles(unitsAnalysisResults.getLongestUnits(), "longest_unit");
        saveUnitFragmentFiles(unitsAnalysisResults.getMostComplexUnits(), "most_complex_unit");

        if (codeConfiguration.getAnalysis().isCacheSourceFiles()) {
            saveAllUnitFragmentFiles(unitsAnalysisResults.getAllUnits(), "all_units");
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
        FileUtils.write(new File(dataFolder, "analysisResults.json"), new JsonGenerator().generate(analysisResults), UTF_8);
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
            try {
                String fileName = fragmentType + "_" + count[0] + "." + unit.getSourceFile().getExtension();
                File file = new File(fragmentsFolder, fileName);
                String body = unit.getSourceFile().getRelativePath() + " ["
                        + unit.getStartLine() + ":" + unit.getEndLine() + "]:\n\n"
                        + unit.getBody();
                FileUtils.write(file, body, UTF_8);
            } catch (IOException e) {
                LOG.warn(e);
            }
        });
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

        if (codeConfiguration.getAnalysis().isCacheSourceFiles()) {
            Map<String, List<String>> contents = new HashMap<>();
            detailedInfo(" - saving source code cache for the <b>" + aspectName + "</b> aspect in <a href='" + aspectCodeCacheFolder.getPath() + "'>" + aspectCodeCacheFolder.getPath() + "</a>");
            aspect.getSourceFiles().forEach(sourceFile -> {
                contents.put(sourceFile.getRelativePath(), sourceFile.getLines());
                try {
                    FileUtils.write(new File(aspectCodeCacheFolder, sourceFile.getRelativePath()), sourceFile.getContent(), UTF_8);
                } catch (IOException e) {
                    LOG.warn(e);
                }
            });
        }
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
