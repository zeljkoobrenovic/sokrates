package nl.obren.sokrates.sourcecode.contributors;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.FilesHistoryAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.HistoryPerExtension;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.filehistory.CommitInfo;
import nl.obren.sokrates.sourcecode.filehistory.FileModificationHistory;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ScopePathsTest {

    private static SourceFile file(String path) {
        SourceFile file = new SourceFile();
        file.setRelativePath(path);
        return file;
    }

    @Test
    void lowercasedPathsPerScopeAlsoWithoutTheFirstFolder() {
        CodeConfiguration configuration = new CodeConfiguration();
        configuration.getMain().getSourceFiles().add(file("src/Main/App.java"));
        configuration.getTest().getSourceFiles().add(file("test/AppTest.java"));

        Map<String, Set<String>> paths = ScopePaths.byScope(configuration);

        assertEquals(Arrays.asList("main", "test"), Arrays.asList(paths.keySet().toArray()));
        assertTrue(paths.get("main").contains("src/main/app.java"));
        assertTrue(paths.get("main").contains("main/app.java"), "also without the first folder");
        assertNull(ScopePaths.byScope(new CodeConfiguration()), "no scope has files");
    }

    private static FileModificationHistory history(String path, String date, String email) {
        FileModificationHistory history = new FileModificationHistory(path);
        CommitInfo commit = new CommitInfo("c-" + path, date);
        commit.setEmail(email);
        history.setCommits(Collections.singletonList(commit));
        return history;
    }

    @Test
    void perExtensionHistoryCanBeLimitedToTheFilesOfAScope() {
        FilesHistoryAnalysisResults results = new FilesHistoryAnalysisResults();
        results.setHistory(Arrays.asList(
                history("src/App.java", "2026-01-10", "a@x"),
                history("test/AppTest.java", "2026-02-10", "b@x"),
                history("README.md", "2025-03-10", "a@x")));

        List<HistoryPerExtension> all = results.getHistoryPerExtensionPerYear();
        List<HistoryPerExtension> main = results.getHistoryPerExtensionPerYear(path -> path.equals("src/app.java"));

        assertEquals(2, all.size(), "java 2026 (both files) and md 2025");
        assertEquals(2, all.stream().filter(h -> h.getExtension().equals("java")).findFirst().get().getCommitsCount());
        assertEquals(1, main.size());
        assertEquals("java", main.get(0).getExtension());
        assertEquals("2026", main.get(0).getYear());
        assertEquals(1, main.get(0).getCommitsCount());
        assertEquals(Collections.singleton("a@x"), main.get(0).getContributors());
    }
}
