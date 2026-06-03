package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.renderingutils.RacingChartItem;
import nl.obren.sokrates.common.renderingutils.VisualizationTemplate;
import nl.obren.sokrates.reports.landscape.statichtml.LandscapeIndividualContributorsReports;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class RacingComponentsBarChartsExporter {
    // Each year of commits is spread over this many animation steps (sub-year increments). The
    // 12-month trailing window therefore spans exactly STEPS_PER_YEAR steps (one year of steps).
    private static final int STEPS_PER_YEAR = 10;

    private CodeAnalysisResults analysisResults;
    private String logicalDecompositionKey;
    private int logicalDecompositionIndex = 1;
    // Use the analysis year (honours the configurable analysis date) rather than the wall-clock year,
    // to stay consistent with the other history reports.
    private int currentYear = DateUtils.getAnalysisYear();
    private int startYear = currentYear;
    private Map<String, Integer> commitsMap = new HashMap<>();
    private List<RacingChartItem> items = new ArrayList<>();
    private List<RacingChartItem> items12Month = new ArrayList<>();

    public RacingComponentsBarChartsExporter(CodeAnalysisResults analysisResults, String logicalDecompositionKey, int logicalDecompositionIndex) {
        this.analysisResults = analysisResults;
        this.logicalDecompositionKey = logicalDecompositionKey;
        this.logicalDecompositionIndex = logicalDecompositionIndex;
    }

    public void export(File reportsFolder) {
        findCommitsPerYear(analysisResults, logicalDecompositionKey);
        save(reportsFolder);
    }

    private void findCommitsPerYear(CodeAnalysisResults analysisResults, String logicalDecompositionKey) {
        Map<String, Map<String, Integer>> componentsMap = new CommitTrendsExtractors(analysisResults).getCommitsPerYear(logicalDecompositionKey);

        componentsMap.keySet().forEach(componentName -> {
            Map<String, Integer> years = componentsMap.get(componentName);
            final double[] monthValue = {0.0};
            List<Double> monthValues = new ArrayList<>();
            years.keySet().stream().filter(year -> StringUtils.isNumeric(year)).sorted().forEach(year -> {
                int count = years.get(year).intValue();
                double stepIncrement = count / (double) STEPS_PER_YEAR;
                for (int i = 0; i < STEPS_PER_YEAR; i++) {
                    double yearNumber = Double.parseDouble(year);
                    startYear = startYear == 0 ? (int) yearNumber : Math.min(startYear, (int) yearNumber);
                    double yearFragment = yearNumber + (double) i / STEPS_PER_YEAR;

                    monthValue[0] += stepIncrement;

                    RacingChartItem item = new RacingChartItem(componentName);
                    item.setYear(yearFragment);
                    item.setValue(monthValue[0]);
                    items.add(item);

                    // Keep the trailing 12 months: one year is STEPS_PER_YEAR steps, so the window
                    // holds the last STEPS_PER_YEAR increments.
                    monthValues.add(stepIncrement);
                    if (monthValues.size() > STEPS_PER_YEAR) {
                        monthValues.remove(0);
                    }
                    RacingChartItem item12Months = new RacingChartItem(componentName);
                    item12Months.setYear(yearFragment);
                    item12Months.setValue(monthValues.stream().mapToDouble(month -> month.doubleValue()).sum());
                    items12Month.add(item12Months);
                }
            });
        });
    }

    private void save(File reportsFolder) {
        File folder = new File(reportsFolder, "html/visuals");
        try {
            FileUtils.write(new File(folder, "racing_charts_component_commits_" + logicalDecompositionIndex + ".html"),
                    new VisualizationTemplate().renderRacingCharts(items, startYear + "",
                            "Commits per component since " + startYear + " (cumulative)"), UTF_8);
            FileUtils.write(new File(folder, "racing_charts_component_commits_12_months_window_" + logicalDecompositionIndex + ".html"),
                    new VisualizationTemplate().renderRacingCharts(items12Month, startYear + "",
                            "Commits per component since " + startYear + " (12 months window)"), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
