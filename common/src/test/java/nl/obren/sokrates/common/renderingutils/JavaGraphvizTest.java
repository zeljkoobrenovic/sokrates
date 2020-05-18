/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.renderingutils;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.Renderer;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class JavaGraphvizTest {
    @Test
    public void test() throws IOException {
        /*String path = "";
        System.out.println(new File(path).exists());
        MutableGraph g = Parser.read(new FileInputStream(new File(path)));
        Graphviz graphviz = Graphviz.fromGraph(g);
        Graphviz graphviz1 = graphviz.width(700);
        Renderer render = graphviz1.render(Format.SVG);
        System.out.println(render.toString());*/
    }
}
