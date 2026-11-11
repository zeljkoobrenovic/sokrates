package nl.obren.sokrates.sourcecode.scoping;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the mock-folder test conventions.
 *
 * <p>Files under a folder whose name starts with {@code mock} are mock resources and belong in the
 * <b>test</b> scope. The rules used to spell the rest of the folder name as
 * {@code [a-zA-Z0-9_\- ]+}, which left three kinds of folder unmatched, so their files stayed in
 * <b>main</b> and were counted as production code:
 * <ul>
 *   <li>a folder named plainly {@code mock} - the {@code +} demanded at least one further character;</li>
 *   <li>a folder carrying a character outside that list, such as {@code mock.data};</li>
 *   <li>a folder carrying any non-ASCII character, such as {@code mock-größe}.</li>
 * </ul>
 * The rest of the name is now {@code [^/]*}. These tests pin both that the newly covered folders are
 * classified as test, and that the folders which already matched still do.
 */
class ScopingConventionsMockFoldersTest {

    private final ScopingConventions conventions = new ScopingConventions();

    private boolean matchesAny(List<Convention> rules, String path) {
        return rules.stream().anyMatch(c -> c.pathMatches(path));
    }

    private void assertTestScope(String path) {
        assertTrue(matchesAny(conventions.getTestFilesConventions(), path),
                "expected TEST scope but was not classified there: " + path);
    }

    private void assertNotTestScope(String path) {
        assertFalse(matchesAny(conventions.getTestFilesConventions(), path),
                "expected NOT test scope but was classified as test: " + path);
    }

    // --- the folders the old character class missed ---------------------------------------------

    @Test
    void plainMockFolderIsTestScope() {
        // The real case: elasticsearch keeps a test-service plugin under .../inference/mock/.
        assertTestScope("/repo/x-pack/plugin/inference/qa/test-service-plugin/src/main/java/"
                + "org/elasticsearch/xpack/inference/mock/TestInferenceServicePlugin.java");
        assertTestScope("/repo/src/mock/M.java");
        assertTestScope("../src/mock/M.java");
        assertTestScope("/repo/src/mock/deep/nested/M.java");
    }

    @Test
    void mockFolderWithPunctuationIsTestScope() {
        assertTestScope("/repo/src/mock.data/M.java");
        assertTestScope("/repo/src/mock+api/M.java");
        assertTestScope("/repo/src/mock(old)/M.java");
    }

    @Test
    void mockFolderWithNonAsciiNameIsTestScope() {
        assertTestScope("/repo/src/mock-größe/M.java");
        assertTestScope("/repo/src/mock-prøve/M.java");
        assertTestScope("/repo/src/mock模拟/M.java");
        assertTestScope("/repo/src/mockданные/M.java");
    }

    @Test
    void doubleUnderscoreMockFolderVariantsAreTestScope() {
        assertTestScope("/repo/src/__mock/M.java");
        assertTestScope("/repo/src/__mock.data/M.java");
        assertTestScope("/repo/src/__mock-größe/M.java");
    }

    // --- what already matched must keep matching -------------------------------------------------

    @Test
    void previouslyMatchingMockFoldersStillTestScope() {
        assertTestScope("/repo/src/mocks/M.java");
        assertTestScope("/repo/src/mockapi/M.java");
        assertTestScope("/repo/src/mock-data/M.java");
        assertTestScope("/repo/src/mock_data/M.java");
        assertTestScope("/repo/src/mock data/M.java");
        assertTestScope("/repo/src/mockStore/M.java");
        assertTestScope("/repo/src/__mocks__/M.java");
    }

    @Test
    void windowsStyleMockPathsAreTestScope() {
        // pathMatches also tests the "\\"->"/" variant of the path; the rules must hold there too.
        assertTestScope("C:\\repo\\src\\mock\\M.java");
        assertTestScope("C:\\repo\\src\\mock-größe\\M.java");
    }

    // --- [^/] must not reach past the folder name ------------------------------------------------

    @Test
    void mockMustStartTheFolderName() {
        assertNotTestScope("/repo/src/remock/M.java");
        assertNotTestScope("/repo/src/notamock/M.java");
        assertNotTestScope("/repo/src/demo/M.java");
    }

    @Test
    void mockMustBeAFolderNotAFileName() {
        // "mockery.java" is a file: there is no closing separator, so no folder rule applies.
        assertNotTestScope("/repo/src/mockery.java");
        assertNotTestScope("/repo/src/mock.java");
    }

    @Test
    void aRelativeRootIsNotItselfAMockFolder() {
        // The default srcRoot is "..", so every path carries a leading "..". A rule anchored on
        // [^/] must not turn that into a match for an ordinary source file.
        assertNotTestScope("../src/main/java/App.java");
        assertNotTestScope("../README.md");
    }
}
