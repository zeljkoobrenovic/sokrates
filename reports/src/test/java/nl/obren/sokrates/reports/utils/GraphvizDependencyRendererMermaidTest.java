package nl.obren.sokrates.reports.utils;

import nl.obren.sokrates.sourcecode.aspects.ComponentGroup;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GraphvizDependencyRendererMermaidTest {

    private ComponentDependency dep(String from, String to, int count) {
        ComponentDependency d = new ComponentDependency(from, to);
        d.setCount(count);
        return d;
    }

    @Test
    void emitsFlowchartHeaderWithOrientation() {
        GraphvizDependencyRenderer r = new GraphvizDependencyRenderer();
        r.setOrientation("LR");
        String m = r.getMermaidContent(Arrays.asList("A", "B"), Arrays.asList(dep("A", "B", 1)));
        assertTrue(m.startsWith("flowchart LR"), m);
    }

    @Test
    void declaresNodesWithSyntheticIdsAndRealLabels() {
        GraphvizDependencyRenderer r = new GraphvizDependencyRenderer();
        String m = r.getMermaidContent(Arrays.asList("Comp One", "Comp Two"),
                Collections.singletonList(dep("Comp One", "Comp Two", 1)));
        // node names with spaces must be in the label, not the id
        assertTrue(m.contains("n0[\"Comp One\"]"), m);
        assertTrue(m.contains("n1[\"Comp Two\"]"), m);
        // directed edge between the synthetic ids
        assertTrue(m.contains("n0 -->|\" 1 \"| n1") || m.contains("n0 -->|\"1\"| n1"), m);
    }

    @Test
    void directedVsUndirectedConnector() {
        GraphvizDependencyRenderer digraph = new GraphvizDependencyRenderer();
        digraph.setTypeDigraph();
        assertTrue(digraph.getMermaidContent(Arrays.asList("A", "B"), Collections.singletonList(dep("A", "B", 1)))
                .contains("-->"), "digraph should use -->");

        GraphvizDependencyRenderer graph = new GraphvizDependencyRenderer();
        graph.setTypeGraph();
        String m = graph.getMermaidContent(Arrays.asList("A", "B"), Collections.singletonList(dep("A", "B", 1)));
        assertTrue(m.contains("---"), "graph should use undirected ---");
        assertFalse(m.contains("-->"), "graph should not use directed arrows");
    }

    @Test
    void emitsOneLinkStylePerEdgeIndexedInOrder() {
        GraphvizDependencyRenderer r = new GraphvizDependencyRenderer();
        // counts chosen so render order (count desc) is A->B (5), C->D (3)
        List<ComponentDependency> deps = Arrays.asList(dep("A", "B", 5), dep("C", "D", 3));
        String m = r.getMermaidContent(Arrays.asList("A", "B", "C", "D"), deps);
        assertTrue(m.contains("linkStyle 0 "), m);
        assertTrue(m.contains("linkStyle 1 "), m);
        assertFalse(m.contains("linkStyle 2 "), "only two edges -> indices 0 and 1");
        // thicker edge first (higher count rendered first)
        int idx0 = m.indexOf("linkStyle 0 ");
        int idx1 = m.indexOf("linkStyle 1 ");
        assertTrue(idx0 < idx1);
    }

    @Test
    void cyclicEdgesUseCyclicColor() {
        GraphvizDependencyRenderer r = new GraphvizDependencyRenderer();
        r.setArrowColor("#00688b");
        r.setCyclicArrowColor("#DC143C");
        // A<->B is cyclic, both directions present
        List<ComponentDependency> deps = Arrays.asList(dep("A", "B", 2), dep("B", "A", 2));
        String m = r.getMermaidContent(Arrays.asList("A", "B"), deps);
        assertTrue(m.toUpperCase().contains("STROKE:#DC143C"), "cyclic edges should use the cyclic colour\n" + m);
    }

    @Test
    void groupsBecomeSubgraphs() {
        GraphvizDependencyRenderer r = new GraphvizDependencyRenderer();
        ComponentGroup group = new ComponentGroup("My Group", Arrays.asList("A", "B"));
        String m = r.getMermaidContent(Collections.emptyList(),
                Collections.singletonList(dep("A", "B", 1)), Collections.singletonList(group));
        assertTrue(m.contains("subgraph cluster_1[\"My Group (2)\"]"), m);
        assertTrue(m.contains("end"), m);
    }

    @Test
    void highlightsExplicitComponents() {
        GraphvizDependencyRenderer r = new GraphvizDependencyRenderer();
        String m = r.getMermaidContent(Arrays.asList("A", "B"), Collections.singletonList(dep("A", "B", 1)));
        assertTrue(m.contains("classDef highlighted"), m);
        assertTrue(m.contains("class n0,n1 highlighted") || m.contains("class n0 highlighted") || m.contains("class n1 highlighted"), m);
    }

    @Test
    void respectsMaxNumberOfDependencies() {
        GraphvizDependencyRenderer r = new GraphvizDependencyRenderer();
        r.setMaxNumberOfDependencies(1);
        List<ComponentDependency> deps = Arrays.asList(dep("A", "B", 5), dep("C", "D", 3));
        String m = r.getMermaidContent(Arrays.asList("A", "B", "C", "D"), deps);
        assertTrue(m.contains("linkStyle 0 "), m);
        assertFalse(m.contains("linkStyle 1 "), "max 1 dependency -> only one edge rendered");
    }

    @Test
    void reverseDirectionFlipsEndpoints() {
        GraphvizDependencyRenderer r = new GraphvizDependencyRenderer();
        r.setReverseDirection(true);
        String m = r.getMermaidContent(Arrays.asList("A", "B"), Collections.singletonList(dep("A", "B", 1)));
        // A=n0, B=n1; reversed edge goes n1 --> n0
        assertTrue(m.contains("n1 -->|") && m.contains("| n0"), m);
    }

    @Test
    void escapesQuotesInLabels() {
        GraphvizDependencyRenderer r = new GraphvizDependencyRenderer();
        String m = r.getMermaidContent(Collections.singletonList("say \"hi\""),
                Collections.emptyList());
        assertFalse(m.contains("\"say \"hi\"\""), "raw inner quotes would break the label");
        assertTrue(m.contains("&quot;"), m);
    }
}
