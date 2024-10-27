package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.renderingutils.VisualizationTemplate;
import nl.obren.sokrates.common.renderingutils.force3d.Force3DLink;
import nl.obren.sokrates.common.renderingutils.force3d.Force3DNode;
import nl.obren.sokrates.common.renderingutils.force3d.Force3DObject;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.util.Pair;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ForceGraphExporter {

    public static final int MAX_DEPENDECIES_GRAPH_SIZE = 10000;

    public static Pair<String,String> export3DForceGraph(List<ComponentDependency> componentDependencies, File reportsFolder, String graphId) {
        Force3DObject forceGraphObject = new Force3DObject();
        Map<String, Integer> names = new HashMap<>();
        componentDependencies.stream().limit(MAX_DEPENDECIES_GRAPH_SIZE).forEach(dependency -> {
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
        String visualsPath = "html/visuals";
        File folder = new File(reportsFolder, visualsPath);
        folder.mkdirs();
        String fileName2D = graphId + "_force_2d.html";
        String fileName3D = graphId + "_force_3d.html";
        try {
            FileUtils.write(new File(folder, fileName2D), new VisualizationTemplate().render2DForceGraph(forceGraphObject), UTF_8);
            FileUtils.write(new File(folder, fileName3D), new VisualizationTemplate().render3DForceGraph(forceGraphObject), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Pair.create("visuals/" + fileName2D, "visuals/" + fileName3D);
    }
}
