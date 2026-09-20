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
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Test
    public void aLinkInsideTheAnalysisRootIsRecordedAsInside() throws IOException {
        // The alias case: the files behind this link are still measured, under their real path.
        // A reader who sees it in the report should not go looking for missing code.
        File root = directory("repo");
        write(new File(root, "src/a.js"), "function f() { return 1; }");
        link(new File(root, "vendor"), new File(root, "src").toPath());

        SymbolicLink link = onlySkippedLinkUnder(root);

        assertEquals("vendor", link.getPath().replace(File.separator, "/"));
        assertTrue(link.getTarget().replace(File.separator, "/").endsWith("repo/src"), link.getTarget());
        assertTrue(link.isInsideAnalysisRoot());
    }

    @Test
    public void aLinkOutsideTheAnalysisRootIsRecordedAsOutside() throws IOException {
        // The population this reporting serves: nothing behind this link is measured anywhere, so
        // the report's file count is smaller than the checkout with no other trace of why.
        File outside = directory("outside");
        write(new File(outside, "b.js"), "function g() { return 2; }");
        File root = directory("repo");
        link(new File(root, "external"), outside.toPath());

        SymbolicLink link = onlySkippedLinkUnder(root);

        assertEquals("external", link.getPath().replace(File.separator, "/"));
        assertFalse(link.isInsideAnalysisRoot());
    }

    @Test
    public void aRelativeTargetIsRecordedAsWrittenOnDisk() throws IOException {
        // The CLAUDE.md -> AGENTS.md shape, and the reason the target is not resolved before it is
        // recorded: an absolute path here would not match what the reader finds in their own tree.
        File root = directory("repo");
        write(new File(root, "AGENTS.md"), "# agents");
        link(new File(root, "CLAUDE.md"), Path.of("AGENTS.md"));

        SymbolicLink link = onlySkippedLinkUnder(root);

        assertEquals("CLAUDE.md", link.getPath().replace(File.separator, "/"));
        assertEquals("AGENTS.md", link.getTarget());
        assertTrue(link.isInsideAnalysisRoot());
    }

    @Test
    public void aDanglingLinkIsStillRecorded() throws IOException {
        // Resolving with Path.toRealPath would throw on a link whose target was never created, and
        // a link dropped here is one the report cannot explain. This one points out of the tree, so
        // it is reported as pointing outside.
        File root = directory("repo");
        link(new File(root, "gone"), new File(tmp.toFile(), "never-created").toPath());

        SymbolicLink link = onlySkippedLinkUnder(root);

        assertEquals("gone", link.getPath().replace(File.separator, "/"));
        assertFalse(link.isInsideAnalysisRoot());
    }

    @Test
    public void aDanglingLinkPointingIntoTheTreeIsReportedAsPointingInside() throws IOException {
        // The flag says where the path LEADS, not that anything is measured there - the reason the
        // report words it that way. docs/latest -> ../build/site with build/ never generated is the
        // real shape. Pinned in both directions so the answer is a decision, not an accident.
        File root = directory("repo");
        write(new File(root, "docs/index.md"), "# docs");
        link(new File(root, "docs/latest"), Path.of("../build/site"));

        SymbolicLink link = onlySkippedLinkUnder(root);

        assertEquals("docs/latest", link.getPath().replace(File.separator, "/"));
        assertTrue(link.isInsideAnalysisRoot());
    }

    @Test
    public void aRelativeTargetIsResolvedAgainstTheLinksOwnDirectory() throws IOException {
        // Resolving against the ROOT instead is indistinguishable for a link at the top level,
        // where the two are the same directory. This link sits two levels down and its target has
        // to climb exactly that far back: correct resolution lands on repo/src, root-relative
        // resolution climbs out of the tree entirely and would report it as pointing outside.
        File root = directory("repo");
        write(new File(root, "src/a.js"), "function a() { return 1; }");
        write(new File(root, "tools/bin/keep.js"), "function k() { return 2; }");
        link(new File(root, "tools/bin/vendored"), Path.of("../../src"));

        SymbolicLink link = onlySkippedLinkUnder(root);

        assertEquals("tools/bin/vendored", link.getPath().replace(File.separator, "/"));
        assertEquals("../../src", link.getTarget());
        assertTrue(link.isInsideAnalysisRoot(), "../../src from tools/bin is repo/src");
    }

    @Test
    public void aSiblingWhoseNameStartsWithTheRootsNameIsNotInside() throws IOException {
        // Why the comparison is Path.startsWith and not a string prefix: "repo-backup" begins with
        // "repo" as text, but shares no whole path element with it.
        File sibling = directory("repo-backup");
        write(new File(sibling, "b.js"), "function b() { return 2; }");
        File root = directory("repo");
        link(new File(root, "backup"), sibling.toPath());

        assertFalse(onlySkippedLinkUnder(root).isInsideAnalysisRoot());
    }

    @Test
    public void aLinkWhoseTargetCannotBeReadDoesNotPointInside() throws IOException {
        // Reached only when readSymbolicLink itself fails, which a real file system will not do on
        // demand - so the classifier is called directly rather than left as an unexercised branch.
        // Without the guard the empty target resolves to the link's own directory, which is inside.
        File root = directory("repo");
        write(new File(root, "a.js"), "function a() { return 1; }");

        SourceCodeFiles sourceCodeFiles = new SourceCodeFiles();
        sourceCodeFiles.load(root, new ProgressFeedback());

        assertFalse(sourceCodeFiles.pointsInsideRoot(new File(root, "unreadable"), ""));
    }

    @Test
    public void theListIsPerRunSoAReusedInstanceDoesNotAccumulate() throws IOException {
        // Same reason as the count above: a list that carried over would name links in the report
        // that this run never encountered.
        File outside = directory("outside");
        File root = directory("repo");
        link(new File(root, "one"), outside.toPath());

        SourceCodeFiles sourceCodeFiles = new SourceCodeFiles();
        sourceCodeFiles.load(root, new ProgressFeedback());
        sourceCodeFiles.load(root, new ProgressFeedback());

        assertEquals(1, sourceCodeFiles.getSkippedSymbolicLinks().size());
    }

    @Test
    public void aListHandedOutSurvivesAReload() throws IOException {
        // The scenario the defensive copy exists for: take the list, reload, and what was already
        // handed over must still hold what it held. Aliasing the internal list would empty it here
        // - and empty the results object the analyzer had already given it to, losing the report
        // line and the file with no error.
        //
        // The reload has to be of a DIFFERENT tree. Reloading the same one clears the list and
        // immediately walks the same link back into it, so an alias and a copy both end up holding
        // one entry and the test would pass against either.
        File outside = directory("outside");
        File withLink = directory("repo");
        link(new File(withLink, "one"), outside.toPath());
        File withoutLink = directory("plain-repo");
        write(new File(withoutLink, "a.js"), "function a() { return 1; }");

        SourceCodeFiles sourceCodeFiles = new SourceCodeFiles();
        sourceCodeFiles.load(withLink, new ProgressFeedback());
        List<SymbolicLink> handedOver = sourceCodeFiles.getSkippedSymbolicLinks();

        sourceCodeFiles.load(withoutLink, new ProgressFeedback());

        assertEquals(1, handedOver.size(), "the list handed over was emptied by a reload");
        assertEquals("one", handedOver.get(0).getPath().replace(File.separator, "/"));
    }

    @Test
    public void aTargetThatIsAllWhitespaceWasStillReadSuccessfully() throws IOException {
        // Why the guard tests emptiness rather than blankness: " " is a legal POSIX file name, so a
        // link pointing at it was read perfectly well and points where it points. Treating it as
        // unreadable would file a link into the tree under "pointing outside" and print it as "?".
        File root = directory("repo");
        write(new File(root, " "), "a file whose whole name is a space");
        link(new File(root, "odd"), Path.of(" "));

        SymbolicLink link = onlySkippedLinkUnder(root);

        assertEquals(" ", link.getTarget());
        assertTrue(link.isInsideAnalysisRoot(), "a readable target inside the root points inside");
    }

    @Test
    public void aSymbolicLinkStandingInForAVcsFolderIsNotReported() throws IOException {
        // .git as a symlink is a real layout (git-annex, some bare-repo setups). The walk skips
        // those folders whether or not they are links, so naming one under "pointing outside the
        // analysis root" would tell the reader code is missing when none ever was in scope.
        File elsewhere = directory("gitstore");
        File root = directory("repo");
        write(new File(root, "src/a.js"), "function a() { return 1; }");
        link(new File(root, ".git"), elsewhere.toPath());
        link(new File(root, "real"), elsewhere.toPath());

        SymbolicLink link = onlySkippedLinkUnder(root);

        assertEquals("real", link.getPath().replace(File.separator, "/"));
    }

    private SymbolicLink onlySkippedLinkUnder(File root) {
        SourceCodeFiles sourceCodeFiles = new SourceCodeFiles();
        sourceCodeFiles.load(root, new ProgressFeedback());

        List<SymbolicLink> links = sourceCodeFiles.getSkippedSymbolicLinks();
        assertEquals(1, links.size(), "expected exactly one skipped link");
        return links.get(0);
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
