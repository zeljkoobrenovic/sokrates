/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.files;

import nl.obren.sokrates.sourcecode.analysis.Analyzer;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.ContributorsImport;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.metrics.MetricsList;

import java.io.File;

public class ContributorsAnalyzer extends Analyzer {
    private CodeConfiguration codeConfiguration;
    private MetricsList metricsList;
    private File sokratesFolder;

    private ContributorsAnalysisResults analysisResults;

    public ContributorsAnalyzer(CodeAnalysisResults results, File sokratesFolder) {
        this.analysisResults = results.getContributorsAnalysisResults();
        this.codeConfiguration = results.getCodeConfiguration();
        this.metricsList = results.getMetricsList();
        this.sokratesFolder = sokratesFolder;
    }

    public void analyze() {
        if (codeConfiguration.getFileHistoryAnalysis().filesHistoryImportPathExists(sokratesFolder)) {
            ContributorsImport contributorsImport = codeConfiguration.getFileHistoryAnalysis().getContributors(sokratesFolder);
            analysisResults.setContributors(contributorsImport.getContributors());
            analysisResults.setContributorsPerYear(contributorsImport.getContributorsPerYear());
            analysisResults.setCommitsPerExtensions(codeConfiguration.getFileHistoryAnalysis().getCommitsPerExtension(sokratesFolder));

            addMetrics();
        }
    }

    private void addMetrics() {
        metricsList.addSystemMetric().id("NUMBER_OF_CONTRIBUTORS")
                .value(analysisResults.getContributors().size())
                .description("Number of contributors");
    }

}
