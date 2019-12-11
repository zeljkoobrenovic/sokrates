/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.cli;

import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.common.renderingutils.Thresholds;
import nl.obren.sokrates.common.renderingutils.VisualizationItem;
import nl.obren.sokrates.common.renderingutils.VisualizationTemplate;
import nl.obren.sokrates.common.renderingutils.x3d.Unit3D;
import nl.obren.sokrates.common.renderingutils.x3d.X3DomExporter;
import nl.obren.sokrates.common.utils.BasicColorInfo;
import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.reports.core.ReportFileExporter;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.dataexporters.DataExporter;
import nl.obren.sokrates.reports.generators.statichtml.BasicSourceCodeReportGenerator;
import nl.obren.sokrates.sourcecode.analysis.CodeAnalyzer;
import nl.obren.sokrates.sourcecode.analysis.CodeAnalyzerSettings;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.core.CodeConfigurationUtils;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzerFactory;
import nl.obren.sokrates.sourcecode.scoping.ScopeCreator;
import nl.obren.sokrates.sourcecode.stats.SourceFileSizeDistribution;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.charset.StandardCharsets.UTF_8;

public class CommandLineInterface {
    public static final String GENERATE_REPORTS = "generateReports";
    public static final String INIT = "init";
    public static final String SRC_ROOT = "srcRoot";
    public static final String CONF_FILE = "confFile";
    public static final String REPORT_ALL = "reportAll";
    public static final String REPORT_JSON = "reportJson";
    public static final String REPORT_TEXT = "reportText";
    public static final String REPORT_OVERVIEW = "reportOverview";
    public static final String REPORT_FINDINGS = "reportFindings";
    public static final String REPORT_DUPLICATION = "reportDuplication";
    public static final String REPORT_LOGICAL_DECOMPOSITION = "reportLogicalDecomposition";
    public static final String REPORT_CROSS_CUTTING_CONCERNS = "reportCrossCuttingConcerns";
    public static final String REPORT_FILE_SIZE = "reportFileSize";
    public static final String REPORT_METRICS = "reportMetrics";
    public static final String REPORT_UNIT_SIZE = "reportUnitSize";
    public static final String REPORT_CONDITIONAL_COMPLEXITY = "reportConditionalComplexity";
    public static final String REPORT_CONTROLS = "reportControls";
    public static final String OUTPUT_FOLDER = "outputFolder";
    public static final String HTML_REPORTS_FOLDER_NAME = "html";
    private static final Log LOG = LogFactory.getLog(CommandLineInterface.class);
    private Option srcRoot = new Option(SRC_ROOT, true, "the path to source code root folder");
    private Option confFile = new Option(CONF_FILE, true, "[OPTIONAL] the path to configuration file (default is \"<srcRoot>/_sokrates/config.json\")");
    private Option all = new Option(REPORT_ALL, false, "generate all reports");
    private Option json = new Option(REPORT_JSON, false, "save report data in JSON format");
    private Option txt = new Option(REPORT_TEXT, false, "save textual summary");
    private Option scope = new Option(REPORT_OVERVIEW, false, "generate report describing the overview of files in scope");
    private Option findings = new Option(REPORT_FINDINGS, false, "generate report describing the manual findings");
    private Option duplication = new Option(REPORT_DUPLICATION, false, "generate the duplication report (stored in <outputFolder>/Duplication.html)");
    private Option logicalDecomposition = new Option(REPORT_LOGICAL_DECOMPOSITION, false, "generate the logical decomposition report (stored in <outputFolder>/LogicalDecomposition.html)");
    private Option crossCuttingConcerns = new Option(REPORT_CROSS_CUTTING_CONCERNS, false, "generate the cross cutting concerns report (stored in <outputFolder>/CrossCuttingConcerns.html)");
    private Option fileSize = new Option(REPORT_FILE_SIZE, false, "generate the file size report (stored in <outputFolder>/FileSize.html)");
    private Option metrics = new Option(REPORT_METRICS, false, "generate the metrics overview report (stored in <outputFolder>/Metrics.html)");
    private Option unitSize = new Option(REPORT_UNIT_SIZE, false, "generate the unit size report (stored in <outputFolder>/UnitSize.html)");
    private Option conditionalComplexity = new Option(REPORT_CONDITIONAL_COMPLEXITY, false, "generate the conditional complexity report (stored in <outputFolder>/ConditionalComplexity.html)");
    private Option controls = new Option(REPORT_CONTROLS, false, "generate the controls report (stored in <outputFolder>/Controls.html)");

    private Option outputFolder = new Option(OUTPUT_FOLDER, true, "the folder where reports will be stored");
    private ProgressFeedback progressFeedback;
    private DataExporter dataExporter = new DataExporter(this.progressFeedback);


    public static void main(String args[]) throws ParseException, IOException {
        BasicConfigurator.configure();

        CommandLineInterface commandLineInterface = new CommandLineInterface();
        commandLineInterface.run(args);
    }

    public void run(String args[]) throws IOException {
        if (args.length == 0) {
            usage();
            return;
        }

        if (progressFeedback != null) {
            progressFeedback.clear();
        }

        try {
            if (args[0].equalsIgnoreCase(INIT)) {
                init(args);
                return;
            } else if (!args[0].equalsIgnoreCase(GENERATE_REPORTS)) {
                usage();
                return;
            }

            generateReports(args);
        } catch (ParseException e) {
            usage();
        }
    }

    private void generateReports(String[] args) throws ParseException, IOException {
        Options options = getReportingOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        generateReports(cmd);
    }

    private void init(String[] args) throws ParseException, IOException {
        Options options = getInitOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (!cmd.hasOption(srcRoot.getOpt())) {
            usage(INIT, options);
            return;
        }

        File root = new File(cmd.getOptionValue(srcRoot.getOpt()));
        if (!root.exists()) {
            LOG.error("The src root \"" + root.getPath() + "\" does not exist.");
            return;
        }

        File conf = getConfigFile(cmd, root);

        new ScopeCreator(root, conf).createScopeFromConventions();
    }

    private File getConfigFile(CommandLine cmd, File root) {
        File conf;
        if (cmd.hasOption(confFile.getOpt())) {
            conf = new File(cmd.getOptionValue(confFile.getOpt()));
        } else {
            conf = CodeConfigurationUtils.getDefaultSokratesConfigFile(root);
        }
        return conf;
    }

    private void generateReports(CommandLine cmd) throws IOException {
        if (!cmd.hasOption(confFile.getOpt())) {
            usage(GENERATE_REPORTS, getReportingOptions());
            return;
        }
        File inputFile = new File(cmd.getOptionValue(confFile.getOpt()));
        String jsonContent = FileUtils.readFileToString(inputFile, UTF_8);
        CodeConfiguration codeConfiguration = (CodeConfiguration) new JsonMapper().getObject(jsonContent, CodeConfiguration.class);
        LanguageAnalyzerFactory.getInstance().setOverrides(codeConfiguration.getAnalysis().getAnalyzerOverrides());

        detailedInfo("Starting analysis based on the configuration file " + inputFile.getPath());

        if (!cmd.hasOption(outputFolder.getOpt())) {
            usage(GENERATE_REPORTS, getReportingOptions());
            return;
        }

        File reportsFolder = prepareReportsFolder(cmd);

        if (this.progressFeedback == null) {
            this.progressFeedback = new ProgressFeedback() {
                public void setText(String text) {
                    System.out.println(text);
                }

                public void setDetailedText(String text) {
                    System.out.println(text);
                }
            };
        }

        try {

            CodeAnalyzer codeAnalyzer = new CodeAnalyzer(getCodeAnalyzerSettings(cmd), codeConfiguration, inputFile);
            CodeAnalysisResults analysisResults = codeAnalyzer.analyze(progressFeedback);

            if (cmd.hasOption(all.getOpt()) || cmd.hasOption(json.getOpt())) {
                dataExporter.saveData(codeConfiguration, reportsFolder, analysisResults);
            }

            if (cmd.hasOption(all.getOpt()) || cmd.hasOption(txt.getOpt())) {
                saveTextualSummary(reportsFolder, analysisResults);
            }

            if (cmd.hasOption(all.getOpt()) || cmd.hasOption(logicalDecomposition.getOpt())) {
                generateVisuals(reportsFolder, analysisResults);
            }

            generateAndSaveReports(inputFile, reportsFolder, codeAnalyzer, analysisResults);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void info(String text) {
        LOG.info(text);
        if (progressFeedback != null) {
            progressFeedback.setText(text);
        }
    }

    public void detailedInfo(String text) {
        LOG.info(text);
        if (progressFeedback != null) {
            progressFeedback.setDetailedText(text);
        }
    }

    private void generateAndSaveReports(File inputFile, File reportsFolder, CodeAnalyzer codeAnalyzer, CodeAnalysisResults analysisResults) {
        File htmlReports = getHtmlFolder(reportsFolder);
        File dataReports = dataExporter.getDataFolder();
        File srcCache = dataExporter.getCodeCacheFolder();
        info("HTML reports: <a href='" + htmlReports.getPath() + "/index.html'>" + htmlReports.getPath() + "</a>");
        info("Raw data: <a href='" + dataReports.getPath() + "'>" + dataReports.getPath() + "</a>");
        if (analysisResults.getCodeConfiguration().getAnalysis().isCacheSourceFiles()) {
            info("Source code cache : <a href='" + srcCache.getPath() + "'>" + srcCache.getPath() + "</a>");
        }
        info("");
        info("");
        BasicSourceCodeReportGenerator generator = new BasicSourceCodeReportGenerator(codeAnalyzer.getCodeAnalyzerSettings(), analysisResults, inputFile, reportsFolder);
        List<RichTextReport> reports = generator.report();
        reports.forEach(report -> {
            info("Generating the '" + report.getId().toUpperCase() + "' report...");
            ReportFileExporter.exportHtml(reportsFolder, report);
        });
        ReportFileExporter.exportReportsIndexFile(reportsFolder, analysisResults);
    }


    private void generateVisuals(File reportsFolder, CodeAnalysisResults analysisResults) {
        AtomicInteger index = new AtomicInteger();
        analysisResults.getCodeConfiguration().getLogicalDecompositions().forEach(logicalDecomposition -> {
            index.getAndIncrement();
            List<VisualizationItem> items = new ArrayList<>();
            logicalDecomposition.getComponents().forEach(component -> {
                items.add(new VisualizationItem(component.getName(), component.getLinesOfCode()));
            });
            try {
                String nameSuffix = "components_" + index.toString() + ".html";
                File folder = new File(reportsFolder, "html/visuals");
                folder.mkdirs();
                FileUtils.write(new File(folder, "bubble_chart_" + nameSuffix), new VisualizationTemplate().renderBubbleChart(items), UTF_8);
                FileUtils.write(new File(folder, "tree_map_" + nameSuffix), new VisualizationTemplate().renderTreeMap(items), UTF_8);

                generate3DUnitsView(folder, analysisResults);
            } catch (IOException e) {
                LOG.warn(e);
            }
        });
    }

    private void generate3DUnitsView(File visualsFolder, CodeAnalysisResults analysisResults) {
        List<Unit3D> unit3DConditionalComplexity = new ArrayList<>();
        analysisResults.getUnitsAnalysisResults().getAllUnits().forEach(unit -> {
            BasicColorInfo color = Thresholds.getColor(Thresholds.UNIT_MCCABE, unit.getMcCabeIndex());
            unit3DConditionalComplexity.add(new Unit3D(unit.getLongName(), unit.getLinesOfCode(), color));
        });

        List<Unit3D> unit3DSize = new ArrayList<>();
        analysisResults.getUnitsAnalysisResults().getAllUnits().forEach(unit -> {
            BasicColorInfo color = Thresholds.getColor(Thresholds.UNIT_LINES, unit.getLinesOfCode());
            unit3DSize.add(new Unit3D(unit.getLongName(), unit.getLinesOfCode(), color));
        });

        List<Unit3D> files3D = new ArrayList<>();
        analysisResults.getCodeConfiguration().getMain().getSourceFiles().forEach(file -> {
            SourceFileSizeDistribution sourceFileSizeDistribution = new SourceFileSizeDistribution();
            BasicColorInfo color = getFileSizeColor(sourceFileSizeDistribution, file.getLinesOfCode());
            files3D.add(new Unit3D(file.getFile().getPath(), file.getLinesOfCode(), color));
        });

        new X3DomExporter(new File(visualsFolder, "units_3d_complexity.html"), "A 3D View of All Units (Conditional Complexity)", "Each block is one unit. The height of the block represents the file unit size in lines of code. The color of the unit represents its conditional complexity category (green=0-5, yellow=6-10, orange=11-25, red=26+).").export(unit3DConditionalComplexity, false, 10);

        new X3DomExporter(new File(visualsFolder, "units_3d_size.html"), "A 3D View of All Units (Unit Size)", "Each block is one unit. The height of the block represents the file unit size in lines of code. The color of the unit represents its unit size category (green=0-20, yellow=21-50, orange=51-100, red=101+).").export(unit3DSize, false, 10);

        new X3DomExporter(new File(visualsFolder, "files_3d.html"), "A 3D View of All Files", "Each block is one file. The height of the block represents the file relative size in lines of code. The color of the file represents its unit size category (green=0-200, yellow=201-500, orange=501-1000, red=1001+).").export(files3D, false, 50);
    }

    public BasicColorInfo getFileSizeColor(SourceFileSizeDistribution distribution, int linesOfCode) {
        if (linesOfCode <= distribution.getMediumRiskThreshold()) {
            return Thresholds.RISK_GREEN;
        } else if (linesOfCode <= distribution.getHighRiskThreshold()) {
            return Thresholds.RISK_YELLOW;
        } else if (linesOfCode <= distribution.getVeryHighRiskThreshold()) {
            return Thresholds.RISK_ORANGE;
        } else {
            return Thresholds.RISK_RED;
        }
    }


    private File getHtmlFolder(File reportsFolder) {
        File folder = new File(reportsFolder, HTML_REPORTS_FOLDER_NAME);
        folder.mkdirs();
        return folder;
    }

    private void saveTextualSummary(File reportsFolder, CodeAnalysisResults analysisResults) throws IOException {
        File jsonFile = new File(dataExporter.getDataFolder(), "textualSummary.txt");
        FileUtils.write(jsonFile, analysisResults.getTextSummary().toString(), UTF_8);
    }

    private File prepareReportsFolder(CommandLine cmd) throws IOException {
        File reportsFolder = new File(cmd.getOptionValue(outputFolder.getOpt()));
        reportsFolder.mkdirs();

        return reportsFolder;
    }

    private void usage() {
        System.out.println("java -jar sokrates.jar init [options]");
        System.out.println("java -jar sokrates.jar generateReports [options]");
    }

    private void usage(String prefix, Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(120);
        formatter.printHelp("java -jar sokrates.jar " + prefix + " [options]", options);
    }

    private CodeAnalyzerSettings getCodeAnalyzerSettings(CommandLine cmd) {
        CodeAnalyzerSettings settings = new CodeAnalyzerSettings();

        if (cmd.hasOption(all.getOpt())) {
            return settings;
        } else {
            settings.deselectAll();

            if (cmd.hasOption(scope.getOpt())) {
                settings.setAnalyzeFilesInScope(true);
            }
            if (cmd.hasOption(findings.getOpt())) {
                settings.setAnalyzeFilesInScope(true);
            }
            if (cmd.hasOption(duplication.getOpt())) {
                settings.setAnalyzeDuplication(true);
            }
            if (cmd.hasOption(logicalDecomposition.getOpt())) {
                settings.setAnalyzeLogicalDecomposition(true);
            }
            if (cmd.hasOption(crossCuttingConcerns.getOpt())) {
                settings.setAnalyzeCrossCuttingConcerns(true);
            }
            if (cmd.hasOption(fileSize.getOpt())) {
                settings.setAnalyzeFileSize(true);
            }
            if (cmd.hasOption(conditionalComplexity.getOpt())) {
                settings.setAnalyzeConditionalComplexity(true);
            }
            if (cmd.hasOption(unitSize.getOpt())) {
                settings.setAnalyzeUnitSize(true);
            }
            if (cmd.hasOption(metrics.getOpt())) {
                settings.setCreateMetricsList(true);
            }
            if (cmd.hasOption(controls.getOpt())) {
                settings.setAnalyzeControls(true);
            }
        }

        return settings;
    }

    private Options getReportingOptions() {
        Options options = new Options();
        options.addOption(all);
        options.addOption(scope);
        options.addOption(json);
        options.addOption(txt);
        options.addOption(duplication);
        options.addOption(logicalDecomposition);
        options.addOption(crossCuttingConcerns);
        options.addOption(fileSize);
        options.addOption(metrics);
        options.addOption(conditionalComplexity);
        options.addOption(confFile);
        options.addOption(outputFolder);
        options.addOption(unitSize);
        options.addOption(findings);
        options.addOption(controls);

        outputFolder.setRequired(true);
        confFile.setRequired(true);

        return options;
    }

    private Options getInitOptions() {
        Options options = new Options();
        options.addOption(srcRoot);
        options.addOption(confFile);

        confFile.setRequired(false);

        return options;
    }

    public void setProgressFeedback(ProgressFeedback progressFeedback) {
        this.progressFeedback = progressFeedback;
    }
}
