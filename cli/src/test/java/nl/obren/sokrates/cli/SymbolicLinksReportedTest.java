package nl.obren.sokrates.cli;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * End-to-end: the links the walk skips reach the generated report.
 *
 * <p>The walk and the rendering are each covered by unit tests, but the single line wiring them
 * together - {@code BasicsAnalyzer} handing the walk's list to the analysis results - is not
 * something either side can see. Delete that line and both suites stay green while the feature
 * produces nothing at all: no line in the Overview, no entry in data.zip. This test runs the real
 * CLI, so it fails when the seam is broken.
 *
 * <p>The second half matters as much as the first: a link-free repository must gain no line and no
 * data entry, which is what lets this change claim it alters nothing for repositories without
 * symbolic links.
 */
class SymbolicLinksReportedTest {

    @Test
    void skippedLinksAreNamedInTheReportAndTheDataExport(@TempDir Path tmp) throws IOException {
        File outside = directory(tmp, "outside-repo");
        write(new File(outside, "c.js"), "function c() { return 3; }");

        File repo = directory(tmp, "repo");
        write(new File(repo, "src/a.js"), "function a() { return 1; }");
        write(new File(repo, "AGENTS.md"), "# agents");
        link(new File(repo, "CLAUDE.md"), Path.of("AGENTS.md"));
        link(new File(repo, "external"), outside.toPath());

        File reportsFolder = generateReports(repo);

        String overview = FileUtils.readFileToString(
                new File(reportsFolder, "html/SourceCodeOverview.html"), UTF_8);
        assertTrue(overview.contains("symbolic links were not followed"), "no symbolic-links line in the Overview");
        assertTrue(overview.contains("<b>2</b> symbolic link"), "expected both links counted; got: " + line(overview));
        assertTrue(overview.contains("text/symbolic_links.txt"), "the line must link to the exported list");

        String links = readDataEntry(reportsFolder, "text/symbolic_links.txt");
        assertNotNull(links, "text/symbolic_links.txt is missing from data.zip");
        assertTrue(links.contains("CLAUDE.md -> AGENTS.md"), links);
        assertTrue(links.contains("external -> " + outside.getPath()), links);
        assertTrue(links.contains("pointing OUTSIDE the analysis root (1)"), links);
        assertTrue(links.contains("pointing INSIDE the analysis root (1)"), links);
    }

    @Test
    void aRepositoryWithoutLinksGainsNothing(@TempDir Path tmp) throws IOException {
        File repo = directory(tmp, "repo");
        write(new File(repo, "src/a.js"), "function a() { return 1; }");

        File reportsFolder = generateReports(repo);

        String overview = FileUtils.readFileToString(
                new File(reportsFolder, "html/SourceCodeOverview.html"), UTF_8);
        assertFalse(overview.contains("symbolic link"), "a link-free repository must gain no line");
        assertFalse(overview.contains("symbolic_links.txt"), "a link-free repository must gain no link");
        assertNull(readDataEntry(reportsFolder, "text/symbolic_links.txt"),
                "a link-free repository must gain no data.zip entry");
    }

    private File generateReports(File repo) throws IOException {
        CommandLineInterface cli = new CommandLineInterface();
        File configFile = new File(repo, "_sokrates/config.json");
        cli.run(new String[]{"init", "-srcRoot", repo.getPath(), "-confFile", configFile.getPath()});
        assertTrue(configFile.exists(), "init should have written " + configFile);
        File reportsFolder = new File(repo, "_sokrates/reports");
        cli.run(new String[]{"generateReports", "-confFile", configFile.getPath(),
                "-outputFolder", reportsFolder.getPath()});
        return reportsFolder;
    }

    /** The data folder is packaged as data/data.zip; null when the entry is not there at all. */
    private String readDataEntry(File reportsFolder, String entryName) throws IOException {
        File dataZip = new File(reportsFolder, "data/data.zip");
        assertTrue(dataZip.isFile(), "expected " + dataZip);
        try (ZipFile zip = new ZipFile(dataZip)) {
            ZipEntry entry = zip.getEntry(entryName);
            if (entry == null) {
                return null;
            }
            return new String(zip.getInputStream(entry).readAllBytes(), UTF_8);
        }
    }

    private String line(String html) {
        int at = html.indexOf("symbolic link");
        return at < 0 ? "<absent>" : html.substring(Math.max(0, at - 120), at + 40);
    }

    private File directory(Path tmp, String name) throws IOException {
        File directory = new File(tmp.toFile(), name);
        Files.createDirectories(directory.toPath());
        return directory;
    }

    private void write(File file, String content) throws IOException {
        Files.createDirectories(file.getParentFile().toPath());
        FileUtils.write(file, content, UTF_8);
    }

    /** Skips where the file system will not make a link; a fixture mistake still fails. */
    private void link(File link, Path target) throws IOException {
        try {
            Files.createSymbolicLink(link.toPath(), target);
        } catch (NoSuchFileException | FileAlreadyExistsException e) {
            throw e;
        } catch (FileSystemException | UnsupportedOperationException e) {
            assumeTrue(false, "this file system cannot create symbolic links: " + e);
        }
    }
}
