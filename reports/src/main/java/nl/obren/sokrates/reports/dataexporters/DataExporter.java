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
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.SourceCodeAspect;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.duplication.DuplicatedFileBlock;
import nl.obren.sokrates.sourcecode.duplication.DuplicationInstance;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class DataExporter {
    public static final String INTERACTIVE_HTML_FOLDER_NAME = "explorers";
    public static final String SRC_CACHE_FOLDER_NAME = "src";
    public static final String DATA_FOLDER_NAME = "data";
    private static final Log LOG = LogFactory.getLog(DataExporter.class);
    private ProgressFeedback progressFeedback;

    public DataExporter(ProgressFeedback progressFeedback) {
        this.progressFeedback = progressFeedback;
    }

    public void saveData(CodeConfiguration codeConfiguration, File reportsFolder, CodeAnalysisResults analysisResults) throws IOException {
        File dataFolder = getDataFolder(reportsFolder);

        exportJson(analysisResults, dataFolder);

        exportInteractiveExplorers(reportsFolder, analysisResults);

        File codeCacheFolder = getCodeCacheFolder(reportsFolder);

        detailedInfo("Saving details and source code cache:");
        saveAspectJsonFiles(dataFolder, codeCacheFolder, codeConfiguration.getMain(), "main");
        saveAspectJsonFiles(dataFolder, codeCacheFolder, codeConfiguration.getTest(), "test");
        saveAspectJsonFiles(dataFolder, codeCacheFolder, codeConfiguration.getGenerated(), "generated");
        saveAspectJsonFiles(dataFolder, codeCacheFolder, codeConfiguration.getBuildAndDeployment(), "build-and-deployment");
        saveAspectJsonFiles(dataFolder, codeCacheFolder, codeConfiguration.getOther(), "other");

        saveUnitFragmentFiles(codeCacheFolder,
                analysisResults.getUnitsAnalysisResults().getLongestUnits(), "longest_unit");

        saveUnitFragmentFiles(codeCacheFolder,
                analysisResults.getUnitsAnalysisResults().getMostComplexUnits(), "most_complex_unit");

        saveAllUnitFragmentFiles(codeCacheFolder,
                analysisResults.getUnitsAnalysisResults().getAllUnits(), "all_units");

        saveDuplicateFragmentFiles(codeCacheFolder,
                analysisResults.getDuplicationAnalysisResults().getLongestDuplicates(), "longest_duplicates");
        saveDuplicateFragmentFiles(codeCacheFolder,
                analysisResults.getDuplicationAnalysisResults().getMostFrequentDuplicates(), "most_frequent_duplicates");
    }

    private void exportInteractiveExplorers(File reportsFolder, CodeAnalysisResults analysisResults) throws IOException {
        File interactiveHtmlFolder = getInteractiveHtmlFolder(reportsFolder);
        FileUtils.write(new File(interactiveHtmlFolder, "MainFiles.html"),
                new FilesExplorerGenerator(analysisResults).generateExplorer(), UTF_8);
        FileUtils.write(new File(interactiveHtmlFolder, "Units.html"),
                new UnitsExplorerGenerator(analysisResults).generateExplorer(), UTF_8);
        FileUtils.write(new File(interactiveHtmlFolder, "Duplicates.html"),
                new DuplicationExplorerGenerator(analysisResults).generateExplorer(), UTF_8);
        FileUtils.write(new File(interactiveHtmlFolder, "Dependencies.html"),
                new DependenciesExplorerGenerator(analysisResults).generateExplorer(), UTF_8);
    }

    private void exportJson(CodeAnalysisResults analysisResults, File dataFolder) throws IOException {
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

    private void saveUnitFragmentFiles(File srcCacheFolder, List<UnitInfo> units, String fragmentType) throws IOException {
        File fragmentsFolder = recreatFolder(srcCacheFolder, "fragments/" + fragmentType);

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

    private void saveDuplicateFragmentFiles(File srcCacheFolder, List<DuplicationInstance> duplicates, String fragmentType) throws IOException {
        File fragmentsFolder = recreatFolder(srcCacheFolder, "fragments/" + fragmentType);

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
                    body.append("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -\n");
                    body.append(block.getSourceFile().getLines().subList(block.getStartLine() - 1, block.getEndLine()).stream().collect(Collectors.joining("\n")) + "\n- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -\n\n\n\n");
                });

                FileUtils.write(file, body.toString(), UTF_8);
            } catch (IOException e) {
                LOG.warn(e);
            }
        });
    }

    private File recreatFolder(File srcCacheFolder, String s) throws IOException {
        File fragmentsFolder = new File(srcCacheFolder, s);
        if (fragmentsFolder.exists()) {
            FileUtils.deleteDirectory(fragmentsFolder);
        }
        fragmentsFolder.mkdirs();
        return fragmentsFolder;
    }

    private void saveAllUnitFragmentFiles(File srcCacheFolder, List<UnitInfo> units, String fragmentType) throws IOException {
        File fragmentsFolder = recreatFolder(srcCacheFolder, "fragments/" + fragmentType);

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


    private void saveAspectJsonFiles(File dataFolder, File srcCacheFolder, SourceCodeAspect aspect, String aspectName) throws IOException {
        File filesListFile = new File(dataFolder, aspectName + "-files.json");
        detailedInfo(" - storing the file list for the <b>" + aspectName + "</b> aspect in <a href='" + filesListFile.getPath() + "'>" + filesListFile.getPath() + "</a>");
        List<String> files = new ArrayList<>();
        aspect.getSourceFiles().forEach(sourceFile -> {
            files.add(sourceFile.getRelativePath());
        });
        FileUtils.write(filesListFile, new JsonGenerator().generate(files), UTF_8);

        File aspectCodeCacheFolder = recreatFolder(srcCacheFolder, aspectName);

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

    public File getCodeCacheFolder(File reportsFolder) {
        File codeCacheFolder = new File(reportsFolder, SRC_CACHE_FOLDER_NAME);
        codeCacheFolder.mkdirs();
        return codeCacheFolder;
    }

    public File getInteractiveHtmlFolder(File reportsFolder) {
        File codeCacheFolder = new File(reportsFolder, INTERACTIVE_HTML_FOLDER_NAME);
        codeCacheFolder.mkdirs();
        return codeCacheFolder;
    }

    public File getDataFolder(File reportsFolder) {
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
