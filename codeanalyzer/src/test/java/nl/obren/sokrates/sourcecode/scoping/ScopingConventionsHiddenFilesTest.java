package nl.obren.sokrates.sourcecode.scoping;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for how hidden / dotted files and directories are scoped.
 *
 * <p>Background: conventions are evaluated by {@link nl.obren.sokrates.sourcecode.SourceFileFilter#pathMatches}
 * against {@code sourceFile.getFile().getPath()}. With the default relative {@code srcRoot} of {@code ".."},
 * those paths look like {@code "../src/Foo.java"}. {@code pathMatches} additionally tests a slash-stripped
 * (backslash) variant of the path, so any {@code [^/]}-anchored dotfile rule risked matching the leading
 * {@code ".."} of every relative path and silently ignoring the whole repository.
 *
 * <p>The standard conventions no longer carry a blanket "Hidden files" ignore rule. Instead:
 * <ul>
 *   <li>plain dotfiles ({@code .gitignore}, {@code .env}, {@code .eslintrc.json}, …) are <b>kept</b>;</li>
 *   <li>only true VCS metadata directories ({@code .git/.svn/.hg/.bzr}) are <b>ignored</b>;</li>
 *   <li>IDE / build-tool hidden directories ({@code .idea}, {@code .vscode}, {@code .gradle}, …) are
 *       classified into the <b>build-and-deployment</b> scope, not ignored.</li>
 * </ul>
 * These tests pin that behaviour.
 */
class ScopingConventionsHiddenFilesTest {

    private final ScopingConventions conventions = new ScopingConventions();

    private boolean matchesAny(List<Convention> rules, String path) {
        return rules.stream().anyMatch(c -> c.pathMatches(path));
    }

    private void assertIgnored(String path) {
        assertTrue(matchesAny(conventions.getIgnoredFilesConventions(), path),
                "expected IGNORED but was kept: " + path);
    }

    private void assertNotIgnored(String path) {
        assertFalse(matchesAny(conventions.getIgnoredFilesConventions(), path),
                "expected KEPT (not ignored) but was ignored: " + path);
    }

    private void assertBuildAndDeployment(String path) {
        assertTrue(matchesAny(conventions.getBuildAndDeploymentFilesConventions(), path),
                "expected BUILD-AND-DEPLOYMENT but was not classified there: " + path);
    }

    // --- the core regression: a relative ".." srcRoot must not ignore everything ---------------

    @Test
    void relativeRootedSourceFilesAreNotIgnored() {
        assertNotIgnored("../clients/src/test/java/org/apache/kafka/clients/producer/ProducerIdExpirationTest.java");
        assertNotIgnored("../README.md");
        assertNotIgnored("../build.gradle");
    }

    @Test
    void relativeRootedSourceFilesAreNotIgnoredEvenInBackslashForm() {
        // pathMatches also tests the "/"->"\\" variant; the rules must hold there too.
        assertNotIgnored("..\\clients\\src\\Foo.java");
        assertNotIgnored("..\\README.md");
    }

    @Test
    void absoluteRootedSourceFilesAreNotIgnored() {
        assertNotIgnored("/Users/dev/Downloads/kafka/clients/src/Foo.java");
        assertNotIgnored("/Users/dev/project/README.md");
    }

    // --- tool/config dotfiles are scoped as build-and-deployment, not ignored ------------------

    @Test
    void toolConfigDotfilesAreBuildAndDeployment() {
        // package-manager / version config
        assertBuildAndDeployment("../.npmrc");
        assertBuildAndDeployment(".yarnrc");
        assertBuildAndDeployment("../.yarnrc.yml");
        assertBuildAndDeployment("../.nvmrc");
        assertBuildAndDeployment("../sub/.tool-versions");
        // linters / formatters, including family variants via the "([.].*)?" suffix
        assertBuildAndDeployment("../.eslintrc");
        assertBuildAndDeployment("../foo/.eslintrc.json");
        assertBuildAndDeployment("../.eslintrc.js");
        assertBuildAndDeployment("../.prettierrc");
        assertBuildAndDeployment("../.editorconfig");
        assertBuildAndDeployment("../.pylintrc");
        // ignore-style tool config
        assertBuildAndDeployment("../.dockerignore");
        assertBuildAndDeployment("../.npmignore");
        // git metadata files
        assertBuildAndDeployment("../.gitignore");
        assertBuildAndDeployment("../.gitattributes");
        assertBuildAndDeployment("../.gitkeep");
        // build tool config
        assertBuildAndDeployment("../.babelrc");
        assertBuildAndDeployment("../.babelrc.js");
        assertBuildAndDeployment("../.browserslistrc");
    }

    @Test
    void toolConfigDotfilesAreNotIgnored() {
        assertNotIgnored("../.npmrc");
        assertNotIgnored("../foo/.eslintrc.json");
        assertNotIgnored("../.editorconfig");
        assertNotIgnored("../.gitignore");
        assertNotIgnored("../.dockerignore");
        assertNotIgnored("../.babelrc");
    }

    // --- .env is the deliberate exception: ignored as it typically holds secrets ----------------

    @Test
    void envFilesAreIgnored() {
        assertIgnored(".env");
        assertIgnored("../.env");
        assertIgnored("a/b/c/.env");
        assertIgnored("../.env.local");
        assertIgnored("../.env.production");
    }

    @Test
    void dottedCiConfigsAreKept() {
        assertNotIgnored(".gitlab-ci.yml");
        assertNotIgnored("../.gitlab-ci.yml");
        assertNotIgnored("../.travis.yml");
        assertNotIgnored("../sub/.drone.yml");
    }

    // --- VCS metadata directories are still ignored at every depth -----------------------------

    @Test
    void vcsMetadataDirectoriesAreIgnored() {
        assertIgnored(".git/config");
        assertIgnored("../.git/config");
        assertIgnored("../clients/.svn/entries");
        assertIgnored("a/b/.hg/store/data.i");
        assertIgnored("../.bzr/checkout");
    }

    // --- IDE / build-tool hidden directories are scoped as build-and-deployment ----------------

    @Test
    void ideAndToolDirectoriesAreBuildAndDeployment() {
        assertBuildAndDeployment("../.idea/workspace.xml");
        assertBuildAndDeployment(".vscode/settings.json");
        assertBuildAndDeployment("../sub/.gradle/cache.properties");
        assertBuildAndDeployment("../.circleci/config.yml");
    }

    @Test
    void ideAndToolDirectoriesAreNotIgnored() {
        assertNotIgnored("../.idea/workspace.xml");
        assertNotIgnored(".vscode/settings.json");
        assertNotIgnored("../.circleci/config.yml");
    }

    // --- source under an incidentally-dotted folder is kept ------------------------------------

    @Test
    void sourceUnderIncidentallyDottedFolderIsKept() {
        assertNotIgnored("src/.config/Foo.java");
        assertNotIgnored("../app/.generated/Bar.kt");
    }
}
