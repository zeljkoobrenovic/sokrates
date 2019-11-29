package nl.obren.sokrates.sourcecode.lang;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.dependencies.Dependency;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import nl.obren.sokrates.sourcecode.lang.js.NodeJavaScriptHeuristicDependenciesExtractor;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class NodeJavaScriptHeuristicDependenciesExtractorTest {
    @Test
    public void extractDependenciesToTargetAnchor() throws Exception {
        NodeJavaScriptHeuristicDependenciesExtractor extractor = new NodeJavaScriptHeuristicDependenciesExtractor();
        List<Dependency> dependencies = new ArrayList<>();
        SourceFile sourceFile1 = new SourceFile();
        sourceFile1.setFile(new File("/root/folder/file1.js"));
        sourceFile1.getLogicalComponents().add(new NamedSourceCodeAspect("A"));
        sourceFile1.setContent("var a = require('file2.js')");
        SourceFile sourceFile2 = new SourceFile();
        sourceFile2.setFile(new File("/root/folder/file2.js"));
        sourceFile2.getLogicalComponents().add(new NamedSourceCodeAspect("B"));

        DependencyAnchor sourceAnchor = new DependencyAnchor("/root/folder/file1.js");
        sourceAnchor.getSourceFiles().add(sourceFile1);
        DependencyAnchor targetAnchor = new DependencyAnchor("/root/folder/file2.js");
        targetAnchor.getSourceFiles().add(sourceFile2);

        extractor.extractDependenciesToTargetAnchor(dependencies, sourceFile1, sourceAnchor, targetAnchor);

        assertEquals(dependencies.size(), 1);
        assertEquals(dependencies.get(0).getFrom().getAnchor(), "/root/folder/file1.js");
        assertEquals(dependencies.get(0).getTo().getAnchor(), "/root/folder/file2.js");
    }

    @Test
    public void extractDependenciesToTargetAnchorMissingReference() throws Exception {
        NodeJavaScriptHeuristicDependenciesExtractor extractor = new NodeJavaScriptHeuristicDependenciesExtractor();
        List<Dependency> dependencies = new ArrayList<>();
        SourceFile sourceFile1 = new SourceFile();
        sourceFile1.setFile(new File("/root/folder/file1.js"));
        sourceFile1.getLogicalComponents().add(new NamedSourceCodeAspect("A"));
        sourceFile1.setContent("var a = require('file2.js')");
        SourceFile sourceFile2 = new SourceFile();
        sourceFile2.setFile(new File("/root/folder/file3.js"));
        sourceFile2.getLogicalComponents().add(new NamedSourceCodeAspect("C"));

        DependencyAnchor sourceAnchor = new DependencyAnchor("/root/folder/file1.js");
        sourceAnchor.getSourceFiles().add(sourceFile1);
        DependencyAnchor targetAnchor = new DependencyAnchor("/root/folder/file3.js");
        targetAnchor.getSourceFiles().add(sourceFile2);

        extractor.extractDependenciesToTargetAnchor(dependencies, sourceFile1, sourceAnchor, targetAnchor);

        assertEquals(dependencies.size(), 0);
    }

}
