/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.renderingutils.ReportTheme;
import nl.obren.sokrates.common.utils.ProcessingStopwatch;
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
import java.util.Arrays;
import java.util.List;

public class BasicSourceCodeReportGenerator {
    private static final Log LOG = LogFactory.getLog(BasicSourceCodeReportGenerator.class);

    private RichTextReport overviewScopeReport = new RichTextReport("Source Code Overview", "SourceCodeOverview.html");
    private RichTextReport logicalComponentsReport = new RichTextReport("Components", "Components.html");
    private RichTextReport logicalComponentsAndDependenciesReport = new RichTextReport("Static Component Dependencies", "ComponentsAndDependencies.html");
    private RichTextReport concernsReport = new RichTextReport("Features of Interest", "FeaturesOfInterest.html");
    private RichTextReport duplicationReport = new RichTextReport("Duplication", "Duplication.html");
    private RichTextReport fileSizeReport = new RichTextReport("File Size", "FileSize.html");
    private RichTextReport fileComplexityReport = new RichTextReport("File Complexity", "FileComplexity.html");
    private RichTextReport fileHistoryReport = new RichTextReport("File Age & Freshness", "FileAge.html");
    private RichTextReport FileChurnReport = new RichTextReport("File Churn", "FileChurn.html");
    private RichTextReport fileTemporalDependenciesReport = new RichTextReport("Temporal Dependencies", "FileTemporalDependencies.html");
    private RichTextReport agentContextReport = new RichTextReport("Agent Context Cost", "AgentContext.html");
    private RichTextReport unitSizeReport = new RichTextReport("Unit Size", "UnitSize.html");
    private RichTextReport conditionalComplexityReport = new RichTextReport("Conditional Complexity", "ConditionalComplexity.html");
    private RichTextReport commitsReport = new RichTextReport("Commits", "Commits.html");
    private RichTextReport contributorsReport = new RichTextReport("Contributors", "Contributors.html");
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
        String svg = ReportTheme.adaptiveIcon(HtmlTemplateUtils.getResource("/icons/" + icon + ".svg"));
        svg = svg.replaceAll("height='.*?'", "height='80px'");
        svg = svg.replaceAll("width='.*?'", "width='80px'");
        return svg;
    }


    private void decorateReport(RichTextReport report, String prefix, String logoLink) {
        if (StringUtils.isNotBlank(prefix)) {
            report.setDisplayName("<div style='color: #bbbbbb; font-size: 20px;'>"
                    + prefix
                    + "</div>"
                    + "<div style='height: 34px; font-size: 42px'>" + report.getDisplayName() + "</div>");
        }

        report.setReportsFolder(reportsFolder);

        report.setLogoLink(logoLink);
        report.setParentUrl("index.html");
    }

    public List<RichTextReport> report() {
        List<RichTextReport> reports = new ArrayList<>();
        if (codeAnalyzerSettings.isDataOnly()) {
            return reports;
        }
        createBasicReport();
        addIf(reports, codeAnalyzerSettings.isAnalyzeFilesInScope(), overviewScopeReport);
        addIf(reports, codeAnalyzerSettings.isAnalyzeLogicalDecomposition(), logicalComponentsReport);
        addIf(reports, codeAnalyzerSettings.isAnalyzeLogicalDecomposition() && codeAnalyzerSettings.isAnalyzeStaticDependencies(), logicalComponentsAndDependenciesReport);
        addIf(reports, codeAnalyzerSettings.isAnalyzeDuplication(), duplicationReport);
        addIf(reports, codeAnalyzerSettings.isAnalyzeFileSize(), fileSizeReport);
        addIf(reports, codeAnalyzerSettings.isAnalyzeConditionalComplexity(), fileComplexityReport);
        addIf(reports, codeAnalyzerSettings.isAnalyzeFileHistory() && hasFileHistory(),
                fileHistoryReport, FileChurnReport, fileTemporalDependenciesReport, agentContextReport, commitsReport, contributorsReport);
        addIf(reports, codeAnalyzerSettings.isAnalyzeUnitSize(), unitSizeReport);
        addIf(reports, codeAnalyzerSettings.isAnalyzeConditionalComplexity(), conditionalComplexityReport);
        addIf(reports, codeAnalyzerSettings.isAnalyzeConcerns(), concernsReport);
        addIf(reports, codeAnalyzerSettings.isAnalyzeFindings(), findingsReport);
        addIf(reports, codeAnalyzerSettings.isCreateMetricsList(), metricsReport, comparisonReport);
        addIf(reports, codeAnalyzerSettings.isAnalyzeControls(), controlsReport);
        return reports;
    }

    private static void addIf(List<RichTextReport> reports, boolean enabled, RichTextReport... toAdd) {
        if (enabled) {
            reports.addAll(Arrays.asList(toAdd));
        }
    }

    private boolean hasFileHistory() {
        return codeAnalysisResults.getCodeConfiguration().getFileHistoryAnalysis().filesHistoryImportPathExists(codeConfigurationFile.getParentFile());
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
        decorateReport(fileComplexityReport, name, logoLink);
        decorateReport(fileHistoryReport, name, logoLink);
        decorateReport(FileChurnReport, name, logoLink);
        decorateReport(fileTemporalDependenciesReport, name, logoLink);
        decorateReport(agentContextReport, name, logoLink);
        decorateReport(commitsReport, name, logoLink);
        decorateReport(contributorsReport, name, logoLink);
        decorateReport(controlsReport, name, logoLink);
        decorateReport(metricsReport, name, logoLink);
        decorateReport(comparisonReport, name, logoLink);
        decorateReport(findingsReport, name, logoLink);
        decorateReport(logicalComponentsReport, name, logoLink);
        decorateReport(logicalComponentsAndDependenciesReport, name, logoLink);
        decorateReport(concernsReport, name, logoLink);
    }

    private void createBasicReport() {
        if (codeAnalyzerSettings.isAnalyzeFilesInScope()) {
            timed("reporting/basic", () -> new OverviewReportGenerator(codeAnalysisResults, codeConfigurationFile).addScopeAnalysisToReport(overviewScopeReport));
        }
        if (codeAnalyzerSettings.isAnalyzeLogicalDecomposition()) {
            timed("reporting/logical decomposition", () -> {
                new LogicalComponentsReportGenerator(codeAnalysisResults, true).addCodeOrganizationToReport(logicalComponentsReport);
                new LogicalComponentsReportGenerator(codeAnalysisResults, false).addCodeOrganizationToReport(logicalComponentsAndDependenciesReport);
            });
        }
        if (codeAnalyzerSettings.isAnalyzeConcerns()) {
            timed("reporting/features of interest", () -> new ConcernsReportGenerator(codeAnalysisResults).addConcernsToReport(concernsReport));
        }
        if (codeAnalyzerSettings.isAnalyzeDuplication()) {
            timed("reporting/duplication", this::createDuplicationReport);
        }
        if (codeAnalyzerSettings.isAnalyzeFileSize()) {
            timed("reporting/file size", () -> new FileSizeReportGenerator(codeAnalysisResults).addFileSizeToReport(fileSizeReport));
        }
        if (codeAnalyzerSettings.isAnalyzeConditionalComplexity()) {
            timed("reporting/file complexity", () -> new FileComplexityReportGenerator(codeAnalysisResults).addFileComplexityToReport(fileComplexityReport));
        }
        if (codeAnalyzerSettings.isAnalyzeFileHistory() && hasFileHistory()) {
            createHistoryReports();
        }
        if (codeAnalyzerSettings.isAnalyzeUnitSize()) {
            timed("reporting/unit size", () -> new UnitsSizeReportGenerator(codeAnalysisResults).addUnitsSizeToReport(unitSizeReport));
        }
        if (codeAnalyzerSettings.isAnalyzeConditionalComplexity()) {
            timed("reporting/conditional complexity", () -> new ConditionalComplexityReportGenerator(codeAnalysisResults).addConditionalComplexityToReport(conditionalComplexityReport));
        }
        timed("reporting/findings", () -> new FindingsReportGenerator(codeConfigurationFile).generateReport(codeAnalysisResults, findingsReport));
        if (codeAnalyzerSettings.isCreateMetricsList()) {
            timed("reporting/metrics", () -> new MetricsListReportGenerator().generateReport(codeAnalysisResults, metricsReport));
        }
        if (codeAnalyzerSettings.isAnalyzeControls()) {
            timed("reporting/controls", () -> new ControlsReportGenerator().generateReport(codeAnalysisResults, controlsReport));
        }
    }

    /** The duplication report, unless the main scope exceeds the configured LOC threshold (then duplication is switched off for the index too). */
    private void createDuplicationReport() {
        int threshold = codeAnalysisResults.getCodeConfiguration().getAnalysis().getLocDuplicationThreshold();
        int mainLoc = codeAnalysisResults.getMainAspectAnalysisResults().getLinesOfCode();
        if (mainLoc <= threshold) {
            new DuplicationReportGenerator(codeAnalysisResults, reportsFolder).addDuplicationToReport(duplicationReport);
        } else {
            codeAnalyzerSettings.setAnalyzeDuplication(false);
        }
    }

    /** The git-history based reports: file age, file churn, temporal dependencies, agent context cost, commits, contributors. */
    private void createHistoryReports() {
        timed("reporting/file age", () -> new FileAgeReportGenerator(codeAnalysisResults).addFileAgeToReport(fileHistoryReport));
        timed("reporting/file change frequency", () -> new FileChurnReportGenerator(codeAnalysisResults).addFileHistoryToReport(FileChurnReport));
        timed("reporting/temporal dependencies", () -> new FileTemporalDependenciesReportGenerator(codeAnalysisResults).addTemporalDependenciesToReport(reportsFolder, fileTemporalDependenciesReport));
        timed("reporting/agent context cost", () -> new AgentContextReportGenerator(codeAnalysisResults, codeConfigurationFile.getParentFile()).addAgentContextToReport(agentContextReport));
        timed("reporting/commits", () -> new CommitsReportGenerator(codeAnalysisResults).addContributorsAnalysisToReport(reportsFolder, commitsReport));
        timed("reporting/contributors", () -> new ContributorsReportGenerator(codeAnalysisResults).addContributorsAnalysisToReport(reportsFolder, contributorsReport));
    }

    /** Runs one reporting step between the matching ProcessingStopwatch start/end marks. */
    private static void timed(String step, Runnable work) {
        ProcessingStopwatch.start(step);
        work.run();
        ProcessingStopwatch.end(step);
    }
}
