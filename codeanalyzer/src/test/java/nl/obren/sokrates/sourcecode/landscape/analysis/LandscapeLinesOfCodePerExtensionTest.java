package nl.obren.sokrates.sourcecode.landscape.analysis;

import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.PeopleConfig;
import nl.obren.sokrates.sourcecode.landscape.TeamsConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Per-extension metrics are named by the bare extension ("java"). Older reports named them
 * "  *.java"; a landscape may read both from the repositories' data.zip files, so the aggregation
 * must merge the two spellings under one bare-extension entry (main and the merged "other" category).
 */
class LandscapeLinesOfCodePerExtensionTest {

    private RepositoryAnalysisResults repository(String name, List<NumericMetric> mainLoc, List<NumericMetric> buildLoc, List<NumericMetric> otherLoc) {
        CodeAnalysisResults analysis = new CodeAnalysisResults();
        analysis.getMetadata().setName(name);
        analysis.getMainAspectAnalysisResults().setLinesOfCode(mainLoc.stream().mapToInt(m -> m.getValue().intValue()).sum());
        analysis.getMainAspectAnalysisResults().getLinesOfCodePerExtension().addAll(mainLoc);
        analysis.getBuildAndDeployAspectAnalysisResults().getLinesOfCodePerExtension().addAll(buildLoc);
        analysis.getOtherAspectAnalysisResults().getLinesOfCodePerExtension().addAll(otherLoc);
        return new RepositoryAnalysisResults(null, analysis, null);
    }

    @Test
    void legacyAndBareExtensionNamesMergeUnderTheBareExtension() {
        RepositoryAnalysisResults legacy = repository("legacy",
                List.of(new NumericMetric("  *.java", 100), new NumericMetric("  *.xml", 10)),
                List.of(new NumericMetric("  *.sh", 5)),
                List.of(new NumericMetric("  *.md", 7)));
        RepositoryAnalysisResults current = repository("current",
                List.of(new NumericMetric("java", 50), new NumericMetric("kt", 20)),
                List.of(new NumericMetric("sh", 1)),
                List.of(new NumericMetric("sh", 2)));

        LandscapeAnalysisResults landscape = new LandscapeAnalysisResults(new TeamsConfig(), new PeopleConfig());
        landscape.setRepositoryAnalysisResults(List.of(legacy, current));

        List<NumericMetric> main = landscape.getMainLinesOfCodePerExtension();
        assertEquals("[java=150, kt=20, xml=10]", describe(main));
        assertEquals(2, main.get(0).getDescription().size()); // java came from both repositories

        List<NumericMetric> other = landscape.getOtherLinesOfCodePerExtension();
        assertEquals("[sh=8, md=7]", describe(other));
    }

    private String describe(List<NumericMetric> metrics) {
        return metrics.stream().map(m -> m.getName() + "=" + m.getValue().intValue()).toList().toString();
    }
}
