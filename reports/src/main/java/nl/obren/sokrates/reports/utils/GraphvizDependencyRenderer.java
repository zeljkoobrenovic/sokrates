/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.utils;

import nl.obren.sokrates.common.renderingutils.ReportTheme;
import nl.obren.sokrates.sourcecode.aspects.ComponentGroup;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GraphvizDependencyRenderer {

    public static String REPORTS_HTML_HEADER = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            ReportTheme.headBlock() +
            "</head>\n";
    private String orientation = "TB";
    private StringBuilder body = new StringBuilder();
    private String type = "digraph";
    private String arrow = "->";
    private String arrowColor = "#00688b";
    private String cyclicArrowColor = "#DC143C";
    private String defaultNodeFillColor = "grey";
    private int maxNumberOfDependencies;
    private boolean reverseDirection = false;

    public GraphvizDependencyRenderer() {
    }

    private static int getThickness(ComponentDependency componentDependency, int maxCount) {
        int thickness;
        if (maxCount <= 10) {
            thickness = componentDependency.getCount();
        } else {
            thickness = (int) (10.0 * componentDependency.getCount() / maxCount);
        }
        return thickness;
    }

    private static String getLabel(ComponentDependency componentDependency) {
        return componentDependency.getText() == null
                ? componentDependency.getCount() + ""
                : componentDependency.getText();
    }

    private static int getMaxDependencyCount(List<ComponentDependency> values) {
        int max[] = {0};
        values.forEach(value -> max[0] = Math.max(max[0], value.getCount()));
        return max[0];
    }

    public String getOrientation() {
        return orientation;
    }

    public void setOrientation(String orientation) {
        this.orientation = orientation;
    }

    // Mermaid node ids must be safe tokens (no quotes/spaces/special chars), unlike DOT's quoted
    // labels. We assign synthetic ids (n0, n1, ...) and keep the real component name in the node
    // label only. Labels are wrapped in double quotes; the characters Mermaid would otherwise read
    // as syntax or, since it renders labels as HTML, as markup are written as Mermaid entity codes
    // (#quot; #lt; #gt; #amp;), which mermaid.js decodes back to the literal character. The names
    // are repository-controlled (component/folder/file names), so the definition must never carry
    // a raw < or >. Known limit: mermaid's entity pre-pass first strips the trailing ";" of any
    // "style…:…#…;" run on a line, so a label such as "freestyle:R&D" renders as "freestyle:R#ampD".
    // Cosmetic and rare; the alternative (raw characters) is what this method exists to prevent.
    static String escapeMermaidLabel(String label) {
        return label.replace("&", "#amp;")
                .replace("\"", "#quot;")
                .replace("<", "#lt;")
                .replace(">", "#gt;");
    }

    // Map the few Graphviz X11 colour names used by callers to CSS-valid colors; hex values and
    // standard CSS names pass through unchanged.
    private static String toCssColor(String color) {
        if (color == null) {
            return "grey";
        }
        switch (color) {
            case "deepskyblue2":
                return "#00b2ee";
            case "grey":
                return "#808080";
            default:
                return color;
        }
    }

    public String getMermaidContent(List<String> allComponents, List<ComponentDependency> componentDependencies) {
        return this.getMermaidContent(allComponents, componentDependencies, new ArrayList<>());
    }

    public String getMermaidContent(List<String> allComponents, List<ComponentDependency> componentDependencies, List<ComponentGroup> groups) {
        int maxCount = getMaxDependencyCount(componentDependencies);
        StringBuilder mermaid = new StringBuilder();
        mermaid.append("flowchart ").append(orientation).append("\n");

        List<ComponentDependency> renderDependencies = renderedDependencies(componentDependencies);
        Map<String, String> nodeIds = collectNodeIds(allComponents, groups, renderDependencies);

        // Components passed explicitly in allComponents are the highlighted ("deepskyblue2") nodes.
        Set<String> highlighted = new HashSet<>();
        allComponents.stream().filter(StringUtils::isNotBlank).forEach(highlighted::add);

        // Pre-index directed edges so the cyclic check is O(1).
        Set<String> edgeKeys = new HashSet<>();
        componentDependencies.forEach(d -> edgeKeys.add(d.getFromComponent() + "::" + d.getToComponent()));

        appendSubgraphs(mermaid, groups, nodeIds);
        appendUngroupedNodes(mermaid, groups, nodeIds);
        List<String> linkStyles = appendEdges(mermaid, renderDependencies, nodeIds, edgeKeys, maxCount);
        appendNodeClasses(mermaid, nodeIds, highlighted);
        linkStyles.forEach(ls -> mermaid.append(ls).append("\n"));

        return mermaid.toString();
    }

    /** The dependencies to draw: count-desc, capped at maxNumberOfDependencies when set. */
    private List<ComponentDependency> renderedDependencies(List<ComponentDependency> componentDependencies) {
        List<ComponentDependency> renderDependencies = new ArrayList<>(componentDependencies);
        Collections.sort(renderDependencies, (a, b) -> b.getCount() - a.getCount());
        if (maxNumberOfDependencies > 0 && renderDependencies.size() > maxNumberOfDependencies) {
            renderDependencies = renderDependencies.subList(0, maxNumberOfDependencies);
        }
        return renderDependencies;
    }

    /** name -> synthetic node id for every explicit component, group member and rendered edge endpoint; LinkedHashMap keeps declaration order stable/deterministic. */
    private Map<String, String> collectNodeIds(List<String> allComponents, List<ComponentGroup> groups, List<ComponentDependency> renderDependencies) {
        Map<String, String> nodeIds = new LinkedHashMap<>();
        // Collect every node name up front (explicit components, group members, and the endpoints
        // of the rendered edges) so each node gets a declared id + label before it is referenced.
        allComponents.stream().filter(StringUtils::isNotBlank).forEach(c -> idFor(nodeIds, c));
        groups.forEach(g -> g.getComponentNames().stream().filter(StringUtils::isNotBlank).forEach(c -> idFor(nodeIds, c)));
        renderDependencies.forEach(d -> {
            if (StringUtils.isNotBlank(d.getFromComponent())) idFor(nodeIds, d.getFromComponent());
            if (StringUtils.isNotBlank(d.getToComponent())) idFor(nodeIds, d.getToComponent());
        });
        return nodeIds;
    }

    /** Subgraphs (clusters) for component groups. */
    private void appendSubgraphs(StringBuilder mermaid, List<ComponentGroup> groups, Map<String, String> nodeIds) {
        int[] clusterId = {0};
        groups.stream().filter(g -> StringUtils.isNotBlank(g.getName())).forEach(g -> {
            clusterId[0] += 1;
            mermaid.append("    subgraph cluster_").append(clusterId[0])
                    .append("[\"").append(escapeMermaidLabel(g.getName() + " (" + g.getComponentNames().size() + ")")).append("\"]\n");
            g.getComponentNames().stream().filter(StringUtils::isNotBlank).forEach(c ->
                    mermaid.append("        ").append(nodeDeclaration(idFor(nodeIds, c), c)).append("\n"));
            mermaid.append("    end\n");
        });
    }

    /** Declares the nodes that are not inside a subgraph (explicit components + edge endpoints). Mermaid tolerates re-declaration, but each id is declared once for clarity. */
    private void appendUngroupedNodes(StringBuilder mermaid, List<ComponentGroup> groups, Map<String, String> nodeIds) {
        Set<String> declaredInGroup = new HashSet<>();
        groups.forEach(g -> g.getComponentNames().forEach(declaredInGroup::add));
        nodeIds.keySet().stream().filter(name -> !declaredInGroup.contains(name)).forEach(name ->
                mermaid.append("    ").append(nodeDeclaration(nodeIds.get(name), name)).append("\n"));
    }

    /**
     * Edges, in deterministic (count-desc) order. Returns the linkStyle lines: linkStyle targets edges by
     * their definition index, so the running edge index sets per-edge thickness/colour.
     */
    private List<String> appendEdges(StringBuilder mermaid, List<ComponentDependency> renderDependencies, Map<String, String> nodeIds, Set<String> edgeKeys, int maxCount) {
        String connector = "graph".equals(type) ? "---" : "-->";
        List<String> linkStyles = new ArrayList<>();
        int edgeIndex = 0;
        for (ComponentDependency componentDependency : renderDependencies) {
            if (StringUtils.isBlank(componentDependency.getFromComponent()) || StringUtils.isBlank(componentDependency.getToComponent())) {
                continue;
            }
            int thickness = getThickness(componentDependency, maxCount);
            String color = edgeColor(componentDependency, edgeKeys, thickness);

            String fromName = reverseDirection ? componentDependency.getToComponent() : componentDependency.getFromComponent();
            String toName = reverseDirection ? componentDependency.getFromComponent() : componentDependency.getToComponent();
            String fromId = idFor(nodeIds, fromName);
            String toId = idFor(nodeIds, toName);

            String label = escapeMermaidLabel(getLabel(componentDependency));
            mermaid.append("    ").append(fromId).append(" ").append(connector)
                    .append("|\"").append(label).append("\"| ").append(toId).append("\n");

            linkStyles.add("    linkStyle " + edgeIndex + " stroke:" + color
                    + ",stroke-width:" + Math.max(1, thickness) + "px");
            edgeIndex++;
        }
        return linkStyles;
    }

    /** The edge's own colour, else the cyclic or the plain arrow colour, with an alpha that grows with the thickness. */
    private String edgeColor(ComponentDependency componentDependency, Set<String> edgeKeys, int thickness) {
        String color = componentDependency.getColor();
        if (StringUtils.isBlank(color)) {
            color = edgeKeys.contains(componentDependency.getToComponent() + "::" + componentDependency.getFromComponent())
                    ? this.cyclicArrowColor : this.arrowColor;
        }
        int transparency = (int) (255.0 * (0.3 + 0.7 * thickness / 10.0));
        return color + String.format("%02X", transparency);
    }

    /** Node fill styling: highlighted components vs the default fill colour. */
    private void appendNodeClasses(StringBuilder mermaid, Map<String, String> nodeIds, Set<String> highlighted) {
        mermaid.append("    classDef default fill:").append(toCssColor(defaultNodeFillColor))
                .append(",stroke:#ffffff,color:#000000;\n");
        mermaid.append("    classDef highlighted fill:").append(toCssColor("deepskyblue2"))
                .append(",stroke:#ffffff,color:#000000;\n");
        List<String> highlightedIds = new ArrayList<>();
        nodeIds.forEach((name, id) -> {
            if (highlighted.contains(name)) {
                highlightedIds.add(id);
            }
        });
        if (!highlightedIds.isEmpty()) {
            mermaid.append("    class ").append(String.join(",", highlightedIds)).append(" highlighted;\n");
        }
    }

    private String nodeDeclaration(String id, String name) {
        return id + "[\"" + escapeMermaidLabel(name) + "\"]";
    }

    private static String idFor(Map<String, String> nodeIds, String name) {
        return nodeIds.computeIfAbsent(name, n -> "n" + nodeIds.size());
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getArrow() {
        return arrow;
    }

    public void setArrow(String arrow) {
        this.arrow = arrow;
    }

    public void append(String text) {
        body.append(text);
    }

    public String getHtmlContent() {
        return REPORTS_HTML_HEADER + "\n<body><div id=\"report\">\n" + "\n" + body.toString() + "</div>\n</body>\n</html>";
    }

    public GraphvizDependencyRenderer orientation(String orientation) {
        this.orientation = orientation;
        return this;
    }

    public String getArrowColor() {
        return arrowColor;
    }

    public void setArrowColor(String arrowColor) {
        this.arrowColor = arrowColor;
    }

    public String getCyclicArrowColor() {
        return cyclicArrowColor;
    }

    public void setCyclicArrowColor(String cyclicArrowColor) {
        this.cyclicArrowColor = cyclicArrowColor;
    }

    public String getDefaultNodeFillColor() {
        return defaultNodeFillColor;
    }

    public void setDefaultNodeFillColor(String defaultNodeFillColor) {
        this.defaultNodeFillColor = defaultNodeFillColor;
    }

    public int getMaxNumberOfDependencies() {
        return maxNumberOfDependencies;
    }

    public void setMaxNumberOfDependencies(int maxNumberOfDependencies) {
        this.maxNumberOfDependencies = maxNumberOfDependencies;
    }

    public boolean isReverseDirection() {
        return reverseDirection;
    }

    public void setReverseDirection(boolean reverseDirection) {
        this.reverseDirection = reverseDirection;
    }

    public void setTypeGraph() {
        setType("graph");
        setArrow("--");
    }

    public void setTypeDigraph() {
        setType("digraph");
        setArrow("->");
    }
}
