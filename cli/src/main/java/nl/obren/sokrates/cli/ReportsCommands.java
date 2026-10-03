package nl.obren.sokrates.cli;

import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.common.utils.*;
import nl.obren.sokrates.reports.core.ReportFileExporter;
import nl.obren.sokrates.reports.core.ReportNavigation;
import nl.obren.sokrates.reports.core.ReportHtmlWriter;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.dataexporters.DataExporter;
import nl.obren.sokrates.reports.generators.explorers.CommitsExplorerGenerators;
import nl.obren.sokrates.reports.generators.explorers.FilesExplorerGenerators;
import nl.obren.sokrates.reports.generators.explorers.UnitsExplorerGenerators;
import nl.obren.sokrates.reports.generators.statichtml.BasicSourceCodeReportGenerator;
import nl.obren.sokrates.sourcecode.analysis.CodeAnalyzer;
import nl.obren.sokrates.sourcecode.analysis.CodeAnalyzerSettings;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzerFactory;
import nl.obren.sokrates.sourcecode.scoping.ScopingConventions;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Collectors;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The generateReports command and the report generation step the analyze pipeline runs: the analysis, the data
 * export, the visuals, the HTML reports, the execution stats and the textual summary. Moved out of
 * {@link CommandLineInterface}, which keeps the option plumbing, the analyze pipeline and the init command.
 */
class ReportsCommands {
    private static final Log LOG = LogFactory.getLog(ReportsCommands.class);
    private final CommandLineInterface cli;
    private final Commands commands;
    private final DataExporter dataExporter;
    private CodeConfiguration codeConfiguration;

    ReportsCommands(CommandLineInterface cli, Commands commands) {
        this.cli = cli;
        this.commands = commands;
        this.dataExporter = new DataExporter(cli.progressFeedback());
    }

    void generateReports(String[] args) throws ParseException, IOException {
        Options options = commands.getReportingOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption(commands.getHelp().getOpt())) {
            CommandLineInterface.helpMode = true;
            commands.usage(Commands.GENERATE_REPORTS, commands.getReportingOptions(), Commands.GENERATE_REPORTS_DESCRIPTION);
            return;
        }

        cli.startTimeoutIfDefined(cmd);

        generateReports(cmd);
    }

    private void generateReports(CommandLine cmd) throws IOException {
        File sokratesConfigFile;
        if (!cmd.hasOption(commands.getConfFile().getOpt())) {
            String confFilePath = "./_sokrates/config.json";
            sokratesConfigFile = new File(confFilePath);
        } else {
            sokratesConfigFile = new File(cmd.getOptionValue(commands.getConfFile().getOpt()));
        }

        File reportsFolder;
        if (!cmd.hasOption(commands.getOutputFolder().getOpt())) {
            reportsFolder = prepareReportsFolder("./_sokrates/reports");
        } else {
            reportsFolder = prepareReportsFolder(cmd.getOptionValue(commands.getOutputFolder().getOpt()));
        }

        generateReports(cmd, sokratesConfigFile, reportsFolder);
    }

    void generateReports(CommandLine cmd, File sokratesConfigFile, File reportsFolder) throws IOException {
        cli.updateDateParam(cmd);

        LOG.info("Configuration file: " + sokratesConfigFile.getPath());
        if (noFileError(sokratesConfigFile)) return;

        ProcessingStopwatch.start("configuring");
        String jsonContent = FileUtils.readFileToString(sokratesConfigFile, UTF_8);
        this.codeConfiguration = (CodeConfiguration) new JsonMapper().getObject(jsonContent, CodeConfiguration.class);
        // Safety net for configurations written before init always ignored the analysis output (in memory only; the file is the user's).
        ScopingConventions.ensureSokratesOutputIgnored(this.codeConfiguration.getIgnore());
        LanguageAnalyzerFactory.getInstance().setOverrides(codeConfiguration.getAnalysis().getAnalyzerOverrides());

        cli.detailedInfo("Starting analysis based on the configuration file " + sokratesConfigFile.getPath());

        LOG.info("Reports folder: " + reportsFolder.getPath());
        ProcessingStopwatch.end("configuring");
        if (noFileError(reportsFolder)) return;

        if (cli.progressFeedback() == null) {
            cli.setProgressFeedback(new ProgressFeedback() {
                public void setText(String text) {
                    LOG.info(text.replaceAll("<.*?>", ""));
                }

                public void setDetailedText(String text) {
                    LOG.info(text.replaceAll("<.*?>", ""));
                }
            });
        }

        boolean dataOnly = cmd.hasOption(commands.getDataOnly().getOpt());
        dataExporter.setDataOnly(dataOnly);
        if (dataOnly) {
            LOG.info("-" + Commands.ARG_DATA_ONLY + ": storing only data/data.zip (no HTML reports, explorers, visuals or source viewer).");
        }

        try {
            CodeAnalyzer codeAnalyzer = new CodeAnalyzer(getCodeAnalyzerSettings(cmd), codeConfiguration, sokratesConfigFile);
            CodeAnalysisResults analysisResults = codeAnalyzer.analyze(cli.progressFeedback());

            ProcessingStopwatch.start("saving data");
            dataExporter.saveData(sokratesConfigFile, codeConfiguration, reportsFolder, analysisResults);
            saveTextualSummary(reportsFolder, analysisResults);
            ProcessingStopwatch.end("saving data");

            if (!dataOnly) {
                ProcessingStopwatch.start("generating visuals");
                new ReportVisualsGenerator(codeConfiguration).generateVisuals(reportsFolder, analysisResults);
                ProcessingStopwatch.end("generating visuals");

                generateAndSaveReports(sokratesConfigFile, reportsFolder, sokratesConfigFile.getParentFile(), codeAnalyzer, analysisResults);
            }
            saveExecutionStats(dataExporter.getDataFolder());
            // Final data step: package the whole data/ folder (incl. textual summary + execution
            // stats just written) into a single data/data.zip; the reports + landscape read from it.
            dataExporter.zipDataFolder();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void saveExecutionStats(File dataFolder) {
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

    private void generateAndSaveReports(File inputFile, File reportsFolder, File sokratesConfigFolder, CodeAnalyzer codeAnalyzer, CodeAnalysisResults analysisResults) {
        File htmlReports = getHtmlFolder(reportsFolder);
        File dataReports = dataExporter.getDataFolder();
        File srcCache = dataExporter.getCodeCacheFolder();
        CodeAnalyzerSettings codeAnalyzerSettings = codeAnalyzer.getCodeAnalyzerSettings();
        if (new File(htmlReports, "index.html").exists() || codeAnalyzerSettings.isUpdateIndex()) {
            cli.info("HTML reports: <a href='" + htmlReports.getPath() + "/index.html'>" + htmlReports.getPath() + "</a>");
        } else {
            cli.info("HTML reports: <a href='" + htmlReports.getPath() + "'>" + htmlReports.getPath() + "</a>");
        }
        cli.info("Raw data: <a href='" + dataReports.getPath() + "'>" + dataReports.getPath() + "</a>");
        if (analysisResults.getCodeConfiguration().getAnalysis().isSaveSourceFiles()) {
            cli.info("Source code cache : <a href='" + srcCache.getPath() + "'>" + srcCache.getPath() + "</a>");
        }
        ProcessingStopwatch.start("reporting");
        BasicSourceCodeReportGenerator generator = new BasicSourceCodeReportGenerator(codeAnalyzerSettings, analysisResults, inputFile, reportsFolder);
        List<RichTextReport> reports = generator.report();
        ProcessingStopwatch.end("reporting");

        ProcessingStopwatch.start("saving report");
        ReportNavigation navigation = ReportFileExporter.repositoryNavigation(analysisResults, sokratesConfigFolder);
        reports.forEach(report -> {
            report.setNavigation(navigation, report.getFileName());
            cli.info("Generating the '" + report.getId().toUpperCase() + "' report...");
            String processingName = "saving report/" + report.getId().toLowerCase() + "";
            ProcessingStopwatch.start(processingName);
            ReportHtmlWriter.exportHtml(reportsFolder, "html", report, analysisResults.getCodeConfiguration().getAnalysis().getCustomHtmlReportHeaderFragment());
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
        UnitsExplorerGenerators unitsExplorerGenerators = new UnitsExplorerGenerators(reportsFolder);
        unitsExplorerGenerators.exportJson(analysisResults);
        CommitsExplorerGenerators commitsExplorerGenerators = new CommitsExplorerGenerators(reportsFolder);
        commitsExplorerGenerators.exportJson(analysisResults, sokratesConfigFolder);
        ProcessingStopwatch.end("saving report/explorer");
        ProcessingStopwatch.end("saving report");
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

    File prepareReportsFolder(String path) throws IOException {
        File reportsFolder = new File(path);
        reportsFolder.mkdirs();

        return reportsFolder;
    }

    private CodeAnalyzerSettings getCodeAnalyzerSettings(CommandLine cmd) {
        CodeAnalyzerSettings settings = new CodeAnalyzerSettings();
        settings.setDataOnly(cmd.hasOption(commands.getDataOnly().getOpt()));

        if (codeConfiguration.getAnalysis().isSkipDependencies()) {
            settings.setAnalyzeStaticDependencies(false);
        }

        return settings;
    }
}
