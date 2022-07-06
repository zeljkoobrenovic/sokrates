package nl.obren.sokrates.reports.landscape.utils;

import nl.obren.sokrates.common.renderingutils.RacingChartItem;
import nl.obren.sokrates.common.renderingutils.VisualizationTemplate;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class RacingRepositoriesBarChartsExporter {
    private int windowSize = 12;

    private LandscapeAnalysisResults landscapeAnalysisResults;
    private List<Pair<String, List<ContributionTimeSlot>>> contributions;
    private int currentYear = Calendar.getInstance().get(Calendar.YEAR);
    private int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
    private int startYear = currentYear;
    private int startMonth = currentMonth;
    private Map<String, Integer> commitsMap = new HashMap<>();
    private Map<String, Integer> contributorsMap = new HashMap<>();
    private List<RacingChartItem> items = new ArrayList<>();
    private List<RacingChartItem> itemsContributorsPerMonth = new ArrayList<>();
    private List<RacingChartItem> items12Month = new ArrayList<>();
    private String suffix = "";

    public RacingRepositoriesBarChartsExporter(LandscapeAnalysisResults landscapeAnalysisResults, List<Pair<String, List<ContributionTimeSlot>>> contributions, String suffix) {
        this.landscapeAnalysisResults = landscapeAnalysisResults;
        this.contributions = contributions;
        this.suffix = suffix;
    }

    public void exportRacingChart(File reportsFolder) {
        findStartYearAndMonth();

        for (Pair<String, List<ContributionTimeSlot>> contribution : contributions) {
            processRepositoryMonth(contribution);
        }

        save(reportsFolder);
    }

    private void processRepositoryMonth(Pair<String, List<ContributionTimeSlot>> contribution) {
        int cumulativeCommits = 0;
        int prevSumCommits = 0;
        double prevAverageMontlhyContributors = 0;
        List<Integer> cumulativeCommitsList = new ArrayList<>();
        List<Integer> cumulativeContributorsList = new ArrayList<>();
        for (int year = startYear; year <= currentYear; year++) {
            int firstTwoMonthsCommits = 0;
            for (int month = (year == startYear ? startMonth - 1 : 0); month < 12; month++) {
                String name = contribution.getLeft();
                String key = name + "::" + year + "-" + (month < 10 ? "0" : "") + month;
                int monthCommitsValue = commitsMap.containsKey(key) ? commitsMap.get(key) : 0;
                int monthContributorsValue = contributorsMap.containsKey(key) ? contributorsMap.get(key) : 0;
                cumulativeCommitsList.add(monthCommitsValue);
                cumulativeContributorsList.add(monthContributorsValue);
                if (cumulativeCommitsList.size() > windowSize) {
                    cumulativeCommitsList.remove(0);
                }
                if (cumulativeContributorsList.size() > windowSize) {
                    cumulativeContributorsList.remove(0);
                }
                int valueCommits = monthCommitsValue;
                if (month < 2) {
                    firstTwoMonthsCommits += valueCommits;
                    continue;
                } else {
                    valueCommits += firstTwoMonthsCommits / 10.0;
                }
                if (Math.round(cumulativeCommits) > 0) {
                    RacingChartItem itemCommits = new RacingChartItem(name);
                    cumulativeCommits += valueCommits;
                    itemCommits.setValue(Math.round(cumulativeCommits) > 0 ? Math.round(cumulativeCommits) : 0.1);
                    itemCommits.setYear(year + (month - 2) / 10.0);
                    items.add(itemCommits);
                }

                double averageContributorsPerMonth = cumulativeContributorsList.stream().collect(Collectors.averagingDouble(Integer::intValue));
                if (averageContributorsPerMonth > 0.1) {
                    RacingChartItem itemContributorsPerMonth = new RacingChartItem(name);
                    itemContributorsPerMonth.setValue(averageContributorsPerMonth > 0.1 ? Math.round(100.0 * averageContributorsPerMonth) / 100 : 0.1);
                    itemContributorsPerMonth.setYear(year + (month - 2) / 10.0);
                    itemsContributorsPerMonth.add(itemContributorsPerMonth);
                }

                int sumCommits = cumulativeCommitsList.stream().collect(Collectors.summingInt(Integer::intValue));
                if (Math.round(sumCommits) > 0) {
                    RacingChartItem itemCommitsSum12Months = new RacingChartItem(name);
                    itemCommitsSum12Months.setValue(Math.round(sumCommits) > 0 ? Math.round(sumCommits) : 0.1);
                    itemCommitsSum12Months.setYear(year + (month - 2) / 10.0);
                    items12Month.add(itemCommitsSum12Months);
                }
            }
        }
    }

    private void save(File reportsFolder) {
        File folder = new File(reportsFolder, "visuals");
        folder.mkdirs();
        try {
            String start = startYear + "." + (startMonth <= 2 ? 1 : startMonth - 3);
            FileUtils.write(new File(folder, "racing_charts_commits_" + suffix + ".html"),
                    new VisualizationTemplate().renderRacingCharts(items, start, "Commits since " + startYear + " (cumulative)"), UTF_8);
            FileUtils.write(new File(folder, "racing_charts_commits_window_" + suffix + ".html"),
                    new VisualizationTemplate().renderRacingCharts(items12Month, start, "Commits since " + startYear + " (" + windowSize + " months window)"), UTF_8);

            if (suffix.equalsIgnoreCase("repositories")) {
                FileUtils.write(new File(folder, "racing_charts_contributors_per_month_" + suffix + ".html"),
                        new VisualizationTemplate().renderRacingCharts(itemsContributorsPerMonth, start,
                                "Contributors per month since " + startYear + " (average over " + windowSize + " months)"), UTF_8);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void findStartYearAndMonth() {
        contributions.forEach(contribution -> {
            contribution.getRight().stream().filter(c -> c.getCommitsCount() > 0).forEach(monthlyContribution -> {
                String timeSlots[] = monthlyContribution.getTimeSlot().split("-");
                if (timeSlots.length > 1 && StringUtils.isNumeric(timeSlots[0]) && StringUtils.isNumeric(timeSlots[1])) {
                    int year = Integer.parseInt(timeSlots[0]);
                    int month = Integer.parseInt(timeSlots[1]);
                    if (year < startYear) {
                        startYear = year;
                        startMonth = month;
                    } else if (year == startYear) {
                        startMonth = Math.min(month, startMonth);
                    }
                    String key = contribution.getLeft() + "::" + monthlyContribution.getTimeSlot();
                    commitsMap.put(key, monthlyContribution.getCommitsCount());
                    contributorsMap.put(key, monthlyContribution.getContributorsCount());
                }
            });
        });

        int limit = landscapeAnalysisResults.getConfiguration().getCommitsMaxYears();
        if (currentYear - startYear + 1 > limit) {
            startYear = currentYear - (limit - 1);
            startMonth = 1;
        }
    }

}
