package nl.obren.sokrates.sourcecode.lang.plsql;

import junit.framework.TestCase;
import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.dependencies.Dependency;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import nl.obren.sokrates.sourcecode.lang.python.PythonDependenciesExtractor;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class PlSqlHeuristicDependenciesExtractorTest {

    @Test
    public void extractDependencyAnchors() {
        PlSqlHeuristicDependenciesExtractor extractor = new PlSqlHeuristicDependenciesExtractor();
        SourceFile sourceFile = new SourceFile(new File("test_anchors.pls"), PlSqlExamples.CONTENT_5);

        List<DependencyAnchor> anchors = extractor.extractDependencyAnchors(sourceFile);

        assertEquals(5, anchors.size());
        assertEquals("c_package", anchors.get(0).getAnchor());
        assertEquals(1, anchors.get(0).getDependencyPatterns().size());
        assertEquals("\\s*c_package[.]+\\S+", anchors.get(0).getDependencyPatterns().get(0));
        assertEquals("test_anchors", anchors.get(4).getAnchor());
        assertEquals(1, anchors.get(4).getDependencyPatterns().size());
        assertEquals("\\s*test_anchors[.]+\\S+", anchors.get(4).getDependencyPatterns().get(0));
    }

    @Test
    public void extractDependencyAnchorsMultipleFiles() {

        SourceFile sourceFile1 = new SourceFile(new File("test_root.pls"), PlSqlExamples.CONTENT_5);
        SourceFile sourceFile2 = new SourceFile(new File("test_child.pls"), PlSqlExamples.CONTENT_6);

        PlSqlHeuristicDependenciesExtractor extractor = new PlSqlHeuristicDependenciesExtractor();
        List<DependencyAnchor> anchors = extractor.getDependencyAnchors(Arrays.asList(sourceFile1, sourceFile2));

        assertEquals(3, anchors.size());
        assertEquals("c_package", anchors.get(0).getAnchor());
        assertEquals("test_root", anchors.get(1).getAnchor());
        assertEquals("test_child", anchors.get(2).getAnchor());
        assertEquals("\\s*c_package[.]+\\S+", anchors.get(0).getDependencyPatterns().get(0));
    }

    @Test
    public void extractDependencyAnchorsMultipleFiles2() {

        SourceFile sourceFile1 = new SourceFile(new File("test_root1.pls"), PlSqlExamples.CONTENT_5);
        SourceFile sourceFile2 = new SourceFile(new File("test_root2.pls"), PlSqlExamples.CONTENT_7);
        SourceFile sourceFile3 = new SourceFile(new File("test_child1.pls"), PlSqlExamples.CONTENT_6);
        SourceFile sourceFile4 = new SourceFile(new File("test_child2.pls"), PlSqlExamples.CONTENT_8);

        PlSqlHeuristicDependenciesExtractor extractor = new PlSqlHeuristicDependenciesExtractor();
        List<DependencyAnchor> anchors = extractor
                .getDependencyAnchors(Arrays.asList(sourceFile1, sourceFile2, sourceFile3, sourceFile4));

        assertEquals(6, anchors.size());
        assertEquals("c_package", anchors.get(0).getAnchor());
        assertEquals("test_root1", anchors.get(1).getAnchor());
        assertEquals("emp_bonus", anchors.get(2).getAnchor());
        assertEquals("test_root2", anchors.get(3).getAnchor());
        assertEquals("test_child1", anchors.get(4).getAnchor());
        assertEquals("test_child2", anchors.get(5).getAnchor());
    }

    @Test
    public void extractDependencies() throws Exception {
        PlSqlAnalyzer analyzer = new PlSqlAnalyzer();
        SourceFile sourceFile1 = new SourceFile(new File("test_root.pls"), PlSqlExamples.CONTENT_5);
        SourceFile sourceFile2 = new SourceFile(new File("test_child.pls"), PlSqlExamples.CONTENT_6);

        List<Dependency> dependencies = analyzer
                .extractDependencies(Arrays.asList(sourceFile1, sourceFile2), new ProgressFeedback())
                .getDependencies();

        assertEquals(1, dependencies.size());
        assertEquals("test_child -> c_package", dependencies.get(0).getDependencyString());
        assertEquals("test_child -> c_package", dependencies.get(0).getComponentDependency(""));
    }

    @Test
    public void extractDependenciesMultipleFiles() throws Exception {
        PlSqlAnalyzer analyzer = new PlSqlAnalyzer();
        SourceFile sourceFile1 = new SourceFile(new File("test_root1.pls"), PlSqlExamples.CONTENT_5);
        SourceFile sourceFile2 = new SourceFile(new File("test_root2.pls"), PlSqlExamples.CONTENT_7);
        SourceFile sourceFile3 = new SourceFile(new File("test_child1.pls"), PlSqlExamples.CONTENT_6);
        SourceFile sourceFile4 = new SourceFile(new File("test_child2.pls"), PlSqlExamples.CONTENT_8);

        List<Dependency> dependencies = analyzer
                .extractDependencies(Arrays.asList(sourceFile1, sourceFile2, sourceFile3, sourceFile4),
                        new ProgressFeedback())
                .getDependencies();

        assertEquals(3, dependencies.size());
        assertEquals("test_child1 -> c_package", dependencies.get(0).getDependencyString());
        assertEquals("test_child1 -> c_package", dependencies.get(0).getComponentDependency(""));
        assertEquals("test_child2 -> c_package", dependencies.get(1).getDependencyString());
        assertEquals("test_child2 -> emp_bonus", dependencies.get(2).getDependencyString());

    }
}