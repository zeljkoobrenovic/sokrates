package nl.obren.sokrates.reports.landscape.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.landscape.SokratesRepositoryLink;
import nl.obren.sokrates.sourcecode.landscape.analysis.RepositoryAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryExportTest {

    private RepositoryAnalysisResults sampleRepository() {
        CodeAnalysisResults analysis = new CodeAnalysisResults();
        analysis.setCodeConfiguration(new CodeConfiguration());
        analysis.getMetadata().setName("group/repo");

        AspectAnalysisResults main = new AspectAnalysisResults("main");
        main.setLinesOfCode(12000);
        main.setLinesOfCodePerExtension(new ArrayList<>(Arrays.asList(
                new NumericMetric("*.java", 10000),
                new NumericMetric("*.xml", 2000))));
        analysis.setMainAspectAnalysisResults(main);

        AspectAnalysisResults test = new AspectAnalysisResults("test");
        test.setLinesOfCode(3000);
        analysis.setTestAspectAnalysisResults(test);

        AspectAnalysisResults generated = new AspectAnalysisResults("generated");
        generated.setLinesOfCode(100);
        analysis.setGeneratedAspectAnalysisResults(generated);

        AspectAnalysisResults build = new AspectAnalysisResults("build");
        build.setLinesOfCode(50);
        analysis.setBuildAndDeployAspectAnalysisResults(build);

        AspectAnalysisResults other = new AspectAnalysisResults("other");
        other.setLinesOfCode(20);
        analysis.setOtherAspectAnalysisResults(other);

        SokratesRepositoryLink link = new SokratesRepositoryLink();
        link.setHtmlReportsRoot("repos/repo");

        return new RepositoryAnalysisResults(link, analysis, new ArrayList<>());
    }

    @Test
    void exportsBasicAspectLinesOfCodeAndMainLang() {
        RepositoryExport export = new RepositoryExport(sampleRepository());

        assertEquals(12000, export.getMainLinesOfCode());
        assertEquals(3000, export.getTestLinesOfCode());
        assertEquals("java", export.getMainLang());
    }

    @Test
    void otherLinesOfCodeIsNotOverwrittenByTheOtherAspect() {
        // Regression: the constructor used to assign other-aspect LOC to generatedLinesOfCode,
        // leaving otherLinesOfCode at 0 and clobbering generatedLinesOfCode.
        RepositoryExport export = new RepositoryExport(sampleRepository());

        assertEquals(100, export.getGeneratedLinesOfCode(), "generated LOC must keep the generated aspect value");
        assertEquals(20, export.getOtherLinesOfCode(), "other LOC must come from the other aspect");
    }

    @Test
    void exportsHistoryDerivedFields() {
        RepositoryExport export = new RepositoryExport(sampleRepository());

        assertEquals(0, export.getAgeInDays());
        assertEquals(0, export.getAgeYears());
        assertNotNull(export.getFirstDate());
        assertNotNull(export.getTags());
        assertTrue(export.getTags().isEmpty());
    }

    @Test
    void exportsMetricsBlock() {
        RepositoryExport export = new RepositoryExport(sampleRepository());

        RepositoryReportData.Metrics metrics = export.getRepositoryMetrics();
        assertNotNull(metrics);
        assertNotNull(metrics.getFileSize());
        assertNotNull(metrics.getUnitSize());
        assertNotNull(metrics.getConditionalComplexity());
        assertNotNull(metrics.getNewness());
        assertNotNull(metrics.getFreshness());
        assertNotNull(metrics.getUpdateFrequency());
        assertNotNull(metrics.getControls());
    }

    @Test
    void serializesToValidJsonWithExpectedFields() throws Exception {
        RepositoryExport export = new RepositoryExport(sampleRepository());
        String json = new JsonGenerator().generateCompressed(export);

        JsonNode node = new ObjectMapper().readTree(json);
        assertEquals("group/repo", node.get("metadata").get("name").asText());
        assertEquals(12000, node.get("mainLinesOfCode").asInt());
        assertEquals(100, node.get("generatedLinesOfCode").asInt());
        assertEquals(20, node.get("otherLinesOfCode").asInt());
        assertEquals("java", node.get("mainLang").asText());
        assertTrue(node.has("ageYears"));
        assertTrue(node.has("repositoryMetrics"));
        assertTrue(node.get("repositoryMetrics").has("fileSize"));
        assertTrue(node.has("weeks"));
        assertTrue(node.has("years"));
    }
}
