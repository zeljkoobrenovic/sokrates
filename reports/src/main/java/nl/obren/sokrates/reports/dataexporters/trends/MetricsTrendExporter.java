/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.dataexporters.trends;

import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.reports.utils.ReportUtils;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.core.TrendAnalysisConfig;
import nl.obren.sokrates.sourcecode.metrics.Metric;
import nl.obren.sokrates.sourcecode.metrics.MetricsList;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MetricsTrendExporter {
    private List<CodeAnalysisResults> analysisResultsList = new ArrayList<>();
    private List<String> labels = new ArrayList<>();

    public MetricsTrendExporter(File codeConfigurationFile, CodeAnalysisResults currentAnalysisResults) {
        analysisResultsList.add(currentAnalysisResults);
        labels.add("Current");

        TrendAnalysisConfig trendAnalysis = currentAnalysisResults.getCodeConfiguration().getTrendAnalysis();
        trendAnalysis.getReferenceAnalyses(codeConfigurationFile.getParentFile()).forEach(refAnalysis -> {
            CodeAnalysisResults refData = new ReferenceResultsLoader().getRefData(refAnalysis.getAnalysisResultsZipFile());
            if (refData != null) {
                analysisResultsList.add(refData);
                labels.add(refAnalysis.getLabel());
            }
        });
    }

    public List<MetricsTrendInfo> getMetricTrends() {
        return getMetricTrends(null, null);
    }

    public List<MetricsTrendInfo> getMetricTrends(String idFilterRegex, String excludeRegex) {
        List<MetricsTrendInfo> trends = new ArrayList<>();

        if (analysisResultsList.size() > 0) {
            MetricsList current = analysisResultsList.get(0).getMetricsList();

            current.getMetrics().forEach(currentMetric -> {
                MetricsTrendInfo metricsTrendInfo = new MetricsTrendInfo(currentMetric);
                String id = currentMetric.getId();
                if (shouldIncludeId(idFilterRegex, excludeRegex, id)) {
                    int counter = 0;
                    MetricsTrendInfo trendInfo = new MetricsTrendInfo(currentMetric.getId(), currentMetric.getDescription());
                    trends.add(trendInfo);
                    for (CodeAnalysisResults analysisResults : analysisResultsList) {
                        Metric metric = analysisResults.getMetricsList().getMetricById(id);

                        if (metric == null) {
                            metric = new Metric().id(id).value(0);
                        }

                        String snapshot = labels.get(counter);
                        Number value = metric.getValue();
                        metricsTrendInfo.getValues().add(new ValueSnapshotPair(snapshot, value));

                        counter++;

                        trendInfo.getValues().add(new ValueSnapshotPair(snapshot, value));
                    }
                }
            });

        }

        return trends;
    }

    public boolean shouldIncludeId(String idFilterRegex, String excludeRegex, String id) {
        return (StringUtils.isBlank(idFilterRegex) || RegexUtils.matchesEntirely(idFilterRegex, id))
                && (StringUtils.isBlank(excludeRegex) || !RegexUtils.matchesEntirely(excludeRegex, id));
    }

    public String getText() {
        return getText(null, null);
    }

    public String getText(String idFilterRegx) {
        return getText(idFilterRegx, null);
    }

    public String getText(String idFilterRegx, String excludeRegex) {
        StringBuilder builder = new StringBuilder();

        List<MetricsTrendInfo> metricTrends = getMetricTrends(idFilterRegx, excludeRegex);

        if (metricTrends.size() > 0) {
            builder.append("Metric\t");
            metricTrends.get(0).getValues().forEach(valueSnapshotPair -> {
                builder.append("\t").append(valueSnapshotPair.getSnapshot());
            });
            builder.append("\n");

            metricTrends.forEach(info -> {
                builder.append(info.getId()).append("\t");
                info.getValues().forEach(valueSnapshotPair -> {
                    builder.append("\t").append(ReportUtils.formatNumber(valueSnapshotPair.getValue()));
                });
                builder.append("\n");
            });
        }

        return builder.toString();
    }
}
