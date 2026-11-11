/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.dataexporters;

import nl.obren.sokrates.sourcecode.SymbolicLink;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The list of symbolic links the source code walk did not follow, as text/symbolic_links.txt.
 *
 * <p>The links pointing out of the analysis root come first: nothing behind those is measured
 * anywhere, so they are the ones that explain a file count smaller than the checkout. "Pointing
 * inside" says where the path leads, not that anything was measured there - a link into the tree
 * whose target was never created still points inside.
 */
public class DataExporterSymbolicLinksTest {

    @Test
    public void linksAreGroupedOutsideFirstWithTheirTargets() {
        String content = DataExporter.symbolicLinksContent(Arrays.asList(
                new SymbolicLink("CLAUDE.md", "AGENTS.md", true),
                new SymbolicLink("scripts/fp-report", "/Users/someone/other-repo/bin/fp-report", false),
                new SymbolicLink(".claude/skills", "../.agents/skills", true)));

        assertTrue(content, content.contains("Symbolic links pointing OUTSIDE the analysis root (1):"));
        assertTrue(content, content.contains("scripts/fp-report -> /Users/someone/other-repo/bin/fp-report"));
        assertTrue(content, content.contains("Symbolic links pointing INSIDE the analysis root (2):"));
        assertTrue(content, content.contains("CLAUDE.md -> AGENTS.md"));
        assertTrue(content, content.contains(".claude/skills -> ../.agents/skills"));
        assertTrue("outside group must come first",
                content.indexOf("OUTSIDE") < content.indexOf("INSIDE"));
    }

    @Test
    public void aGroupWithNoLinksIsNotRendered() {
        String content = DataExporter.symbolicLinksContent(
                Arrays.asList(new SymbolicLink("vendor", "src", true)));

        assertTrue(content, content.contains("INSIDE"));
        assertEquals("a heading for an empty group would report links that do not exist",
                -1, content.indexOf("OUTSIDE"));
    }

    @Test
    public void aLinkWhoseTargetCouldNotBeReadIsStillListed() {
        // Dropping it would leave the report unable to explain a path the walk did skip.
        String content = DataExporter.symbolicLinksContent(
                Arrays.asList(new SymbolicLink("broken", "", false)));

        assertTrue(content, content.contains("broken -> ?"));
    }

    @Test
    public void noLinksProduceNoContentSoNoFileIsWritten() {
        // The guard behind the promise that a repository without symbolic links produces exactly
        // the output it produced before this file existed - no empty entry added to data.zip.
        assertEquals("", DataExporter.symbolicLinksContent(new ArrayList<>()));
    }

    @Test
    public void aVeryLongListIsCappedButTheHeadingKeepsTheTrueCount() {
        // A pnpm store is almost entirely symbolic links. Without the cap one such repository puts
        // megabytes of unactionable paths into data.zip; without the true count in the heading the
        // reader would not know the list was trimmed.
        List<SymbolicLink> many = new ArrayList<>();
        for (int i = 0; i < DataExporter.MAX_EXPORT_LIST_SIZE + 25; i++) {
            many.add(new SymbolicLink("node_modules/pkg" + i, "../.store/pkg" + i, true));
        }

        String content = DataExporter.symbolicLinksContent(many);

        assertTrue(content, content.contains("(" + (DataExporter.MAX_EXPORT_LIST_SIZE + 25) + ")"));
        assertTrue(content, content.contains("showing the first " + DataExporter.MAX_EXPORT_LIST_SIZE));
        assertEquals("one line per listed link, and no more than the cap",
                DataExporter.MAX_EXPORT_LIST_SIZE, countOccurrences(content, " -> "));
    }

    @Test
    public void aNewlineInAPathCannotForgeExtraEntries() {
        // POSIX allows a newline in a file name. Left raw it splits into lines that read as further
        // entries, leaving the group's own count disagreeing with what the reader can see.
        String content = DataExporter.symbolicLinksContent(Arrays.asList(
                new SymbolicLink("odd\nname", "target\nhere", false)));

        assertTrue(content, content.contains("(1):"));
        assertEquals("exactly one entry line", 1, countOccurrences(content, " -> "));
        assertTrue(content, content.contains("odd name -> target here"));
    }

    @Test
    public void noFileIsWrittenWhenThereAreNoLinks() throws Exception {
        // The file-level half of the promise: a repository without symbolic links must not gain an
        // entry in data.zip. Asserting on the rendered string alone would not have shown this.
        File folder = Files.createTempDirectory("symbolic-links-none").toFile();
        folder.deleteOnExit();

        DataExporter.writeSymbolicLinks(folder, new ArrayList<>());

        assertEquals("no symbolic_links.txt may be created", 0, folder.list().length);
    }

    @Test
    public void theFileIsWrittenWhenThereAreLinks() throws Exception {
        File folder = Files.createTempDirectory("symbolic-links-some").toFile();
        folder.deleteOnExit();

        DataExporter.writeSymbolicLinks(folder, Arrays.asList(new SymbolicLink("vendor", "src", true)));

        File written = new File(folder, "symbolic_links.txt");
        assertTrue("symbolic_links.txt must be created", written.isFile());
        assertTrue(FileUtils.readFileToString(written, StandardCharsets.UTF_8).contains("vendor -> src"));
    }

    private int countOccurrences(String text, String needle) {
        int count = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) {
            count++;
        }
        return count;
    }

    @Test
    public void anAbsentListIsTreatedAsNoLinks() {
        // A results object built by a path that never set the list must render, not throw.
        List<SymbolicLink> absent = null;
        assertEquals("", DataExporter.symbolicLinksContent(absent));
    }
}
