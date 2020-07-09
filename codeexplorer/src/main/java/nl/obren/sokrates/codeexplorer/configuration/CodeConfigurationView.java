/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.codeexplorer.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import javafx.concurrent.Worker;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import nl.obren.sokrates.cli.CommandLineInterface;
import nl.obren.sokrates.codeexplorer.CodeExplorerLauncher;
import nl.obren.sokrates.codeexplorer.codebrowser.CodeBrowserPane;
import nl.obren.sokrates.codeexplorer.newproject.NewProjectDialog;
import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.common.io.UserProperties;
import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.common.utils.SystemUtils;
import nl.obren.sokrates.common.utils.Templates;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.core.CodeConfigurationUtils;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzerFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class CodeConfigurationView extends ConfigurationEditorView {
    public static final String LAST_CONFIGURATION_FILE_PROPERTY = "lastCodeExplorerConfigurationFile";
    public static final String RECENT_CONFIGURATION_FILES_PROPERTY = "recentConfigurationFiles";
    private static final Log LOG = LogFactory.getLog(CodeConfigurationView.class);
    private File file;
    private Stage primaryStage;
    private CodeBrowserPane codeBrowserPane;

    public CodeConfigurationView(Stage primaryStage, CodeBrowserPane codeBrowserPane) {
        super();
        this.primaryStage = primaryStage;
        this.codeBrowserPane = codeBrowserPane;
    }

    @Override
    protected void initEngine() {
        editorWebView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == Worker.State.SUCCEEDED) {
                if (CodeExplorerLauncher.initSrcRoot != null) {
                    file = new File(CodeExplorerLauncher.initSrcRoot);
                    if (!file.exists()) {
                        file = null;
                    }
                    addToRecentlyUsedFilesList();
                    openFile();
                } else if (UserProperties.getInstance("sokrates").getFileProperty(LAST_CONFIGURATION_FILE_PROPERTY) != null) {
                    file = UserProperties.getInstance("sokrates").getFileProperty(LAST_CONFIGURATION_FILE_PROPERTY);
                    addToRecentlyUsedFilesList();
                    openFile();
                } else {
                    newConfiguration();
                }
            }
        });
        LOG.info("Loading " + Templates.EDITOR_TEMPLATE_PATH);
        editorWebView.getEngine().load(Templates.EDITOR_TEMPLATE_PATH);
    }

    private void addToRecentlyUsedFilesList() {
        UserProperties.getInstance("sokrates").addToListProperty(RECENT_CONFIGURATION_FILES_PROPERTY, file);
    }

    public void newConfiguration() {
        new NewProjectDialog().showAndWait(configuration -> {
            executeScript(configuration.getRight());
            goToLine(0);
            file = configuration.getLeft();
            setWindowTitle(primaryStage);
            run();
            return null;
        });
    }

    public CodeConfiguration getConfigurationFromEditor() {
        try {
            CodeConfiguration codeConfiguration = (CodeConfiguration) new JsonMapper().getObject(getText(), CodeConfiguration.class);
            LanguageAnalyzerFactory.getInstance().setOverrides(codeConfiguration.getAnalysis().getAnalyzerOverrides());
            return codeConfiguration;
        } catch (IOException | IllegalArgumentException e) {
            LOG.error(e);
        }
        return null;
    }

    public boolean completeAndSave() {
        CodeConfiguration codeConfiguration = getConfigurationFromEditor();
        if (codeConfiguration != null) {
            try {
                String configurationContent = new JsonGenerator().generate(codeConfiguration);
                setEditorValue(configurationContent);
                if (file == null) {
                    FileChooser fileChooser = new FileChooser();
                    fileChooser.setTitle("Save Configuration File");
                    fileChooser.setInitialFileName(CodeConfigurationUtils.getDefaultSokratesConfigFile(new File(codeConfiguration.getSrcRoot())).getName());
                    fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("Configuration", "json"));
                    file = fileChooser.showSaveDialog(primaryStage);
                }
                return saveFile(configurationContent, primaryStage);
            } catch (JsonProcessingException e) {
                LOG.info(e);
            }
        }

        return false;
    }

    private boolean saveFile(String configurationContent, Stage primaryStage) {
        if (file != null) {
            try {
                addToRecentlyUsedFilesList();
                FileUtils.write(file, configurationContent, StandardCharsets.UTF_8);
                setWindowTitle(primaryStage);
                UserProperties.getInstance("sokrates").setProperty(LAST_CONFIGURATION_FILE_PROPERTY, file);
                return true;
            } catch (IOException e) {
                LOG.error(e);
            }
        }
        return false;
    }

    private void setWindowTitle(Stage primaryStage) {
        if (primaryStage != null) {
            primaryStage.setTitle("Sokrates Code Explorer" + (file != null ? " - " + file.getPath() : ""));
        }
    }

    public void saveAs() {
        File originalFile = file;
        file = null;
        if (!completeAndSave()) {
            file = originalFile;
        }
    }

    public void openConfiguration() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Configuration File");
        fileChooser.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("Configuration", "json"));
        file = fileChooser.showOpenDialog(primaryStage);
        openFile();
    }

    public void openFile() {
        if (file != null) {
            try {
                addToRecentlyUsedFilesList();
                setWindowTitle(primaryStage);
                String codeConfigurationString = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                setEditorValue(codeConfigurationString);
                UserProperties.getInstance("sokrates").setProperty(LAST_CONFIGURATION_FILE_PROPERTY, file);
                goToLine(0);
                Executors.newCachedThreadPool().execute(() -> {
                    try {
                        CodeConfiguration codeConfiguration = (CodeConfiguration) new JsonMapper().getObject(codeConfigurationString, CodeConfiguration.class);
                        LanguageAnalyzerFactory.getInstance().setOverrides(codeConfiguration.getAnalysis().getAnalyzerOverrides());
                        codeBrowserPane.load(codeConfiguration);
                    } catch (IOException e) {
                        LOG.info(e);
                    }
                });
            } catch (IOException e) {
                LOG.error(e);
            }
        }
    }

    public void run() {
        completeAndSave();
        codeBrowserPane.load();
    }

    public void openReportsFolder() {
        File absoluteSrcRoot = new File(CodeConfiguration.getAbsoluteSrcRoot(getConfigurationFromEditor().getSrcRoot(), file));
        File reportsFolder = CodeConfigurationUtils.getDefaultSokratesReportsFolder(absoluteSrcRoot);
        SystemUtils.openFile(reportsFolder);
    }

    private void generateReports(String reportType) {
        completeAndSave();

        File absoluteSrcRoot = new File(CodeConfiguration.getAbsoluteSrcRoot(getConfigurationFromEditor().getSrcRoot(), file));
        File reportsFolder = CodeConfigurationUtils.getDefaultSokratesReportsFolder(absoluteSrcRoot);
        ProgressFeedback progressFeedback = codeBrowserPane.getConsole().getProgressFeedback();
        progressFeedback.start();
        CommandLineInterface commandLineInterface = new CommandLineInterface();
        commandLineInterface.setProgressFeedback(progressFeedback);
        Executors.newCachedThreadPool().execute(() -> {
            try {
                String reportTypeOption = "-" + reportType;
                String configFileOption = "-" + CommandLineInterface.CONF_FILE;
                String outputFolderOption = "-" + CommandLineInterface.OUTPUT_FOLDER;
                String commandLine = "java -jar sokrates.jar "
                        + CommandLineInterface.GENERATE_REPORTS + " "
                        + reportTypeOption + " "
                        + configFileOption + " '" + file.getPath() + "' "
                        + outputFolderOption + " '" + reportsFolder.getPath() + "'";
                progressFeedback.setText("Command line: <span style='background-color: #e5ffe5;'>" + commandLine + "</span>");
                progressFeedback.setText("");
                commandLineInterface.run(new String[]{CommandLineInterface.GENERATE_REPORTS,
                        reportTypeOption,
                        configFileOption, file.getPath(),
                        outputFolderOption, reportsFolder.getPath()});
            } catch (IOException e) {
                LOG.warn(e);
            } catch (Throwable e) {
                LOG.warn(e);
            } finally {
                progressFeedback.setText("Done.");
                progressFeedback.end();
            }
        });
    }

    public void generateFullReport() {
        generateReports(CommandLineInterface.REPORT_ALL);
    }

    public void generateFilesInScopeReport() {
        generateReports(CommandLineInterface.REPORT_OVERVIEW);
    }

    public void generateDuplicationReport() {
        generateReports(CommandLineInterface.REPORT_DUPLICATION);
    }

    public void generateLogicalDecompositionReport() {
        generateReports(CommandLineInterface.REPORT_LOGICAL_DECOMPOSITION);
    }

    public void generateConcernsReport() {
        generateReports(CommandLineInterface.REPORT_CONCERNS);
    }

    public void generateFileSizeReport() {
        generateReports(CommandLineInterface.REPORT_FILE_SIZE);
    }

    public void generateUnitSizeReport() {
        generateReports(CommandLineInterface.REPORT_UNIT_SIZE);
    }

    public void generateConditionalComplexity() {
        generateReports(CommandLineInterface.REPORT_CONDITIONAL_COMPLEXITY);
    }

    public void generateFindingsOverviewReport() {
        generateReports(CommandLineInterface.REPORT_OVERVIEW);
    }

    public void generateMetricsOverviewReport() {
        generateReports(CommandLineInterface.REPORT_OVERVIEW);
    }

    public void generateControlsReport() {
        generateReports(CommandLineInterface.REPORT_CONTROLS);
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void openFindings() {
        File findingsFile = CodeConfigurationUtils.getDefaultSokratesFindingsFile(this.file.getParentFile());
        if (!findingsFile.exists()) {
            try {
                FileUtils.write(findingsFile, "", StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        SystemUtils.openFile(findingsFile);
    }
}
