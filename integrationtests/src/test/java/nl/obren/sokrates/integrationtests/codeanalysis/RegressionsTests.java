package nl.obren.sokrates.integrationtests.codeanalysis;

import nl.obren.sokrates.codeexplorer.CommandLineInterface;
import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.sourcecode.CodeConfiguration;
import nl.obren.sokrates.sourcecode.CodeConfigurationUtils;
import nl.obren.sokrates.sourcecode.analysis.results.*;
import nl.obren.sokrates.sourcecode.duplication.DuplicationInstance;
import nl.obren.sokrates.sourcecode.metrics.MetricsList;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.BasicConfigurator;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

import static junit.framework.TestCase.*;
import static nl.obren.sokrates.integrationtests.codeanalysis.RegressionTestUtils.json;

public class RegressionsTests {

    @Test
    public void analyzeWholeSystems() throws IOException {
        BasicConfigurator.configure();
        //analyzeSystem("bitcoin");
        //analyzeSystem("cherami-server");
        //analyzeSystem("deck.gl");
        //analyzeSystem("kafka");
        //analyzeSystem("spark");
        analyzeSystem("cherami-client-java");
    }

    private void analyzeSystem(String systemName) throws IOException {
        analyzeSystem(systemName, RegressionTestUtils.createSourceFolder(systemName));
    }

    private void analyzeSystem(String systemName, File srcRoot) throws IOException {
        assertTrue(srcRoot.exists());

        File conf = CodeConfigurationUtils.getDefaultSokratesConfigFile(srcRoot);
        assertFalse(conf.exists());

        CommandLineInterface commandLineInterface = RegressionTestUtils.initConfiguration(srcRoot, conf);

        assertTrue(conf.exists());

        CodeConfiguration codeConfiguration = (CodeConfiguration) new JsonMapper().getObject(FileUtils.readFileToString(conf, StandardCharsets.UTF_8), CodeConfiguration.class);

        assertNotNull(codeConfiguration);
        assertTrue(codeConfiguration.getExtensions().size() > 0);

        File sokrates_reports = RegressionTestUtils.generateReports(srcRoot, conf, commandLineInterface);

        assertTrue(sokrates_reports.exists());
        File[] reports = sokrates_reports.listFiles();
        assertEquals(reports.length, 11);

        CodeAnalysisResults analysisResults =
                (CodeAnalysisResults) new JsonMapper().getObject(FileUtils.readFileToString(new File(sokrates_reports, "analysisResults.json"), StandardCharsets.UTF_8), CodeAnalysisResults.class);

        ClassLoader clazz = this.getClass().getClassLoader();
        InputStream inputStream = clazz.getResourceAsStream("regressioncases/" + systemName + "/analysisResults.json");

        assertNotNull(inputStream);
        CodeAnalysisResults regressionAnalysisResults =
                (CodeAnalysisResults) new JsonMapper().getObject(IOUtils.toString(inputStream, StandardCharsets.UTF_8), CodeAnalysisResults.class);

        assertEquals(json(analysisResults.getMainAspectAnalysisResults()), json(regressionAnalysisResults.getMainAspectAnalysisResults()));
        assertEquals(json(analysisResults.getTestAspectAnalysisResults()), json(regressionAnalysisResults.getTestAspectAnalysisResults()));
        assertEquals(json(analysisResults.getGeneratedAspectAnalysisResults()), json(regressionAnalysisResults.getGeneratedAspectAnalysisResults()));
        assertEquals(json(analysisResults.getBuildAndDeployAspectAnalysisResults()), json(regressionAnalysisResults.getBuildAndDeployAspectAnalysisResults()));
        assertEquals(json(analysisResults.getOtherAspectAnalysisResults()), json(regressionAnalysisResults.getOtherAspectAnalysisResults()));

        clean(analysisResults.getLogicalDecompositionsAnalysisResults());
        clean(regressionAnalysisResults.getLogicalDecompositionsAnalysisResults());
        assertEquals(json(analysisResults.getLogicalDecompositionsAnalysisResults()), json(regressionAnalysisResults.getLogicalDecompositionsAnalysisResults()));
        assertEquals(json(analysisResults.getCrossCuttingConcernsAnalysisResults()), json(regressionAnalysisResults.getCrossCuttingConcernsAnalysisResults()));
        clean(analysisResults.getDuplicationAnalysisResults());
        clean(regressionAnalysisResults.getDuplicationAnalysisResults());
        assertEquals(json(analysisResults.getDuplicationAnalysisResults()), json(regressionAnalysisResults.getDuplicationAnalysisResults()));
        clean(analysisResults.getUnitsAnalysisResults());
        clean(regressionAnalysisResults.getUnitsAnalysisResults());
        assertEquals(json(analysisResults.getUnitsAnalysisResults()), json(regressionAnalysisResults.getUnitsAnalysisResults()));
        clean(analysisResults.getFilesAnalysisResults());
        clean(regressionAnalysisResults.getFilesAnalysisResults());
        assertEquals(json(analysisResults.getFilesAnalysisResults()), json(regressionAnalysisResults.getFilesAnalysisResults()));
        assertEquals(json(analysisResults.getControlResults()), json(regressionAnalysisResults.getControlResults()));
        clean(analysisResults.getMetricsList());
        clean(regressionAnalysisResults.getMetricsList());
        assertEquals(json(analysisResults.getMetricsList()), json(regressionAnalysisResults.getMetricsList()));
        assertEquals(json(analysisResults.getExcludedExtensions()), json(regressionAnalysisResults.getExcludedExtensions()));
        assertEquals(json(analysisResults.getNumberOfExcludedFiles()), json(regressionAnalysisResults.getNumberOfExcludedFiles()));

        FileUtils.deleteDirectory(srcRoot);

        assertFalse(srcRoot.exists());
    }

    private void clean(MetricsList metricsList) {
        metricsList.remove("TOTAL_ANALYSIS_TIME_IN_MILLIS");
    }

    private void clean(DuplicationAnalysisResults duplicationAnalysisResults) {
        Consumer<DuplicationInstance> cleaner = duplication -> {
            duplication.getDuplicatedFileBlocks().forEach(block -> {
                block.setSourceFile(null);
            });
        };
        duplicationAnalysisResults.getLongestDuplicates().forEach(cleaner);
        duplicationAnalysisResults.getMostFrequentDuplicates().forEach(cleaner);
    }

    private void clean(UnitsAnalysisResults unitsAnalysisResults) {
        unitsAnalysisResults.getLongestUnits().forEach(unit -> unit.setSourceFile(null));
        unitsAnalysisResults.getMostComplexUnits().forEach(unit -> unit.setSourceFile(null));
    }

    private void clean(FilesAnalysisResults filesAnalysisResults) {
        filesAnalysisResults.getLongestFiles().forEach(file -> {
            file.setFile(new File(""));
            file.getLogicalComponents().forEach(comp -> comp.getSourceFileFilters().forEach(filter -> filter.setPathPattern("")));
        });
    }

    private void clean(List<LogicalDecompositionAnalysisResults> logicalDecompositionsAnalysisResults) {
        logicalDecompositionsAnalysisResults.forEach(result -> {
            result.getComponents().forEach(comp -> comp.getAspect().getSourceFileFilters().forEach(filter -> filter.setPathPattern("")));
            result.getLogicalDecomposition().getComponents().forEach(comp -> {
                comp.getAspectsPerExtensions().forEach(aspect -> aspect.getSourceFileFilters().forEach(filter -> filter.setPathPattern("")));
                comp.getSourceFileFilters().forEach(filter -> filter.setPathPattern(""));
            });
        });
    }
}
