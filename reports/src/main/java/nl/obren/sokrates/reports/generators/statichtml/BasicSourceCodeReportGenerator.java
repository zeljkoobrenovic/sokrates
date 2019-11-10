package nl.obren.sokrates.reports.generators.statichtml;

import nl.obren.sokrates.common.renderingutils.VisualizationItem;
import nl.obren.sokrates.common.renderingutils.VisualizationTemplate;
import nl.obren.sokrates.common.utils.SystemUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.analysis.CodeAnalyzerSettings;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BasicSourceCodeReportGenerator {
    private static final Log LOG = LogFactory.getLog(BasicSourceCodeReportGenerator.class);
    private RichTextReport overviewScopeReport = new RichTextReport("Source Code Overview", "");
    private RichTextReport logicalComponentsReport = new RichTextReport("Components", "");
    private RichTextReport crossCuttingConcernsReport = new RichTextReport("Cross-Cutting Concerns", "");
    private RichTextReport duplicationReport = new RichTextReport("Duplication", "");
    private RichTextReport fileSizeReport = new RichTextReport("File Size", "");
    private RichTextReport unitSizeReport = new RichTextReport("Unit Size", "");
    private RichTextReport cyclomaticComplexityReport = new RichTextReport("Cyclomatic Complexity", "");
    private RichTextReport metricsReport = new RichTextReport("Metrics", "");
    private RichTextReport controlsReport = new RichTextReport("Controls", "");
    private CodeAnalyzerSettings codeAnalyzerSettings;
    private CodeAnalysisResults codeAnalysisResults;
    private File codeConfigurationFile;

    public BasicSourceCodeReportGenerator(CodeAnalyzerSettings codeAnalyzerSettings, CodeAnalysisResults codeAnalysisResults, File codeConfigurationFile) {
        this.codeAnalyzerSettings = codeAnalyzerSettings;
        this.codeAnalysisResults = codeAnalysisResults;
        this.codeConfigurationFile = codeConfigurationFile;
        decorateReports();
    }

    private void decorateReport(RichTextReport report, String prefix, String logoLink) {
        if (StringUtils.isNotBlank(prefix)) {
            report.setDisplayName("<span style='color: #bbbbbb; font-size: 80%'>"
                    + prefix
                    + "<div style='height: 22px'></div></span>"
                    + report.getDisplayName());
        }

        report.setLogoLink(logoLink);
    }

    public List<RichTextReport> report() {
        List<RichTextReport> reports = new ArrayList<>();

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
        if (codeAnalyzerSettings.isAnalyzeUnitSize()) {
            reports.add(unitSizeReport);
        }
        if (codeAnalyzerSettings.isAnalyzeCyclomaticComplexity()) {
            reports.add(cyclomaticComplexityReport);
        }
        if (codeAnalyzerSettings.isAnalyzeCrossCuttingConcerns()) {
            reports.add(crossCuttingConcernsReport);
        }
        if (codeAnalyzerSettings.isCreateMetricsList()) {
            reports.add(metricsReport);
        }

        if (codeAnalyzerSettings.isAnalyzeControls()) {
            reports.add(controlsReport);
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
        decorateReport(cyclomaticComplexityReport, name, logoLink);
        decorateReport(fileSizeReport, name, logoLink);
        decorateReport(controlsReport, name, logoLink);
        decorateReport(metricsReport, name, logoLink);
        decorateReport(logicalComponentsReport, name, logoLink);
        decorateReport(crossCuttingConcernsReport, name, logoLink);
    }

    private void createBasicReport() {
        if (codeAnalyzerSettings.isAnalyzeFilesInScope()) {
            new OverviewReportGenerator(codeAnalysisResults, codeConfigurationFile).addScopeAnalysisToReport(overviewScopeReport);
        }

        if (codeAnalyzerSettings.isAnalyzeLogicalDecomposition()) {
            new LogicalComponentsReportGenerator(codeAnalysisResults).addCodeOrganizationToReport(logicalComponentsReport);
        }

        if (codeAnalyzerSettings.isAnalyzeCrossCuttingConcerns()) {
            new CrossCuttingConcernsReportGenerator(codeAnalysisResults).addCrossCuttingConcernsToReport(crossCuttingConcernsReport);
        }

        if (codeAnalyzerSettings.isAnalyzeDuplication()) {
            new DuplicationReportGenerator(codeAnalysisResults).addDuplicationToReport(duplicationReport);
        }

        if (codeAnalyzerSettings.isAnalyzeFileSize()) {
            new FileSizeReportGenerator(codeAnalysisResults).addFileSizeToReport(fileSizeReport);
        }

        if (codeAnalyzerSettings.isAnalyzeUnitSize()) {
            new UnitsSizeReportGenerator(codeAnalysisResults).addUnitsSizeToReport(unitSizeReport);
        }

        if (codeAnalyzerSettings.isAnalyzeCyclomaticComplexity()) {
            new CyclomaticComplexityReportGenerator(codeAnalysisResults).addCyclomaticComplexityToReport(cyclomaticComplexityReport);
        }

        if (codeAnalyzerSettings.isCreateMetricsList()) {
            new MetricsListReportGenerator().generateReport(codeAnalysisResults, metricsReport);
        }

        if (codeAnalyzerSettings.isAnalyzeControls()) {
            new ControlsReportGenerator().generateReport(codeAnalysisResults, controlsReport);
        }
    }
}
