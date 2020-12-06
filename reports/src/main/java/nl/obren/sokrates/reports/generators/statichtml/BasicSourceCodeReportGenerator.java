/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.utils.HtmlTemplateUtils;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.analysis.CodeAnalyzerSettings;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BasicSourceCodeReportGenerator {
    private static final Log LOG = LogFactory.getLog(BasicSourceCodeReportGenerator.class);

    private RichTextReport overviewScopeReport = new RichTextReport("Source Code Overview", "SourceCodeOverview.html");
    private RichTextReport logicalComponentsReport = new RichTextReport("Components & Dependencies", "Components.html");
    private RichTextReport concernsReport = new RichTextReport("Features of Interest", "FeaturesOfInterest.html");
    private RichTextReport duplicationReport = new RichTextReport("Duplication", "Duplication.html");
    private RichTextReport fileSizeReport = new RichTextReport("File Size", "FileSize.html");
    private RichTextReport fileHistoryReport = new RichTextReport("File Age", "FileAge.html");
    private RichTextReport fileChangeFrequencyReport = new RichTextReport("File Change Frequency", "FileChangeFrequency.html");
    private RichTextReport fileTemporalDependenciesReport = new RichTextReport("Temporal Dependencies", "FileTemporalDependencies.html");
    private RichTextReport unitSizeReport = new RichTextReport("Unit Size", "UnitSize.html");
    private RichTextReport conditionalComplexityReport = new RichTextReport("Conditional Complexity", "ConditionalComplexity.html");
    private RichTextReport contributorsReport = new RichTextReport("Commits &amp; Contributors", "Commits.html");
    private RichTextReport findingsReport = new RichTextReport("Notes & Findings", "Notes.html");
    private RichTextReport metricsReport = new RichTextReport("Metrics", "Metrics.html");
    private RichTextReport comparisonReport = new RichTextReport("Trend", "Trend.html");
    private RichTextReport controlsReport = new RichTextReport("Goals & Controls", "Controls.html");
    private CodeAnalyzerSettings codeAnalyzerSettings;
    private CodeAnalysisResults codeAnalysisResults;
    private File codeConfigurationFile;
    private File reportsFolder;

    public BasicSourceCodeReportGenerator(CodeAnalyzerSettings codeAnalyzerSettings, CodeAnalysisResults codeAnalysisResults, File codeConfigurationFile, File reportsFolder) {
        this.codeAnalyzerSettings = codeAnalyzerSettings;
        this.codeAnalysisResults = codeAnalysisResults;
        this.codeConfigurationFile = codeConfigurationFile;
        this.reportsFolder = reportsFolder;
        decorateReports();
    }

    private static String getIconSvg(String icon) {
        String svg = HtmlTemplateUtils.getResource("/icons/" + icon + ".svg");
        svg = svg.replaceAll("height='.*?'", "height='80px'");
        svg = svg.replaceAll("width='.*?'", "width='80px'");
        return svg;
    }


    private void decorateReport(RichTextReport report, String prefix, String logoLink) {
        if (StringUtils.isNotBlank(prefix)) {
            report.setDisplayName("<span style='color: #bbbbbb; font-size: 80%'>"
                    + prefix
                    + "<div style='height: 22px'></div></span>"
                    + report.getDisplayName());
        }
        report.setReportsFolder(reportsFolder);

        report.setLogoLink(logoLink);
    }

    public List<RichTextReport> report() {
        List<RichTextReport> reports = new ArrayList<>();

        if (!codeAnalyzerSettings.isDataOnly()) {
            createBasicReport();

            if (codeAnalyzerSettings.isAnalyzeFilesInScope()) {
                reports.add(overviewScopeReport);
            }
            if (codeAnalyzerSettings.isAnalyzeLogicalDecomposition()) {
                reports.add(logicalComponentsReport);
            }
            if (codeAnalyzerSettings.isAnalyzeDuplication()) {
                reports.add(duplicationReport);
            }
            if (codeAnalyzerSettings.isAnalyzeFileSize()) {
                reports.add(fileSizeReport);
            }
            if (codeAnalyzerSettings.isAnalyzeFileHistory()) {
                if (codeAnalysisResults.getCodeConfiguration().getFileHistoryAnalysis().filesHistoryImportPathExists(codeConfigurationFile.getParentFile())) {
                    reports.add(fileHistoryReport);
                    reports.add(fileChangeFrequencyReport);
                    reports.add(fileTemporalDependenciesReport);
                    reports.add(contributorsReport);
                }
            }
            if (codeAnalyzerSettings.isAnalyzeUnitSize()) {
                reports.add(unitSizeReport);
            }
            if (codeAnalyzerSettings.isAnalyzeConditionalComplexity()) {
                reports.add(conditionalComplexityReport);
            }
            if (codeAnalyzerSettings.isAnalyzeConcerns()) {
                reports.add(concernsReport);
            }

            if (codeAnalyzerSettings.isAnalyzeFindings()) {
                reports.add(findingsReport);
            }

            if (codeAnalyzerSettings.isCreateMetricsList()) {
                reports.add(metricsReport);
                reports.add(comparisonReport);
            }

            if (codeAnalyzerSettings.isAnalyzeControls()) {
                reports.add(controlsReport);
            }
        }

        return reports;
    }

    private void decorateReports() {
        Metadata metadata = codeAnalysisResults.getCodeConfiguration().getMetadata();
        String name = metadata.getName();
        String logoLink = metadata.getLogoLink();

        decorateReport(overviewScopeReport, name, logoLink);
        decorateReport(duplicationReport, name, logoLink);
        decorateReport(unitSizeReport, name, logoLink);
        decorateReport(conditionalComplexityReport, name, logoLink);
        decorateReport(fileSizeReport, name, logoLink);
        decorateReport(fileHistoryReport, name, logoLink);
        decorateReport(fileChangeFrequencyReport, name, logoLink);
        decorateReport(fileTemporalDependenciesReport, name, logoLink);
        decorateReport(contributorsReport, name, logoLink);
        decorateReport(controlsReport, name, logoLink);
        decorateReport(metricsReport, name, logoLink);
        decorateReport(comparisonReport, name, logoLink);
        decorateReport(findingsReport, name, logoLink);
        decorateReport(logicalComponentsReport, name, logoLink);
        decorateReport(concernsReport, name, logoLink);
    }

    private void createBasicReport() {
        if (codeAnalyzerSettings.isAnalyzeFilesInScope()) {
            new OverviewReportGenerator(codeAnalysisResults, codeConfigurationFile).addScopeAnalysisToReport(overviewScopeReport);
        }

        if (codeAnalyzerSettings.isAnalyzeLogicalDecomposition()) {
            new LogicalComponentsReportGenerator(codeAnalysisResults).addCodeOrganizationToReport(logicalComponentsReport);
        }

        if (codeAnalyzerSettings.isAnalyzeConcerns()) {
            new ConcernsReportGenerator(codeAnalysisResults).addConcernsToReport(concernsReport);
        }

        if (codeAnalyzerSettings.isAnalyzeDuplication()) {
            new DuplicationReportGenerator(codeAnalysisResults).addDuplicationToReport(duplicationReport);
        }

        if (codeAnalyzerSettings.isAnalyzeFileSize()) {
            new FileSizeReportGenerator(codeAnalysisResults).addFileSizeToReport(fileSizeReport);
        }

        if (codeAnalyzerSettings.isAnalyzeFileHistory()) {
            if (codeAnalysisResults.getCodeConfiguration().getFileHistoryAnalysis().filesHistoryImportPathExists(codeConfigurationFile.getParentFile())) {
                new FileHistoryReportGenerator(codeAnalysisResults).addFileHistoryToReport(fileHistoryReport);
                new FileChurnReportGenerator(codeAnalysisResults).addFileHistoryToReport(fileChangeFrequencyReport);
                new FileTemporalDependenciesReportGenerator(codeAnalysisResults).addFileHistoryToReport(fileTemporalDependenciesReport);
                new ContributorsReportGenerator(codeAnalysisResults).addContributorsAnalysisToReport(contributorsReport);
            }
        }

        if (codeAnalyzerSettings.isAnalyzeUnitSize()) {
            new UnitsSizeReportGenerator(codeAnalysisResults).addUnitsSizeToReport(unitSizeReport);
        }

        if (codeAnalyzerSettings.isAnalyzeConditionalComplexity()) {
            new ConditionalComplexityReportGenerator(codeAnalysisResults).addConditionalComplexityToReport(conditionalComplexityReport);
        }

        new FindingsReportGenerator(codeConfigurationFile).generateReport(codeAnalysisResults, findingsReport);

        if (codeAnalyzerSettings.isCreateMetricsList()) {
            new MetricsListReportGenerator().generateReport(codeAnalysisResults, metricsReport);
            new TrendReportGenerator(codeConfigurationFile).generateReport(codeAnalysisResults, comparisonReport);
        }

        if (codeAnalyzerSettings.isAnalyzeControls()) {
            new ControlsReportGenerator().generateReport(codeAnalysisResults, controlsReport);
        }
    }
}
