/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.cli;

import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.common.renderingutils.GraphvizUtil;
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
import nl.obren.sokrates.reports.landscape.statichtml.LandscapeAnalysisCommands;
import nl.obren.sokrates.sourcecode.Link;
import nl.obren.sokrates.sourcecode.analysis.CodeAnalyzer;
import nl.obren.sokrates.sourcecode.analysis.CodeAnalyzerSettings;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.core.CodeConfigurationUtils;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzerFactory;
import nl.obren.sokrates.sourcecode.scoping.ScopeCreator;
import nl.obren.sokrates.sourcecode.scoping.custom.CustomConventionsHelper;
import nl.obren.sokrates.sourcecode.scoping.custom.CustomScopingConventions;
import nl.obren.sokrates.sourcecode.stats.SourceFileSizeDistribution;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.charset.StandardCharsets.UTF_8;

public class CommandLineInterface {
    public static final String INIT = "init";
    public static final String GENERATE_REPORTS = "generateReports";
    public static final String UPDATE_CONFIG = "updateConfig";
    public static final String EXPORT_STANDARD_CONVENTIONS = "exportStandardConventions";
    public static final String SRC_ROOT = "srcRoot";
    public static final String CONVENTIONS_FILE = "conventionsFile";
    public static final String CONF_FILE = "confFile";
    public static final String DATE = "date";
    public static final String REPORT_ALL = "reportAll";
    public static final String REPORT_DATA = "reportData";
    public static final String REPORT_OVERVIEW = "reportOverview";
    public static final String REPORT_FINDINGS = "reportFindings";
    public static final String REPORT_DUPLICATION = "reportDuplication";
    public static final String REPORT_LOGICAL_DECOMPOSITION = "reportLogicalDecompositions";
    public static final String REPORT_CONCERNS = "reportConcerns";
    public static final String REPORT_FILE_SIZE = "reportFileSize";
    public static final String REPORT_FILE_HISTORY = "reportFileHistory";
    public static final String REPORT_METRICS = "reportMetrics";
    public static final String REPORT_UNIT_SIZE = "reportUnitSize";
    public static final String REPORT_CONDITIONAL_COMPLEXITY = "reportConditionalComplexity";
    public static final String REPORT_CONTROLS = "reportControls";
    public static final String OUTPUT_FOLDER = "outputFolder";
    public static final String USE_INTERNAL_GRAPHVIZ = "internalGraphviz";
    public static final String HTML_REPORTS_FOLDER_NAME = "html";
    public static final String INIT_LANDSCAPE = "initLandscape";
    public static final String UPDATE_LANDSCAPE = "updateLandscape";
    public static final String ANALYSIS_ROOT = "analysisRoot";
    public static final String TIMEOUT = "timeout";

    public static final String SKIP_COMPLEX_ANALYSES = "skipComplexAnalyses";
    public static final String SET_CACHE_FILES = "setCacheFiles";

    public static final String SET_NAME = "setName";
    public static final String SET_LOGO_LINK = "setLogoLink";
    public static final String SET_DESCRIPTION = "setDescription";
    public static final String ADD_LINK = "addLink";

    private static final Log LOG = LogFactory.getLog(CommandLineInterface.class);
    private Option srcRoot = new Option(SRC_ROOT, true, "[OPTIONAL] the folder where reports will be stored (default is \"<currentFolder>/_sokrates/reports/\")");
    private Option conventionsFile = new Option(CONVENTIONS_FILE, true, "the custom conventions JSON file path\")");
    private Option confFile = new Option(CONF_FILE, true, "[OPTIONAL] the path to configuration file (default is \"<currentFolder>/_sokrates/config.json\"");
    private Option date = new Option(DATE, true, "[OPTIONAL] last date of source code update (default today), used for reports on active contributors");
    private Option analysisRoot = new Option(ANALYSIS_ROOT, true, "[OPTIONAL] the path to configuration file (default is \"<currentFolder>/_sokrates/config.json\"");
    private Option timeout = new Option(TIMEOUT, true, "[OPTIONAL] timeout in seconds");
    private Option all = new Option(REPORT_ALL, false, "[DEFAULT] generate all reports");
    private Option data = new Option(REPORT_DATA, false, "save analysis data in JSON and text format (in the _sokrates/reports/data folder)");
    private Option scope = new Option(REPORT_OVERVIEW, false, "generate the report describing the overview of files in scope");
    private Option findings = new Option(REPORT_FINDINGS, false, "generate the report describing the manual findings");
    private Option duplication = new Option(REPORT_DUPLICATION, false, "generate the duplication report (stored in <outputFolder>/Duplication.html)");
    private Option logicalDecomposition = new Option(REPORT_LOGICAL_DECOMPOSITION, false, "generate the logical decomposition report (stored in <outputFolder>/LogicalDecomposition.html)");
    private Option concerns = new Option(REPORT_CONCERNS, false, "generate the concerns report (stored in <outputFolder>/Concerns.html)");
    private Option fileSize = new Option(REPORT_FILE_SIZE, false, "generate the file size report (stored in <outputFolder>/FileSize.html)");
    private Option fileHistory = new Option(REPORT_FILE_HISTORY, false, "generate the file history reports (stored in <outputFolder>/FileAge.html, FileChangeFrequency.html, and TemporalDependencies.html)");
    private Option unitSize = new Option(REPORT_UNIT_SIZE, false, "generate the unit size report (stored in <outputFolder>/UnitSize.html)");
    private Option conditionalComplexity = new Option(REPORT_CONDITIONAL_COMPLEXITY, false, "generate the conditional complexity report (stored in <outputFolder>/ConditionalComplexity.html)");
    private Option metrics = new Option(REPORT_METRICS, false, "generate the metrics overview report (stored in <outputFolder>/Metrics.html)");
    private Option controls = new Option(REPORT_CONTROLS, false, "generate the controls report (stored in <outputFolder>/Controls.html)");
    private Option internalGraphviz = new Option(USE_INTERNAL_GRAPHVIZ, false, "use internal Graphviz library (by default external dot program is used, you may specify the external dot path via the system variable GRAPHVIZ_DOT)");

    private Option outputFolder = new Option(OUTPUT_FOLDER, true, "[OPTIONAL] the folder where reports will be stored (default value is <currentFolder/_sokrates/reports>)");

    private Option skipComplexAnalyses = new Option(SKIP_COMPLEX_ANALYSES, false, "[OPTIONAL] skips complex analyses (duplication, dependencies, file caching)");

    private Option setName = new Option(SET_NAME, true, "[OPTIONAL] sets a project name");
    private Option setDescription = new Option(SET_DESCRIPTION, true, "[OPTIONAL] sets a project description");
    private Option setLogoLink = new Option(SET_LOGO_LINK, true, "[OPTIONAL] sets a project logo link");
    private Option setCacheFiles = new Option(SET_CACHE_FILES, true, "[OPTIONAL] sets a cahche file flag ('true' or 'false')");
    private Option addLink = new Option(ADD_LINK, true, "link label [OPTIONAL] adds a new link to an external document");

    private ProgressFeedback progressFeedback;
    private DataExporter dataExporter = new DataExporter(this.progressFeedback);

    public static void main(String args[]) throws ParseException, IOException {
        BasicConfigurator.configure();

        CommandLineInterface commandLineInterface = new CommandLineInterface();
        commandLineInterface.run(args);

        System.exit(0);
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
            } else if (args[0].equalsIgnoreCase(UPDATE_CONFIG)) {
                updateConfig(args);
                return;
            } else if (args[0].equalsIgnoreCase(EXPORT_STANDARD_CONVENTIONS)) {
                exportConventions(args);
                return;
            } else if (args[0].equalsIgnoreCase(INIT_LANDSCAPE)) {
                initLandscape(args);
                return;
            } else if (args[0].equalsIgnoreCase(UPDATE_LANDSCAPE)) {
                updateLandscape(args);
                return;
            } else if (!args[0].equalsIgnoreCase(GENERATE_REPORTS)) {
                usage();
                return;
            }

            generateReports(args);
        } catch (ParseException e) {
            System.out.println("ERROR: " + e.getMessage() + "\n");
            usage();
        }
    }

    private void initLandscape(String[] args) throws ParseException {
        Options options = getInitLandscapeOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        startTimeoutIfDefined(cmd);

        String strRootPath = cmd.getOptionValue(analysisRoot.getOpt());
        if (!cmd.hasOption(analysisRoot.getOpt())) {
            strRootPath = ".";
        }

        File root = new File(strRootPath);
        if (!root.exists()) {
            LOG.error("The analysis root \"" + root.getPath() + "\" does not exist.");
            return;
        }

        String confFilePath = cmd.getOptionValue(confFile.getOpt());

        updateDateParam(cmd);

        LandscapeAnalysisCommands.init(root, confFilePath != null ? new File(confFilePath) : null);
    }

    private void updateDateParam(CommandLine cmd) {
        String dateString = cmd.getOptionValue(date.getOpt());
        if (dateString != null) {
            System.out.println("Using '" + dateString + "' as latest source code update date for active contributors reports.");
            DateUtils.setDateParam(dateString);
        }
    }

    private void updateLandscape(String[] args) throws ParseException {
        Options options = getInitLandscapeOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        startTimeoutIfDefined(cmd);

        String strRootPath = cmd.getOptionValue(analysisRoot.getOpt());
        if (!cmd.hasOption(analysisRoot.getOpt())) {
            strRootPath = ".";
        }

        File root = new File(strRootPath);
        if (!root.exists()) {
            LOG.error("The analysis root \"" + root.getPath() + "\" does not exist.");
            return;
        }

        String confFilePath = cmd.getOptionValue(confFile.getOpt());
        updateDateParam(cmd);

        LandscapeAnalysisCommands.update(root, confFilePath != null ? new File(confFilePath) : null);
    }

    private void generateReports(String[] args) throws ParseException, IOException {
        Options options = getReportingOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        startTimeoutIfDefined(cmd);

        generateReports(cmd);
    }

    private void init(String[] args) throws ParseException, IOException {
        Options options = getInitOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        startTimeoutIfDefined(cmd);

        String strRootPath = cmd.getOptionValue(srcRoot.getOpt());
        if (!cmd.hasOption(srcRoot.getOpt())) {
            strRootPath = ".";
        }

        CustomScopingConventions customScopingConventions = null;
        if (cmd.hasOption(conventionsFile.getOpt())) {
            File scopingConventionsFile = new File(cmd.getOptionValue(conventionsFile.getOpt()));
            if (scopingConventionsFile.exists()) {
                customScopingConventions = CustomConventionsHelper.readFromFile(scopingConventionsFile);
            }
        }

        File root = new File(strRootPath);
        if (!root.exists()) {
            LOG.error("The src root \"" + root.getPath() + "\" does not exist.");
            return;
        }

        File conf = getConfigFile(cmd, root);

        updateDateParam(cmd);

        new ScopeCreator(root, conf, customScopingConventions).createScopeFromConventions();

        System.out.println("Configuration stored in " + conf.getPath());
    }

    private void startTimeoutIfDefined(CommandLine cmd) {
        String timeoutSeconds = cmd.getOptionValue(timeout.getOpt());
        if (StringUtils.isNumeric(timeoutSeconds)) {
            int seconds = Integer.parseInt(timeoutSeconds);
            System.out.println("Timeout timer set to " + seconds + " seconds.");
            Executors.newCachedThreadPool().execute(() -> {
                try {
                    Thread.sleep(seconds * 1000);
                    System.out.println("Timeout after " + seconds + " seconds.");
                    System.exit(-1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void updateConfig(String[] args) throws ParseException, IOException {
        Options options = getUpdateConfigOptions();

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        startTimeoutIfDefined(cmd);

        String strRootPath = cmd.getOptionValue(srcRoot.getOpt());
        if (!cmd.hasOption(srcRoot.getOpt())) {
            strRootPath = ".";
        }

        File root = new File(strRootPath);
        if (!root.exists()) {
            LOG.error("The src root \"" + root.getPath() + "\" does not exist.");
            return;
        }

        File confFile = getConfigFile(cmd, root);
        System.out.println("Configuration file '" + confFile.getPath() + "'.");

        String jsonContent = FileUtils.readFileToString(confFile, UTF_8);
        CodeConfiguration codeConfiguration = (CodeConfiguration) new JsonMapper().getObject(jsonContent, CodeConfiguration.class);

        if (cmd.hasOption(skipComplexAnalyses.getOpt())) {
            codeConfiguration.getAnalysis().setSkipDependencies(true);
            codeConfiguration.getAnalysis().setSkipDuplication(true);
            codeConfiguration.getAnalysis().setCacheSourceFiles(false);
        }

        if (cmd.hasOption(setName.getOpt())) {
            String name = cmd.getOptionValue(setName.getOpt());
            if (StringUtils.isNotBlank(name)) {
                codeConfiguration.getMetadata().setName(name);
            }
        }

        if (cmd.hasOption(setDescription.getOpt())) {
            String description = cmd.getOptionValue(setDescription.getOpt());
            if (StringUtils.isNotBlank(description)) {
                codeConfiguration.getMetadata().setDescription(description);
            }
        }

        if (cmd.hasOption(setLogoLink.getOpt())) {
            String logoLink = cmd.getOptionValue(setLogoLink.getOpt());
            if (StringUtils.isNotBlank(logoLink)) {
                codeConfiguration.getMetadata().setLogoLink(logoLink);
            }
        }

        if (cmd.hasOption(setCacheFiles.getOpt())) {
            String cacheFileValue = cmd.getOptionValue(setCacheFiles.getOpt());
            if (StringUtils.isNotBlank(cacheFileValue)) {
                codeConfiguration.getAnalysis().setCacheSourceFiles(cacheFileValue.equalsIgnoreCase("true"));
            }
        }

        if (cmd.hasOption(addLink.getOpt())) {
            String linkData[] = cmd.getOptionValues(addLink.getOpt());
            if (linkData.length >= 1 && StringUtils.isNotBlank(linkData[0])) {
                String href = linkData[0];
                String label = linkData.length >= 1 ? linkData[1] : "";
                codeConfiguration.getMetadata().getLinks().add(new Link(label, href));
            }
        }

        FileUtils.write(confFile, new JsonGenerator().generate(codeConfiguration), UTF_8);
    }

    private void exportConventions(String[] args) throws ParseException, IOException {
        File file = new File("sokrates_standard_conventions.json");

        CustomConventionsHelper.saveStandardConventionsToFile(file);

        System.out.println("Conventions saved to '" + file.getPath() + "'.");
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
        updateDateParam(cmd);

        File sokratesConfigFile;

        if (!cmd.hasOption(confFile.getOpt())) {
            String confFilePath = "./_sokrates/config.json";
            sokratesConfigFile = new File(confFilePath);
        } else {
            sokratesConfigFile = new File(cmd.getOptionValue(confFile.getOpt()));
        }

        System.out.println("Configuration file: " + sokratesConfigFile.getPath());
        if (noFileError(sokratesConfigFile)) return;

        String jsonContent = FileUtils.readFileToString(sokratesConfigFile, UTF_8);
        CodeConfiguration codeConfiguration = (CodeConfiguration) new JsonMapper().getObject(jsonContent, CodeConfiguration.class);
        LanguageAnalyzerFactory.getInstance().setOverrides(codeConfiguration.getAnalysis().getAnalyzerOverrides());

        detailedInfo("Starting analysis based on the configuration file " + sokratesConfigFile.getPath());

        File reportsFolder;

        if (!cmd.hasOption(outputFolder.getOpt())) {
            reportsFolder = prepareReportsFolder("./_sokrates/reports");
        } else {
            reportsFolder = prepareReportsFolder(cmd.getOptionValue(outputFolder.getOpt()));
        }

        System.out.println("Reports folder: " + reportsFolder.getPath());
        if (noFileError(reportsFolder)) return;

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
            CodeAnalyzer codeAnalyzer = new CodeAnalyzer(getCodeAnalyzerSettings(cmd), codeConfiguration, sokratesConfigFile);
            CodeAnalysisResults analysisResults = codeAnalyzer.analyze(progressFeedback);

            boolean useDefault = noReportingOptions(cmd);

            if (useDefault || cmd.hasOption(all.getOpt()) || cmd.hasOption(data.getOpt())) {
                dataExporter.saveData(sokratesConfigFile, codeConfiguration, reportsFolder, analysisResults);
                saveTextualSummary(reportsFolder, analysisResults);
            }

            if (useDefault || cmd.hasOption(all.getOpt()) || cmd.hasOption(logicalDecomposition.getOpt())) {
                generateVisuals(reportsFolder, analysisResults);
            }

            generateAndSaveReports(sokratesConfigFile, reportsFolder, sokratesConfigFile.getParentFile(), codeAnalyzer, analysisResults);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean noFileError(File inputFile) {
        if (!inputFile.exists()) {
            System.out.println("ERROR: " + inputFile.getPath() + " does not exist.");
            return true;
        }
        return false;
    }

    private boolean noReportingOptions(CommandLine cmd) {
        for (Option arg : cmd.getOptions()) {
            if (arg.getOpt().toLowerCase().startsWith("report")) {
                return false;
            }
        }
        return true;
    }

    private boolean dataReportsOnly(CommandLine cmd) {
        boolean anyReports = false;
        for (Option arg : cmd.getOptions()) {
            String reportOption = arg.getOpt().toLowerCase();
            if (reportOption.startsWith("report")) {
                anyReports = true;
                if (!reportOption.equalsIgnoreCase(data.getOpt())) {
                    return false;
                }
            }
        }
        return anyReports;
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

    private void generateAndSaveReports(File inputFile, File reportsFolder, File sokratesConfigFolder, CodeAnalyzer codeAnalyzer, CodeAnalysisResults analysisResults) {
        File htmlReports = getHtmlFolder(reportsFolder);
        File dataReports = dataExporter.getDataFolder();
        File srcCache = dataExporter.getCodeCacheFolder();
        CodeAnalyzerSettings codeAnalyzerSettings = codeAnalyzer.getCodeAnalyzerSettings();
        if (new File(htmlReports, "index.html").exists() || codeAnalyzerSettings.isUpdateIndex()) {
            info("HTML reports: <a href='" + htmlReports.getPath() + "/index.html'>" + htmlReports.getPath() + "</a>");
        } else {
            info("HTML reports: <a href='" + htmlReports.getPath() + "'>" + htmlReports.getPath() + "</a>");
        }
        info("Raw data: <a href='" + dataReports.getPath() + "'>" + dataReports.getPath() + "</a>");
        if (analysisResults.getCodeConfiguration().getAnalysis().isCacheSourceFiles()) {
            info("Source code cache : <a href='" + srcCache.getPath() + "'>" + srcCache.getPath() + "</a>");
        }
        info("");
        info("");
        BasicSourceCodeReportGenerator generator = new BasicSourceCodeReportGenerator(codeAnalyzerSettings, analysisResults, inputFile, reportsFolder);
        List<RichTextReport> reports = generator.report();
        reports.forEach(report -> {
            info("Generating the '" + report.getId().toUpperCase() + "' report...");
            ReportFileExporter.exportHtml(reportsFolder, "html", report);
        });
        if (!codeAnalyzerSettings.isDataOnly() && codeAnalyzerSettings.isUpdateIndex()) {
            ReportFileExporter.exportReportsIndexFile(reportsFolder, analysisResults, sokratesConfigFolder);
        }
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
        File jsonFile = new File(dataExporter.getTextDataFolder(), "textualSummary.txt");
        FileUtils.write(jsonFile, analysisResults.getTextSummary().toString(), UTF_8);
    }

    private File prepareReportsFolder(String path) throws IOException {
        File reportsFolder = new File(path);
        reportsFolder.mkdirs();

        return reportsFolder;
    }

    private void usage() {
        System.out.println("\njava -jar sokrates.jar " + INIT + " [options]\n    Creates a Sokrates configuration file for a codebase");
        System.out.println("\njava -jar sokrates.jar " + GENERATE_REPORTS + " [options]\n    Generates Sokrates reports based on the configuration");
        System.out.println("\njava -jar sokrates.jar " + UPDATE_CONFIG + " [options]\n    Updates a configuration file and completes missing fields\n");
        System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - ");
        usage(INIT, getInitOptions());
        System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - ");
        usage(GENERATE_REPORTS, getReportingOptions());
        System.out.println("- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - ");
        usage(UPDATE_CONFIG, getUpdateConfigOptions());
        usage(INIT_LANDSCAPE, getInitLandscapeOptions());
        usage(UPDATE_LANDSCAPE, getUpdateLandscapeOptions());
    }

    private void usage(String prefix, Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(120);
        formatter.printHelp("java -jar sokrates.jar " + prefix + " [options]", options);
    }

    private CodeAnalyzerSettings getCodeAnalyzerSettings(CommandLine cmd) {
        CodeAnalyzerSettings settings = new CodeAnalyzerSettings();
        settings.setDataOnly(dataReportsOnly(cmd));

        boolean useDefault = noReportingOptions(cmd);
        settings.setUpdateIndex(useDefault || cmd.hasOption(all.getOpt()));

        if (fullAnalysisNeeded(cmd)) {
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
            if (cmd.hasOption(concerns.getOpt())) {
                settings.setAnalyzeConcerns(true);
            }
            if (cmd.hasOption(fileSize.getOpt())) {
                settings.setAnalyzeFileSize(true);
            }
            if (cmd.hasOption(fileHistory.getOpt())) {
                settings.setAnalyzeFileHistory(true);
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

            GraphvizUtil.useExternalGraphviz = !cmd.hasOption(internalGraphviz.getOpt());
        }

        return settings;
    }

    private boolean fullAnalysisNeeded(CommandLine cmd) {
        return noReportingOptions(cmd) || cmd.hasOption(all.getOpt()) || cmd.hasOption(data.getOpt());
    }

    private Options getReportingOptions() {
        Options options = new Options();
        options.addOption(confFile);
        options.addOption(outputFolder);
        options.addOption(all);
        options.addOption(scope);
        options.addOption(data);
        options.addOption(duplication);
        options.addOption(logicalDecomposition);
        options.addOption(concerns);
        options.addOption(fileSize);
        options.addOption(unitSize);
        options.addOption(conditionalComplexity);
        options.addOption(metrics);
        options.addOption(controls);
        options.addOption(findings);
        options.addOption(internalGraphviz);
        options.addOption(timeout);
        options.addOption(date);

        outputFolder.setRequired(true);
        confFile.setRequired(true);

        return options;
    }

    private Options getInitOptions() {
        Options options = new Options();
        options.addOption(srcRoot);
        options.addOption(confFile);
        options.addOption(conventionsFile);
        options.addOption(timeout);

        confFile.setRequired(false);

        return options;
    }

    private Options getUpdateConfigOptions() {
        Options options = new Options();
        options.addOption(confFile);
        options.addOption(skipComplexAnalyses);
        options.addOption(setCacheFiles);
        options.addOption(setName);
        options.addOption(setDescription);
        options.addOption(setLogoLink);
        options.addOption(addLink);
        options.addOption(timeout);

        addLink.setArgs(2);
        confFile.setRequired(false);
        skipComplexAnalyses.setRequired(false);
        setName.setRequired(false);
        setDescription.setRequired(false);
        setLogoLink.setRequired(false);

        return options;
    }

    private Options getInitLandscapeOptions() {
        Options options = new Options();
        options.addOption(analysisRoot);
        options.addOption(confFile);
        options.addOption(timeout);
        options.addOption(date);

        confFile.setRequired(false);

        return options;
    }

    private Options getUpdateLandscapeOptions() {
        Options options = new Options();
        options.addOption(analysisRoot);
        options.addOption(confFile);
        options.addOption(timeout);
        options.addOption(date);

        confFile.setRequired(false);

        return options;
    }

    public void setProgressFeedback(ProgressFeedback progressFeedback) {
        this.progressFeedback = progressFeedback;
    }
}
