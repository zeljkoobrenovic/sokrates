/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.ruby;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.dependencies.Dependency;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import nl.obren.sokrates.sourcecode.dependencies.DependencyUtils;
import nl.obren.sokrates.sourcecode.lang.java.JavaHeuristicDependenciesExtractor;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class RubyHeuristicDependenciesExtractorTest {
    @Test
    public void getDependencyAnchors() throws Exception {
        JavaHeuristicDependenciesExtractor parser = new JavaHeuristicDependenciesExtractor();
        ArrayList<SourceFile> sourceFiles = new ArrayList<>();

        sourceFiles.add(new SourceFile(new File("file1"), "package a.b.c; class D {}"));
        sourceFiles.add(new SourceFile(new File("file2"), "package a.b.c; class E {}"));
        sourceFiles.add(new SourceFile(new File("file3"), "package a.b.c; class F {}"));
        sourceFiles.add(new SourceFile(new File("file4"), "package d.e.f; class G {}"));
        sourceFiles.add(new SourceFile(new File("file5"), "package g.h.i; class J {}"));

        List<DependencyAnchor> dependencyAnchors = parser.getDependencyAnchors(sourceFiles);

        assertEquals(dependencyAnchors.size(), 3);

        assertEquals(dependencyAnchors.get(0).getAnchor(), "a.b.c");
        assertEquals(dependencyAnchors.get(0).getSourceFiles().size(), 3);
        assertEquals(dependencyAnchors.get(0).getSourceFiles().get(0).getFile().getPath(), "file1");
        assertEquals(dependencyAnchors.get(0).getSourceFiles().get(1).getFile().getPath(), "file2");
        assertEquals(dependencyAnchors.get(0).getSourceFiles().get(2).getFile().getPath(), "file3");

        assertEquals(dependencyAnchors.get(1).getAnchor(), "d.e.f");
        assertEquals(dependencyAnchors.get(1).getSourceFiles().size(), 1);
        assertEquals(dependencyAnchors.get(1).getSourceFiles().get(0).getFile().getPath(), "file4");

        assertEquals(dependencyAnchors.get(2).getAnchor(), "g.h.i");
        assertEquals(dependencyAnchors.get(2).getSourceFiles().size(), 1);
        assertEquals(dependencyAnchors.get(2).getSourceFiles().get(0).getFile().getPath(), "file5");
    }

    @Test
    public void parse1() throws Exception {
        JavaHeuristicDependenciesExtractor parser = new JavaHeuristicDependenciesExtractor();

        List<DependencyAnchor> anchors = parser.extractDependencyAnchors(new SourceFile(new File("file1"), "package a.b.c;"));

        assertEquals(anchors.size(), 1);
        assertEquals(anchors.get(0).getAnchor(), "a.b.c");
        assertEquals(anchors.get(0).getDependencyPatterns().get(0), "import.* a[.]b[.]c([.][A-Z].*|[.][*]|);");
    }

    @Test
    public void parse2() throws Exception {
        JavaHeuristicDependenciesExtractor parser = new JavaHeuristicDependenciesExtractor();

        List<DependencyAnchor> anchors = parser.extractDependencyAnchors(new SourceFile(new File("file1"), "package a;"));

        assertEquals(anchors.size(), 1);
        assertEquals(anchors.get(0).getAnchor(), "a");
        assertEquals(anchors.get(0).getDependencyPatterns().get(0), "import.* a([.][A-Z].*|[.][*]|);");
    }


    @Test
    public void parse3() throws Exception {
        JavaHeuristicDependenciesExtractor parser = new JavaHeuristicDependenciesExtractor();

        List<DependencyAnchor> anchors = parser.extractDependencyAnchors(new SourceFile(new File("file1"), "package nl.obren.codeexplorer.sourcecode.dependencies;\n" +
                "\n" +
                "import org.junit.Test;\n" +
                "\n" +
                "import java.io.File;\n" +
                "import java.util.List;\n" +
                "\n" +
                "import static org.junit.Assert.*;\n" +
                "\n" +
                "public class JavaHeuristicDependenciesExtractorTest {\n}"));

        assertEquals(anchors.size(), 1);
        assertEquals(anchors.get(0).getAnchor(), "nl.obren.codeexplorer.sourcecode.dependencies");
        assertEquals(anchors.get(0).getDependencyPatterns().get(0), "import.* nl[.]obren[.]codeexplorer[.]sourcecode[.]dependencies([.][A-Z].*|[.][*]|);");
    }


    @Test
    public void parse4() throws Exception {
        JavaHeuristicDependenciesExtractor parser = new JavaHeuristicDependenciesExtractor();

        SourceFile file1 = new SourceFile(new File("file1"), "package nl.obren.codeexplorer.sourcecode.dependencies;\n" +
                "\n" +
                "import org.junit.Test;\n" +
                "\n" +
                "import java.io.File;\n" +
                "import java.util.List;\n" +
                "\n" +
                "import static org.junit.Assert.*;\n" +
                "\n" +
                "public class JavaHeuristicDependenciesExtractorTest {\n}");

        SourceFile file2 = new SourceFile(new File("file2"), "package nl.obren.codeexplorer.sourcecode.test1;\n" +
                "\n" +
                "import org.junit.Test;\n" +
                "\n" +
                "import java.io.File;\n" +
                "import java.util.List;\n" +
                "\n" +
                "import nl.obren.codeexplorer.sourcecode.dependencies.*;\n" +
                "\n" +
                "public class A {\n}");

        SourceFile file3 = new SourceFile(new File("file3"), "package nl.obren.codeexplorer.sourcecode.test1;\n" +
                "\n" +
                "import org.junit.Test;\n" +
                "\n" +
                "import java.io.File;\n" +
                "import java.util.List;\n" +
                "\n" +
                "import nl.obren.codeexplorer.sourcecode.dependencies.A;\n" +
                "\n" +
                "public class B {\n}");

        SourceFile file4 = new SourceFile(new File("file4"), "package nl.obren.codeexplorer.sourcecode.test2;\n" +
                "\n" +
                "import org.junit.Test;\n" +
                "\n" +
                "import java.io.File;\n" +
                "import java.util.List;\n" +
                "\n" +
                "import nl.obren.codeexplorer.sourcecode.dependencies.A;\n" +
                "import nl.obren.codeexplorer.sourcecode.dependencies.B;\n" +
                "import nl.obren.codeexplorer.sourcecode.dependencies.C;\n" +
                "\n" +
                "public class B {\n}");

        SourceFile file5 = new SourceFile(new File("file5"), "package nl.obren.codeexplorer.sourcecode.test2;\n" +
                "\n" +
                "import org.junit.Test;\n" +
                "\n" +
                "import java.io.File;\n" +
                "import java.util.List;\n" +
                "\n" +
                "import nl.obren.codeexplorer.sourcecode.test1.*;\n" +
                "import nl.obren.codeexplorer.sourcecode.dependencies.A;\n" +
                "import static nl.obren.codeexplorer.sourcecode.dependencies.B;\n" +
                "import nl.obren.codeexplorer.sourcecode.dependencies.C;\n" +
                "import static nl.obren.codeexplorer.sourcecode.dependencies.*;\n" +
                "\n" +
                "public class B {\n}");

        file1.getLogicalComponents().add(new NamedSourceCodeAspect("A"));
        file2.getLogicalComponents().add(new NamedSourceCodeAspect("B"));
        file3.getLogicalComponents().add(new NamedSourceCodeAspect("B"));
        file4.getLogicalComponents().add(new NamedSourceCodeAspect("C"));
        file5.getLogicalComponents().add(new NamedSourceCodeAspect("C"));

        List<Dependency> dependencies = parser.extractDependencies(Arrays.asList(file1, file2, file3, file4, file5), new ProgressFeedback()).getDependencies();

        assertEquals(dependencies.size(), 3);
        assertEquals(dependencies.get(0).getFrom().getAnchor(), "nl.obren.codeexplorer.sourcecode.test1");
        assertEquals(dependencies.get(0).getTo().getAnchor(), "nl.obren.codeexplorer.sourcecode.dependencies");
        assertEquals(dependencies.get(0).getFromFiles().size(), 2);
        assertEquals(dependencies.get(0).getFromFiles().get(0).getSourceFile().getFile().getName(), "file2");
        assertEquals(dependencies.get(0).getFromFiles().get(1).getSourceFile().getFile().getName(), "file3");

        assertEquals(dependencies.get(1).getFrom().getAnchor(), "nl.obren.codeexplorer.sourcecode.test2");
        assertEquals(dependencies.get(1).getTo().getAnchor(), "nl.obren.codeexplorer.sourcecode.dependencies");
        assertEquals(dependencies.get(1).getFromFiles().size(), 2);
        assertEquals(dependencies.get(1).getFromFiles().get(0).getSourceFile().getFile().getName(), "file4");
        assertEquals(dependencies.get(1).getFromFiles().get(1).getSourceFile().getFile().getName(), "file5");

        assertEquals(dependencies.get(2).getFrom().getAnchor(), "nl.obren.codeexplorer.sourcecode.test2");
        assertEquals(dependencies.get(2).getTo().getAnchor(), "nl.obren.codeexplorer.sourcecode.test1");
        assertEquals(dependencies.get(2).getFromFiles().size(), 1);
        assertEquals(dependencies.get(2).getFromFiles().get(0).getSourceFile().getFile().getName(), "file5");

        String group = "";
        assertEquals(DependencyUtils.getComponentDependencies(dependencies, group).size(), 3);
        assertEquals(DependencyUtils.getComponentDependencies(dependencies, group).get(0).getFromComponent(), "B");
        assertEquals(DependencyUtils.getComponentDependencies(dependencies, group).get(0).getToComponent(), "A");
        assertEquals(DependencyUtils.getComponentDependencies(dependencies, group).get(0).getCount(), 2);
        assertEquals(DependencyUtils.getComponentDependencies(dependencies, group).get(1).getFromComponent(), "C");
        assertEquals(DependencyUtils.getComponentDependencies(dependencies, group).get(1).getToComponent(), "A");
        assertEquals(DependencyUtils.getComponentDependencies(dependencies, group).get(1).getCount(), 2);
        assertEquals(DependencyUtils.getComponentDependencies(dependencies, group).get(2).getFromComponent(), "C");
        assertEquals(DependencyUtils.getComponentDependencies(dependencies, group).get(2).getToComponent(), "B");
        assertEquals(DependencyUtils.getComponentDependencies(dependencies, group).get(2).getCount(), 1);
    }

}
