package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.renderingutils.RacingChartItem;
import nl.obren.sokrates.common.renderingutils.VisualizationTemplate;
import nl.obren.sokrates.sourcecode.analysis.results.HistoryPerExtension;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class RacingLanguagesBarChartsExporter {
    private LandscapeAnalysisResults landscapeAnalysisResults;
    private List<HistoryPerExtension> yearlyCommitHistoryPerExtension;
    private List<String> extensions;
    private int currentYear = Calendar.getInstance().get(Calendar.YEAR);
    private int startYear = currentYear;
    private Map<String, HistoryPerExtension> yearExtensionMap = new HashMap<>();
    private List<RacingChartItem> commits = new ArrayList<>();
    private List<RacingChartItem> commitsWindow = new ArrayList<>();
    private List<RacingChartItem> contributors = new ArrayList<>();


    public RacingLanguagesBarChartsExporter(LandscapeAnalysisResults landscapeAnalysisResults, List<HistoryPerExtension> yearlyCommitHistoryPerExtension, List<String> extensions) {
        this.landscapeAnalysisResults = landscapeAnalysisResults;
        this.yearlyCommitHistoryPerExtension = yearlyCommitHistoryPerExtension;
        this.extensions = extensions;
    }

    public void exportRacingChart(File reportsFolder) {
        yearlyCommitHistoryPerExtension.stream().filter(year -> StringUtils.isNumeric(year.getYear())).forEach(yearExtension -> {
            startYear = Math.min(startYear, Integer.parseInt(yearExtension.getYear(), 10));
            String key = yearExtension.getExtension() + "::" + yearExtension.getYear();
            yearExtensionMap.put(key, yearExtension);
        });
        int limit = landscapeAnalysisResults.getConfiguration().getCommitsMaxYears();
        if (currentYear - startYear + 1 > limit) {
            startYear = currentYear - (limit - 1);
        }

        for (String extension : extensions) {
            processCommits(extension, commits, 100);
            processCommits(extension, commitsWindow, 1);
            processContributors(extension);
        }

        save(reportsFolder);
    }

    private void processCommits(String extension, List<RacingChartItem> list, int windowSize) {
        List<Double> values = new ArrayList<>();
        for (int year = startYear; year <= currentYear; year++) {
            String key = extension + "::" + year;
            HistoryPerExtension yearExtension = yearExtensionMap.get(key);
            double tickCommits = yearExtension != null ? yearExtension.getCommitsCount() / 10.0 : 0;
            for (int i = 0; i < 10; i++) {
                RacingChartItem itemCommits = new RacingChartItem(extension);
                double prevSum = values.stream().collect(Collectors.summingDouble(Double::doubleValue));
                itemCommits.setLastValue(prevSum > 0 ? prevSum : 0.1);
                values.add(tickCommits);
                if (values.size() > windowSize * 10) {
                    values.remove(0);
                }
                double sum = values.stream().collect(Collectors.summingDouble(Double::doubleValue));
                itemCommits.setValue(sum > 0 ? sum : 0.1);
                itemCommits.setYear(year + 0.1 * i);
                list.add(itemCommits);
            }
        }
    }
    private void processContributors(String extension) {
        int prevYearlyCount = 0;
        for (int year = startYear; year <= currentYear; year++) {
            String key = extension + "::" + year;
            HistoryPerExtension yearExtension = yearExtensionMap.get(key);
            int yearlyCount = yearExtension != null ? yearExtension.getContributors().size() : 0;
            int delta = yearlyCount - prevYearlyCount;
            double contributorsValue = prevYearlyCount;
            double tick = delta / 10.0;
            for (int i = 0; i < 10; i++) {
                RacingChartItem itemContributors = new RacingChartItem(extension);
                itemContributors.setLastValue(contributorsValue > 0 ? contributorsValue : 0.1);
                contributorsValue += tick;
                itemContributors.setValue(contributorsValue > 0 ? contributorsValue : 0.1);
                itemContributors.setYear(year + 0.1 * i);
                contributors.add(itemContributors);
            }
            prevYearlyCount = yearlyCount;
        }
    }

    private void save(File reportsFolder) {

        File folder = new File(reportsFolder, "visuals");
        folder.mkdirs();
        try {
            FileUtils.write(new File(folder, "racing_charts_extensions_commits.html"),
                    new VisualizationTemplate().renderRacingCharts(commits, startYear + "",
                            "Commits per file extensions since " + startYear + " (cumulative)"), UTF_8);
            FileUtils.write(new File(folder, "racing_charts_extensions_commits_window.html"),
                    new VisualizationTemplate().renderRacingCharts(commitsWindow, startYear + "",
                            "Commits per file extensions since " + startYear + " (12 months window)"), UTF_8);
            FileUtils.write(new File(folder, "racing_charts_extensions_contributors.html"),
                    new VisualizationTemplate().renderRacingCharts(contributors, startYear + "",
                            "Yearly contributors per file extensions since " + startYear), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
