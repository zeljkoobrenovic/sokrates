/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.files;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.Analyzer;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.FileAgeDistributionPerLogicalDecomposition;
import nl.obren.sokrates.sourcecode.analysis.results.FilesHistoryAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.filehistory.FileModificationHistory;
import nl.obren.sokrates.sourcecode.filehistory.FilePairsChangedTogether;
import nl.obren.sokrates.sourcecode.metrics.MetricsList;
import nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution;
import nl.obren.sokrates.sourcecode.stats.SourceFileChangeDistribution;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

import static nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution.Types.FIRST_MODIFIED;
import static nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution.Types.LAST_MODIFIED;

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
            List<Contributor> history = codeConfiguration.getContributorsAnalysis().getContributors(sokratesFolder);

            analysisResults.setContributors(history);
        }
    }

    private void addMetrics() {
        metricsList.addSystemMetric().id("NUMBER_OF_CONTRIBUTORS")
                .value(analysisResults.getContributors().size())
                .description("Number of contributors");
    }

}
