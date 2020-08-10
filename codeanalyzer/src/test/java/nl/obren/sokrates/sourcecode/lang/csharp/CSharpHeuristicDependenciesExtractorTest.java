/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.csharp;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.dependencies.Dependency;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import nl.obren.sokrates.sourcecode.lang.csharp.CSharpHeuristicDependenciesExtractor;

import org.junit.Test;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class CSharpHeuristicDependenciesExtractorTest {
    @Test
    public void extractDependencyAnchors() throws Exception {
        SourceFile sourceFile = new SourceFile(new File(""), "namespace a {\n" +
                "}");

        List<DependencyAnchor> anchors = new CSharpHeuristicDependenciesExtractor().extractDependencyAnchors(sourceFile);
        assertEquals(anchors.size(), 1);
        assertEquals(anchors.get(0).getAnchor(), "a");
        assertEquals(anchors.get(0).getDependencyPatterns().size(), 1);
        assertEquals(anchors.get(0).getDependencyPatterns().get(0), "[ ]*using[ ]+a([.][*]|);");
    }

    @Test
    public void extractDependencies() throws Exception {
        CSharpAnalyzer analyzer = new CSharpAnalyzer();

        NamedSourceCodeAspect rootComponent = new NamedSourceCodeAspect("Root");
        String codeRoot1 = "namespace Root.First {\n" +
        "}";
        String codeRoot2 = "namespace Root.Second {\n" +
        "}";
        SourceFile sourceFileRoot1 = new SourceFile(new File("a.cs"), codeRoot1);
        sourceFileRoot1.getLogicalComponents().add(rootComponent);
        SourceFile sourceFileRoot2 = new SourceFile(new File("b.cs"), codeRoot2);
        sourceFileRoot2.getLogicalComponents().add(rootComponent);

        String user = "using Root.First;\n" +
        "\n" + 
        "namespace User.My {}";
        SourceFile usageFile = new SourceFile(new File("c.cs"), user);
        usageFile.getLogicalComponents().add(new NamedSourceCodeAspect("User"));

        List<Dependency> dependencies = analyzer.extractDependencies(Arrays.asList(sourceFileRoot1, sourceFileRoot2, usageFile), new ProgressFeedback()).getDependencies();
        assertEquals(1, dependencies.size());
        assertEquals("User.My -> Root.First", dependencies.get(0).getDependencyString());
        assertEquals("User -> Root", dependencies.get(0).getComponentDependency(""));
    }

    @Test
    public void extractDependenciesSubNamespaces() throws Exception {
        CSharpAnalyzer analyzer = new CSharpAnalyzer();

        NamedSourceCodeAspect rootComponent = new NamedSourceCodeAspect("Root");
        String codeRoot1 = "namespace Root {\n" +
        "}";
        String codeRoot2 = "namespace Root.Other {\n" +
        "}";
        SourceFile sourceFileRoot1 = new SourceFile(new File("a.cs"), codeRoot1);
        sourceFileRoot1.getLogicalComponents().add(rootComponent);
        SourceFile sourceFileRoot2 = new SourceFile(new File("h.cs"), codeRoot2);
        sourceFileRoot2.getLogicalComponents().add(rootComponent);

        String user = "using Root;\n" +
        "\n" + 
        "namespace User.My {}";
        SourceFile usageFile = new SourceFile(new File("c.cs"), user);
        usageFile.getLogicalComponents().add(new NamedSourceCodeAspect("User"));

        List<Dependency> dependencies = analyzer.extractDependencies(Arrays.asList(sourceFileRoot1, sourceFileRoot2, usageFile), new ProgressFeedback()).getDependencies();
        assertEquals(1, dependencies.size());
        assertEquals("User.My -> Root", dependencies.get(0).getDependencyString());
        assertEquals("User -> Root", dependencies.get(0).getComponentDependency(""));
    }

    @Test
    public void extractDependenciesWithNamespaceOverlappingSubProjects() throws Exception {
        CSharpAnalyzer analyzer = new CSharpAnalyzer();

        NamedSourceCodeAspect rootComponent = new NamedSourceCodeAspect("Root");
        String codeRoot1 = "namespace Root {\n" +
        "}";
        String codeRoot2 = "namespace Root.Other {\n" +
        "}";
        SourceFile sourceFileRoot1 = new SourceFile(new File("a.cs"), codeRoot1);
        sourceFileRoot1.getLogicalComponents().add(rootComponent);
        SourceFile sourceFileRoot2 = new SourceFile(new File("h.cs"), codeRoot2);
        sourceFileRoot2.getLogicalComponents().add(rootComponent);

        String code2 = "namespace Root.SubInOtherComponent {\n" +
        "}";
        SourceFile sourceFileSub1 = new SourceFile(new File("b.cs"), code2);
        sourceFileSub1.getLogicalComponents().add(new NamedSourceCodeAspect("Root.Sub"));

        String user = "using Root.SubInOtherComponent;\n" +
        "\n" + 
        "namespace User.My {}";
        SourceFile usageFile = new SourceFile(new File("c.cs"), user);
        usageFile.getLogicalComponents().add(new NamedSourceCodeAspect("User"));

        List<Dependency> dependencies = analyzer.extractDependencies(Arrays.asList(sourceFileRoot1, sourceFileRoot2, sourceFileSub1, usageFile), new ProgressFeedback()).getDependencies();
        assertEquals(1, dependencies.size());
        assertEquals("User.My -> Root.SubInOtherComponent", dependencies.get(0).getDependencyString());
        assertEquals("User -> Root.Sub", dependencies.get(0).getComponentDependency(""));
    }

}
