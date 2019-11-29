package nl.obren.sokrates.sourcecode.analysis;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SearcheableFilesCache;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.CrossCuttingConcern;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.aspects.SourceCodeAspectUtils;
import nl.obren.sokrates.sourcecode.metrics.Metric;
import nl.obren.sokrates.sourcecode.metrics.MetricsList;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import nl.obren.sokrates.sourcecode.search.SearchExpression;
import nl.obren.sokrates.sourcecode.search.SearchRequest;
import nl.obren.sokrates.sourcecode.search.SearchResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.DecimalFormat;

public class AnalysisUtils {
    private static final Log LOG = LogFactory.getLog(AnalysisUtils.class);

    public static void analyze(NamedSourceCodeAspect aspect, ProgressFeedback progressFeedback, AspectAnalysisResults aspectAnalysisResults,
                               MetricsList metricsList, StringBuffer textSummary, long start) {
        aspectAnalysisResults.setAspect(aspect);
        metricsList.addMetric()
                .id(getMetricId("NUMBER_OF_FILES_" + aspect.getName()))
                .description("Number of files in scope")
                .scope(Metric.Scope.LOGICAL_COMPONENT)
                .scopeQualifier(aspect.getName())
                .value(aspect.getSourceFiles().size());

        metricsList.addMetric()
                .id(getMetricId("LINES_OF_CODE_" + aspect.getName()))
                .description("Lines of code in scope")
                .scope(Metric.Scope.LOGICAL_COMPONENT)
                .scopeQualifier(aspect.getName())
                .value(aspect.getLinesOfCode());

        detailedInfo(textSummary, progressFeedback, aspect.getSourceFiles().size() + " are in the " + aspect.getName() + "'s scope "
                + " (" + aspect.getLinesOfCode() + " lines of code)", start);

        aspectAnalysisResults.setFilesCount(aspect.getSourceFiles().size());
        aspectAnalysisResults.setLinesOfCode(aspect.getLinesOfCode());

        if (aspect instanceof CrossCuttingConcern) {
            LOG.info("Creating searcheable file chache for " + aspect.getName());
            SearcheableFilesCache searcheableFilesCache = SearcheableFilesCache.getInstance(aspect.getSourceFiles());
            aspect.getSourceFileFilters().forEach(filter -> {
                SearchRequest searchRequest = new SearchRequest(
                        new SearchExpression(filter.getPathPattern()),
                        new SearchExpression(filter.getContentPattern()));
                LOG.info("Searching for path line \"" + searchRequest.getPathSearchExpression().getExpression() + "\" and/or content like \""
                        + searchRequest.getContentSearchExpression().getExpression() + "\"");
                SearchResult searchResult = searcheableFilesCache.search(searchRequest, new ProgressFeedback());
                aspectAnalysisResults.setNumberOfRegexLineMatches(aspectAnalysisResults.getNumberOfRegexLineMatches()
                        + searchResult.getTotalNumberOfMatchingLines());
            });
        }

        SourceCodeAspectUtils.getAspectsPerExtensions(aspect).forEach(aspectPerExtension -> {
            metricsList.addMetric()
                    .id(getMetricId("NUMBER_OF_FILES_" + aspectPerExtension.getName().replace("*.", "")))
                    .description("Number of files in scope")
                    .scope(Metric.Scope.LOGICAL_COMPONENT)
                    .scopeQualifier(aspect.getName() + "::" + aspectPerExtension.getName())
                    .value(aspectPerExtension.getSourceFiles().size());

            metricsList.addMetric()
                    .id(getMetricId("LINES_OF_CODE_" + aspectPerExtension.getName().replace("*.", "")))
                    .description("Lines of code in scope")
                    .scope(Metric.Scope.LOGICAL_COMPONENT)
                    .scopeQualifier(aspect.getName() + aspectPerExtension.getName())
                    .value(aspectPerExtension.getLinesOfCode());

            detailedInfo(textSummary, progressFeedback, aspectPerExtension.getName() + ": " + aspectPerExtension.getSourceFiles().size() + " files, "
                    + " (" + aspectPerExtension.getLinesOfCode() + " lines of code)", start);
            aspectAnalysisResults.getFileCountPerExtension().add(new NumericMetric(aspectPerExtension.getName(), aspectPerExtension.getSourceFiles().size()));
            aspectAnalysisResults.getLinesOfCodePerExtension().add(new NumericMetric(aspectPerExtension.getName(), aspectPerExtension.getLinesOfCode()));
        });
    }

    public static String getMetricId(String name) {
        name = name.replace(" ", "_").replace("-", "_");
        name = name.replaceAll("[(].*?[)]", "");
        while (name.contains("__")) {
            name = name.replace("__", "_");
        }
        name = StringUtils.removeEnd(name, "_");
        return name.trim().toUpperCase();
    }


    public static void info(StringBuffer textSummary, ProgressFeedback progressFeedback, String line, long start) {
        DecimalFormat formatter = new DecimalFormat("#.00");
        LOG.info(formatter.format(((System.currentTimeMillis() - start) / 10) * 0.01) + "s\t\t" + line.replaceAll("<.*?>", ""));
        textSummary.append(line + "\n");
        if (progressFeedback != null) {
            progressFeedback.setText(line);
        }
    }

    public static void detailedInfo(StringBuffer textSummary, ProgressFeedback progressFeedback, String line, long start) {
        DecimalFormat formatter = new DecimalFormat("#.00");
        LOG.info(formatter.format(((System.currentTimeMillis() - start) / 10) * 0.01) + "s\t\t" + line.replaceAll("<.*?>", ""));
        textSummary.append(line + "\n");
        if (progressFeedback != null) {
            progressFeedback.setDetailedText(line);
        }
    }


}
