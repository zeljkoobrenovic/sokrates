package nl.obren.sokrates.reports.generators.explorers;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.filehistory.FileModificationHistory;
import nl.obren.sokrates.sourcecode.landscape.analysis.FileExport;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FilesExplorerGeneratorsTest {

    private SourceFile sourceFile(String path, int loc, FileModificationHistory history) {
        SourceFile file = new SourceFile(new File(path));
        file.setRelativePath(path);
        file.setLinesOfCode(loc);
        file.setFileModificationHistory(history);
        return file;
    }

    private NamedSourceCodeAspect aspectOf(SourceFile... files) {
        NamedSourceCodeAspect aspect = new NamedSourceCodeAspect("main");
        aspect.setSourceFiles(Arrays.asList(files));
        return aspect;
    }

    @Test
    void populatesCommitColumnsFromFileHistory() {
        FileModificationHistory history = new FileModificationHistory("src/Foo.java");
        // Three commits; the latest date is the max of these.
        history.setDates(new java.util.ArrayList<>(Arrays.asList("2019-01-01", "2020-06-15", "2018-03-10")));

        FilesExplorerGenerators generators = new FilesExplorerGenerators(new File("."));
        List<FileExport> files = generators.getFiles(aspectOf(sourceFile("src/Foo.java", 100, history)), "main");

        assertEquals(1, files.size());
        FileExport export = files.get(0);
        assertEquals(3, export.getCommitsCount());
        assertEquals("2020-06-15", export.getLatestCommitDate());
        // All three commits are years old, so none count as recent (last 30 days).
        assertEquals(0, export.getRecentCommitsCount30Days());
    }

    @Test
    void leavesDefaultsWhenNoHistory() {
        FilesExplorerGenerators generators = new FilesExplorerGenerators(new File("."));
        List<FileExport> files = generators.getFiles(aspectOf(sourceFile("src/Bar.java", 50, null)), "test");

        assertEquals(1, files.size());
        FileExport export = files.get(0);
        assertEquals(0, export.getCommitsCount());
        assertEquals(0, export.getRecentCommitsCount30Days());
        assertEquals("", export.getLatestCommitDate());
    }

    @Test
    void countsRecentCommitsWithinThirtyDays() {
        // Build dates relative to a recent reference so the "recent" count is deterministic:
        // one commit ~5 days ago (recent) and one ~400 days ago (not recent).
        java.time.LocalDate today = java.time.LocalDate.now();
        String recent = today.minusDays(5).toString();
        String old = today.minusDays(400).toString();

        FileModificationHistory history = new FileModificationHistory("src/Baz.java");
        history.setDates(new java.util.ArrayList<>(Arrays.asList(old, recent)));

        FilesExplorerGenerators generators = new FilesExplorerGenerators(new File("."));
        List<FileExport> files = generators.getFiles(aspectOf(sourceFile("src/Baz.java", 10, history)), "main");

        FileExport export = files.get(0);
        assertEquals(2, export.getCommitsCount());
        assertEquals(1, export.getRecentCommitsCount30Days());
        assertEquals(recent, export.getLatestCommitDate());
    }
}
