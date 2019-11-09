package nl.obren.sokrates.sourcecode.dependencies;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.lang.java.JavaHeuristicDependenciesExtractor;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DependencyAnchorTest {
    @Test
    public void isLinkedToAnchor1() throws Exception {
        DependencyAnchor anchor = new DependencyAnchor("test");
        anchor.getDependencyPatterns().add("import.* test([.][A-Z_]+|[.][*]|);");

        assertTrue(anchor.isLinkedFrom("import test;"));
        assertTrue(anchor.isLinkedFrom("import   test;"));
        assertTrue(anchor.isLinkedFrom("import   test.A;"));
        assertTrue(anchor.isLinkedFrom("import   test.A;"));
        assertTrue(anchor.isLinkedFrom("import  static test.A;"));
        assertTrue(anchor.isLinkedFrom("import   test.*;"));
        assertTrue(anchor.isLinkedFrom("import\t\ttest;"));
        assertTrue(anchor.isLinkedFrom("import a;\nimport b;\nimport test;\n"));

        assertFalse(anchor.isLinkedFrom("import a;\nimport b;\nimport c.*;\n"));
        assertFalse(anchor.isLinkedFrom("import a;\nimport b;\nimport a.test;\n"));
    }

    @Test
    public void isLinkedToAnchor2() throws Exception {
        JavaHeuristicDependenciesExtractor parser = new JavaHeuristicDependenciesExtractor();
        List<DependencyAnchor> parseAnchors = parser.extractDependencyAnchors(new SourceFile(new File("file1"), "package test; class A {}"));

        DependencyAnchor anchor = parseAnchors.get(0);

        assertTrue(anchor.isLinkedFrom("import   test;"));
        assertTrue(anchor.isLinkedFrom("import   test.ClassA;"));

        assertFalse(anchor.isLinkedFrom("import   test.otherpackage;"));
    }
}