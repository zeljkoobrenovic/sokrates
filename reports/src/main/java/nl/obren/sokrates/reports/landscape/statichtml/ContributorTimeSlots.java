package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.reports.landscape.utils.*;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.landscape.*;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorRepositories;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import java.util.*;
import java.util.stream.Collectors;
import static nl.obren.sokrates.reports.landscape.statichtml.LandscapeReportGenerator.*;

/**
 * The contributor time-slot bookkeeping of the landscape contributors tab: per scope (main, test, build,
 * generated, other, unscoped, and the all-scope key {@link #ALL_SCOPE}), which contributors and rookies were
 * active in each week, day, month and year, each contributor's first and last commit date in the scope, and
 * the lookups the activity diagrams read. {@link #setCurrentScope} selects the scope the lookups answer for;
 * the panels render sequentially, so one mutable field is enough. Moved out of
 * {@link LandscapeReportContributorsTab}, which owns the rendering.
 */
class ContributorTimeSlots {
    private final List<ContributorRepositories> contributors;
    private final LandscapeAnalysisResults landscapeAnalysisResults;
    // Contributor/rookie time-slot maps, indexed by scope (main/test/build/generated/other/unscoped),
    // plus the all-scope key ALL_SCOPE. Each inner map is timeSlot -> distinct contributor emails. The
    // scope tabs in the activity diagrams select among these via currentScope; ALL_SCOPE backs the
    // "All" tab (and is the only one populated for analyses generated before per-scope contributor data
    // existed). The leaf getters read the currentScope's inner map.
    static final String ALL_SCOPE = "*";
    private final Map<String, Map<String, List<String>>> contributorsPerWeekMapByScope = new LinkedHashMap<>();
    private final Map<String, Map<String, List<String>>> rookiesPerWeekMapByScope = new LinkedHashMap<>();
    private final Map<String, Map<String, List<String>>> contributorsPerDayMapByScope = new LinkedHashMap<>();
    private final Map<String, Map<String, List<String>>> rookiesPerDayMapByScope = new LinkedHashMap<>();
    private final Map<String, Map<String, List<String>>> contributorsPerMonthMapByScope = new LinkedHashMap<>();
    private final Map<String, Map<String, List<String>>> rookiesPerMonthMapByScope = new LinkedHashMap<>();
    private final Map<String, Map<String, List<String>>> contributorsPerYearMapByScope = new LinkedHashMap<>();
    private final Map<String, Map<String, List<String>>> rookiesPerYearMapByScope = new LinkedHashMap<>();
    // Per-scope first/last commit date per contributor email, derived from commitDatesByScope, so the
    // "first/last contribution" rows stay consistent with the selected scope. email -> "yyyy-MM-dd".
    private final Map<String, Map<String, String>> firstCommitDateByScope = new LinkedHashMap<>();
    private final Map<String, Map<String, String>> lastCommitDateByScope = new LinkedHashMap<>();
    // The scope whose maps the leaf getters currently read. Set before rendering each scope panel; the
    // panels render sequentially (addScopeToggle invokes each Runnable in turn), so a single mutable
    // field is safe. Defaults to ALL_SCOPE (the only scope when there is no scope toggle).
    private String currentScope = ALL_SCOPE;

    ContributorTimeSlots(List<ContributorRepositories> contributors, LandscapeAnalysisResults landscapeAnalysisResults) {
        this.contributors = contributors;
        this.landscapeAnalysisResults = landscapeAnalysisResults;
    }

    String getCurrentScope() {
        return currentScope;
    }

    void setCurrentScope(String scope) {
        this.currentScope = scope;
    }

    /** The scope keys any contributor carries data for (besides ALL_SCOPE); empty for analyses without per-scope data. */
    Set<String> scopesWithData() {
        return contributorsPerYearMapByScope.keySet();
    }

    // The churn/commits ContributionTimeSlot list for the current scope: the all-scope landscape
    // aggregate for ALL_SCOPE, otherwise the per-scope landscape aggregate (empty list when that scope
    // carries no data). Used by the activity charts; the contributor-count rows read the per-scope maps
    // via currentScope independently.
    List<ContributionTimeSlot> scopedYear() {
        if (ALL_SCOPE.equals(currentScope)) return landscapeAnalysisResults.getContributorsPerYear();
        return landscapeAnalysisResults.getContributorsPerYearByScope().getOrDefault(currentScope, new ArrayList<>());
    }

    List<ContributionTimeSlot> scopedMonth() {
        if (ALL_SCOPE.equals(currentScope)) return landscapeAnalysisResults.getContributorsPerMonth();
        return landscapeAnalysisResults.getContributorsPerMonthByScope().getOrDefault(currentScope, new ArrayList<>());
    }

    List<ContributionTimeSlot> scopedWeek() {
        if (ALL_SCOPE.equals(currentScope)) return landscapeAnalysisResults.getContributorsPerWeek();
        return landscapeAnalysisResults.getContributorsPerWeekByScope().getOrDefault(currentScope, new ArrayList<>());
    }

    List<ContributionTimeSlot> scopedDay() {
        if (ALL_SCOPE.equals(currentScope)) return landscapeAnalysisResults.getContributorsPerDay();
        return landscapeAnalysisResults.getContributorsPerDayByScope().getOrDefault(currentScope, new ArrayList<>());
    }

    // Reads an inner (timeSlot -> emails) map for the current scope, falling back to an empty map when
    // the current scope has no entries (e.g. a scope with no contributors).
    Map<String, List<String>> scoped(Map<String, Map<String, List<String>>> byScope) {
        Map<String, List<String>> map = byScope.get(currentScope);
        return map != null ? map : Collections.emptyMap();
    }

    // Total commits for the current scope: the landscape all-scope total for ALL_SCOPE, otherwise the
    // sum of the scope's per-year commit counts. Drives the "commits" trend card so it tracks the tab.
    int scopedTotalCommits() {
        if (ALL_SCOPE.equals(currentScope)) {
            return landscapeAnalysisResults.getCommitsCount();
        }
        return scopedYear().stream().mapToInt(ContributionTimeSlot::getCommitsCount).sum();
    }

    // Distinct contributors for the current scope: the full contributor list for ALL_SCOPE, otherwise
    // the union of emails across the scope's per-year map. Drives the "contributors" trend card.
    int scopedTotalContributors() {
        if (ALL_SCOPE.equals(currentScope)) {
            return contributors.size();
        }
        Set<String> emails = new HashSet<>();
        scoped(contributorsPerYearMapByScope).values().forEach(emails::addAll);
        return emails.size();
    }

    int getContributorsCountPerYear(String year) {
        Map<String, List<String>> map = scoped(contributorsPerYearMapByScope);
        return map.containsKey(year) ? map.get(year).size() : 0;
    }

    void populateTimeSlotMaps() {
        // Always build the all-scope maps from each contributor's flat commit dates.
        contributors.forEach(cr -> populateTimeSlotMapsForScope(cr, ALL_SCOPE, cr.getContributor().getCommitDates()));

        // Build per-scope maps from each contributor's per-scope commit dates (empty for older
        // analyses, so those scopes simply stay absent and the toggle falls back to "All" only).
        contributors.forEach(cr -> {
            Map<String, List<String>> byScope = cr.getContributor().getCommitDatesByScope();
            if (byScope != null) {
                byScope.forEach((scope, dates) -> populateTimeSlotMapsForScope(cr, scope, dates));
            }
        });
    }

    void populateTimeSlotMapsForScope(ContributorRepositories contributorRepositories, String scope, List<String> commitDates) {
        if (commitDates == null || commitDates.isEmpty()) {
            return;
        }
        Map<String, List<String>> perDay = contributorsPerDayMapByScope.computeIfAbsent(scope, k -> new HashMap<>());
        Map<String, List<String>> rookiesDay = rookiesPerDayMapByScope.computeIfAbsent(scope, k -> new HashMap<>());
        Map<String, List<String>> perWeek = contributorsPerWeekMapByScope.computeIfAbsent(scope, k -> new HashMap<>());
        Map<String, List<String>> rookiesWeek = rookiesPerWeekMapByScope.computeIfAbsent(scope, k -> new HashMap<>());
        Map<String, List<String>> perMonth = contributorsPerMonthMapByScope.computeIfAbsent(scope, k -> new HashMap<>());
        Map<String, List<String>> rookiesMonth = rookiesPerMonthMapByScope.computeIfAbsent(scope, k -> new HashMap<>());
        Map<String, List<String>> perYear = contributorsPerYearMapByScope.computeIfAbsent(scope, k -> new HashMap<>());
        Map<String, List<String>> rookiesYear = rookiesPerYearMapByScope.computeIfAbsent(scope, k -> new HashMap<>());

        commitDates.forEach(day -> {
            String week = DateUtils.getWeekMonday(day);
            String month = DateUtils.getMonth(day);
            String year = DateUtils.getYear(day);

            updateTimeSlotMap(contributorRepositories, perDay, rookiesDay, day, day);
            updateTimeSlotMap(contributorRepositories, perWeek, rookiesWeek, week, week);
            updateTimeSlotMap(contributorRepositories, perMonth, rookiesMonth, month, month + "-01");
            updateTimeSlotMap(contributorRepositories, perYear, rookiesYear, year, year + "-01-01");
        });

        // Track this contributor's first/last commit day within the scope (min/max of its dates).
        String email = contributorRepositories.getContributor().getEmail();
        String min = commitDates.get(0);
        String max = commitDates.get(0);
        for (String d : commitDates) {
            if (d.compareTo(min) < 0) min = d;
            if (d.compareTo(max) > 0) max = d;
        }
        firstCommitDateByScope.computeIfAbsent(scope, k -> new HashMap<>()).merge(email, min, (a, b) -> a.compareTo(b) <= 0 ? a : b);
        lastCommitDateByScope.computeIfAbsent(scope, k -> new HashMap<>()).merge(email, max, (a, b) -> a.compareTo(b) >= 0 ? a : b);
    }

    List<String> getSignificantContributorsPerYear(List<ContributorRepositories> contributorRepositories, String year, boolean rookiesOnly, int thresholdCommitDays) {
        if (rookiesOnly) {
            return getLastContributorsPerYear(year, true);
        }
        // Count this year's commit DAYS within the current scope (per-scope dates when scoped, the flat
        // commit dates for the all-scope tab) so "significant" contributors are scope-consistent.
        return contributorRepositories.stream()
                .filter(c -> scopedCommitDates(c.getContributor()).stream().filter(d -> d.startsWith(year)).count() >= thresholdCommitDays)
                .map(c -> c.getContributor().getEmail())
                .collect(Collectors.toList());
    }

    // This contributor's commit dates for the current scope: the flat list for ALL_SCOPE, otherwise the
    // scope's entry from commitDatesByScope (empty if the contributor never touched that scope).
    List<String> scopedCommitDates(Contributor contributor) {
        if (ALL_SCOPE.equals(currentScope)) {
            return contributor.getCommitDates();
        }
        List<String> dates = contributor.getCommitDatesByScope().get(currentScope);
        return dates != null ? dates : Collections.emptyList();
    }

    // First/last commit date for an email within the current scope (the all-scope contributor dates for
    // ALL_SCOPE, otherwise the per-scope derived map). Empty string when the contributor has no activity
    // in the scope, which the callers treat as "no match".
    String scopedFirstCommitDate(Contributor contributor) {
        if (ALL_SCOPE.equals(currentScope)) {
            return contributor.getFirstCommitDate();
        }
        Map<String, String> map = firstCommitDateByScope.get(currentScope);
        return map != null ? map.getOrDefault(contributor.getEmail(), "") : "";
    }

    String scopedLastCommitDate(Contributor contributor) {
        if (ALL_SCOPE.equals(currentScope)) {
            return contributor.getLatestCommitDate();
        }
        Map<String, String> map = lastCommitDateByScope.get(currentScope);
        return map != null ? map.getOrDefault(contributor.getEmail(), "") : "";
    }

    void updateTimeSlotMap(ContributorRepositories contributorRepositories,
                                   Map<String, List<String>> map, Map<String, List<String>> rookiesMap, String key, String rookieDate) {
        boolean rookie = contributorRepositories.getContributor().isRookieAtDate(rookieDate);

        String email = contributorRepositories.getContributor().getEmail();
        if (map.containsKey(key)) {
            if (!map.get(key).contains(email)) {
                map.get(key).add(email);
            }
        } else {
            map.put(key, new ArrayList<>(Arrays.asList(email)));
        }
        if (rookie) {
            if (rookiesMap.containsKey(key)) {
                if (!rookiesMap.get(key).contains(email)) {
                    rookiesMap.get(key).add(email);
                }
            } else {
                rookiesMap.put(key, new ArrayList<>(Arrays.asList(email)));
            }
        }
    }

    List<String> getContributorsPerWeek(String week, boolean rookiesOnly) {
        Map<String, List<String>> map = scoped(rookiesOnly ? rookiesPerWeekMapByScope : contributorsPerWeekMapByScope);
        return map.containsKey(week) ? map.get(week) : new ArrayList<>();
    }

    List<String> getContributorsPerDay(String day, boolean rookiesOnly) {
        Map<String, List<String>> map = scoped(rookiesOnly ? rookiesPerDayMapByScope : contributorsPerDayMapByScope);
        return map.containsKey(day) ? map.get(day) : new ArrayList<>();
    }

    List<String> getLastContributorsPerWeek(String week, boolean first) {
        Map<String, String> emails = new HashMap<>();

        contributors.stream()
                .sorted((a, b) -> b.getContributor().getCommitsCount30Days() - a.getContributor().getCommitsCount30Days())
                .filter(c -> {
                    String f = scopedFirstCommitDate(c.getContributor());
                    String l = scopedLastCommitDate(c.getContributor());
                    return !f.isEmpty() && !DateUtils.getWeekMonday(f).equals(DateUtils.getWeekMonday(l));
                })
                .forEach(contributorRepositories -> {
                    Contributor contributor = contributorRepositories.getContributor();
                    String date = first ? scopedFirstCommitDate(contributor) : scopedLastCommitDate(contributor);
                    if (!date.isEmpty() && DateUtils.getWeekMonday(date).equals(week)) {
                        String email = contributor.getEmail();
                        emails.put(email, email);
                        return;
                    }
                });

        return new ArrayList<>(emails.values());
    }

    List<String> getLastContributorsPerDay(String day, boolean first) {
        Map<String, String> emails = new HashMap<>();

        contributors.stream()
                .sorted((a, b) -> b.getContributor().getCommitsCount30Days() - a.getContributor().getCommitsCount30Days())
                .filter(c -> {
                    String f = scopedFirstCommitDate(c.getContributor());
                    String l = scopedLastCommitDate(c.getContributor());
                    return !f.isEmpty() && !f.equals(l);
                })
                .forEach(contributorRepositories -> {
                    Contributor contributor = contributorRepositories.getContributor();
                    String date = first ? scopedFirstCommitDate(contributor) : scopedLastCommitDate(contributor);
                    if (!date.isEmpty() && date.equals(day)) {
                        String email = contributor.getEmail();
                        emails.put(email, email);
                        return;
                    }
                });

        return new ArrayList<>(emails.values());
    }

    List<String> getContributorsPerMonth(String month, boolean rookiesOnly) {
        Map<String, List<String>> map = scoped(rookiesOnly ? rookiesPerMonthMapByScope : contributorsPerMonthMapByScope);
        return map.containsKey(month) ? map.get(month) : new ArrayList<>();
    }

    List<String> getLastContributorsPerYear(String year, boolean first) {
        Map<String, String> emails = new HashMap<>();

        contributors.stream()
                .sorted((a, b) -> b.getContributor().getCommitsCount30Days() - a.getContributor().getCommitsCount30Days())
                .filter(c -> {
                    String f = scopedFirstCommitDate(c.getContributor());
                    String l = scopedLastCommitDate(c.getContributor());
                    return !f.isEmpty() && !DateUtils.getYear(l).equals(DateUtils.getYear(f));
                })
                .forEach(contributorRepositories -> {
                    Contributor contributor = contributorRepositories.getContributor();
                    String date = first ? scopedFirstCommitDate(contributor) : scopedLastCommitDate(contributor);
                    if (!date.isEmpty() && DateUtils.getYear(date).equals(year)) {
                        String email = contributor.getEmail();
                        // only look at contributors with at least N commit days per year (within the scope)
                        if (scopedCommitDates(contributor).size() >= landscapeAnalysisResults.getConfiguration().getSignificantContributorMinCommitDaysPerYear()) {
                            emails.put(email, email);
                        }
                        return;
                    }
                });

        return new ArrayList<>(emails.values());
    }

    List<String> getLastContributorsPerMonth(String month, boolean first) {
        Map<String, String> emails = new HashMap<>();

        contributors.stream()
                .sorted((a, b) -> b.getContributor().getCommitsCount30Days() - a.getContributor().getCommitsCount30Days())
                .filter(c -> {
                    String f = scopedFirstCommitDate(c.getContributor());
                    String l = scopedLastCommitDate(c.getContributor());
                    return !f.isEmpty() && !DateUtils.getMonth(l).equals(DateUtils.getMonth(f));
                })
                .forEach(contributorRepositories -> {
                    Contributor contributor = contributorRepositories.getContributor();
                    String date = first ? scopedFirstCommitDate(contributor) : scopedLastCommitDate(contributor);
                    if (!date.isEmpty() && DateUtils.getMonth(date).equals(month)) {
                        String email = contributor.getEmail();
                        emails.put(email, email);
                        return;
                    }
                });

        return new ArrayList<>(emails.values());
    }
}
