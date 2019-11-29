package nl.obren.sokrates.common.renderingutils.x3d;

import nl.obren.sokrates.common.utils.SystemUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class X3DomExporter {
    public static final String HTML_3D_HEADER = "<!DOCTYPE html >\n" +
            "<html>\n" +
            "    <head>\n" +
            "        <meta http-equiv=\"X-UA-Compatible\" content=\"chrome=1\" />\n" +
            "        <link rel=\"stylesheet\" type=\"text/css\" href=\"https://examples.x3dom.org/example/x3dom.css\" />\n" +
            "        <script type=\"text/javascript\" src=\"https://examples.x3dom.org/example/x3dom.js\"></script>\n" +
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
            "        document.onload = function(event) {\n" +
            "            var x3d = document.getElementById('x3droot');\n" +
            "            x3d.runtime.showAll();\n" +
            "        };\n" +
            "    </script>" +
            "    </div>" +
            "</body>\n" +
            "</html>\n";
    public static final double MARGIN_RATION = 1.2;
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
            StringBuilder body = new StringBuilder();
            Map<String, List<X3DomNodeInfo>> nodesPerComponent = getNodesPerComponent(nodes);
            double position = 0.0;
            for (String component : nodesPerComponent.keySet()) {
                List<X3DomNodeInfo> nodesInComponent = nodesPerComponent.get(component);
                body.append("<Transform translation=\"" + position + " 0 0\">\n");
                body.append(getX3DBody(nodesInComponent));
                body.append("</Transform>\n");
                position += getSqrtRows(nodesInComponent) + 2;
            }
            String headSectionFragment = "<h1>" + title + "</h1>";
            headSectionFragment += "<p>" + description + "</p>";
            FileUtils.write(file, "\n" + HTML_3D_HEADER
                    + headSectionFragment
                    + body.toString()
                    + HTML_3D_FOOTER, UTF_8);
            if (exportFile == null) SystemUtils.openFile(file);
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    private String getX3DBody(List<X3DomNodeInfo> nodes) {
        StringBuilder body = new StringBuilder();
        int index = 0;
        int rows = getSqrtRows(nodes);
        boolean flipDirection = false;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < rows; col++) {
                if (nodes.size() > index) {
                    body.append(get3DBody(index, row, flipDirection ? col : rows - col - 1, nodes.get(index)) + "\n");
                    index++;
                }
            }
            flipDirection = !flipDirection;
        }
        return body.toString();
    }

    private int getSqrtRows(List<X3DomNodeInfo> nodes) {
        return (int) Math.sqrt(nodes.size()) + 1;
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

    private String get3DBody(int index, int row, int col, X3DomNodeInfo node) {
        double height = node.getSize() + 0.05;
        return "<Transform DEF=\"transform" + index + "\" translation=\"" + row * MARGIN_RATION + " " + (height / 2) + " " + col * MARGIN_RATION + "\">\n" +
                "    <Shape DEF=\"sphereShape\" onclick=\"" + "" + "\">\n" +
                "        <Appearance DEF=\"sphereApp\">\n" +
                "            <Material diffuseColor=\"" + node.getColor() + "\" specularColor=\".5 .5 .5\" />\n" +
                "        </Appearance>\n" +
                "        <Box size=\"1 " + height + " 1\" DEF=\"node" + index + "\" />\n" +
                "    </Shape>\n" +
                "</Transform>";
    }
}


