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

    @Test
    void limitedFileDependenciesAreTheFirstOnesOfAnUnlimitedRunWithTheSameCounts() {
        SourceFile a = file("a.java", "comp", "A");
        SourceFile b = file("b.java", "comp", "B");
        SourceFile c = file("c.java", "comp", "C");

        // the a-b pair occurs again after the limit is reached; its commits must still be counted
        List<FilePairChangedTogether> pairs = Arrays.asList(
                pair(a, b, "c1"),
                pair(a, c, "c2"),
                pair(b, c, "c3"),
                pair(b, a, "c4", "c5"));

        List<ComponentDependency> unlimited = new TemporalDependenciesHelper().extractFileDependencies(pairs);
        List<ComponentDependency> limited = new TemporalDependenciesHelper().extractFileDependencies(pairs, 1);

        assertEquals(3, unlimited.size());
        assertEquals(1, limited.size());
        assertEquals(unlimited.get(0).getFromComponent(), limited.get(0).getFromComponent());
        assertEquals(unlimited.get(0).getToComponent(), limited.get(0).getToComponent());
        assertEquals(3, limited.get(0).getCount(), "{c1, c4, c5}");
        assertEquals(unlimited.get(0).getCount(), limited.get(0).getCount());
    }

    @Test
    void limitedDependenciesWithCommitsAreTheFirstOnesOfAnUnlimitedRunWithTheSameCounts() {
        SourceFile a = file("a.java", "comp", "A");
        SourceFile b = file("b.java", "comp", "B");
        SourceFile c = file("c.java", "comp", "C");

        List<FilePairChangedTogether> pairs = Arrays.asList(
                pair(a, b, "c1", "c2"),
                pair(a, c, "c1"),
                pair(b, c, "c2", "c3"));

        List<ComponentDependency> unlimited = new TemporalDependenciesHelper().extractDependenciesWithCommits(pairs);
        for (int limit = 0; limit <= unlimited.size(); limit++) {
            List<ComponentDependency> limited = new TemporalDependenciesHelper().extractDependenciesWithCommits(pairs, limit);
            assertEquals(limit, limited.size());
            for (int i = 0; i < limit; i++) {
                assertEquals(unlimited.get(i).getFromComponent(), limited.get(i).getFromComponent());
                assertEquals(unlimited.get(i).getToComponent(), limited.get(i).getToComponent());
                assertEquals(unlimited.get(i).getCount(), limited.get(i).getCount());
            }
        }
    }
}
