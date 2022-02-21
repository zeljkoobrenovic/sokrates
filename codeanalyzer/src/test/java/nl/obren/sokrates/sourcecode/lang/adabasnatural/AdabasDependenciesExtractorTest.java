/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.adabasnatural;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class AdabasDependenciesExtractorTest {
    @Test
    public void extractDependencyAnchors() throws Exception {
        AdabasDependenciesExtractor extractor = new AdabasDependenciesExtractor();

        List<DependencyAnchor> anchors = extractor.extractDependencyAnchors(new SourceFile(new File("/root/folder/subfolder/file.nsn")));

        assertEquals(anchors.size(), 2);
        assertEquals(anchors.get(0).getAnchor(), "file.nsn");
        assertEquals(anchors.get(0).getDependencyPatterns().size(), 2);
        assertEquals(anchors.get(0).getDependencyPatterns().get(0), "INCLUDE *\"file\"*");
        assertEquals(anchors.get(0).getDependencyPatterns().get(1), "INCLUDE *file*");
        assertEquals(anchors.get(1).getDependencyPatterns().get(0), "USING *file*");

        }

}
