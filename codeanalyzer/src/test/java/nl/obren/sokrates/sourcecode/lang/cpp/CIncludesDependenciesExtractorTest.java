/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.cpp;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import nl.obren.sokrates.sourcecode.lang.cpp.CIncludesDependenciesExtractor;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class CIncludesDependenciesExtractorTest {
    @Test
    public void extractDependencyAnchors() throws Exception {
        CIncludesDependenciesExtractor extractor = new CIncludesDependenciesExtractor();

        List<DependencyAnchor> anchors = extractor.extractDependencyAnchors(new SourceFile(new File("/root/folder/subfolder/file.h")));

        assertEquals(anchors.size(), 5);
        assertEquals(anchors.get(0).getAnchor(), "file.h");
        assertEquals(anchors.get(0).getDependencyPatterns().size(), 2);
        assertEquals(anchors.get(0).getDependencyPatterns().get(0), "[#]include.*<file.h>.*");
        assertEquals(anchors.get(0).getDependencyPatterns().get(1), "[#]include.*\"file.h\".*");

        assertEquals(anchors.get(1).getAnchor(), "file.h");
        assertEquals(anchors.get(1).getDependencyPatterns().size(), 2);
        assertEquals(anchors.get(1).getDependencyPatterns().get(0), "[#]include.*<subfolder(/|\\\\)file.h>.*");
        assertEquals(anchors.get(1).getDependencyPatterns().get(1), "[#]include.*\"subfolder(/|\\\\)file.h\".*");

        assertEquals(anchors.get(2).getAnchor(), "file.h");
        assertEquals(anchors.get(2).getDependencyPatterns().size(), 2);
        assertEquals(anchors.get(2).getDependencyPatterns().get(0), "[#]include.*<folder(/|\\\\)subfolder(/|\\\\)file.h>.*");
        assertEquals(anchors.get(2).getDependencyPatterns().get(1), "[#]include.*\"folder(/|\\\\)subfolder(/|\\\\)file.h\".*");

        assertEquals(anchors.get(3).getAnchor(), "file.h");
        assertEquals(anchors.get(3).getDependencyPatterns().size(), 2);
        assertEquals(anchors.get(3).getDependencyPatterns().get(0), "[#]include.*<root(/|\\\\)folder(/|\\\\)subfolder(/|\\\\)file.h>.*");
        assertEquals(anchors.get(3).getDependencyPatterns().get(1), "[#]include.*\"root(/|\\\\)folder(/|\\\\)subfolder(/|\\\\)file.h\".*");

        assertEquals(anchors.get(4).getAnchor(), "file.h");
        assertEquals(anchors.get(4).getDependencyPatterns().size(), 2);
        assertEquals(anchors.get(4).getDependencyPatterns().get(0), "[#]include.*<(/|\\\\)root(/|\\\\)folder(/|\\\\)subfolder(/|\\\\)file.h>.*");
        assertEquals(anchors.get(4).getDependencyPatterns().get(1), "[#]include.*\"(/|\\\\)root(/|\\\\)folder(/|\\\\)subfolder(/|\\\\)file.h\".*");
    }

}
