package nl.obren.sokrates.sourcecode.scoping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for the standard "Hidden files" ignore convention.
 *
 * <p>Background: the convention is evaluated by {@link nl.obren.sokrates.sourcecode.SourceFileFilter#pathMatches}
 * against {@code sourceFile.getFile().getPath()}. With the default relative {@code srcRoot} of {@code ".."},
 * those paths look like {@code "../src/Foo.java"}. {@code pathMatches} additionally tests a slash-stripped
 * (backslash) variant of the path, so a naive {@code [^/]}-anchored rule matched the leading {@code ".."} of
 * every relative path and silently ignored the whole repository. These tests pin the corrected behaviour.
 */
class ScopingConventionsHiddenFilesTest {

    private Convention hiddenFiles;

    @BeforeEach
    void setUp() {
        hiddenFiles = new ScopingConventions().getIgnoredFilesConventions().stream()
                .filter(c -> "Hidden files".equals(c.getNote()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("'Hidden files' ignore convention not found"));
    }

    private void assertKept(String path) {
        assertFalse(hiddenFiles.pathMatches(path), "expected KEPT but was ignored: " + path);
    }

    private void assertIgnored(String path) {
        assertTrue(hiddenFiles.pathMatches(path), "expected IGNORED but was kept: " + path);
    }

    // --- the core regression: a relative ".." srcRoot must not ignore everything ---------------

    @Test
    void relativeRootedSourceFilesAreKept() {
        assertKept("../clients/src/test/java/org/apache/kafka/clients/producer/ProducerIdExpirationTest.java");
        assertKept("../README.md");
        assertKept("../bin/foo.sh");
        assertKept("../build.gradle");
    }

    @Test
    void relativeRootedSourceFilesAreKeptEvenInBackslashForm() {
        // pathMatches also tests the "/"->"\\" variant; the rule must hold there too.
        assertKept("..\\clients\\src\\Foo.java");
        assertKept("..\\README.md");
    }

    @Test
    void absoluteRootedSourceFilesAreKept() {
        assertKept("/Users/dev/Downloads/kafka/clients/src/Foo.java");
        assertKept("/Users/dev/project/README.md");
    }

    // --- hidden files are still ignored at every depth -----------------------------------------

    @Test
    void hiddenFilesAreIgnored() {
        assertIgnored(".gitignore");
        assertIgnored("../.gitignore");
        assertIgnored("../clients/.gitignore");
        assertIgnored("a/b/c/.env");
        assertIgnored("../foo/.eslintrc.json");
    }

    // --- non-tool dotted folders keep their real source ----------------------------------------

    @Test
    void sourceUnderIncidentallyDottedFolderIsKept() {
        assertKept("src/.config/Foo.java");
        assertKept("../app/.generated/Bar.kt");
    }

    // --- dotted CI configs are exempted (handled by the build-and-deployment scope) ------------

    @Test
    void dottedCiConfigsAreNotIgnored() {
        assertKept(".gitlab-ci.yml");
        assertKept("../.gitlab-ci.yml");
        assertKept("../.travis.yml");
        assertKept("../sub/.drone.yml");
    }

    @Test
    void otherDottedConfigsAreStillIgnored() {
        // only the three CI configs are exempted; other dotfiles remain ignored
        assertIgnored("../.editorconfig");
        assertIgnored("../.npmrc");
    }
}
