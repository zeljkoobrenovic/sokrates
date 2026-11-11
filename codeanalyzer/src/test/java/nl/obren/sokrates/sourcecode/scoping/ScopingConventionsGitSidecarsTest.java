package nl.obren.sokrates.sourcecode.scoping;

import nl.obren.sokrates.sourcecode.githistory.GitHistoryUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the ignore conventions that keep the git-history export out of the scoped
 * source files.
 *
 * <p>{@code extractGitHistory} writes {@code git-history.txt} plus two optional sidecars next to it,
 * {@code git-commits.txt} and {@code git-commit-trailers.txt} (see {@link GitHistoryUtils}), and
 * {@code extractGitSubHistory} copies the sidecars into each split folder. All of them are analysis
 * inputs, not source code, so a repository whose configuration lists {@code txt} among its extensions
 * must not count them in any scope. The trailers file was not ignored: its rule was spelled
 * {@code git]-]commit[-]trailers} (two literal {@code ]} where a character class was meant), so it
 * matched nothing. {@code git-commits.txt} has no rule of its own but is covered by the generic
 * {@code git[-][a-zA-Z0-9_]+[.]txt} export rule, which the second hyphen in
 * {@code git-commit-trailers.txt} keeps out of.
 */
class ScopingConventionsGitSidecarsTest {

    private final ScopingConventions conventions = new ScopingConventions();

    private boolean matchesAny(List<Convention> rules, String path) {
        return rules.stream().anyMatch(c -> c.pathMatches(path));
    }

    private void assertIgnored(String path) {
        assertTrue(matchesAny(conventions.getIgnoredFilesConventions(), path),
                "expected to be ignored but was not: " + path);
    }

    private void assertNotIgnored(String path) {
        assertFalse(matchesAny(conventions.getIgnoredFilesConventions(), path),
                "expected NOT to be ignored but was: " + path);
    }

    // --- every file the history extraction writes is ignored --------------------------------------

    @Test
    void historyFileIsIgnored() {
        assertIgnored("/repo/" + GitHistoryUtils.GIT_HISTORY_FILE_NAME);
        assertIgnored("../" + GitHistoryUtils.GIT_HISTORY_FILE_NAME);
    }

    @Test
    void commitsSidecarIsIgnored() {
        assertIgnored("/repo/" + GitHistoryUtils.GIT_COMMITS_FILE_NAME);
        assertIgnored("../" + GitHistoryUtils.GIT_COMMITS_FILE_NAME);
    }

    @Test
    void commitTrailersSidecarIsIgnored() {
        assertIgnored("/repo/" + GitHistoryUtils.GIT_COMMIT_TRAILERS_FILE_NAME);
        assertIgnored("../" + GitHistoryUtils.GIT_COMMIT_TRAILERS_FILE_NAME);
    }

    @Test
    void mergesFileIsIgnored() {
        assertIgnored("/repo/git-merges.txt");
    }

    @Test
    void sidecarsCopiedIntoSubHistoryFoldersAreIgnored() {
        // extractGitSubHistory copies the sidecars verbatim into each split folder.
        assertIgnored("/repo/module-a/" + GitHistoryUtils.GIT_COMMITS_FILE_NAME);
        assertIgnored("/repo/module-a/" + GitHistoryUtils.GIT_COMMIT_TRAILERS_FILE_NAME);
    }

    @Test
    void windowsStyleSidecarPathsAreIgnored() {
        assertIgnored("C:\\repo\\" + GitHistoryUtils.GIT_COMMITS_FILE_NAME);
        assertIgnored("C:\\repo\\" + GitHistoryUtils.GIT_COMMIT_TRAILERS_FILE_NAME);
    }

    // --- the rules name one file each, not a family -----------------------------------------------

    @Test
    void similarlyNamedSourceFilesAreNotIgnored() {
        assertNotIgnored("/repo/notes/git-commits.md");
        assertNotIgnored("/repo/git-commits.txt.bak");
        assertNotIgnored("/repo/mygit-commits.txt");
        assertNotIgnored("/repo/git-commit-trailers.java");
        assertNotIgnored("/repo/git-commit-trailers-parser.txt");
    }
}
