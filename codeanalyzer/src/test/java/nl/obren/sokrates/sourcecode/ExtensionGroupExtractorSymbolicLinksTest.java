/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode;

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
 * The extension scan stops at a symbolic link too. This is the second walker of the analysed tree:
 * {@code ScopeCreator.getExtensions} runs it on every {@code init}, so what it finds becomes the
 * {@code extensions} list in the generated {@code config.json}.
 *
 * <p>Same rule and same root exception as {@link SourceCodeFilesSymbolicLinksTest}.
 */
public class ExtensionGroupExtractorSymbolicLinksTest {

    @TempDir
    Path tmp;

    @Test
    public void extensionsAreNotCollectedThroughASymbolicLink() throws IOException {
        // Before the guard the link contributed "ts", and init wrote a TypeScript project's
        // extension into a JavaScript project's configuration.
        File outside = directory("other-project");
        write(new File(outside, "b.ts"), "export const g = 2;");
        File root = directory("repo");
        write(new File(root, "src/a.js"), "function f() { return 1; }");
        link(new File(root, "external"), outside.toPath());

        assertEquals("[js: 1 files]", extensionsUnder(root));
    }

    @Test
    public void aLinkBackToAnAncestorIsNotWalked() throws IOException {
        // The count, not the extension, is what moves here: one file on disk was counted once per
        // pass through the link, so "js" was reported with as many files as the kernel allowed.
        File root = directory("repo");
        write(new File(root, "src/a.js"), "function f() { return 1; }");
        link(new File(root, "src/loop"), root.toPath());

        assertEquals("[js: 1 files]", extensionsUnder(root));
    }

    @Test
    public void aLinkToASiblingDirectoryDoesNotCountItsFilesTwice() throws IOException {
        File root = directory("repo");
        write(new File(root, "src/a.js"), "function f() { return 1; }");
        link(new File(root, "shared"), new File(root, "src").toPath());

        assertEquals("[js: 1 files]", extensionsUnder(root));
    }

    @Test
    public void theAnalysisRootMayItselfBeASymbolicLink() throws IOException {
        // Without the root exception, init on a linked -srcRoot proposes no extensions at all.
        File real = directory("real-repo");
        write(new File(real, "src/a.js"), "function f() { return 1; }");
        File rootLink = new File(tmp.toFile(), "repo-link");
        link(rootLink, real.toPath());

        assertEquals("[js: 1 files]", extensionsUnder(rootLink));
    }

    @Test
    public void aPathThatCannotBeTestedForALinkIsWalkedAsBefore() {
        // File accepts paths java.nio.file.Path rejects; SourceFile.relativize already catches
        // InvalidPathException on files this walk had collected. Letting it escape here would end
        // the whole walk on a codebase that used to be reported.
        assertFalse(ExtensionGroupExtractor.isSymbolicLink(new File("/tmp/bad" + ((char) 0) + "name")));
    }

    @Test
    public void everySkippedLinkIsCountedSoTheRunCanSayWhatItDidNotFollow() throws IOException {
        File outside = directory("other-project");
        write(new File(outside, "b.ts"), "export const g = 2;");
        File root = directory("repo");
        link(new File(root, "one"), outside.toPath());
        link(new File(root, "two"), outside.toPath());

        ExtensionGroupExtractor extractor = new ExtensionGroupExtractor();
        extractor.extractExtensionsInfo(root);

        assertEquals(0, extractor.getExtensionsList().size());
        assertEquals(2, extractor.getSkippedSymbolicLinksCount());
    }

    @Test
    public void theCountIsPerRunSoAReusedInstanceDoesNotAccumulate() throws IOException {
        File outside = directory("other-project");
        File root = directory("repo");
        link(new File(root, "one"), outside.toPath());

        ExtensionGroupExtractor extractor = new ExtensionGroupExtractor();
        extractor.extractExtensionsInfo(root);
        extractor.extractExtensionsInfo(root);

        assertEquals(1, extractor.getSkippedSymbolicLinksCount());
    }

    private String extensionsUnder(File root) {
        ExtensionGroupExtractor extractor = new ExtensionGroupExtractor();
        extractor.extractExtensionsInfo(root);

        List<String> extensions = extractor.getExtensionsList().stream()
                .map(ExtensionGroup::toString)
                .sorted()
                .collect(Collectors.toList());

        return extensions.toString();
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
