/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.python;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import nl.obren.sokrates.sourcecode.lang.python.PythonDependenciesExtractor;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class PythonDependenciesExtractorTest {
    @Test
    public void extractDependencyAnchors() throws Exception {
        SourceFile sourceFile = new SourceFile(new File("a.py"));

        List<DependencyAnchor> anchors = new PythonDependenciesExtractor().extractDependencyAnchors(sourceFile);

        assertEquals(anchors.size(), 1);
        assertEquals(anchors.get(0).getAnchor(), "a");
        assertEquals(anchors.get(0).getDependencyPatterns().size(), 2);
        assertEquals(anchors.get(0).getDependencyPatterns().get(0), "import.*a.*");
        assertEquals(anchors.get(0).getDependencyPatterns().get(1), "from.*a.*import.*");
    }

}
