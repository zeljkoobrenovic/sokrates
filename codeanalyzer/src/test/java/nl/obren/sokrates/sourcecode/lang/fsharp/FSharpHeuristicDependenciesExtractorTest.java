/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.fsharp;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.dependencies.Dependency;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import nl.obren.sokrates.sourcecode.lang.fsharp.FSharpHeuristicDependenciesExtractor;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class FSharpHeuristicDependenciesExtractorTest {
    @Test
    public void extractDependencyAnchors() throws Exception {
        SourceFile sourceFile = new SourceFile(new File(""), "namespace rec a \n");

        List<DependencyAnchor> anchors = new FSharpHeuristicDependenciesExtractor().extractDependencyAnchors(sourceFile);
        assertEquals(anchors.size(), 1);
        assertEquals(anchors.get(0).getAnchor(), "a");
        assertEquals(anchors.get(0).getDependencyPatterns().size(), 1);
        assertEquals(anchors.get(0).getDependencyPatterns().get(0), "[ ]*open[ ]+a([.][*]|)");
    }
}
