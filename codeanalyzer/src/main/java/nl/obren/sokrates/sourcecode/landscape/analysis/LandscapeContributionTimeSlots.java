package nl.obren.sokrates.sourcecode.landscape.analysis;

import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.threshold.Thresholds;
import org.apache.commons.lang3.tuple.Pair;
import java.util.*;
import java.util.function.Supplier;

/**
 * The landscape's contribution time slots summed over its repositories: commits and contributors per year, week, day and
 * month (all scopes and per scope), per repository and month, and per contributor. Computed on demand from the
 * filtered repositories; moved out of {@link LandscapeAnalysisResults}, whose getters delegate here.
 */
class LandscapeContributionTimeSlots {
    private final Supplier<List<RepositoryAnalysisResults>> repositories;
    private final Supplier<List<ContributorRepositories>> allContributors;

    LandscapeContributionTimeSlots(Supplier<List<RepositoryAnalysisResults>> repositories, Supplier<List<ContributorRepositories>> allContributors) {
        this.repositories = repositories;
        this.allContributors = allContributors;
    }

    public List<ContributionTimeSlot> getContributorsPerYear() {
        List<ContributionTimeSlot> list = new ArrayList<>();
        Map<String, ContributionTimeSlot> map = new HashMap<>();

        repositories.get().forEach(repositoryAnalysisResults -> {
            ContributorsAnalysisResults contributorsAnalysisResults = repositoryAnalysisResults.getAnalysisResults().getContributorsAnalysisResults();
            updateContributors(list, map, contributorsAnalysisResults.getContributorsPerYear());
        });

        Collections.sort(list, Comparator.comparing(ContributionTimeSlot::getTimeSlot).reversed());

        return list;
    }

    public List<ContributionTimeSlot> getContributorsPerWeek() {
        List<ContributionTimeSlot> list = new ArrayList<>();
        Map<String, ContributionTimeSlot> map = new HashMap<>();

        repositories.get().forEach(repositoryAnalysisResults -> {
            ContributorsAnalysisResults contributorsAnalysisResults = repositoryAnalysisResults.getAnalysisResults().getContributorsAnalysisResults();
            List<ContributionTimeSlot> contributorsPerWeek = contributorsAnalysisResults.getContributorsPerWeek();
            updateContributors(list, map, contributorsPerWeek);
        });

        Collections.sort(list, Comparator.comparing(ContributionTimeSlot::getTimeSlot));

        return list;
    }

    public List<ContributionTimeSlot> getContributorsPerDay() {
        List<ContributionTimeSlot> list = new ArrayList<>();
        Map<String, ContributionTimeSlot> map = new HashMap<>();

        repositories.get().forEach(repositoryAnalysisResults -> {
            ContributorsAnalysisResults contributorsAnalysisResults = repositoryAnalysisResults.getAnalysisResults().getContributorsAnalysisResults();
            List<ContributionTimeSlot> contributorsPerDay = contributorsAnalysisResults.getContributorsPerDay();
            updateContributors(list, map, contributorsPerDay);
        });

        Collections.sort(list, Comparator.comparing(ContributionTimeSlot::getTimeSlot));

        return list;
    }

    public List<ContributionTimeSlot> getContributorsPerMonth() {
        List<ContributionTimeSlot> list = new ArrayList<>();
        Map<String, ContributionTimeSlot> map = new HashMap<>();

        repositories.get().forEach(repositoryAnalysisResults -> {
            ContributorsAnalysisResults contributorsAnalysisResults = repositoryAnalysisResults.getAnalysisResults().getContributorsAnalysisResults();
            List<ContributionTimeSlot> contributorsPerMonth = contributorsAnalysisResults.getContributorsPerMonth();
            updateContributors(list, map, contributorsPerMonth);
        });

        Collections.sort(list, Comparator.comparing(ContributionTimeSlot::getTimeSlot).reversed());

        return list;
    }

    // Per-scope aggregations: sum each repository's per-scope time-slot maps (main/test/build/
    // generated/other/unscoped) into landscape-level totals, mirroring the all-scope getters above.
    // The per-scope maps already live on each repository's serialized ContributorsAnalysisResults
    // (read from analysisResults.json), so no re-analysis is needed. Repositories analyzed before
    // per-scope churn existed simply have empty maps and contribute nothing to a scope, so the
    // per-scope totals can be smaller than the all-scope total until every repository is re-analyzed.
    public Map<String, List<ContributionTimeSlot>> getContributorsPerYearByScope() {
        return aggregatePerScope(ContributorsAnalysisResults::getContributorsPerYearByScope, true);
    }

    public Map<String, List<ContributionTimeSlot>> getContributorsPerMonthByScope() {
        return aggregatePerScope(ContributorsAnalysisResults::getContributorsPerMonthByScope, true);
    }

    public Map<String, List<ContributionTimeSlot>> getContributorsPerWeekByScope() {
        return aggregatePerScope(ContributorsAnalysisResults::getContributorsPerWeekByScope, false);
    }

    public Map<String, List<ContributionTimeSlot>> getContributorsPerDayByScope() {
        return aggregatePerScope(ContributorsAnalysisResults::getContributorsPerDayByScope, false);
    }

    public List<Pair<String, List<ContributionTimeSlot>>> getContributorsPerRepositoryAndMonth() {
        List<Pair<String, List<ContributionTimeSlot>>> list = new ArrayList<>();

        repositories.get().forEach(repository -> {
            ContributorsAnalysisResults contributorsAnalysisResults = repository.getAnalysisResults().getContributorsAnalysisResults();
            List<ContributionTimeSlot> contributorsPerMonth = new ArrayList<>(contributorsAnalysisResults.getContributorsPerMonth());
            Collections.sort(contributorsPerMonth, Comparator.comparing(ContributionTimeSlot::getTimeSlot));
            String name = repository.getAnalysisResults().getMetadata().getName();
            list.add(Pair.of(name, contributorsPerMonth));
        });

        return list;
    }

    public List<Pair<String, List<ContributionTimeSlot>>> getContributorsCommits() {
        Map<String, Pair<String, Map<String, ContributionTimeSlot>>> map = new HashMap<>();

        allContributors.get().forEach(contributor -> {
            Map<String, ContributionTimeSlot> commits = new HashMap<>();
            contributor.getContributor().getCommitDates().forEach(commitDate -> {
                String month = DateUtils.getMonth(commitDate);
                if (commits.containsKey(month)) {
                    commits.get(month).setCommitsCount(commits.get(month).getCommitsCount() + 1);
                } else {
                    ContributionTimeSlot timeSlot = new ContributionTimeSlot(month, Thresholds.defaultCommitFilesCountThresholds());
                    timeSlot.setContributorsCount(1);
                    commits.put(month, timeSlot);
                }
            });
            String email = contributor.getContributor().getEmail();

            Pair<String, Map<String, ContributionTimeSlot>> pair = map.get(email);

            if (pair == null) {
                pair = Pair.of(email, new HashMap<>());
                map.put(email, pair);
            }

            Pair<String, Map<String, ContributionTimeSlot>> finalPair = pair;
            commits.values().forEach(commitTimeSlot -> {
                String timeSlot = commitTimeSlot.getTimeSlot();
                if (finalPair.getRight().containsKey(timeSlot)) {
                    finalPair.getRight().get(timeSlot).setCommitsCount(finalPair.getRight().get(timeSlot).getCommitsCount() + commitTimeSlot.getCommitsCount());
                } else {
                    ContributionTimeSlot contributionTimeSlot = new ContributionTimeSlot(timeSlot, Thresholds.defaultCommitFilesCountThresholds());
                    contributionTimeSlot.setCommitsCount(commitTimeSlot.getCommitsCount());
                    contributionTimeSlot.setContributorsCount(1);
                    finalPair.getRight().put(timeSlot, contributionTimeSlot);
                }
            });
        });

        List<Pair<String, List<ContributionTimeSlot>>> list = new ArrayList<>();

        map.values().forEach(pair -> list.add(Pair.of(pair.getLeft(), new ArrayList<>(pair.getRight().values()))));

        return list;
    }

    private Map<String, List<ContributionTimeSlot>> aggregatePerScope(
            java.util.function.Function<ContributorsAnalysisResults, Map<String, List<ContributionTimeSlot>>> perScopeMapGetter,
            boolean reversed) {
        // scope -> (timeSlot -> aggregated slot); LinkedHashMap preserves first-seen scope order.
        Map<String, List<ContributionTimeSlot>> resultLists = new LinkedHashMap<>();
        Map<String, Map<String, ContributionTimeSlot>> indexByScope = new LinkedHashMap<>();

        repositories.get().forEach(repositoryAnalysisResults -> {
            ContributorsAnalysisResults contributorsAnalysisResults = repositoryAnalysisResults.getAnalysisResults().getContributorsAnalysisResults();
            Map<String, List<ContributionTimeSlot>> perScope = perScopeMapGetter.apply(contributorsAnalysisResults);
            if (perScope == null) {
                return;
            }
            perScope.forEach((scope, slots) -> {
                List<ContributionTimeSlot> list = resultLists.computeIfAbsent(scope, k -> new ArrayList<>());
                Map<String, ContributionTimeSlot> map = indexByScope.computeIfAbsent(scope, k -> new HashMap<>());
                updateContributors(list, map, slots);
            });
        });

        Comparator<ContributionTimeSlot> comparator = Comparator.comparing(ContributionTimeSlot::getTimeSlot);
        if (reversed) {
            comparator = comparator.reversed();
        }
        for (List<ContributionTimeSlot> list : resultLists.values()) {
            Collections.sort(list, comparator);
        }

        return resultLists;
    }

    private void updateContributors(List<ContributionTimeSlot> list, Map<String, ContributionTimeSlot> map, List<ContributionTimeSlot> contributorsPerTimeSlot) {
        contributorsPerTimeSlot.forEach(timeSlot -> {
            ContributionTimeSlot contributionTimeSlot = map.get(timeSlot.getTimeSlot());
            if (contributionTimeSlot == null) {
                contributionTimeSlot = new ContributionTimeSlot(Thresholds.defaultFileUpdateFrequencyThresholds());
                contributionTimeSlot.setTimeSlot(timeSlot.getTimeSlot());
                contributionTimeSlot.setContributorsCount(timeSlot.getContributorsCount());
                contributionTimeSlot.setCommitsCount(timeSlot.getCommitsCount());
                contributionTimeSlot.setAiCoAuthoredCommitsCount(timeSlot.getAiCoAuthoredCommitsCount());
                contributionTimeSlot.addChurn(timeSlot.getLinesAdded(), timeSlot.getLinesDeleted());
                list.add(contributionTimeSlot);
                map.put(timeSlot.getTimeSlot(), contributionTimeSlot);
            } else {
                contributionTimeSlot.setContributorsCount(contributionTimeSlot.getContributorsCount() + timeSlot.getContributorsCount());
                contributionTimeSlot.setCommitsCount(contributionTimeSlot.getCommitsCount() + timeSlot.getCommitsCount());
                contributionTimeSlot.setAiCoAuthoredCommitsCount(contributionTimeSlot.getAiCoAuthoredCommitsCount() + timeSlot.getAiCoAuthoredCommitsCount());
                contributionTimeSlot.addChurn(timeSlot.getLinesAdded(), timeSlot.getLinesDeleted());
            }
        });
    }
}
