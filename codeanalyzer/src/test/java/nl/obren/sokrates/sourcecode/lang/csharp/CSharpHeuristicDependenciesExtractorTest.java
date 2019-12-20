/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.csharp;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import nl.obren.sokrates.sourcecode.lang.csharp.CSharpHeuristicDependenciesExtractor;
import org.junit.Test;
import java.io.File;
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
        assertEquals(anchors.get(0).getDependencyPatterns().get(0), "[ ]*using* a([.][A-Z].*|[.][*]|);");
    }

}
