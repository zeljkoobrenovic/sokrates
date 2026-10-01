package nl.obren.sokrates.cli.git;

import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Which of an organization's repositories analyzeGitHubOrg / analyzeGitLabGroup analyze. Pure and order-defining: the
 * result is sorted by last push (newest first), so {@code maxRepos} keeps the most active ones.
 * Defaults: forks and archived repositories are excluded, everything else is kept.
 */
public class CodeHostRepoFilter {
    private boolean includeForks = false;
    private boolean includeArchived = false;
    private int pushedWithinDays = 0;
    private int maxRepos = 0;
    private final List<String> includeNamePatterns = new ArrayList<>();
    private final List<String> excludeNamePatterns = new ArrayList<>();
    private final List<String> exclusions = new ArrayList<>();

    /** The kept repositories, newest push first; {@link #getExclusions()} says why the others were dropped. */
    public List<CodeHostRepo> apply(List<CodeHostRepo> repos, LocalDate referenceDate) {
        exclusions.clear();
        Instant cutoff = pushedWithinDays > 0 ? referenceDate.minusDays(pushedWithinDays).atStartOfDay().toInstant(ZoneOffset.UTC) : null;
        List<CodeHostRepo> kept = new ArrayList<>();
        for (CodeHostRepo repo : repos) {
            String reason = exclusionReason(repo, cutoff);
            if (reason == null) {
                kept.add(repo);
            } else {
                exclusions.add(repo + ": " + reason);
            }
        }
        kept.sort(Comparator.comparing(CodeHostRepo::getPushedAt, Comparator.reverseOrder()).thenComparing(CodeHostRepo::getName));
        if (maxRepos > 0 && kept.size() > maxRepos) {
            kept.subList(maxRepos, kept.size()).forEach(repo -> exclusions.add(repo + ": beyond the " + maxRepos + " most recently pushed repositories"));
            kept = new ArrayList<>(kept.subList(0, maxRepos));
        }
        return kept;
    }

    private String exclusionReason(CodeHostRepo repo, Instant cutoff) {
        if (!includeForks && repo.isFork()) {
            return "fork";
        }
        if (!includeArchived && repo.isArchived()) {
            return "archived";
        }
        if (cutoff != null) {
            Instant pushedAt = parseInstant(repo.getPushedAt());
            if (pushedAt == null || pushedAt.isBefore(cutoff)) {
                return "last push " + (StringUtils.isBlank(repo.getPushedAt()) ? "unknown" : repo.getPushedAt().substring(0, Math.min(10, repo.getPushedAt().length())))
                        + " is older than " + pushedWithinDays + " days";
            }
        }
        if (!includeNamePatterns.isEmpty() && !matchesAny(repo, includeNamePatterns)) {
            return "name matches no include pattern";
        }
        if (matchesAny(repo, excludeNamePatterns)) {
            return "name matches an exclude pattern";
        }
        return null;
    }

    // A pattern matches when it matches the whole repository name or the whole owner/name.
    private static boolean matchesAny(CodeHostRepo repo, List<String> patterns) {
        for (String pattern : patterns) {
            try {
                Pattern compiled = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
                if (compiled.matcher(repo.getName()).matches() || compiled.matcher(repo.getFullName()).matches()) {
                    return true;
                }
            } catch (PatternSyntaxException e) {
                throw new IllegalArgumentException("Invalid repository name pattern '" + pattern + "': " + e.getDescription());
            }
        }
        return false;
    }

    private static Instant parseInstant(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        try {
            return Instant.parse(text);
        } catch (Exception e) {
            return null;
        }
    }

    /** A one-line summary of the active criteria, for logs and the generated repos.txt header. */
    public String describe() {
        List<String> parts = new ArrayList<>();
        parts.add(includeForks ? "forks included" : "forks excluded");
        parts.add(includeArchived ? "archived included" : "archived excluded");
        if (pushedWithinDays > 0) {
            parts.add("pushed within " + pushedWithinDays + " days");
        }
        if (!includeNamePatterns.isEmpty()) {
            parts.add("name matches " + includeNamePatterns);
        }
        if (!excludeNamePatterns.isEmpty()) {
            parts.add("name not matching " + excludeNamePatterns);
        }
        if (maxRepos > 0) {
            parts.add("at most " + maxRepos + " repositories");
        }
        return String.join(", ", parts);
    }

    public List<String> getExclusions() {
        return exclusions;
    }

    public boolean isIncludeForks() {
        return includeForks;
    }

    public void setIncludeForks(boolean includeForks) {
        this.includeForks = includeForks;
    }

    public boolean isIncludeArchived() {
        return includeArchived;
    }

    public void setIncludeArchived(boolean includeArchived) {
        this.includeArchived = includeArchived;
    }

    public int getPushedWithinDays() {
        return pushedWithinDays;
    }

    public void setPushedWithinDays(int pushedWithinDays) {
        this.pushedWithinDays = pushedWithinDays;
    }

    public int getMaxRepos() {
        return maxRepos;
    }

    public void setMaxRepos(int maxRepos) {
        this.maxRepos = maxRepos;
    }

    public List<String> getIncludeNamePatterns() {
        return includeNamePatterns;
    }

    public List<String> getExcludeNamePatterns() {
        return excludeNamePatterns;
    }
}
