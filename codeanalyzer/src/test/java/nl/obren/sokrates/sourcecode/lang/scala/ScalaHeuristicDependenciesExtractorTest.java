/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.scala;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import nl.obren.sokrates.sourcecode.lang.scala.ScalaHeuristicDependenciesExtractor;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class ScalaHeuristicDependenciesExtractorTest {
    @Test
    public void extractDependencyAnchors() throws Exception {
        ScalaHeuristicDependenciesExtractor extractor = new ScalaHeuristicDependenciesExtractor();

        SourceFile target = new SourceFile(new File("temp"), "package kafka.controller\n" +
                "\n" +
                "import java.util\n");
        List<DependencyAnchor> anchors = extractor.extractDependencyAnchors(target);

        assertEquals(anchors.size(), 1);
        assertEquals(anchors.get(0).getAnchor(), "kafka.controller");
        assertEquals(anchors.get(0).getDependencyPatterns().size(), 1);
        assertEquals(anchors.get(0).getDependencyPatterns().get(0), "import .*kafka[.]controller([.][A-Z].*|[.][*]|[.]_|[{].*|)");
    }

}
