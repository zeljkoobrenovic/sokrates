package nl.obren.sokrates.sourcecode.filehistory;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TemporalDependenciesHelperTest {

    private SourceFile file(String relativePath, String componentKey, String componentName) {
        SourceFile sourceFile = new SourceFile(new File(relativePath));
        sourceFile.setRelativePath(relativePath);
        if (componentName != null) {
            NamedSourceCodeAspect component = new NamedSourceCodeAspect(componentName);
            component.setFiltering(componentKey);
            sourceFile.setLogicalComponents(new ArrayList<>(Arrays.asList(component)));
        }
        return sourceFile;
    }

    private FilePairChangedTogether pair(SourceFile a, SourceFile b, String... commits) {
        FilePairChangedTogether pair = new FilePairChangedTogether(a, b);
        pair.setCommits(new ArrayList<>(Arrays.asList(commits)));
        return pair;
    }

    @Test
    void extractFileDependenciesDeduplicatesCommitsAcrossPairs() {
        SourceFile a = file("a.java", "comp", "A");
        SourceFile b = file("b.java", "comp", "B");

        // Same file pair appears twice with overlapping commit sets; commit count must be the union size.
        List<FilePairChangedTogether> pairs = Arrays.asList(
                pair(a, b, "c1", "c2"),
                pair(a, b, "c2", "c3"));

        TemporalDependenciesHelper helper = new TemporalDependenciesHelper();
        List<ComponentDependency> dependencies = helper.extractFileDependencies(pairs);

        assertEquals(1, dependencies.size());
        assertEquals(3, dependencies.get(0).getCount(),
                "count should be the deduplicated union {c1,c2,c3}, not 4");
    }

    @Test
    void extractFileDependenciesSkipsSelfPairs() {
        SourceFile a = file("a.java", "comp", "A");

        List<FilePairChangedTogether> pairs = Arrays.asList(pair(a, a, "c1"));

        TemporalDependenciesHelper helper = new TemporalDependenciesHelper();
        List<ComponentDependency> dependencies = helper.extractFileDependencies(pairs);

        assertTrue(dependencies.isEmpty(), "a file paired with itself is not a dependency");
    }

    @Test
    void extractComponentDependenciesGroupsByComponentAndUnionsCommits() {
        // Two distinct file pairs that both map to the same component pair (A<->B).
        SourceFile a1 = file("a1.java", "comp", "A");
        SourceFile a2 = file("a2.java", "comp", "A");
        SourceFile b1 = file("b1.java", "comp", "B");
        SourceFile b2 = file("b2.java", "comp", "B");

        List<FilePairChangedTogether> pairs = Arrays.asList(
                pair(a1, b1, "c1", "c2"),
                pair(a2, b2, "c2", "c3"));

        TemporalDependenciesHelper helper = new TemporalDependenciesHelper();
        List<ComponentDependency> dependencies = helper.extractComponentDependencies("comp", pairs);

        assertEquals(1, dependencies.size(), "both file pairs collapse to a single A<->B component dependency");
        assertEquals(3, dependencies.get(0).getCount(),
                "commits unioned across both contributing file pairs: {c1,c2,c3}");
    }

    @Test
    void extractComponentDependenciesIgnoresFilesWithoutComponents() {
        SourceFile a = file("a.java", "comp", "A");
        SourceFile b = file("b.java", "comp", null); // no logical component

        List<FilePairChangedTogether> pairs = Arrays.asList(pair(a, b, "c1"));

        TemporalDependenciesHelper helper = new TemporalDependenciesHelper();
        List<ComponentDependency> dependencies = helper.extractComponentDependencies("comp", pairs);

        assertTrue(dependencies.isEmpty(), "a pair with an unmapped file yields no component dependency");
    }

    @Test
    void extractComponentDependenciesTreatsReversedComponentOrderAsSamePair() {
        SourceFile a = file("a.java", "comp", "A");
        SourceFile b = file("b.java", "comp", "B");

        List<FilePairChangedTogether> pairs = Arrays.asList(
                pair(a, b, "c1"),
                pair(b, a, "c2")); // reversed order

        TemporalDependenciesHelper helper = new TemporalDependenciesHelper();
        List<ComponentDependency> dependencies = helper.extractComponentDependencies("comp", pairs);

        assertEquals(1, dependencies.size(), "A<->B and B<->A are the same component dependency");
        assertEquals(2, dependencies.get(0).getCount());
    }

    @Test
    void extractDependenciesWithCommitsCreatesPerCommitNodes() {
        SourceFile a = file("a.java", "comp", "A");
        SourceFile b = file("b.java", "comp", "B");

        List<FilePairChangedTogether> pairs = Arrays.asList(pair(a, b, "c1", "c2"));

        TemporalDependenciesHelper helper = new TemporalDependenciesHelper();
        List<ComponentDependency> dependencies = helper.extractDependenciesWithCommits(pairs);

        // Each shared commit links each of the two files to the commit node -> 2 commits * 2 files = 4 edges.
        assertEquals(4, dependencies.size());
        dependencies.forEach(d -> assertEquals(1, d.getCount()));
    }
}
