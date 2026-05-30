/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.cli;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static junit.framework.TestCase.*;

public class SokratesFileUtilsTest {

    private static final String SAMPLE_HISTORY =
            "abc123 2021-01-01 jane@example.com src/main/java/A.java\n" +
            "def456 2021-01-02 john@example.com src/test/java/ATest.java\n" +
            "ghi789 2021-01-03 jane@example.com docs/readme.md";

    @Test
    public void extractSubHistoryKeepsOnlyMatchingPaths() {
        String result = SokratesFileUtils.extractSubHistory(SAMPLE_HISTORY, "src/main/.*", "");

        assertEquals("abc123 2021-01-01 jane@example.com src/main/java/A.java", result);
    }

    @Test
    public void extractSubHistoryMatchesMultipleLines() {
        String result = SokratesFileUtils.extractSubHistory(SAMPLE_HISTORY, "src/.*", "");

        List<String> lines = List.of(result.split("\n"));
        assertEquals(2, lines.size());
        assertTrue(lines.get(0).endsWith("src/main/java/A.java"));
        assertTrue(lines.get(1).endsWith("src/test/java/ATest.java"));
    }

    @Test
    public void extractSubHistoryPrependsPrefixToPath() {
        String result = SokratesFileUtils.extractSubHistory(SAMPLE_HISTORY, "src/main/.*", "module-a/");

        assertEquals("abc123 2021-01-01 jane@example.com module-a/src/main/java/A.java", result);
    }

    @Test
    public void extractSubHistoryReturnsEmptyWhenNothingMatches() {
        String result = SokratesFileUtils.extractSubHistory(SAMPLE_HISTORY, "no/such/path/.*", "");

        assertEquals("", result);
    }

    @Test
    public void extractSubHistoryIgnoresMalformedLines() {
        // lines with fewer than 4 space-separated elements have no path column and must be skipped
        String history = "incomplete line\nabc123 2021-01-01 jane@example.com src/main/A.java";

        String result = SokratesFileUtils.extractSubHistory(history, "src/main/.*", "");

        assertEquals("abc123 2021-01-01 jane@example.com src/main/A.java", result);
    }

    @Test
    public void listFilesReturnsOnlyRegexMatchesRecursively() throws IOException {
        Path root = Files.createTempDirectory("sokrates-listfiles");
        try {
            Path javaFile = root.resolve("a").resolve("Main.java");
            Path txtFile = root.resolve("a").resolve("notes.txt");
            Files.createDirectories(javaFile.getParent());
            Files.writeString(javaFile, "class Main {}");
            Files.writeString(txtFile, "notes");

            List<Path> matches = SokratesFileUtils.listFiles(".*[.]java", root);

            List<String> names = matches.stream()
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toList());
            assertEquals(1, names.size());
            assertTrue(names.contains("Main.java"));
        } finally {
            deleteRecursively(root.toFile());
        }
    }

    private static void deleteRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }
}
