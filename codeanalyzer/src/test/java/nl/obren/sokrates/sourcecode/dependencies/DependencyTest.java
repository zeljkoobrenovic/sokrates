/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.dependencies;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class DependencyTest {
    @Test
    public void getFromComponents() throws Exception {
        DependencyAnchor a = new DependencyAnchor("a");
        SourceFile sourceFile1 = new SourceFile();
        sourceFile1.getLogicalComponents().add(new NamedSourceCodeAspect("Comp1"));
        a.getSourceFiles().add(sourceFile1);

        DependencyAnchor b = new DependencyAnchor("b");
        SourceFile sourceFile2 = new SourceFile();
        sourceFile2.getLogicalComponents().add(new NamedSourceCodeAspect("Comp2"));
        b.getSourceFiles().add(sourceFile2);

        Dependency dependency = new Dependency(a, b);
        dependency.getFromFiles().add(new SourceFileDependency(sourceFile1));
        List<NamedSourceCodeAspect> fromComponents = dependency.getFromComponents("");

        assertEquals(fromComponents.size(), 1);
        assertEquals(fromComponents.get(0).getName(), "Comp1");
    }

    @Test
    public void getToComponents() throws Exception {
        DependencyAnchor a = new DependencyAnchor("a");
        SourceFile sourceFile1 = new SourceFile();
        sourceFile1.getLogicalComponents().add(new NamedSourceCodeAspect("Comp1"));
        a.getSourceFiles().add(sourceFile1);

        DependencyAnchor b = new DependencyAnchor("b");
        SourceFile sourceFile2 = new SourceFile();
        sourceFile2.getLogicalComponents().add(new NamedSourceCodeAspect("Comp2"));
        b.getSourceFiles().add(sourceFile2);

        Dependency dependency = new Dependency(a, b);
        List<NamedSourceCodeAspect> toComponents = dependency.getToComponents("");

        assertEquals(toComponents.size(), 1);
        assertEquals(toComponents.get(0).getName(), "Comp2");
    }

    @Test
    public void equals() throws Exception {
        assertTrue(new Dependency(new DependencyAnchor("a"), new DependencyAnchor("b"))
                .equals(new Dependency(new DependencyAnchor("a"), new DependencyAnchor("b"))));

        assertFalse(new Dependency(new DependencyAnchor("a"), new DependencyAnchor("b")).equals(
                new Dependency(new DependencyAnchor("a"), new DependencyAnchor("c"))));
    }

    @Test
    public void getDependencyString() throws Exception {
        assertEquals(new Dependency(new DependencyAnchor("a"), new DependencyAnchor("b")).getDependencyString(), "a -> b");
    }

    @Test
    public void getFromFiles() throws Exception {

    }

    @Test
    public void getComponentDependency() throws Exception {
        DependencyAnchor a = new DependencyAnchor("a");
        SourceFile sourceFile1 = new SourceFile();
        sourceFile1.getLogicalComponents().add(new NamedSourceCodeAspect("Comp1"));
        a.getSourceFiles().add(sourceFile1);

        DependencyAnchor b = new DependencyAnchor("b");
        SourceFile sourceFile2 = new SourceFile();
        sourceFile2.getLogicalComponents().add(new NamedSourceCodeAspect("Comp2"));
        b.getSourceFiles().add(sourceFile2);

        String componentDependency = new Dependency(a, b).getComponentDependency("");

        assertEquals(componentDependency, "a -> Comp2");
    }

}
