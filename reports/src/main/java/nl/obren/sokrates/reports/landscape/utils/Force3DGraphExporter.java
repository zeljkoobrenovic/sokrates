package nl.obren.sokrates.reports.landscape.utils;

import nl.obren.sokrates.common.renderingutils.VisualizationTemplate;
import nl.obren.sokrates.common.renderingutils.force3d.Force3DLink;
import nl.obren.sokrates.common.renderingutils.force3d.Force3DNode;
import nl.obren.sokrates.common.renderingutils.force3d.Force3DObject;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Force3DGraphExporter {

    public static final int MAX_SIZE = 100000;

    public String export2D3DForceGraph(List<ComponentDependency> componentDependencies, File reportsFolder, String graphId) {
        Force3DObject forceGraphObject = new Force3DObject();
        Map<String, Integer> names = new HashMap<>();
        componentDependencies.stream().limit(MAX_SIZE).forEach(dependency -> {
            String from = dependency.getFromComponent();
            String to = dependency.getToComponent();
            if (names.containsKey(from)) {
                names.put(from, names.get(from) + 1);
            } else {
                names.put(from, 1);
            }
            if (names.containsKey(to)) {
                names.put(to, names.get(to) + 1);
            } else {
                names.put(to, 1);
            }
            forceGraphObject.getLinks().add(new Force3DLink(from, to, dependency.getCount()));
            forceGraphObject.getLinks().add(new Force3DLink(to, from, dependency.getCount()));
        });
        names.keySet().forEach(key -> {
            forceGraphObject.getNodes().add(new Force3DNode(key, names.get(key)));
        });
        File folder = new File(reportsFolder, "visuals");
        folder.mkdirs();
        String fileName2D = graphId + "_force_2d.html";
        String fileName3D = graphId + "_force_3d.html";
        try {
            FileUtils.write(new File(folder, fileName2D), new VisualizationTemplate().render2DForceGraph(forceGraphObject), UTF_8);
            FileUtils.write(new File(folder, fileName3D), new VisualizationTemplate().render3DForceGraph(forceGraphObject), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "visuals/" + fileName3D;
    }
}
