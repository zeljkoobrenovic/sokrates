package nl.obren.sokrates.sourcecode.stats;

import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.core.AnalysisConfig;
import nl.obren.sokrates.sourcecode.threshold.Thresholds;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SourceFileComplexityDistributionTest {

    private static SourceFile file(String path, String extension, int linesOfCode, int units, int mcCabeSum) {
        SourceFile file = new SourceFile();
        file.setRelativePath(path);
        file.setExtension(extension);
        file.setLinesOfCode(linesOfCode);
        file.setUnitsCount(units);
        file.setUnitsMcCabeIndexSum(mcCabeSum);
        return file;
    }

    private final List<SourceFile> files = Arrays.asList(
            file("a/Simple.java", "java", 100, 4, 25),      // 1-25
            file("a/Slight.java", "java", 200, 8, 26),      // 26-50
            file("a/Medium.py", "py", 300, 10, 125),        // 51-125
            file("a/Complex.py", "py", 400, 20, 250),       // 126-250
            file("a/VeryComplex.java", "java", 900, 40, 251), // 251+
            file("a/page.html", "html", 500, 0, 0));        // no units: not classified

    @Test
    void filesAreBandedByTheirMcCabeSumAndWeightedByLinesOfCode() {
        RiskDistributionStats overall = SourceFileComplexityDistribution.overall(files, Thresholds.defaultFileComplexityThresholds());

        assertEquals(5, overall.getTotalCount());
        assertEquals(1900, overall.getTotalValue());
        assertEquals(100, overall.getNegligibleRiskValue());
        assertEquals(200, overall.getLowRiskValue());
        assertEquals(300, overall.getMediumRiskValue());
        assertEquals(400, overall.getHighRiskValue());
        assertEquals(900, overall.getVeryHighRiskValue());
    }

    @Test
    void perExtensionLeavesOutExtensionsWithoutUnits() {
        List<RiskDistributionStats> perExtension = SourceFileComplexityDistribution.perExtension(files, Thresholds.defaultFileComplexityThresholds());

        assertEquals(Arrays.asList("java", "py"), perExtension.stream().map(RiskDistributionStats::getKey).collect(java.util.stream.Collectors.toList()));
        assertEquals(3, perExtension.get(0).getTotalCount());
        assertEquals(2, perExtension.get(1).getTotalCount());
    }

    @Test
    void theDefaultBandsAreFiveTimesTheUnitBands() {
        Thresholds file = Thresholds.defaultFileComplexityThresholds();
        Thresholds unit = Thresholds.defaultConditionalComplexityThresholds();

        assertEquals(5 * unit.getLow(), file.getLow());
        assertEquals(5 * unit.getMedium(), file.getMedium());
        assertEquals(5 * unit.getHigh(), file.getHigh());
        assertEquals(5 * unit.getVeryHigh(), file.getVeryHigh());
    }

    @Test
    void theOldUnusedKeyIsIgnoredSoOlderConfigurationsGetTheFileBands() throws Exception {
        // init used to write the never-read "fileConditionalComplexityThresholds" with the unit bands.
        String json = "{\"fileConditionalComplexityThresholds\": {\"low\": 5, \"medium\": 10, \"high\": 25, \"veryHigh\": 50}}";
        AnalysisConfig config = (AnalysisConfig) new JsonMapper().getObject(json, AnalysisConfig.class);

        assertEquals(25, config.getFileComplexityThresholds().getLow());
        assertEquals(250, config.getFileComplexityThresholds().getVeryHigh());

        AnalysisConfig custom = (AnalysisConfig) new JsonMapper().getObject(
                "{\"fileComplexityThresholds\": {\"low\": 10, \"medium\": 20, \"high\": 40, \"veryHigh\": 80}}", AnalysisConfig.class);
        assertEquals(80, custom.getFileComplexityThresholds().getVeryHigh());
    }
}
