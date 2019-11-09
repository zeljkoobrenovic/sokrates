package nl.obren.sokrates.common.renderingutils;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.GraphvizJdkEngine;
import guru.nidi.graphviz.engine.Renderer;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class GraphvizUtil {
    private static final Log LOG = LogFactory.getLog(GraphvizUtil.class);

    private static boolean useExternalGrapnviz = false;

    static {
        Graphviz.useEngine(new GraphvizJdkEngine());
    }

    private GraphvizUtil() {

    }

    public static String getSvgExternal(String dotCode) {
        if (useExternalGrapnviz) {
            return getSvgExternal(dotCode, new String[]{});
        } else {
            return getSvgInternal(dotCode);
        }
    }

    public static String getSvgExternal(String dotCode, String extraDotArguments[]) {
        File dotFile = null;
        try {
            dotFile = File.createTempFile("grapviz_dot_graph", ".dot");
            FileUtils.writeStringToFile(dotFile, dotCode, UTF_8);
            String svgFromDotFile = getSvgFromDotFileExternal(dotFile, extraDotArguments);
            int svgBeginIndex = svgFromDotFile.indexOf("<svg");
            if (svgBeginIndex >= 0) {
                svgFromDotFile = svgFromDotFile.substring(svgBeginIndex);
            }
            return svgFromDotFile;
        } catch (IOException e) {
            LOG.error(e);
        } finally {
            if (dotFile != null) {
                dotFile.delete();
            }
        }

        return null;
    }

    public static String getSvgFromDotFileExternal(File dotFile, String extraDotArguments[]) {
        File svgFile = null;
        try {
            svgFile = File.createTempFile("sat_calls_dot_image", ".svg");
            List<String> dotArguments = getDotParameters(dotFile, svgFile);
            Collections.addAll(dotArguments, extraDotArguments);
            runDot(dotArguments);

            if (svgFile.exists()) {
                return FileUtils.readFileToString(svgFile, UTF_8);
            } else {
                return null;
            }
        } catch (IOException | InterruptedException e) {
            LOG.error(e);
        } finally {
            if (svgFile != null) {
                svgFile.delete();
            }
        }

        return null;
    }


    public static String getSvgInternal(String dotCode) {
        try {
            MutableGraph g = Parser.read(dotCode);
            Graphviz graphviz = Graphviz.fromGraph(g);
            Graphviz graphviz1 = graphviz.width(700);
            Renderer render = graphviz1.render(Format.SVG);

            String svg = render.toString();

            int svgBeginIndex = svg.indexOf("<svg");
            if (svgBeginIndex >= 0) {
                svg = svg.substring(svgBeginIndex);
            }

            return svg;
        } catch (IOException e) {
            LOG.error(e);
        } catch (Exception e) {
            LOG.error(e);
        }

        return null;
    }

    private static void runDot(List<String> dotArguments) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(dotArguments.toArray(new String[dotArguments.size()]));
        String graphVizDotPath = GraphvizSettings.getGraphVizDotPath();
        if (graphVizDotPath != null) {
            processBuilder.directory(new File(graphVizDotPath).getParentFile());
            Process theProcess = processBuilder.start();
            theProcess.waitFor();
        }
    }

    private static List<String> getDotParameters(File dotFile, File imgFile) {
        List<String> dotParams = new ArrayList<String>();

        dotParams.add(GraphvizSettings.getGraphVizDotPath());
        dotParams.add("-Tsvg");

        dotParams.add("-o" + imgFile.getAbsolutePath());
        dotParams.add(dotFile.getAbsolutePath());
        return dotParams;
    }
}
