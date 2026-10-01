package nl.obren.sokrates.cli.git;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class GitHubRepoFilterTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);

    private static GitHubRepo repo(String name, String pushedAt, boolean fork, boolean archived) {
        GitHubRepo repo = new GitHubRepo(name, "https://github.com/acme/" + name + ".git");
        repo.setFullName("acme/" + name);
        repo.setPushedAt(pushedAt);
        repo.setFork(fork);
        repo.setArchived(archived);
        return repo;
    }

    private static List<String> names(List<GitHubRepo> repos) {
        return repos.stream().map(GitHubRepo::getName).collect(Collectors.toList());
    }

    private final List<GitHubRepo> repos = List.of(
            repo("old-lib", "2024-01-15T10:00:00Z", false, false),
            repo("core", "2026-09-30T10:00:00Z", false, false),
            repo("forked-tool", "2026-09-29T10:00:00Z", true, false),
            repo("legacy", "2026-09-28T10:00:00Z", false, true),
            repo("web", "2026-09-20T10:00:00Z", false, false),
            repo("no-push-date", "", false, false));

    @Test
    void byDefaultForksAndArchivedAreDroppedAndTheRestIsNewestFirst() {
        GitHubRepoFilter filter = new GitHubRepoFilter();
        assertEquals(List.of("core", "web", "old-lib", "no-push-date"), names(filter.apply(repos, TODAY)));
        assertEquals(List.of("acme/forked-tool: fork", "acme/legacy: archived"), filter.getExclusions());
        assertEquals("forks excluded, archived excluded", filter.describe());
    }

    @Test
    void forksAndArchivedCanBeIncluded() {
        GitHubRepoFilter filter = new GitHubRepoFilter();
        filter.setIncludeForks(true);
        filter.setIncludeArchived(true);
        assertEquals(6, filter.apply(repos, TODAY).size());
        assertTrue(filter.getExclusions().isEmpty());
    }

    @Test
    void pushedWithinDaysDropsOldAndUndatedRepositories() {
        GitHubRepoFilter filter = new GitHubRepoFilter();
        filter.setPushedWithinDays(30);
        assertEquals(List.of("core", "web"), names(filter.apply(repos, TODAY)));
        assertTrue(filter.getExclusions().contains("acme/old-lib: last push 2024-01-15 is older than 30 days"));
        assertTrue(filter.getExclusions().contains("acme/no-push-date: last push unknown is older than 30 days"));
    }

    @Test
    void namePatternsMatchTheWholeNameOrOwnerName() {
        GitHubRepoFilter filter = new GitHubRepoFilter();
        filter.getIncludeNamePatterns().add("acme/.*");       // matches every full name
        filter.getExcludeNamePatterns().add("WEB");           // case-insensitive, whole name
        filter.getExcludeNamePatterns().add("old.*");
        assertEquals(List.of("core", "no-push-date"), names(filter.apply(repos, TODAY)));

        GitHubRepoFilter partial = new GitHubRepoFilter();
        partial.getIncludeNamePatterns().add("cor");          // not a whole-name match
        assertTrue(partial.apply(repos, TODAY).isEmpty());
    }

    @Test
    void maxReposKeepsTheMostRecentlyPushed() {
        GitHubRepoFilter filter = new GitHubRepoFilter();
        filter.setMaxRepos(2);
        assertEquals(List.of("core", "web"), names(filter.apply(repos, TODAY)));
        assertTrue(filter.getExclusions().contains("acme/old-lib: beyond the 2 most recently pushed repositories"));
    }

    @Test
    void invalidPatternIsReported() {
        GitHubRepoFilter filter = new GitHubRepoFilter();
        filter.getIncludeNamePatterns().add("(unclosed");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> filter.apply(repos, TODAY));
        assertTrue(e.getMessage().contains("(unclosed"));
    }
}
