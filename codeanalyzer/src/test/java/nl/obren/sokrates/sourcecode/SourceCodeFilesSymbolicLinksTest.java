/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The walk stops at a symbolic link, which {@code File.isDirectory()} and {@code File.listFiles()}
 * would otherwise resolve and walk as the directory it points at - re-entering the same files where
 * the link resolves to an ancestor, until the operating system refuses to resolve another link
 * component.
 *
 * <p>The analysis root itself is the exception: it may legitimately be handed to Sokrates as a link
 * ({@code -srcRoot /path/to/link-to-repo}).
 */
public class SourceCodeFilesSymbolicLinksTest {

    @TempDir
    Path tmp;

    @Test
    public void aLinkBackToAnAncestorIsNotWalked() throws IOException {
        // Before the guard: src/a.js, src/loop/src/a.js, src/loop/src/loop/src/a.js, and so on,
        // as many passes as the kernel allowed - a count set by the host, not by this fixture.
        File root = directory("repo");
        write(new File(root, "src/a.js"), "function f() { return 1; }");
        link(new File(root, "src/loop"), root.toPath());

        assertEquals("[src/a.js]", relativePathsUnder(root));
    }

    @Test
    public void aLinkToASiblingDirectoryDoesNotDoubleCountItsFiles() throws IOException {
        // No cycle and nothing pathological - the case that misreports without a dramatic number.
        // Before the guard: two files, one on disk, reported to the duplication analysis as a pair.
        File root = directory("repo");
        write(new File(root, "src/a.js"), "function f() { return 1; }");
        link(new File(root, "vendor"), new File(root, "src").toPath());

        assertEquals("[src/a.js]", relativePathsUnder(root));
    }

    @Test
    public void aLinkPointingOutsideTheAnalysisRootIsNotMeasured() throws IOException {
        // Measuring it would import another project's code into this project's numbers.
        File outside = directory("outside");
        write(new File(outside, "b.js"), "function g() { return 2; }");
        File root = directory("repo");
        write(new File(root, "src/a.js"), "function f() { return 1; }");
        link(new File(root, "external"), outside.toPath());

        assertEquals("[src/a.js]", relativePathsUnder(root));
    }

    @Test
    public void theAnalysisRootMayItselfBeASymbolicLink() throws IOException {
        // The guard applies to what the walk descends into, not to what it is pointed at. A guard
        // written without this exception reports zero files for a linked -srcRoot, silently.
        File real = directory("real-repo");
        write(new File(real, "src/a.js"), "function f() { return 1; }");
        File rootLink = new File(tmp.toFile(), "repo-link");
        link(rootLink, real.toPath());

        assertEquals("[src/a.js]", relativePathsUnder(rootLink));
    }

    @Test
    public void aPathThatCannotBeTestedForALinkIsWalkedAsBefore() {
        // File accepts paths java.nio.file.Path rejects; SourceFile.relativize already catches
        // InvalidPathException on files this walk had collected. Letting it escape here would end
        // the whole walk on a codebase that used to be reported.
        assertFalse(SourceCodeFiles.isSymbolicLink(new File("/tmp/bad" + ((char) 0) + "name")));
    }

    @Test
    public void everySkippedLinkIsCountedSoTheRunCanSayWhatItDidNotFollow() throws IOException {
        // A root that reaches its code only through links now measures nothing. Without the count
        // behind the log line it would do so in silence, which is the complaint about the old
        // behaviour turned around.
        File outside = directory("outside");
        write(new File(outside, "b.js"), "function g() { return 2; }");
        File root = directory("repo");
        link(new File(root, "one"), outside.toPath());
        link(new File(root, "two"), outside.toPath());

        SourceCodeFiles sourceCodeFiles = new SourceCodeFiles();
        sourceCodeFiles.load(root, new ProgressFeedback());

        assertEquals(0, sourceCodeFiles.getAllFiles().size());
        assertEquals(2, sourceCodeFiles.getSkippedSymbolicLinksCount());
    }

    @Test
    public void theCountIsPerRunSoAReusedInstanceDoesNotAccumulate() throws IOException {
        // Every caller today constructs a fresh instance, so this is defensive: nothing reuses one
        // yet, and a count that carried over would put a number in the log that never happened.
        File outside = directory("outside");
        File root = directory("repo");
        link(new File(root, "one"), outside.toPath());

        SourceCodeFiles sourceCodeFiles = new SourceCodeFiles();
        sourceCodeFiles.load(root, new ProgressFeedback());
        sourceCodeFiles.load(root, new ProgressFeedback());

        assertEquals(1, sourceCodeFiles.getSkippedSymbolicLinksCount());
    }

    private String relativePathsUnder(File root) {
        SourceCodeFiles sourceCodeFiles = new SourceCodeFiles();
        sourceCodeFiles.load(root, new ProgressFeedback());

        List<String> paths = sourceCodeFiles.getAllFiles().stream()
                .map(sourceFile -> sourceFile.getRelativePath().replace(File.separator, "/"))
                .sorted()
                .collect(Collectors.toList());

        return paths.toString();
    }

    private File directory(String name) throws IOException {
        File directory = new File(tmp.toFile(), name);
        Files.createDirectories(directory.toPath());
        return directory;
    }

    private void write(File file, String content) throws IOException {
        Files.createDirectories(file.getParentFile().toPath());
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Skips the test where the file system will not make a link at all - Windows without the
     * privilege reports that as a plain {@link FileSystemException}. A fixture mistake arrives as
     * one of its subtypes and is rethrown, so it fails rather than passing as a skip.
     */
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
