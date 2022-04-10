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
    private int windowSize = 12;

    private CodeAnalysisResults analysisResults;
    private String logicalDecompositionKey;
    private int logicalDecompositionIndex = 1;
    private int currentYear = Calendar.getInstance().get(Calendar.YEAR);
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
                double monthlyIncrement = count / 10.0;
                for (int i = 0; i < 10; i++) {
                    double yearNumber = Double.parseDouble(year);
                    startYear = startYear == 0 ? (int) yearNumber : Math.min(startYear, (int) yearNumber);
                    double yearFragment = yearNumber + i / 10.0;

                    monthValue[0] += monthlyIncrement;

                    RacingChartItem item = new RacingChartItem(componentName);
                    item.setYear(yearFragment);
                    item.setValue(monthValue[0]);
                    items.add(item);

                    monthValues.add(monthlyIncrement);
                    if (monthValues.size() > 12) {
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
