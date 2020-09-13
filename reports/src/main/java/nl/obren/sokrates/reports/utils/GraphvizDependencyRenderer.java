/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.utils;

import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;

public class GraphvizDependencyRenderer {

    public static String REPORTS_HTML_HEADER = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <style type=\"text/css\" media=\"screen\">\n" +
            "        body {\n" +
            "            font-family: Optima, Segoe, Segoe UI, Candara, Calibri, Arial, sans-serif;\n" +
            "        }\n" +
            "    </style>\n" +
            "</head>\n";
    private String orientation = "TB";
    private StringBuilder body = new StringBuilder();
    private String type = "digraph";
    private String arrow = "->";
    private String arrowColor = "deepskyblue4";
    private String defaultNodeFillColor = "grey";
    private int maxNumberOfDependencies;

    public GraphvizDependencyRenderer() {
    }

    public static String encodeLabel(String label) {
        return label.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\t", " ");
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

    private static boolean isCyclic(List<ComponentDependency> componentDependencies, ComponentDependency dependency) {
        ComponentDependency reverseDependency = new ComponentDependency(dependency.getToComponent(), dependency.getFromComponent());
        for (ComponentDependency componentDependency : componentDependencies) {
            if (componentDependency.getFromComponent().equals(reverseDependency.getFromComponent())
                    && componentDependency.getToComponent().equals(reverseDependency.getToComponent())) {
                return true;
            }
        }
        return false;
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

    private String getHeader() {
        return type + " G {\n" +
                "    compound=\"true\"\n" +
                "    rankdir=\"" + orientation + "\"\n" +
                "    bgcolor=\"white\"\n" +
                "    node [\n" +
                "        fixedsize=\"false\"\n" +
                "        fontname=\"Tahoma\"\n" +
                "        color=\"white\"\n" +
                "        fillcolor=\""+ defaultNodeFillColor +"\"\n" +
                "        fontcolor=\"black\"\n" +
                "        shape=\"box\"\n" +
                "        style=\"filled\"\n" +
                "        penwidth=\"1.0\"\n" +
                "    ]\n" +
                "    edge [\n" +
                "        fontname=\"Arial\"\n" +
                "        color=\"" + arrowColor + "\"\n" +
                "        fontcolor=\"black\"\n" +
                "        fontsize=\"12\"\n" +
                "        arrowsize=\"0.5\"\n" +
                "        penwidth=\"1.0\"\n" +
                "    ]\n";
    }

    public String getGraphvizContent(List<String> allComponents, List<ComponentDependency> componentDependencies) {
        int maxCount = getMaxDependencyCount(componentDependencies);
        StringBuilder graphviz = new StringBuilder();
        graphviz.append(getHeader());

        graphviz.append("\n");
        allComponents.stream().filter(c -> StringUtils.isNotBlank(c)).forEach(c -> {
            graphviz.append("    \"" + encodeLabel(c) + "\" [fillcolor=\"deepskyblue2\"];\n");
        });
        graphviz.append("\n");

        List<ComponentDependency> renderDependencies = componentDependencies;
        Collections.sort(renderDependencies, (a, b) -> b.getCount() - a.getCount());

        if (maxNumberOfDependencies > 0 && renderDependencies.size() > maxNumberOfDependencies) {
            renderDependencies = renderDependencies.subList(0, maxNumberOfDependencies);
        }

        renderDependencies.stream()
                .filter(d -> StringUtils.isNotBlank(d.getFromComponent()) && StringUtils.isNotBlank(d.getToComponent()))
                .forEach(componentDependency -> {
                    int thickness = getThickness(componentDependency, maxCount);
                    graphviz.append("    \"" + encodeLabel(componentDependency.getFromComponent())
                            + "\" " + arrow + " \""
                            + encodeLabel(componentDependency.getToComponent()) + "\""
                            + " [label=\" " + encodeLabel(getLabel(componentDependency))
                            + " \", penwidth=\"" + Math.max(1, thickness) + "\""
                            + (isCyclic(componentDependencies, componentDependency) ? ", color=\"crimson\"" : "")
                            + "];\n");
                });
        graphviz.append("\n}");

        return graphviz.toString();
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

    public String getDefaultNodeFillColor() {
        return defaultNodeFillColor;
    }

    public void setDefaultNodeFillColor(String defaultNodeFillColor) {
        this.defaultNodeFillColor = defaultNodeFillColor;
    }

    public void setMaxNumberOfDependencies(int maxNumberOfDependencies) {
        this.maxNumberOfDependencies = maxNumberOfDependencies;
    }

    public int getMaxNumberOfDependencies() {
        return maxNumberOfDependencies;
    }
}
