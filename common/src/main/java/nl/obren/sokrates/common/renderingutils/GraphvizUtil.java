/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

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

    public static boolean useExternalGraphviz = true;

    static {
        Graphviz.useEngine(new GraphvizJdkEngine());
    }

    private GraphvizUtil() {

    }

    public static String getSvgFromDot(String dotCode) {
        if (useExternalGraphviz()) {
            return getSvgFromDot(dotCode, new String[]{});
        } else {
            return getSvgInternal(dotCode);
        }
    }

    private static boolean useExternalGraphviz() {
        return useExternalGraphviz && GraphvizSettings.getGraphVizDotPath() != null;
    }

    public static String getSvgFromDot(String dotCode, String extraDotArguments[]) {
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
            svgFile = File.createTempFile("dependencies_dot_image", ".svg");
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


    private static String getSvgInternal(String dotCode) {
        try {
            Parser parser = new Parser();
            MutableGraph g = parser.read(dotCode);
            Graphviz graphviz = Graphviz.fromGraph(g);
            Graphviz graphvizStandardWidth = graphviz;
            Renderer renderSvg = graphvizStandardWidth.render(Format.SVG);

            String svg = renderSvg.toString();

            int svgBeginIndex = svg.indexOf("<svg");
            if (svgBeginIndex >= 0) {
                svg = svg.substring(svgBeginIndex);
            }

            return svg;
        } catch (IOException e) {
            LOG.error(e);
        } catch (Exception e) {
            LOG.error(e);
        } catch (Throwable e) {
            LOG.error(e);
        }

        return null;
    }

    public static String saveToPngFileReturnSvg(String dotCode, File file) {
        try {
            String svg = getSvgFromDot(dotCode);
            //MutableGraph g = new Parser().read(dotCode);
            //Graphviz graphviz = Graphviz.fromGraph(g);
            //Graphviz graphvizStandardWidth = graphviz;

            //Renderer renderPng = graphvizStandardWidth.render(Format.PNG);
            //renderPng.toFile(file);

            int svgBeginIndex = svg.indexOf("<svg");
            if (svgBeginIndex >= 0) {
                svg = svg.substring(svgBeginIndex);
            }

            return svg;
        } catch (Throwable e) {
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
