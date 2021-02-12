/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.files;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.analysis.AnalysisUtils;
import nl.obren.sokrates.sourcecode.analysis.Analyzer;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ConcernsAnalysisResults;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.metrics.MetricsList;

import java.util.List;

public class ConcernsAnalyzer extends Analyzer {
    private final StringBuffer textSummary;
    private final CodeConfiguration codeConfiguration;
    private final MetricsList metricsList;
    private final long start;
    private final List<ConcernsAnalysisResults> analysisResults;
    private ProgressFeedback progressFeedback;

    public ConcernsAnalyzer(CodeAnalysisResults analysisResults, ProgressFeedback progressFeedback) {
        this.analysisResults = analysisResults.getConcernsAnalysisResults();
        this.codeConfiguration = analysisResults.getCodeConfiguration();
        this.metricsList = analysisResults.getMetricsList();
        this.start = analysisResults.getAnalysisStartTimeMs();
        this.textSummary = analysisResults.getTextSummary();
        this.progressFeedback = progressFeedback;
    }

    public void analyze() {
        codeConfiguration.getConcernGroups().forEach(group -> {
            ConcernsAnalysisResults concernsAnalysisResults = new ConcernsAnalysisResults(group.getName());
            analysisResults.add(concernsAnalysisResults);

            group.getConcerns().forEach(concern -> {
                AspectAnalysisResults aspectAnalysisResults = new AspectAnalysisResults(concern.getName());
                concernsAnalysisResults.getConcerns().add(aspectAnalysisResults);
                AnalysisUtils.analyze("CONCERN_" + group.getName(), concern, concern.getTextOperations(),
                        progressFeedback, aspectAnalysisResults, metricsList, textSummary, start);
            });
        });
    }

}
