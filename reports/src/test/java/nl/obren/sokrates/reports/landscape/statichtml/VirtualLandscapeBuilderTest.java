package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.landscape.VirtualLandscapeConfig;
import nl.obren.sokrates.sourcecode.landscape.VirtualLandscapesConfig;
import nl.obren.sokrates.sourcecode.landscape.SokratesRepositoryLink;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.RepositoryAnalysisResults;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class VirtualLandscapeBuilderTest {

    private RepositoryAnalysisResults repo(String name) {
        CodeAnalysisResults analysis = new CodeAnalysisResults();
        analysis.getMetadata().setName(name);
        SokratesRepositoryLink link = new SokratesRepositoryLink(name + "/data/analysisResults.json");
        return new RepositoryAnalysisResults(link, analysis, new ArrayList<>());
    }

    private LandscapeAnalysisResults parentWith(VirtualLandscapesConfig vlConfig, String... repoNames) {
        LandscapeAnalysisResults results = new LandscapeAnalysisResults(null, null);
        LandscapeConfiguration config = new LandscapeConfiguration();
        config.setVirtualLandscapes(vlConfig);
        results.setConfiguration(config);
        for (String name : repoNames) {
            results.getRepositoryAnalysisResults().add(repo(name));
        }
        return results;
    }

    private VirtualLandscapeConfig vl(String name, List<String> include, List<String> exclude) {
        VirtualLandscapeConfig vl = new VirtualLandscapeConfig();
        vl.getMetadata().setName(name);
        vl.setIncludeRepoNamePatterns(include);
        vl.setExcludeRepoNamePatterns(exclude);
        return vl;
    }

    private List<String> names(VirtualLandscapeBuilder.VirtualLandscape vl) {
        return vl.getResults().getRepositoryAnalysisResults().stream()
                .map(r -> r.getAnalysisResults().getMetadata().getName())
                .collect(Collectors.toList());
    }

    @Test
    void noVirtualLandscapesProducesNothing() {
        LandscapeAnalysisResults parent = parentWith(new VirtualLandscapesConfig(), "a", "b");
        VirtualLandscapeBuilder builder = new VirtualLandscapeBuilder(parent);
        assertFalse(builder.hasVirtualLandscapes());
        assertTrue(builder.build().isEmpty());
    }

    @Test
    void includeAndExcludePatternsSelectMembers() {
        VirtualLandscapesConfig cfg = new VirtualLandscapesConfig();
        cfg.getLandscapes().add(vl("Datadog", Arrays.asList(".*datadog.*"), Arrays.asList(".*legacy.*")));
        LandscapeAnalysisResults parent = parentWith(cfg, "team/datadog-agent", "team/datadog-legacy", "team/other");

        List<VirtualLandscapeBuilder.VirtualLandscape> built = new VirtualLandscapeBuilder(parent).build();

        // Datadog + Remainder
        assertEquals(2, built.size());
        assertEquals("Datadog", built.get(0).getName());
        assertEquals(Arrays.asList("team/datadog-agent"), names(built.get(0)));
        assertEquals("Remainder", built.get(1).getName());
        // datadog-legacy is excluded from Datadog, so it falls into the Remainder along with "other"
        assertEquals(Arrays.asList("team/datadog-legacy", "team/other"), names(built.get(1)));
    }

    @Test
    void repositoryCanBelongToMultipleVirtualLandscapes() {
        VirtualLandscapesConfig cfg = new VirtualLandscapesConfig();
        cfg.getLandscapes().add(vl("A", Arrays.asList(".*shared.*"), new ArrayList<>()));
        cfg.getLandscapes().add(vl("B", Arrays.asList(".*shared.*"), new ArrayList<>()));
        LandscapeAnalysisResults parent = parentWith(cfg, "x/shared", "x/lonely");

        List<VirtualLandscapeBuilder.VirtualLandscape> built = new VirtualLandscapeBuilder(parent).build();

        assertEquals(Arrays.asList("x/shared"), names(built.get(0))); // A
        assertEquals(Arrays.asList("x/shared"), names(built.get(1))); // B
        // x/shared was assigned, so only x/lonely is in the Remainder
        assertEquals(Arrays.asList("x/lonely"), names(built.get(2)));
    }

    @Test
    void remainderIsEmptyWhenEverythingMatches() {
        VirtualLandscapesConfig cfg = new VirtualLandscapesConfig();
        cfg.getLandscapes().add(vl("All", Arrays.asList(".*"), new ArrayList<>()));
        LandscapeAnalysisResults parent = parentWith(cfg, "a", "b");

        List<VirtualLandscapeBuilder.VirtualLandscape> built = new VirtualLandscapeBuilder(parent).build();
        assertEquals(2, built.size());
        assertEquals(2, built.get(0).getResults().getRepositoryAnalysisResults().size());
        assertTrue(built.get(1).getResults().getRepositoryAnalysisResults().isEmpty());
    }
}
