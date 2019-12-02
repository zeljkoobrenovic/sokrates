package nl.obren.sokrates.sourcecode.analysis.files;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.analysis.AnalysisUtils;
import nl.obren.sokrates.sourcecode.analysis.Analyzer;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CrossCuttingConcernsAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.MetricsList;

import java.util.List;

public class CrossCuttingConcernsAnalyzer extends Analyzer {
    private final StringBuffer textSummary;
    private ProgressFeedback progressFeedback;
    private final CodeConfiguration codeConfiguration;
    private final MetricsList metricsList;
    private final long start;
    private final List<CrossCuttingConcernsAnalysisResults> analysisResults;

    public CrossCuttingConcernsAnalyzer(CodeAnalysisResults analysisResults, ProgressFeedback progressFeedback) {
        this.analysisResults = analysisResults.getCrossCuttingConcernsAnalysisResults();
        this.codeConfiguration = analysisResults.getCodeConfiguration();
        this.metricsList = analysisResults.getMetricsList();
        this.start = analysisResults.getAnalysisStartTimeMs();
        this.textSummary = analysisResults.getTextSummary();
        this.progressFeedback = progressFeedback;
    }

    public void analyze() {
        codeConfiguration.getCrossCuttingConcerns().forEach(group -> {
            CrossCuttingConcernsAnalysisResults crossCuttingConcernsAnalysisResults = new CrossCuttingConcernsAnalysisResults(group.getName());
            analysisResults.add(crossCuttingConcernsAnalysisResults);
            group.getConcerns().forEach(concern -> {
                AspectAnalysisResults aspectAnalysisResults = new AspectAnalysisResults(concern.getName());
                crossCuttingConcernsAnalysisResults.getCrossCuttingConcerns().add(aspectAnalysisResults);
                AnalysisUtils.analyze(group.getName(), concern, progressFeedback, aspectAnalysisResults, metricsList, textSummary, start);
            });
        });
    }

}
