package nl.obren.sokrates.reports.landscape.data;

import nl.obren.sokrates.reports.landscape.statichtml.LandscapeReportGenerator;
import nl.obren.sokrates.reports.landscape.statichtml.repositories.TagMap;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FilesHistoryAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.landscape.SokratesRepositoryLink;
import nl.obren.sokrates.sourcecode.landscape.analysis.RepositoryAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.MetricRangeControl;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class RepositoryExport {
    private final Metadata metadata;
    private final String latestCommitDate;
    private final int commitsCount30Days;
    private final int commitsCount90Days;
    private final int commitsCount;

    private List<String> contributors30Days;
    private List<String> contributors90Days;
    private List<String> contributors;
    // Internal only (used to compute reportFolderUrl) — not exported (no getter), the template
    // reads reportFolderUrl directly.
    private SokratesRepositoryLink sokratesRepositoryLink;
    // Only lines-of-code per scope is rendered by repositories-report.html; the per-scope FILE
    // counts were never read, so they aren't kept as fields (they bloated every repo row's JSON).
    private int mainLinesOfCode;
    private int testLinesOfCode;
    private int generatedLinesOfCode;
    private int buildAndDeployLinesOfCode;
    private int otherLinesOfCode;
    private String mainLang;
    // Every language (file extension, lowercased) the repository contains any code in, across all
    // scopes — lets the report filter with includesLang:<lang> even when it is not the main language.
    private List<String> langs = new ArrayList<>();
    // The same, split per scope (main/test/build/generated/other) — lets the report scope an
    // includesLang: filter to specific scopes (e.g. includesLang:main:tf).
    private Map<String, List<String>> langsByScope = new LinkedHashMap<>();

    // Richer fields consumed by the client-rendered landscape repositories report (repositories.html).
    private int commitsCount180Days;
    private int ageInDays;
    private int ageYears;
    private String firstDate = "";
    private int recentContributorsCount;
    private int rookiesCount;
    private String reportFolderUrl = "";
    private List<RepositoryReportData.Tag> tags = new ArrayList<>();
    private RepositoryReportData.History weeks;
    private RepositoryReportData.History years;
    private RepositoryReportData.Metrics repositoryMetrics;
    // year -> contributor emails active that year; lets the report count DISTINCT
    // contributors per year across repositories (counts cannot simply be summed).
    private Map<String, List<String>> contributorYears = new LinkedHashMap<>();

    public RepositoryExport(RepositoryAnalysisResults repository) {
        this(repository, null, null, null);
    }

    /**
     * @param repository       the analysed repository
     * @param tagMap           custom tags map (may be null — then no tags are exported)
     * @param configuration    landscape configuration (may be null — then thresholds/url-prefix/history defaults apply)
     * @param latestCommitDate landscape-wide latest commit date used to window the history slots (may be null)
     */
    public RepositoryExport(RepositoryAnalysisResults repository, TagMap tagMap, LandscapeConfiguration configuration, String latestCommitDate) {
        CodeAnalysisResults analysis = repository.getAnalysisResults();

        metadata = analysis.getMetadata();

        ContributorsAnalysisResults contributorsAnalysisResults = analysis.getContributorsAnalysisResults();
        this.latestCommitDate = contributorsAnalysisResults.getLatestCommitDate();
        commitsCount = contributorsAnalysisResults.getCommitsCount();
        commitsCount30Days = contributorsAnalysisResults.getCommitsCount30Days();
        commitsCount90Days = contributorsAnalysisResults.getCommitsCount90Days();
        commitsCount180Days = contributorsAnalysisResults.getCommitsCount180Days();

        contributors30Days = new ArrayList<>();
        contributors90Days = new ArrayList<>();
        contributors = new ArrayList<>();

        contributorsAnalysisResults.getContributors().forEach(contributor -> {
            if (contributor.getCommitsCount30Days() > 0) {
                contributors30Days.add(contributor.getEmail());
            }
            if (contributor.getCommitsCount90Days() > 0) {
                contributors90Days.add(contributor.getEmail());
            }
            contributors.add(contributor.getEmail());
            if (contributor.getActiveYears() != null) {
                contributor.getActiveYears().forEach(year ->
                        contributorYears.computeIfAbsent(year, k -> new ArrayList<>()).add(contributor.getEmail()));
            }
        });

        // Recent / rookie contributor counts mirror LandscapeRepositoriesReport.addRepositoryRow:
        // filter to contributors above the commit threshold, then by activity in the recent window.
        int thresholdCommits = configuration != null ? configuration.getContributorThresholdCommits() : 1;
        List<Contributor> thresholdContributors = contributorsAnalysisResults.getContributors().stream()
                .filter(c -> c.getCommitsCount() >= thresholdCommits)
                .collect(Collectors.toCollection(ArrayList::new));
        recentContributorsCount = (int) thresholdContributors.stream()
                .filter(c -> c.isActive(LandscapeReportGenerator.RECENT_THRESHOLD_DAYS)).count();
        rookiesCount = (int) thresholdContributors.stream()
                .filter(c -> c.isRookie(LandscapeReportGenerator.RECENT_THRESHOLD_DAYS)).count();

        sokratesRepositoryLink = repository.getSokratesRepositoryLink();

        AspectAnalysisResults main = analysis.getMainAspectAnalysisResults();
        AspectAnalysisResults test = analysis.getTestAspectAnalysisResults();
        AspectAnalysisResults build = analysis.getBuildAndDeployAspectAnalysisResults();
        AspectAnalysisResults generated = analysis.getGeneratedAspectAnalysisResults();
        AspectAnalysisResults other = analysis.getOtherAspectAnalysisResults();

        mainLinesOfCode = main.getLinesOfCode();
        testLinesOfCode = test.getLinesOfCode();
        generatedLinesOfCode = generated.getLinesOfCode();
        buildAndDeployLinesOfCode = build.getLinesOfCode();
        otherLinesOfCode = other.getLinesOfCode();

        // The dominant main-aspect extension is the repository's main language.
        // getLinesOfCodePerExtension() is sorted by LOC descending; names look like "  *.java".
        List<NumericMetric> mainPerExtension = main.getLinesOfCodePerExtension();
        if (mainPerExtension != null && !mainPerExtension.isEmpty()) {
            mainLang = mainPerExtension.get(0).getName().replace("*.", "").trim().toLowerCase();
        }

        // Languages the repository contains any code in, per scope (deduplicated, ordered by LOC
        // desc as getLinesOfCodePerExtension() already is). This lets the report scope an
        // includesLang: filter to specific scopes (e.g. main only), so the repositories list can
        // match what an Overview section that shows a single scope displays.
        langsByScope.put("main", scopeLangs(main));
        langsByScope.put("test", scopeLangs(test));
        langsByScope.put("build", scopeLangs(build));
        langsByScope.put("generated", scopeLangs(generated));
        langsByScope.put("other", scopeLangs(other));

        // Every language across all scopes (main first), for an unscoped includesLang: filter.
        Set<String> seenLangs = new LinkedHashSet<>();
        langsByScope.values().forEach(seenLangs::addAll);
        langs = new ArrayList<>(seenLangs);

        FilesHistoryAnalysisResults filesHistory = analysis.getFilesHistoryAnalysisResults();
        ageInDays = filesHistory.getAgeInDays();
        ageYears = (int) Math.round(ageInDays / 365.0);
        firstDate = filesHistory.getFirstDate() != null ? filesHistory.getFirstDate() : "";

        if (configuration != null) {
            reportFolderUrl = configuration.getRepositoryReportsUrlPrefix()
                    + sokratesRepositoryLink.getHtmlReportsRoot() + "/";
        }

        if (tagMap != null) {
            tagMap.getRepositoryTags(repository).forEach(tag -> {
                String color = tag.getGroup() != null && StringUtils.isNotBlank(tag.getGroup().getColor())
                        ? tag.getGroup().getColor() : "#99badd";
                tags.add(new RepositoryReportData.Tag(tag.getTag(), color));
            });
        }

        weeks = buildHistory(contributorsAnalysisResults.getContributorsPerWeek(), 52, latestCommitDate, true);
        int historyYears = configuration != null ? configuration.getRepositoriesHistoryLimit() : 20;
        years = buildHistory(contributorsAnalysisResults.getContributorsPerYear(), historyYears, latestCommitDate, false);

        repositoryMetrics = buildMetrics(analysis, configuration);
    }

    // The languages (lowercased extensions, deduplicated, LOC-desc order) with any code in one
    // aspect/scope. getLinesOfCodePerExtension() is already sorted by LOC descending.
    private static List<String> scopeLangs(AspectAnalysisResults aspect) {
        List<String> langs = new ArrayList<>();
        if (aspect == null || aspect.getLinesOfCodePerExtension() == null) {
            return langs;
        }
        aspect.getLinesOfCodePerExtension().forEach(metric -> {
            if (metric.getValue() != null && metric.getValue().intValue() > 0) {
                String lang = metric.getName().replace("*.", "").trim().toLowerCase();
                if (!lang.isEmpty() && !langs.contains(lang)) {
                    langs.add(lang);
                }
            }
        });
        return langs;
    }

    private static RepositoryReportData.History buildHistory(List<ContributionTimeSlot> slots, int limit, String latestCommitDate, boolean weekly) {
        RepositoryReportData.History history = new RepositoryReportData.History();
        if (latestCommitDate == null) {
            return history;
        }
        List<ContributionTimeSlot> windowed = weekly
                ? LandscapeReportGenerator.getContributionWeeks(slots, limit, latestCommitDate)
                : LandscapeReportGenerator.getContributionYears(slots, limit, latestCommitDate);
        windowed.sort(Comparator.comparing(ContributionTimeSlot::getTimeSlot).reversed());
        if (windowed.size() > limit) {
            windowed = windowed.subList(0, limit);
        }
        windowed.forEach(slot -> history.add(slot.getTimeSlot(), slot.getCommitsCount(), slot.getContributorsCount()));
        return history;
    }

    private static RepositoryReportData.Metrics buildMetrics(CodeAnalysisResults analysis, LandscapeConfiguration configuration) {
        RepositoryReportData.Metrics m = new RepositoryReportData.Metrics();
        m.setSkipDuplication(analysis.skipDuplicationAnalysis());
        m.setDuplicationPercentage(analysis.getDuplicationAnalysisResults().getOverallDuplication()
                .getDuplicationPercentage().doubleValue());
        m.setFileSize(new RepositoryReportData.RiskBands(analysis.getFilesAnalysisResults().getOverallFileSizeDistribution()));
        m.setUnitSize(new RepositoryReportData.RiskBands(analysis.getUnitsAnalysisResults().getUnitSizeRiskDistribution()));
        m.setConditionalComplexity(new RepositoryReportData.RiskBands(analysis.getUnitsAnalysisResults().getConditionalComplexityRiskDistribution()));
        m.setNewness(new RepositoryReportData.RiskBands(analysis.getFilesHistoryAnalysisResults().getOverallFileFirstModifiedDistribution()));
        m.setFreshness(new RepositoryReportData.RiskBands(analysis.getFilesHistoryAnalysisResults().getOverallFileLastModifiedDistribution()));
        m.setUpdateFrequency(new RepositoryReportData.RiskBands(analysis.getFilesHistoryAnalysisResults().getOverallFileChangeDistribution()));

        if (configuration != null && configuration.isShowRepositoryControls()) {
            List<RepositoryReportData.Control> controls = new ArrayList<>();
            analysis.getControlResults().getGoalsAnalysisResults().forEach(goals ->
                    goals.getControlStatuses().forEach(status -> {
                        MetricRangeControl control = status.getControl();
                        String description = control.getDescription() + "\n"
                                + control.getDesiredRange().getTextDescription() + "\n\n"
                                + status.getMetric().getValue();
                        controls.add(new RepositoryReportData.Control(status.getStatus(), description,
                                "" + status.getMetric().getValue()));
                    }));
            m.setControls(controls);
        }
        return m;
    }

    public int getMainLinesOfCode() {
        return mainLinesOfCode;
    }

    public int getTestLinesOfCode() {
        return testLinesOfCode;
    }

    public int getGeneratedLinesOfCode() {
        return generatedLinesOfCode;
    }

    public int getBuildAndDeployLinesOfCode() {
        return buildAndDeployLinesOfCode;
    }

    public int getOtherLinesOfCode() {
        return otherLinesOfCode;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public String getLatestCommitDate() {
        return latestCommitDate;
    }

    public int getCommitsCount30Days() {
        return commitsCount30Days;
    }

    public int getCommitsCount90Days() {
        return commitsCount90Days;
    }

    public List<String> getContributors30Days() {
        return contributors30Days;
    }

    public List<String> getContributors90Days() {
        return contributors90Days;
    }

    public List<String> getContributors() {
        return contributors;
    }

    public int getCommitsCount() {
        return commitsCount;
    }

    public String getMainLang() {
        return mainLang;
    }

    public List<String> getLangs() {
        return langs;
    }

    public Map<String, List<String>> getLangsByScope() {
        return langsByScope;
    }

    public int getCommitsCount180Days() {
        return commitsCount180Days;
    }

    public int getAgeInDays() {
        return ageInDays;
    }

    public int getAgeYears() {
        return ageYears;
    }

    public String getFirstDate() {
        return firstDate;
    }

    public int getRecentContributorsCount() {
        return recentContributorsCount;
    }

    public int getRookiesCount() {
        return rookiesCount;
    }

    public String getReportFolderUrl() {
        return reportFolderUrl;
    }

    public List<RepositoryReportData.Tag> getTags() {
        return tags;
    }

    public RepositoryReportData.History getWeeks() {
        return weeks;
    }

    public RepositoryReportData.History getYears() {
        return years;
    }

    public RepositoryReportData.Metrics getRepositoryMetrics() {
        return repositoryMetrics;
    }

    public Map<String, List<String>> getContributorYears() {
        return contributorYears;
    }

}
