package nl.obren.sokrates.sourcecode.lang;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import nl.obren.sokrates.sourcecode.lang.php.PhpHeuristicDependenciesExtractor;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class PhpHeuristicDependenciesExtractorTest {
    @Test
    public void extractDependencyAnchors() throws Exception {
        PhpHeuristicDependenciesExtractor extractor = new PhpHeuristicDependenciesExtractor();
        String namspaceDef = "namespace some\\namespace;";
        List<DependencyAnchor> anchors = extractor.extractDependencyAnchors(new SourceFile(new File(""), namspaceDef));

        assertEquals(anchors.size(), 1);
        assertEquals(anchors.get(0).getAnchor(), "some\\namespace");
        assertEquals(anchors.get(0).getDependencyPatterns().size(), 2);
        assertEquals(anchors.get(0).getDependencyPatterns().get(0), "use.*some\\\\namespace[ ]*;");
        assertEquals(anchors.get(0).getDependencyPatterns().get(1), "use.*some\\\\namespace\\\\[a-zA-Z_\\x7f-\\xff][a-zA-Z0-9_\\x7f-\\xff]*( |,|;|[ ]+as).*");

    }

}