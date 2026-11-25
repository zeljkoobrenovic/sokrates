package nl.obren.sokrates.server.api;

import java.util.List;
import java.util.Map;

/**
 * The response shapes of the server's REST API and MCP tools (version {@link #VERSION}).
 * <p>
 * These are the stable contract: they are mapped from the analysis model (CodeAnalysisResults, the data.zip
 * entries, the AI findings files), which keeps changing with the analyzer, so a change there is absorbed by the
 * mapping in {@link nl.obren.sokrates.server.SokratesQueryService} rather than reaching API clients.
 * Add fields freely; renaming or removing one needs a new version.
 */
public final class Api {
    public static final String VERSION = "v1";

    private Api() {
    }

    /** One page of a longer list: {@code total} items match, {@code items} starts at {@code offset}. */
    public record Page<T>(int total, int offset, List<T> items) {
    }

    /** A landscape (a {@code _sokrates_landscape} folder) and the number of repository analyses below its root. */
    public record Landscape(String id, String name, String description, int repositories, String reportUrl) {
    }

    /** One row of the repository list. */
    public record RepositoryItem(String id, String name, String description, String mainLanguage,
                                 int mainLinesOfCode, int testLinesOfCode,
                                 ScoreBrief humanScore, ScoreBrief aiScore,
                                 int commits30Days, int contributors30Days, String latestCommitDate,
                                 int aiFindings, String reportUrl) {
    }

    public record ScoreBrief(double value, String grade) {
    }

    /** One repository's summary: size, languages, quality profiles, ease-of-change scores, activity, components. */
    public record RepositoryDetails(String id, String name, String description, List<Link> links,
                                    List<ScopeSize> scopes, List<LanguageSize> mainLanguages,
                                    Double duplicationPercentage,
                                    RiskProfile fileSize, RiskProfile unitSize, RiskProfile unitComplexity,
                                    Score humanScore, Score aiScore,
                                    Activity activity, List<Decomposition> decompositions,
                                    String reportUrl) {
    }

    public record Link(String label, String href) {
    }

    public record ScopeSize(String scope, int files, int linesOfCode) {
    }

    public record LanguageSize(String language, int linesOfCode) {
    }

    /** How the measured code is spread over the risk bands of one metric ({@code measure} names what the ranges are). */
    public record RiskProfile(String measure, List<RiskBand> bands) {
    }

    /** A risk band: its range of the metric, the lines of code in it and their share of the total (0-100). */
    public record RiskBand(String risk, String range, int linesOfCode, double percentage) {
    }

    /** An ease-of-change score (0-10, grade A-E), what holds it down and its sub-scores. */
    public record Score(double value, String grade, String heldDownBy, String coverage, List<SubScore> subScores) {
    }

    public record SubScore(String key, String label, double score, double weight, double drag, String measure) {
    }

    /** Commit activity, counted at analysis time (windows end at the analysis date, not at the time of the request). */
    public record Activity(int commits, int commits30Days, int commits90Days, int commits365Days,
                           int contributors, int contributors30Days, int churn30Days, int churn90Days,
                           String firstCommitDate, String latestCommitDate) {
    }

    public record Decomposition(String name, int componentsCount, List<ComponentSize> components) {
    }

    public record ComponentSize(String name, int files, int linesOfCode) {
    }

    /**
     * The main files most worth looking at first: complexity (the McCabe sum of the file's units, or its lines of
     * code when the language has no units) times how often the file changed in the window — the days it changed in
     * the past 365 days by default, as the report's Highlights tab ranks them ({@code basis} says which applied).
     */
    public record Hotspots(String repository, String window, String basis, List<Hotspot> items) {
    }

    /** {@code daysChanged365Days} is null for analyses made before the export carried it. */
    public record Hotspot(String path, int linesOfCode, int complexity, Integer daysChanged365Days, int daysChanged,
                          int commits30Days, int commits90Days, int commits, int contributors, String lastUpdated, long score) {
    }

    /** An AI finding written by a sokrates-skills scanner, severity normalized to critical/high/medium/low/info. */
    public record Finding(String repository, String scanner, String id, String group, String title, String severity,
                          String confidence, String description, String recommendation, List<String> tags, String url) {
    }

    /** A contributor as the analysis identified them (after the repository's ignore/anonymize/people-config rules). */
    public record Contributor(String email, String userName, boolean bot, int commits, int commits30Days, int commits90Days,
                              int commits365Days, String firstCommitDate, String latestCommitDate) {
    }

    /**
     * Who knows a part of the code (a file, a folder, a logical component and/or a language) in one repository or
     * across a landscape: the people who changed its main files, most active in the past 365 days first.
     * {@code fileChanges} counts a commit once per file it touched. {@code topContributorShare365Days} is the share
     * (0-100) of the past year's file changes by the first person, bots left out: a high share with few people is a
     * knowledge (bus-factor) risk. {@code repositories} (the repositories with matching files) is only set across
     * repositories; {@code repository} is then the landscape, or "all", and {@code repositoriesNotCovered} counts the
     * repositories whose analysis predates the per-file contributors export (regenerate them to include them).
     */
    public record Experts(String repository, String scope, int files, int filesChanged365Days, int contributors,
                          Double topContributorShare365Days, Integer repositories, Integer repositoriesNotCovered, List<Expert> items) {
    }

    /** {@code repositories} and {@code topRepositories} (most changes in the past year first) only across repositories. */
    public record Expert(String contributor, boolean bot, int files, int fileChanges, int fileChanges365Days, String lastChangeDate,
                         Integer repositories, List<String> topRepositories) {
    }

    /** The dependencies between the components of one logical decomposition (from → to, with the number of evidence places). */
    public record Dependencies(String repository, String decomposition, List<String> decompositions, int componentsCount,
                               int cyclicPairs, List<Dependency> items) {
    }

    /** {@code cyclic}: the two components also depend the other way around. */
    public record Dependency(String from, String to, int count, boolean cyclic) {
    }

    /**
     * A landscape (or every analysis) at a glance: counts and totals only, no people identities. Averages are weighted
     * by main lines of code; the grade distributions and {@code lowestHumanScores} only count repositories with at least
     * {@code minLinesForScoreRanking} main lines, as the scores of tiny repositories say little. Contributors are
     * people (merged, bots apart), so fewer than the distinct email addresses the landscape's own counters report.
     */
    public record LandscapeOverview(String landscape, String name, String description,
                                    int repositories, int repositoriesWithCode, int activeRepositories30Days, int activeRepositories365Days,
                                    List<ScopeSize> scopes, List<LanguageShare> mainLanguages,
                                    Map<String, Integer> humanGrades, Map<String, Integer> aiGrades,
                                    Double averageHumanScore, Double averageAiScore, Double duplicationPercentage,
                                    PortfolioActivity activity, Map<String, Integer> aiFindingsBySeverity,
                                    List<RepositoryItem> largest, List<RepositoryItem> mostActive30Days,
                                    int minLinesForScoreRanking, List<RepositoryItem> lowestHumanScores, String reportUrl) {
    }

    public record LanguageShare(String language, int linesOfCode, double percentage, int repositories) {
    }

    /** Commits summed over the repositories; contributors are distinct people (merged as the landscape merges them), bots left out. */
    public record PortfolioActivity(int commits30Days, int commits90Days, int commits365Days,
                                    int contributors30Days, int contributors365Days, int bots365Days) {
    }
}
