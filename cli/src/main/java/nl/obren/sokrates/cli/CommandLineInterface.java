/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.cli;

import nl.obren.sokrates.cli.git.GitHistoryExtractor;
import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.common.renderingutils.Thresholds;
import nl.obren.sokrates.common.renderingutils.VisualizationItem;
import nl.obren.sokrates.common.renderingutils.VisualizationTemplate;
import nl.obren.sokrates.common.renderingutils.charts.Palette;
import nl.obren.sokrates.common.renderingutils.force3d.Force3DLink;
import nl.obren.sokrates.common.renderingutils.force3d.Force3DNode;
import nl.obren.sokrates.common.renderingutils.force3d.Force3DObject;
import nl.obren.sokrates.common.renderingutils.x3d.Unit3D;
import nl.obren.sokrates.common.renderingutils.x3d.X3DomExporter;
import nl.obren.sokrates.common.utils.*;
import nl.obren.sokrates.reports.core.ReportFileExporter;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.dataexporters.DataExporter;
import nl.obren.sokrates.reports.generators.explorers.FilesExplorerGenerators;
import nl.obren.sokrates.reports.generators.statichtml.BasicSourceCodeReportGenerator;
import nl.obren.sokrates.reports.landscape.statichtml.LandscapeAnalysisCommands;
import nl.obren.sokrates.sourcecode.Link;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.CodeAnalyzer;
import nl.obren.sokrates.sourcecode.analysis.CodeAnalyzerSettings;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.core.AnalysisConfig;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.core.CodeConfigurationUtils;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.githistory.ExtractGitHistoryFileHandler;
import nl.obren.sokrates.sourcecode.githistory.GitHistoryUtils;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisUtils;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class CommandLineInterface {
    public static final int THOUSAND_YEARS = 365 * 1000;
    private static final Log LOG = LogFactory.getLog(CommandLineInterface.class);
    private ProgressFeedback progressFeedback;
    private final DataExporter dataExporter = new DataExporter(this.progressFeedback);

    private final Commands commands = new Commands();
    private CodeConfiguration codeConfiguration;

    private static boolean helpMode = false;

    public static void main(String[] args) throws IOException {
        ProcessingStopwatch.startAsReference("everything");

        CommandLineInterface commandLineInterface = new CommandLineInterface();
        commandLineInterface.run(args);

        ProcessingStopwatch.end("everything");

        if (!helpMode) {
            ProcessingStopwatch.print();
        }

        System.exit(0);
    }

    public void run(String[] args) throws IOException {
        if (args.length == 0) {
            helpMode = true;
            commands.usage();
            return;
        }

        if (progressFeedback != null) {
            progressFeedback.clear();
        }

        try {
            if (args[0].equalsIgnoreCase(Commands.INIT)) {
                init(args);
                return;
            } else if (args[0].equalsIgnoreCase(Commands.UPDATE_CONFIG)) {
                updateConfig(args);
                return;
            } else if (args[0].equalsIgnoreCase(Commands.EXPORT_STANDARD_CONVENTIONS)) {
                exportConventions(args);
                return;
            } else if (args[0].equalsIgnoreCase(Commands.UPDATE_LANDSCAPE)) {
                updateLandscape(args);
                return;
            } else if (args[0].equalsIgnoreCase(Commands.INIT_CONVENTIONS)) {
                createNewConventionsFile(args);
                return;
            } else if (args[0].equalsIgnoreCase(Commands.EXTRACT_GIT_SUB_HISTORY)) {
                extractGitSubHistory(args);
                return;
            } else if (args[0].equalsIgnoreCase(Commands.EXTRACT_FILES)) {
                extractFiles(args);
                return;
            } else if (args[0].equalsIgnoreCase(Commands.EXTRACT_GIT_HISTORY)) {
                extractGitHistory(args);
                return;
            } else if (!args[0].equalsIgnoreCase(Commands.GENERATE_REPORTS)) {
                helpMode = true;
                commands.usage();
                return;
            }

            generateReports(args);
        } catch (ParseException e) {
            LOG.info("ERROR: " + e.getMessage() + "\n");
            e.printStackTrace();
            helpMode = true;
            commands.usage();
        }
    }

    private void extractGitHistory(String[] args) throws ParseException {
        Options options = commands.getExtractGitHistoryOption();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(commands.getHelp().getOpt())) {
            helpMode = true;
            commands.usage(Commands.EXTRACT_GIT_HISTORY, commands.getExtractGitHistoryOption(), Commands.EXTRACT_GIT_HISTORY_DESCRIPTION);
            return;
        }

        String strRootPath = cmd.getOptionValue(commands.getAnalysisRoot().getOpt());
        if (!cmd.hasOption(commands.getAnalysisRoot().getOpt())) {
            strRootPath = ".";
        }

        File root = new File(strRootPath);
        if (!root.exists()) {
            LOG.error("The analysis root \"" + root.getPath() + "\" does not exist.");
            return;
        }

        new GitHistoryExtractor().extractGitHistory(root);
    }

    private void extractGitSubHistory(String[] args) throws ParseException, IOException {
        Options options = commands.getExtractGitSubHistoryOption();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(commands.getHelp().getOpt())) {
            helpMode = true;
            commands.usage(Commands.EXTRACT_GIT_SUB_HISTORY, commands.getExtractGitSubHistoryOption(), Commands.EXTRACT_GIT_SUB_HISTORY_DESCRIPTION);
            return;
        }

        String strRootPath = cmd.getOptionValue(commands.getAnalysisRoot().getOpt());
        if (!cmd.hasOption(commands.getAnalysisRoot().getOpt())) {
            strRootPath = ".";
        }

        File root = new File(strRootPath);
        if (!root.exists()) {
            LOG.error("The analysis root \"" + root.getPath() + "\" does not exist.");
            return;
        }

        String prefixValue = cmd.getOptionValue(commands.getPrefix().getOpt());

        new ExtractGitHistoryFileHandler().extractSubHistory(new File(root, GitHistoryUtils.GIT_HISTORY_FILE_NAME), prefixValue);
    }

    private void extractFiles(String[] args) throws ParseException, IOException {
        Options options = commands.getExtractFilesOption();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(commands.getHelp().getOpt())) {
            helpMode = true;
            commands.usage(Commands.EXTRACT_FILES, commands.getExtractFilesOption(), Commands.EXTRACT_FILES_DESCRIPTION);
            return;
        }

        File root = cmd.hasOption(commands.getAnalysisRoot().getOpt()) ? new File(cmd.getOptionValue(commands.getAnalysisRoot().getOpt())) : new File(".");
        String patternValue = cmd.getOptionValue(commands.getPattern().getOpt());
        String dest = cmd.getOptionValue(commands.getDestRoot().getOpt());
        String destParentValue = cmd.getOptionValue(commands.getDestParent().getOpt());

        if (patternValue == null) {
            LOG.info("the pattern value is missing");
            return;
        }
        if (dest == null) {
            LOG.info("the destination folder value is missing");
            return;
        }
        if (destParentValue == null) {
            destParentValue = dest;
        }

        SokratesFileUtils.extractFiles(root, new File(root, dest), new File(root, destParentValue), patternValue);
    }

    private void updateDateParam(CommandLine cmd) {
        String dateString = cmd.getOptionValue(commands.getDate().getOpt());
        if (dateString != null) {
            LOG.info("Using '" + dateString + "' as latest source code update date for active contributors reports.");
            DateUtils.setDateParam(dateString);
        }
    }

    private void updateLandscape(String[] args) throws ParseException {
        Options options = commands.getUpdateLandscapeOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(commands.getHelp().getOpt())) {
            helpMode = true;
            commands.usage(Commands.UPDATE_LANDSCAPE, commands.getUpdateLandscapeOptions(), Commands.UPDATE_LANDSCAPE_DESCRIPTION);
            return;
        }

        startTimeoutIfDefined(cmd);

        String strRootPath = cmd.getOptionValue(commands.getAnalysisRoot().getOpt());
        if (!cmd.hasOption(commands.getAnalysisRoot().getOpt())) {
            strRootPath = ".";
        }

        File root = new File(strRootPath);
        if (!root.exists()) {
            LOG.error("The analysis root \"" + root.getPath() + "\" does not exist.");
            return;
        }

        Metadata metadata = new Metadata();

        updateMetadataFromCommandLine(cmd, metadata);

        String confFilePath = cmd.getOptionValue(commands.getConfFile().getOpt());
        updateDateParam(cmd);

        if (cmd.hasOption(commands.getRecursive().getOpt())) {
            List<File> landscapeConfigFiles = LandscapeAnalysisUtils.findAllSokratesLandscapeConfigFiles(root);
            landscapeConfigFiles.forEach(landscapeConfigFile -> {
                File landscapeFolder = landscapeConfigFile.getParentFile().getParentFile();
                String absolutePath = landscapeFolder.getAbsolutePath().replace("/./", "/");
                LOG.info(System.getProperty("user.dir"));
                System.setProperty("user.dir", absolutePath);
                LOG.info(System.getProperty("user.dir"));
                LandscapeAnalysisCommands.update(new File(landscapeFolder.getAbsolutePath()), null, metadata);
                DateUtils.reset();
                RegexUtils.reset();
                System.gc();
            });
            LOG.info("Analysed " + landscapeConfigFiles + " landscape(s):");
            landscapeConfigFiles.forEach(landscapeConfigFile -> {
                LOG.info(" -  " + landscapeConfigFile.getPath());
            });
            if (landscapeConfigFiles.size() > 0) {
                saveExecutionStats(new File(landscapeConfigFiles.get(landscapeConfigFiles.size() - 1).getParentFile(), "data"));
            }
        } else {
            File reportsFolder = LandscapeAnalysisCommands.update(root, confFilePath != null ? new File(confFilePath) : null, metadata);
            saveExecutionStats(new File(reportsFolder, "data"));
        }
    }

    private void generateReports(String[] args) throws ParseException, IOException {
        Options options = commands.getReportingOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(commands.getHelp().getOpt())) {
            helpMode = true;
            commands.usage(Commands.GENERATE_REPORTS, commands.getReportingOptions(), Commands.GENERATE_REPORTS_DESCRIPTION);
            return;
        }

        startTimeoutIfDefined(cmd);

        generateReports(cmd);
    }

    private void init(String[] args) throws ParseException, IOException {
        Options options = commands.getInitOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(commands.getHelp().getOpt())) {
            helpMode = true;
            commands.usage(Commands.INIT, commands.getInitOptions(), Commands.INIT_DESCRIPTION);
            return;
        }

        startTimeoutIfDefined(cmd);

        String strRootPath = cmd.getOptionValue(commands.getSrcRoot().getOpt());
        if (!cmd.hasOption(commands.getSrcRoot().getOpt())) {
            strRootPath = ".";
        }

        CustomScopingConventions customScopingConventions = null;
        if (cmd.hasOption(commands.getConventionsFile().getOpt())) {
            File scopingConventionsFile = new File(cmd.getOptionValue(commands.getConventionsFile().getOpt()));
            if (scopingConventionsFile.exists()) {
                customScopingConventions = CustomConventionsHelper.readFromFile(scopingConventionsFile);
            }
        }
        String nameValue = "";
        String descriptionValue = "";
        String logoLinkValue = "";
        if (cmd.hasOption(commands.getName().getOpt())) {
            nameValue = cmd.getOptionValue(commands.getName().getOpt());
        }
        if (cmd.hasOption(commands.getDescription().getOpt())) {
            descriptionValue = cmd.getOptionValue(commands.getDescription().getOpt());
        }
        if (cmd.hasOption(commands.getLogoLink().getOpt())) {
            logoLinkValue = cmd.getOptionValue(commands.getLogoLink().getOpt());
        }
        Link link = null;
        if (cmd.hasOption(commands.getAddLink().getOpt())) {
            String[] linkData = cmd.getOptionValues(commands.getAddLink().getOpt());
            if (linkData.length >= 1 && StringUtils.isNotBlank(linkData[0])) {
                String href = linkData[0];
                String label = linkData.length > 1 ? linkData[1] : "";
                link = new Link(label, href);
            }
        }


        File root = new File(strRootPath);
        if (!root.exists()) {
            LOG.error("The src root \"" + root.getPath() + "\" does not exist.");
            return;
        }

        File conf = getConfigFile(cmd, root);

        updateDateParam(cmd);

        new ScopeCreator(root, conf, customScopingConventions).createScopeFromConventions(nameValue, descriptionValue, logoLinkValue, link);

        LOG.info("Configuration stored in " + conf.getPath());
    }

    private void startTimeoutIfDefined(CommandLine cmd) {
        String timeoutSeconds = cmd.getOptionValue(commands.getTimeout().getOpt());
        if (StringUtils.isNumeric(timeoutSeconds)) {
            int seconds = Integer.parseInt(timeoutSeconds);
            LOG.info("Timeout timer set to " + seconds + " seconds.");
            Executors.newCachedThreadPool().execute(() -> {
                try {
                    Thread.sleep(seconds * 1000L);
                    LOG.info("Timeout after " + seconds + " seconds.");
                    System.exit(-1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void updateConfig(String[] args) throws ParseException, IOException {
        Options options = commands.getUpdateConfigOptions();

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(commands.getHelp().getOpt())) {
            helpMode = true;
            commands.usage(Commands.UPDATE_CONFIG, commands.getUpdateConfigOptions(), Commands.UPDATE_CONFIG_DESCRIPTION);
            return;
        }

        startTimeoutIfDefined(cmd);

        String strRootPath = cmd.getOptionValue(commands.getSrcRoot().getOpt());
        if (!cmd.hasOption(commands.getSrcRoot().getOpt())) {
            strRootPath = ".";
        }

        File root = new File(strRootPath);
        if (!root.exists()) {
            LOG.error("The src root \"" + root.getPath() + "\" does not exist.");
            return;
        }

        File confFile = getConfigFile(cmd, root);
        LOG.info("Configuration file '" + confFile.getPath() + "'.");

        String jsonContent = FileUtils.readFileToString(confFile, UTF_8);
        CodeConfiguration codeConfiguration = (CodeConfiguration) new JsonMapper().getObject(jsonContent, CodeConfiguration.class);

        if (cmd.hasOption(commands.getSkipComplexAnalyses().getOpt())) {
            codeConfiguration.getAnalysis().setSkipDependencies(true);
            codeConfiguration.getAnalysis().setSkipDuplication(true);
            codeConfiguration.getAnalysis().setSkipCorrelations(true);
            codeConfiguration.getAnalysis().setSaveSourceFiles(false);
        }

        if (cmd.hasOption(commands.getSkipDuplicationAnalyses().getOpt())) {
            codeConfiguration.getAnalysis().setSkipDuplication(true);
        }

        if (cmd.hasOption(commands.getSkipCorrelationAnalyses().getOpt())) {
            codeConfiguration.getAnalysis().setSkipCorrelations(true);
        }

        if (cmd.hasOption(commands.getEnableDuplicationAnalyses().getOpt())) {
            codeConfiguration.getAnalysis().setSkipDuplication(false);
        }

        Metadata metadata = codeConfiguration.getMetadata();
        updateMetadataFromCommandLine(cmd, metadata);

        if (cmd.hasOption(commands.getSetCacheFiles().getOpt())) {
            String cacheFileValue = cmd.getOptionValue(commands.getSetCacheFiles().getOpt());
            if (StringUtils.isNotBlank(cacheFileValue)) {
                codeConfiguration.getAnalysis().setSaveSourceFiles(cacheFileValue.equalsIgnoreCase("true"));
            }
        }

        FileUtils.write(confFile, new JsonGenerator().generate(codeConfiguration), UTF_8);
    }

    private void updateMetadataFromCommandLine(CommandLine cmd, Metadata metadata) {
        if (cmd.hasOption(commands.getSetName().getOpt())) {
            String name = cmd.getOptionValue(commands.getSetName().getOpt());
            if (StringUtils.isNotBlank(name)) {
                metadata.setName(name);
            }
        }

        if (cmd.hasOption(commands.getSetDescription().getOpt())) {
            String description = cmd.getOptionValue(commands.getSetDescription().getOpt());
            if (StringUtils.isNotBlank(description)) {
                metadata.setDescription(description);
            }
        }

        if (cmd.hasOption(commands.getSetLogoLink().getOpt())) {
            String logoLink = cmd.getOptionValue(commands.getSetLogoLink().getOpt());
            if (StringUtils.isNotBlank(logoLink)) {
                metadata.setLogoLink(logoLink);
            }
        }

        if (cmd.hasOption(commands.getAddLink().getOpt())) {
            String[] linkData = cmd.getOptionValues(commands.getAddLink().getOpt());
            if (linkData.length >= 1 && StringUtils.isNotBlank(linkData[0])) {
                String href = linkData[0];
                String label = linkData.length > 1 ? linkData[1] : "";
                metadata.getLinks().add(new Link(label, href));
            }
        }
    }

    private void exportConventions(String[] args) throws ParseException, IOException {
        File file = new File("standard_analysis_conventions.json");

        CustomConventionsHelper.saveStandardConventionsToFile(file);

        LOG.info("A standard conventions file saved to '" + file.getPath() + "'.");
    }

    private void createNewConventionsFile(String[] args) throws ParseException, IOException {
        File file = new File("analysis_conventions.json");

        CustomConventionsHelper.saveEmptyConventionsToFile(file);

        LOG.info("A new conventions file saved to '" + file.getPath() + "'.");
    }

    private File getConfigFile(CommandLine cmd, File root) {
        File conf;
        if (cmd.hasOption(commands.getConfFile().getOpt())) {
            conf = new File(cmd.getOptionValue(commands.getConfFile().getOpt()));
        } else {
            conf = CodeConfigurationUtils.getDefaultSokratesConfigFile(root);
        }
        return conf;
    }

    private void generateReports(CommandLine cmd) throws IOException {
        updateDateParam(cmd);

        File sokratesConfigFile;
        if (!cmd.hasOption(commands.getConfFile().getOpt())) {
            String confFilePath = "./_sokrates/config.json";
            sokratesConfigFile = new File(confFilePath);
        } else {
            sokratesConfigFile = new File(cmd.getOptionValue(commands.getConfFile().getOpt()));
        }

        LOG.info("Configuration file: " + sokratesConfigFile.getPath());
        if (noFileError(sokratesConfigFile)) return;

        ProcessingStopwatch.start("configuring");
        String jsonContent = FileUtils.readFileToString(sokratesConfigFile, UTF_8);
        this.codeConfiguration = (CodeConfiguration) new JsonMapper().getObject(jsonContent, CodeConfiguration.class);
        LanguageAnalyzerFactory.getInstance().setOverrides(codeConfiguration.getAnalysis().getAnalyzerOverrides());

        detailedInfo("Starting analysis based on the configuration file " + sokratesConfigFile.getPath());

        File reportsFolder;

        if (!cmd.hasOption(commands.getOutputFolder().getOpt())) {
            reportsFolder = prepareReportsFolder("./_sokrates/reports");
        } else {
            reportsFolder = prepareReportsFolder(cmd.getOptionValue(commands.getOutputFolder().getOpt()));
        }

        LOG.info("Reports folder: " + reportsFolder.getPath());
        ProcessingStopwatch.end("configuring");
        if (noFileError(reportsFolder)) return;

        if (this.progressFeedback == null) {
            this.progressFeedback = new ProgressFeedback() {
                public void setText(String text) {
                    LOG.info(text.replaceAll("<.*?>", ""));
                }

                public void setDetailedText(String text) {
                    LOG.info(text.replaceAll("<.*?>", ""));
                }
            };
        }

        try {
            CodeAnalyzer codeAnalyzer = new CodeAnalyzer(getCodeAnalyzerSettings(cmd), codeConfiguration, sokratesConfigFile);
            CodeAnalysisResults analysisResults = codeAnalyzer.analyze(progressFeedback);

            ProcessingStopwatch.start("saving data");
            dataExporter.saveData(sokratesConfigFile, codeConfiguration, reportsFolder, analysisResults);
            saveTextualSummary(reportsFolder, analysisResults);
            ProcessingStopwatch.end("saving data");

            ProcessingStopwatch.start("generating visuals");
            generateVisuals(reportsFolder, analysisResults);
            ProcessingStopwatch.end("generating visuals");

            generateAndSaveReports(sokratesConfigFile, reportsFolder, sokratesConfigFile.getParentFile(), codeAnalyzer, analysisResults);
            saveExecutionStats(dataExporter.getDataFolder());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveExecutionStats(File dataFolder) {
        try {
            List<ProcessingTimes> monitors = ProcessingStopwatch.getMonitors();

            String json = new JsonGenerator().generate(monitors);
            List<String> lines = monitors.stream().map(m -> m.getDurationMs() / 1000.0 + "s => " + m.getProcessing() + " " + ProcessingStopwatch.getPercentage(m.getDurationMs())).collect(Collectors.toList());
            String text = lines.stream().map(l -> StringUtils.repeat("  ", StringUtils.countMatches(l, '/')) + l).collect(Collectors.joining("\n"));

            FileUtils.write(new File(dataFolder, "executionTimes.json"), json, UTF_8);
            FileUtils.write(new File(dataFolder, "executionTimes.txt"), text, UTF_8);
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    private boolean noFileError(File inputFile) {
        if (!inputFile.exists()) {
            LOG.info("ERROR: " + inputFile.getPath() + " does not exist.");
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

    private void info(String text) {
        if (progressFeedback != null) {
            progressFeedback.setText(text);
        } else {
            LOG.info(text.replaceAll("<.*?>", ""));
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
        if (analysisResults.getCodeConfiguration().getAnalysis().isSaveSourceFiles()) {
            info("Source code cache : <a href='" + srcCache.getPath() + "'>" + srcCache.getPath() + "</a>");
        }
        ProcessingStopwatch.start("reporting");
        BasicSourceCodeReportGenerator generator = new BasicSourceCodeReportGenerator(codeAnalyzerSettings, analysisResults, inputFile, reportsFolder);
        List<RichTextReport> reports = generator.report();
        ProcessingStopwatch.end("reporting");

        ProcessingStopwatch.start("saving report");
        reports.forEach(report -> {
            info("Generating the '" + report.getId().toUpperCase() + "' report...");
            String processingName = "saving report/" + report.getId().toLowerCase() + "";
            ProcessingStopwatch.start(processingName);
            ReportFileExporter.exportHtml(reportsFolder, "html", report, analysisResults.getCodeConfiguration().getAnalysis().getCustomHtmlReportHeaderFragment());
            ProcessingStopwatch.end(processingName);
        });
        ProcessingStopwatch.start("saving report/index");
        if (!codeAnalyzerSettings.isDataOnly() && codeAnalyzerSettings.isUpdateIndex()) {
            ReportFileExporter.exportReportsIndexFile(reportsFolder, analysisResults, sokratesConfigFolder);
        }
        ProcessingStopwatch.end("saving report/index");
        ProcessingStopwatch.start("saving report/explorer");
        FilesExplorerGenerators filesExplorerGenerators = new FilesExplorerGenerators(reportsFolder);
        filesExplorerGenerators.exportJson(analysisResults);
        ProcessingStopwatch.end("saving report/explorer");
        ProcessingStopwatch.end("saving report");
    }


    private void generateVisuals(File reportsFolder, CodeAnalysisResults analysisResults) {
        AtomicInteger index = new AtomicInteger();
        analysisResults.getLogicalDecompositionsAnalysisResults().forEach(logicalDecomposition -> {
            index.getAndIncrement();
            List<VisualizationItem> items = new ArrayList<>();
            Force3DObject force3DObject = new Force3DObject();
            logicalDecomposition.getComponents().forEach(component -> {
                items.add(new VisualizationItem(component.getName(), component.getLinesOfCode()));
                force3DObject.getNodes().add(new Force3DNode(component.getName(), component.getLinesOfCode()));
            });
            logicalDecomposition.getComponentDependencies().forEach(dependency -> {
                force3DObject.getLinks().add(new Force3DLink(dependency.getFromComponent(), dependency.getToComponent(), dependency.getCount()));
            });
            try {
                String nameSuffix = "components_" + index.toString() + ".html";
                String nameSuffixDependencies = "dependencies_" + index.toString() + ".html";
                File folder = new File(reportsFolder, "html/visuals");
                folder.mkdirs();
                FileUtils.write(new File(folder, "bubble_chart_" + nameSuffix), new VisualizationTemplate().renderBubbleChart(items), UTF_8);
                FileUtils.write(new File(folder, "tree_map_" + nameSuffix), new VisualizationTemplate().renderTreeMap(items), UTF_8);
                FileUtils.write(new File(folder, "force_2d_" + nameSuffixDependencies), new VisualizationTemplate().render2DForceGraph(force3DObject), UTF_8);
                FileUtils.write(new File(folder, "force_3d_" + nameSuffixDependencies), new VisualizationTemplate().render3DForceGraph(force3DObject), UTF_8);

                generate3DUnitsView(folder, analysisResults);
            } catch (IOException e) {
                LOG.warn(e);
            }
        });

        try {
            File folder = new File(reportsFolder, "html/visuals");
            folder.mkdirs();

            List<SourceFile> mainSourceFiles = analysisResults.getMainAspectAnalysisResults().getAspect().getSourceFiles();
            generateFileStructureExplorers("main", folder, mainSourceFiles);
            generateFileStructureExplorers("test", folder, analysisResults.getTestAspectAnalysisResults().getAspect().getSourceFiles());
            generateFileStructureExplorers("generated", folder, analysisResults.getGeneratedAspectAnalysisResults().getAspect().getSourceFiles());
            generateFileStructureExplorers("build", folder, analysisResults.getBuildAndDeployAspectAnalysisResults().getAspect().getSourceFiles());
            generateFileStructureExplorers("other", folder, analysisResults.getOtherAspectAnalysisResults().getAspect().getSourceFiles());

            addCommitZoomableCircles("main", folder, mainSourceFiles, 30);
            addCommitZoomableCircles("main", folder, mainSourceFiles, 90);
            addCommitZoomableCircles("main", folder, mainSourceFiles, 180);
            addCommitZoomableCircles("main", folder, mainSourceFiles, 365);
            addCommitZoomableCircles("main", folder, mainSourceFiles, 0);

            addContributorsZoomableCircles("main", folder, mainSourceFiles, 30);
            addContributorsZoomableCircles("main", folder, mainSourceFiles, 90);
            addContributorsZoomableCircles("main", folder, mainSourceFiles, 180);
            addContributorsZoomableCircles("main", folder, mainSourceFiles, 365);
            addContributorsZoomableCircles("main", folder, mainSourceFiles, 0);

            addRiskColoredZoomableCircles(folder, mainSourceFiles, "loc", codeConfiguration.getAnalysis().getFileSizeThresholds(), Palette.getRiskPalette(), (sourceFile) -> sourceFile.getLinesOfCode(), (sourceFile) -> sourceFile.getLinesOfCode());

            addRiskColoredZoomableCircles(folder, mainSourceFiles, "age", codeConfiguration.getAnalysis().getFileAgeThresholds(), Palette.getAgePalette(), (sourceFile) -> sourceFile.getFileModificationHistory() != null ? sourceFile.getFileModificationHistory().daysSinceFirstUpdate() : 0, (sourceFile) -> sourceFile.getLinesOfCode());
            addRiskColoredZoomableCircles(folder, mainSourceFiles, "freshness", codeConfiguration.getAnalysis().getFileAgeThresholds(), Palette.getFreshnessPalette(),
                    (sourceFile) -> sourceFile.getFileModificationHistory() != null ? sourceFile.getFileModificationHistory().daysSinceLatestUpdate() : 0, (sourceFile) -> sourceFile.getLinesOfCode());

            addRiskColoredZoomableCircles(folder, mainSourceFiles, "update_frequency", codeConfiguration.getAnalysis().getFileUpdateFrequencyThresholds(), Palette.getHeatPalette(),
                    (sourceFile) -> sourceFile.getFileModificationHistory() != null ? sourceFile.getFileModificationHistory().getDates().size() : 0, (sourceFile) -> sourceFile.getLinesOfCode());

            addRiskColoredZoomableCircles(folder, mainSourceFiles, "contributors_count", codeConfiguration.getAnalysis().getFileContributorsCountThresholds(), Palette.getHeatPalette(),
                    (sourceFile) -> sourceFile.getFileModificationHistory() != null ? sourceFile.getFileModificationHistory().countContributors() : 0, (sourceFile) -> sourceFile.getLinesOfCode());

            generate3DUnitsView(folder, analysisResults);
        } catch (IOException e) {
            LOG.warn(e);
        }

    }

    private void generateFileStructureExplorers(String nameSuffix, File folder, List<SourceFile> sourceFiles) throws IOException {
        List<VisualizationItem> items = getZoomableCirclesItems(sourceFiles);
        FileUtils.write(new File(folder, "zoomable_circles_" + nameSuffix + ".html"), new VisualizationTemplate().renderZoomableCircles(items), UTF_8);
        FileUtils.write(new File(folder, "zoomable_sunburst_" + nameSuffix + ".html"), new VisualizationTemplate().renderZoomableSunburst(items), UTF_8);
    }

    private void addCommitZoomableCircles(String nameSuffix, File folder, List<SourceFile> sourceFiles, int daysAgo) throws IOException {
        List<VisualizationItem> commitItems = getZoomableCirclesCommitItems(sourceFiles, daysAgo > 0 ? daysAgo : THOUSAND_YEARS);
        String suffix = daysAgo > 0 ? "_" + daysAgo + "_" + nameSuffix : "";
        FileUtils.write(new File(folder, "zoomable_circles_commits" + suffix + ".html"), new VisualizationTemplate().renderZoomableCircles(commitItems), UTF_8);
        FileUtils.write(new File(folder, "zoomable_sunburst_commits" + suffix + ".html"), new VisualizationTemplate().renderZoomableSunburst(commitItems), UTF_8);
    }

    private void addContributorsZoomableCircles(String nameSuffix, File folder, List<SourceFile> sourceFiles, int daysAgo) throws IOException {
        List<VisualizationItem> commitItems = getZoomableCirclesContributorItems(sourceFiles, daysAgo > 0 ? daysAgo : THOUSAND_YEARS);
        String suffix = (daysAgo > 0 ? ("_" + daysAgo) : "") + ("_" + nameSuffix);
        FileUtils.write(new File(folder, "zoomable_circles_contributors" + suffix + ".html"), new VisualizationTemplate().renderZoomableCircles(commitItems), UTF_8);
        FileUtils.write(new File(folder, "zoomable_sunburst_contributors" + suffix + ".html"), new VisualizationTemplate().renderZoomableSunburst(commitItems), UTF_8);
    }

    private void addRiskColoredZoomableCircles(File folder, List<SourceFile> sourceFiles, String type,
                                               nl.obren.sokrates.sourcecode.threshold.Thresholds thresholds, Palette palette, DirectoryNode.SourceFileValueExtractor colorValueExtractor, DirectoryNode.SourceFileValueExtractor sizeValueExtractor) throws IOException {
        List<VisualizationItem> items = getZoomableCirclesRiskProfileItems(sourceFiles, thresholds, palette, colorValueExtractor, sizeValueExtractor);
        FileUtils.write(new File(folder, "zoomable_circles_main_" + type + "_coloring.html"), new VisualizationTemplate().renderZoomableCircles(items), UTF_8);

        List<VisualizationItem> itemsByCategory = getZoomableCirclesRiskProfileItemsCategories(sourceFiles, thresholds, palette, colorValueExtractor);
        FileUtils.write(new File(folder, "zoomable_circles_main_" + type + "_coloring_categories.html"), new VisualizationTemplate().renderZoomableCircles(itemsByCategory), UTF_8);
    }

    private List<VisualizationItem> getZoomableCirclesItems(List<SourceFile> sourceFiles) {
        DirectoryNode directoryTree = PathStringsToTreeStructure.createDirectoryTree(sourceFiles);
        if (directoryTree != null) {
            return directoryTree.toVisualizationItems();
        }

        return new ArrayList<>();
    }

    private List<VisualizationItem> getZoomableCirclesCommitItems(List<SourceFile> sourceFiles, int daysAgo) {
        DirectoryNode directoryTree = PathStringsToTreeStructure.createDirectoryTree(sourceFiles);
        if (directoryTree != null) {
            return directoryTree.toVisualizationCommitItems(daysAgo);
        }

        return new ArrayList<>();
    }

    private List<VisualizationItem> getZoomableCirclesContributorItems(List<SourceFile> sourceFiles, int daysAgo) {
        DirectoryNode directoryTree = PathStringsToTreeStructure.createDirectoryTree(sourceFiles);
        if (directoryTree != null) {
            return directoryTree.toVisualizationContributorItems(daysAgo);
        }

        return new ArrayList<>();
    }

    private List<VisualizationItem> getZoomableCirclesRiskProfileItems(
            List<SourceFile> sourceFiles, nl.obren.sokrates.sourcecode.threshold.Thresholds thresholds, Palette palette, DirectoryNode.SourceFileValueExtractor colorValueExtractor, DirectoryNode.SourceFileValueExtractor sizeValueExtractor) {
        DirectoryNode directoryTree = PathStringsToTreeStructure.createDirectoryTree(sourceFiles);
        if (directoryTree != null) {
            return directoryTree.toVisualizationRiskColoringItems(thresholds, palette, colorValueExtractor, sizeValueExtractor);
        }

        return new ArrayList<>();
    }

    private List<VisualizationItem> getZoomableCirclesRiskProfileItemsCategories(
            List<SourceFile> sourceFiles, nl.obren.sokrates.sourcecode.threshold.Thresholds thresholds, Palette palette, DirectoryNode.SourceFileValueExtractor valueExtractor) {

        VisualizationItem item1 = new VisualizationItem(thresholds.getNegligibleRiskLabel(), 0);
        VisualizationItem item2 = new VisualizationItem(thresholds.getLowRiskLabel(), 0);
        VisualizationItem item3 = new VisualizationItem(thresholds.getMediumRiskLabel(), 0);
        VisualizationItem item4 = new VisualizationItem(thresholds.getHighRiskLabel(), 0);
        VisualizationItem item5 = new VisualizationItem(thresholds.getVeryHighRiskLabel(), 0);

        sourceFiles.forEach(sourceFile -> {
            int value = valueExtractor.getValue(sourceFile);
            String path = sourceFile.getRelativePath();
            int loc = sourceFile.getLinesOfCode();
            if (value <= thresholds.getLow()) {
                item1.getChildren().add(new VisualizationItem(path, loc, PathStringsToTreeStructure.getColor(thresholds, palette, value)));
            } else if (value <= thresholds.getMedium()) {
                item2.getChildren().add(new VisualizationItem(path, loc, PathStringsToTreeStructure.getColor(thresholds, palette, value)));
            } else if (value <= thresholds.getHigh()) {
                item3.getChildren().add(new VisualizationItem(path, loc, PathStringsToTreeStructure.getColor(thresholds, palette, value)));
            } else if (value <= thresholds.getVeryHigh()) {
                item4.getChildren().add(new VisualizationItem(path, loc, PathStringsToTreeStructure.getColor(thresholds, palette, value)));
            } else {
                item5.getChildren().add(new VisualizationItem(path, loc, PathStringsToTreeStructure.getColor(thresholds, palette, value)));
            }
        });

        item1.setName(item1.getName() + " (" + item1.getChildren().size() + ")");
        item2.setName(item2.getName() + " (" + item2.getChildren().size() + ")");
        item3.setName(item3.getName() + " (" + item3.getChildren().size() + ")");
        item4.setName(item4.getName() + " (" + item4.getChildren().size() + ")");
        item5.setName(item5.getName() + " (" + item5.getChildren().size() + ")");

        return new ArrayList<>(Arrays.asList(item1, item2, item3, item4, item5));
    }

    private void generate3DUnitsView(File visualsFolder, CodeAnalysisResults analysisResults) {
        AnalysisConfig analysisConfig = analysisResults.getCodeConfiguration().getAnalysis();

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
            SourceFileSizeDistribution sourceFileSizeDistribution = new SourceFileSizeDistribution(analysisConfig.getFileSizeThresholds());
            BasicColorInfo color = getFileSizeColor(sourceFileSizeDistribution, file.getLinesOfCode());
            files3D.add(new Unit3D(file.getFile().getPath(), file.getLinesOfCode(), color));
        });

        new X3DomExporter(new File(visualsFolder, "units_3d_complexity.html"), "A 3D View of All Units (Conditional Complexity)", "Each block is one unit. The height of the block represents the file unit size in lines of code. The color of the unit represents its conditional complexity category.").export(unit3DConditionalComplexity, false, 10);

        new X3DomExporter(new File(visualsFolder, "units_3d_size.html"), "A 3D View of All Units (Unit Size)", "Each block is one unit. The height of the block represents the file unit size in lines of code. The color of the unit represents its size category.").export(unit3DSize, false, 10);

        new X3DomExporter(new File(visualsFolder, "files_3d.html"), "A 3D View of All Files", "Each block is one file. The height of the block represents the file relative size in lines of code. The color of the file represents its size category.").export(files3D, false, 50);
    }

    public BasicColorInfo getFileSizeColor(SourceFileSizeDistribution distribution, int linesOfCode) {
        if (linesOfCode <= distribution.getLowRiskThreshold()) {
            return Thresholds.RISK_GREEN;
        } else if (linesOfCode <= distribution.getMediumRiskThreshold()) {
            return Thresholds.RISK_LIGHT_GREEN;
        } else if (linesOfCode <= distribution.getHighRiskThreshold()) {
            return Thresholds.RISK_YELLOW;
        } else if (linesOfCode <= distribution.getVeryHighRiskThreshold()) {
            return Thresholds.RISK_ORANGE;
        } else {
            return Thresholds.RISK_RED;
        }
    }


    private File getHtmlFolder(File reportsFolder) {
        File folder = new File(reportsFolder, Commands.ARG_HTML_REPORTS_FOLDER_NAME);
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

    private CodeAnalyzerSettings getCodeAnalyzerSettings(CommandLine cmd) {
        CodeAnalyzerSettings settings = new CodeAnalyzerSettings();

        if (codeConfiguration.getAnalysis().isSkipDependencies()) {
            settings.setAnalyzeStaticDependencies(false);
        }

        return settings;
    }


    public void setProgressFeedback(ProgressFeedback progressFeedback) {
        this.progressFeedback = progressFeedback;
    }


}
