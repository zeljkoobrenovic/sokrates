/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.renderingutils.x3d;

import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.renderingutils.VisualizationTemplate;
import nl.obren.sokrates.common.utils.SystemUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class X3DomExporter {
    public static final double MARGIN_RATION = 1.2;
    // The page no longer embeds one <Transform><Shape><Box> per node (that produced ~1.3MB files
    // for large repos). Instead it embeds the small per-node data (color + size, grouped by
    // component) compressed inline, and rebuilds the X3D DOM in the browser with the SAME layout
    // math as buildComponentBoxes() below. The diagram stays self-contained (works from file://).
    public static final String HTML_3D_HEADER = "<!DOCTYPE html >\n" +
            "<html>\n" +
            "    <head>\n" +
            "        <meta http-equiv=\"X-UA-Compatible\" content=\"chrome=1\" />\n" +
            "        <link rel=\"stylesheet\" type=\"text/css\" href=\"https://examples.x3dom.org/example/x3dom.css\" />\n" +
            "        <script type=\"text/javascript\" src=\"https://examples.x3dom.org/example/x3dom.js\"></script>\n" +
            "        ${sokrates-inflate-lib}\n" +
            "    </head>\n" +
            "    <style>body { font-family: Arial; margin: 20px;}</style>\n" +
            "       \n" +
            "<body>\n" +
            "\n" +
            "    <div>\n" +
            "        <p id=\"moduleInfo\" >&nbsp;</p>\n" +
            "    </div>\n" +
            "    <div>\n" +
            "    <x3d id=\"x3droot\" xmlns=\"http://www.x3dom.org/x3dom\" showStat=\"false\" showLog=\"false\" x=\"0px\" y=\"0px\" width=\"800px\" height=\"600px\">\n" +
            "        <scene>\n" +
            "            <viewpoint position='5 0 30' ></viewpoint>\n";
    public static final String HTML_3D_FOOTER = "        </scene>\n" +
            "    </x3d>\n" +
            "\n" +
            "<script>\n" +
            "        var MARGIN_RATION = " + MARGIN_RATION + ";\n" +
            "        // Mirror of the Java layout: components laid left-to-right; within a component\n" +
            "        // nodes fill a sqrt(n)+1 grid in boustrophedon (alternating) column order.\n" +
            "        function sqrtRows(n) { return Math.floor(Math.sqrt(n)) + 1; }\n" +
            "        function buildComponentBoxes(components) {\n" +
            "            var scene = document.querySelector('#x3droot scene');\n" +
            "            var position = 0.0;\n" +
            "            var index = 0;\n" +
            "            for (var c = 0; c < components.length; c++) {\n" +
            "                var nodes = components[c].nodes;\n" +
            "                var rows = sqrtRows(nodes.length);\n" +
            "                var group = document.createElement('Transform');\n" +
            "                group.setAttribute('translation', position + ' 0 0');\n" +
            "                var i = 0, flip = false;\n" +
            "                for (var row = 0; row < rows; row++) {\n" +
            "                    for (var col = 0; col < rows; col++) {\n" +
            "                        if (nodes.length > i) {\n" +
            "                            var n = nodes[i];\n" +
            "                            var height = n.size + 0.05;\n" +
            "                            var colPos = flip ? col : rows - col - 1;\n" +
            "                            var t = document.createElement('Transform');\n" +
            "                            t.setAttribute('translation', (row * MARGIN_RATION) + ' ' + (height / 2) + ' ' + (colPos * MARGIN_RATION));\n" +
            "                            var shape = document.createElement('Shape');\n" +
            "                            var app = document.createElement('Appearance');\n" +
            "                            var mat = document.createElement('Material');\n" +
            "                            mat.setAttribute('diffuseColor', n.color);\n" +
            "                            mat.setAttribute('specularColor', '.5 .5 .5');\n" +
            "                            app.appendChild(mat);\n" +
            "                            var box = document.createElement('Box');\n" +
            "                            box.setAttribute('size', '1 ' + height + ' 1');\n" +
            "                            shape.appendChild(app); shape.appendChild(box);\n" +
            "                            t.appendChild(shape); group.appendChild(t);\n" +
            "                            index++; i++;\n" +
            "                        }\n" +
            "                    }\n" +
            "                    flip = !flip;\n" +
            "                }\n" +
            "                scene.appendChild(group);\n" +
            "                position += rows + 2;\n" +
            "            }\n" +
            "            if (document.getElementById('x3droot').runtime) { document.getElementById('x3droot').runtime.showAll(); }\n" +
            "        }\n" +
            "        document.addEventListener('DOMContentLoaded', function () {\n" +
            "            buildComponentBoxes(sokratesInflate(SOKRATES_3D_DATA));\n" +
            "        });\n" +
            "        document.onload = function(event) {\n" +
            "            var x3d = document.getElementById('x3droot');\n" +
            "            if (x3d.runtime) { x3d.runtime.showAll(); }\n" +
            "        };\n" +
            "    </script>" +
            "    </div>" +
            "</body>\n" +
            "</html>\n";
    private static final Log LOG = LogFactory.getLog(X3DomExporter.class);
    private boolean groupPerComponent = true;
    private File exportFile;
    private String title = "A 3D View";
    private String description = "";

    public X3DomExporter(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public X3DomExporter(File exportFile, String title, String description) {
        this(title, description);
        this.exportFile = exportFile;
    }

    public void export(List<Unit3D> units, boolean groupPerComponent, double divideBy) {
        if (units == null) {
            return;
        }
        this.groupPerComponent = groupPerComponent;
        List<X3DomNodeInfo> nodes = new ArrayList<X3DomNodeInfo>();

        if (divideBy == 0) {
            divideBy = 1;
        }

        for (Unit3D unit : getSortedUnits(units)) {
            X3DomNodeInfo nodeInfo = new X3DomNodeInfo(unit.getName());
            nodeInfo.setSize((int) (unit.getValue().doubleValue() / divideBy));
            nodeInfo.setColor(unit.getColor());
            nodes.add(nodeInfo);
        }

        exportNodesPerComponent(nodes);
    }

    private List<Unit3D> getSortedUnits(List<Unit3D> units) {
        List<Unit3D> sortedUnits = new ArrayList<>();
        units.forEach(sortedUnits::add);
        Collections.sort(sortedUnits, (o1, o2) -> o1.getValue().doubleValue() > o2.getValue().doubleValue()
                ? -1
                : o1.getValue().doubleValue() < o2.getValue().doubleValue() ? 1 : 0);
        return sortedUnits;
    }

    public void exportNodesPerComponent(List<X3DomNodeInfo> nodes) {
        try {
            File file = exportFile != null ? exportFile : File.createTempFile("3d_", ".html");

            // Serialize the per-node data grouped by component (color + size only — everything else
            // in the X3D markup is positional and recomputed in the browser). Compressed inline.
            Map<String, List<X3DomNodeInfo>> nodesPerComponent = getNodesPerComponent(nodes);
            List<Map<String, Object>> components = new ArrayList<>();
            for (String component : nodesPerComponent.keySet()) {
                List<Map<String, Object>> nodeData = new ArrayList<>();
                for (X3DomNodeInfo node : nodesPerComponent.get(component)) {
                    Map<String, Object> n = new LinkedHashMap<>();
                    n.put("color", node.getColor());
                    n.put("size", node.getSize());
                    nodeData.add(n);
                }
                Map<String, Object> comp = new LinkedHashMap<>();
                comp.put("nodes", nodeData);
                components.add(comp);
            }
            String json = new JsonGenerator().generate(components);
            String dataScript = "<script>var SOKRATES_3D_DATA = \"" + VisualizationTemplate.deflateBase64(json) + "\";</script>\n";

            String headSectionFragment = "<h1>" + title + "</h1>";
            headSectionFragment += "<p>" + description + "</p>";

            String html = "\n" + HTML_3D_HEADER.replace("${sokrates-inflate-lib}", VisualizationTemplate.inflateLib())
                    + headSectionFragment
                    + dataScript
                    + HTML_3D_FOOTER;
            FileUtils.write(file, html, UTF_8);
            if (exportFile == null) SystemUtils.openFile(file);
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    private Map<String, List<X3DomNodeInfo>> getNodesPerComponent(List<X3DomNodeInfo> nodes) {
        Map<String, List<X3DomNodeInfo>> nodesPerComponent = new HashMap<String, List<X3DomNodeInfo>>();
        for (X3DomNodeInfo node : nodes) {
            String component = groupPerComponent ? node.getComponent() : "";
            List<X3DomNodeInfo> list;
            if (nodesPerComponent.containsKey(component)) {
                list = nodesPerComponent.get(component);
            } else {
                list = new ArrayList<>();
                nodesPerComponent.put(component, list);
            }
            list.add(node);
        }
        return nodesPerComponent;
    }
}
