package nl.obren.sokrates.sourcecode;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Path patterns are matched against the path below the source root, not against the whole path as loaded:
 * the folders above a repository (a user's "tests" or "docs" folder, Docker's /code) must never classify
 * its files.
 */
class SourceFileFilterMatchingPathTest {

    private static SourceFile fileUnder(String root, String relativePath) {
        SourceFile sourceFile = new SourceFile(new File(root, relativePath));
        sourceFile.relativize(new File(root));
        return sourceFile;
    }

    @Test
    void foldersAboveTheSourceRootDoNotTakePart() {
        SourceFileFilter tests = new SourceFileFilter(".*/[Tt]ests/.*", "");
        SourceFileFilter docs = new SourceFileFilter(".*/docs/.*", "");

        SourceFile mainFile = fileUnder("/home/me/tests/docs/repo", "src/app/service.py");
        assertEquals("/src/app/service.py", SourceFileFilter.matchingPath(mainFile));
        assertFalse(tests.matches(mainFile));
        assertFalse(docs.matches(mainFile));

        SourceFile testFile = fileUnder("/home/me/tests/docs/repo", "tests/test_service.py");
        assertTrue(tests.matches(testFile));
    }

    @Test
    void theLeadingSeparatorKeepsTopLevelFolderPatternsWorking() {
        SourceFileFilter src = new SourceFileFilter(".*/src/.*", "");
        assertTrue(src.matches(fileUnder("/code", "src/Main.java")));
        assertFalse(src.matches(fileUnder("/code", "lib/Main.java")));
    }

    @Test
    void aFileWithoutRelativePathStillMatchesOnItsFullPath() {
        SourceFile sourceFile = new SourceFile(new File("/a/b/tests/C.java"));
        assertEquals(new File("/a/b/tests/C.java").getPath(), SourceFileFilter.matchingPath(sourceFile));
        assertTrue(new SourceFileFilter(".*/tests/.*", "").matches(sourceFile));
    }
}
