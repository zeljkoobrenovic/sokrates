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

import static nl.obren.sokrates.sourcecode.landscape.ContributorConnectionUtils.getPeopleDependencies;

public class ContributorsAnalyzer extends Analyzer {
    private CodeConfiguration codeConfiguration;
    private MetricsList metricsList;
    private CodeAnalysisResults codeAnalysisResults;
    private File sokratesFolder;

    private ContributorsAnalysisResults analysisResults;

    public ContributorsAnalyzer(CodeAnalysisResults results, File sokratesFolder) {
        this.analysisResults = results.getContributorsAnalysisResults();
        this.codeConfiguration = results.getCodeConfiguration();
        this.metricsList = results.getMetricsList();
        codeAnalysisResults = results;
        this.sokratesFolder = sokratesFolder;
    }

    public void analyze() {
        if (codeConfiguration.getFileHistoryAnalysis().filesHistoryImportPathExists(sokratesFolder)) {
            ContributorsImport contributorsImport = codeConfiguration.getFileHistoryAnalysis().getContributors(sokratesFolder);
            analysisResults.setContributors(contributorsImport.getContributors());
            analysisResults.setContributorsPerYear(contributorsImport.getContributorsPerYear());
            analysisResults.setContributorsPerMonth(contributorsImport.getContributorsPerMonth());
            analysisResults.setContributorsPerWeek(contributorsImport.getContributorsPerWeek());
            analysisResults.setContributorsPerDay(contributorsImport.getContributorsPerDay());
            analysisResults.setCommitsPerExtensions(codeConfiguration.getFileHistoryAnalysis().getCommitsPerExtension(sokratesFolder));

            analysisResults.setPeopleDependencies30Days(getPeopleDependencies(codeAnalysisResults, 30));
            analysisResults.setPeopleDependencies90Days(getPeopleDependencies(codeAnalysisResults, 90));
            analysisResults.setPeopleDependencies180Days(getPeopleDependencies(codeAnalysisResults, 180));
            analysisResults.setPeopleDependencies365Days(getPeopleDependencies(codeAnalysisResults, 365));
            analysisResults.setPeopleDependenciesAllTime(getPeopleDependencies(codeAnalysisResults, 36500));

            addMetrics();
        }
    }

    private void addMetrics() {
        metricsList.addSystemMetric().id("NUMBER_OF_CONTRIBUTORS")
                .value(analysisResults.getContributors().size())
                .description("Number of contributors");
    }

}
