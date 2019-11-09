package nl.obren.sokrates.common.renderingutils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;

public final class GraphvizSettings {
    public static final String DEFAULT_NODE_COLOR = "#345386";
    public static final String DEFAULT_EDGE_COLOR = "lightgrey";
    public static final String DEFAULT_FONT_NAME = "Tahoma";
    public static final String DEFAULT_NODE_HEADER = "    node [\n" +
            "        shape=\"box\";\n" +
            "        style=\"filled\";\n" +
            "        fontsize=\"13\";\n" +
            "        fontname=\"" + DEFAULT_FONT_NAME + "\";\n" +
            "        fontcolor=\"white\";\n" +
            "        fillcolor=\"" + DEFAULT_NODE_COLOR + "\";\n" +
            "    ]\n";
    public static final String DEFAULT_EDGE_HEADER = "    edge [\n" +
            "        color=\"" + DEFAULT_EDGE_COLOR + "\";\n" +
            "        fontsize=\"8\";\n" +
            "        penwidth=\"2\";\n" +
            "        fontname=\"" + DEFAULT_FONT_NAME + "\";\n" +
            "    ]\n";

    public static final String DEFAULT_GRAPH_HEADER = "graph G {\n" +
            "    compound=\"true\";\n" +
            "    rankdir=\"TB\";\n" +
            "    bgcolor=\"white\";\n" +
            DEFAULT_NODE_HEADER +
            DEFAULT_EDGE_HEADER;

    public static final String DEFAULT_DIGRAPH_HEADER = "digraph G {\n" +
            "    compound=\"true\";\n" +
            "    rankdir=\"LR\";\n" +
            "    bgcolor=\"white\";\n" +
            DEFAULT_NODE_HEADER +
            DEFAULT_EDGE_HEADER;

    private static final Log LOG = LogFactory.getLog(GraphvizSettings.class);
    private static final String POSSIBLE_DOT_PATHS[] = {"/opt/local/bin/dot", "/usr/local/bin/dot", "/usr/bin/dot",
            "c:\\Program Files\\Graphviz*\\dot.exe", "c:\\Program Files (x86)\\Graphviz*\\dot.exe"};
    private static GraphvizSettings graphvizSetting;

    private GraphvizSettings() {
    }

    public static GraphvizSettings getInstance() {
        if (graphvizSetting == null) {
            graphvizSetting = new GraphvizSettings();
        }

        return graphvizSetting;
    }

    public static String getGraphVizDotPath() {
        String path = getSystemVariableOrProperty("GRAPHVIZ_DOT", null);
        if (path != null) {
            return path;
        } else {
            for (String possibleDotPath : POSSIBLE_DOT_PATHS) {
                File file = new File(possibleDotPath);
                if (file.exists()) {
                    return file.getAbsolutePath();
                }
            }
        }

        LOG.error("Cannot file the Graphviz dot program. Set the system variable GRAPHVIZ_DOT "
                + "to point to the Graphviz dot executable file.");
        return null;
    }

    private static String getSystemVariableOrProperty(String property, String defaultValue) {
        String value = System.getenv(property);

        if (StringUtils.isBlank(value)) {
            value = System.getProperty(property);
            if (StringUtils.isBlank(value)) {
                value = defaultValue;
            }
        }

        if (value == null) {
            return value;
        }

        if (value.endsWith("/") || value.endsWith("\\")) {
            value = value.substring(0, value.length() - 1);
        }

        return value.trim();
    }
}
