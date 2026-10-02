package nl.obren.sokrates.reports.landscape.utils;

import nl.obren.sokrates.common.renderingutils.RacingChartItem;
import nl.obren.sokrates.common.renderingutils.VisualizationTemplate;
import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
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
    // Analysis year (honours the configurable analysis date), consistent with the other reports.
    private int currentYear = DateUtils.getAnalysisYear();
    // 1-based month, matching the yyyy-MM time-slot keys parsed in findStartYearAndMonth.
    private int currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
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
        String name = contribution.getLeft();
        int cumulativeCommits = 0;
        List<Integer> cumulativeCommitsList = new ArrayList<>();
        List<Integer> cumulativeContributorsList = new ArrayList<>();
        for (int year = startYear; year <= currentYear; year++) {
            int firstTwoMonthsCommits = 0;
            // Time slots are keyed by 1-based calendar month (yyyy-MM, 01..12); iterate accordingly.
            // startMonth is the 1-based month parsed in findStartYearAndMonth.
            int firstMonth = (year == startYear ? startMonth : 1);
            for (int month = firstMonth; month <= 12; month++) {
                String key = name + "::" + year + "-" + (month < 10 ? "0" : "") + month;
                int monthCommitsValue = commitsMap.containsKey(key) ? commitsMap.get(key) : 0;
                int monthContributorsValue = contributorsMap.containsKey(key) ? contributorsMap.get(key) : 0;
                pushWindow(cumulativeCommitsList, monthCommitsValue);
                pushWindow(cumulativeContributorsList, monthContributorsValue);
                // The racing-chart x-axis fits 10 frames per year (year + 0.0 .. year + 0.9). Fold the
                // first two months of the year into the third frame so the remaining 10 months map to
                // those 10 slots. monthFrame is the 0-based position of the rendered frame within the year.
                int monthFrame = month - 3;
                int valueCommits = monthCommitsValue;
                if (month < 3) {
                    firstTwoMonthsCommits += valueCommits;
                    continue;
                } else {
                    valueCommits += firstTwoMonthsCommits / 10.0;
                }
                // Accumulate first, then emit: the previous order gated on cumulativeCommits before
                // adding the current month, so the running total never left 0 and the cumulative
                // ("all time") chart stayed empty ("No data to display").
                cumulativeCommits += valueCommits;
                double frameYear = year + monthFrame / 10.0;
                if (Math.round(cumulativeCommits) > 0) {
                    addItem(items, name, Math.round(cumulativeCommits), frameYear);
                }
                double averageContributorsPerMonth = cumulativeContributorsList.stream().collect(Collectors.averagingDouble(Integer::intValue));
                if (averageContributorsPerMonth > 0.1) {
                    addItem(itemsContributorsPerMonth, name, Math.round(100.0 * averageContributorsPerMonth) / 100.0, frameYear);
                }
                int sumCommits = cumulativeCommitsList.stream().collect(Collectors.summingInt(Integer::intValue));
                if (Math.round(sumCommits) > 0) {
                    addItem(items12Month, name, Math.round(sumCommits), frameYear);
                }
            }
        }
    }

    /** Appends the month's value to a sliding window of the last windowSize months. */
    private void pushWindow(List<Integer> window, int value) {
        window.add(value);
        if (window.size() > windowSize) {
            window.remove(0);
        }
    }

    private static void addItem(List<RacingChartItem> list, String name, double value, double frameYear) {
        RacingChartItem item = new RacingChartItem(name);
        item.setValue(value);
        item.setYear(frameYear);
        list.add(item);
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
