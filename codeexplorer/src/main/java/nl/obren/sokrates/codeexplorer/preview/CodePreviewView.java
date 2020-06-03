/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.codeexplorer.preview;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import nl.obren.sokrates.codeexplorer.common.UXUtils;
import nl.obren.sokrates.common.utils.Templates;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.findings.Findings;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzerFactory;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

public class CodePreviewView extends BorderPane {
    private static Map<String, String> types = new HashMap<>();
    private static String summary = "";
    private final Label infoLabel;
    private WebView editorWebView = new WebView();
    private SourceFile sourceFile;
    private ComboBox<String> contentCleaningOptions = new ComboBox<>();
    private CheckBox autoScroll = new CheckBox("auto scroll");
    private Button evidenceButton = new Button("add finding");
    private String info = "";
    private int linesOfCode = 0;
    private boolean running = false;
    private int autoScrollPause = 150;
    private Findings findings;

    public CodePreviewView(Findings findings) {
        this.findings = findings;
        UXUtils.addCopyHandler(editorWebView);
        editorWebView.setZoom(0.9);
        editorWebView.getEngine().load(Templates.CODE_PREVIEW_TEMPLATE_PATH);

        setTop(getToolBar());
        setCenter(editorWebView);
        infoLabel = new Label();
        setBottom(infoLabel);
    }

    private ToolBar getToolBar() {
        ToolBar toolBar = new ToolBar();

        contentCleaningOptions.setMaxWidth(100);
        contentCleaningOptions.setItems(FXCollections.observableArrayList(
                "original",
                "cleaned",
                "for duplication"
        ));

        contentCleaningOptions.setOnAction(event -> reload());

        toolBar.getItems().add(new Label("Show: "));
        toolBar.getItems().add(contentCleaningOptions);
        toolBar.getItems().add(evidenceButton);
        evidenceButton.setOnAction(event -> {
            addFinding();
        });
        toolBar.getItems().add(autoScroll);
        autoScroll.setOnAction(event -> {
            if (autoScroll.isSelected() && updatePauseValue()) {
                animate();
            } else {
                running = false;
                autoScroll.setSelected(false);
            }
        });
        return toolBar;
    }

    private void addFinding() {
        String selectedText = getSelectedText();
        if (StringUtils.isBlank(selectedText)) {
            return;
        }

        String summary = getSummary();
        if (summary != null) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();

            findings.setSummary(summary);
            String content = "";
            content += "<p><b>" + sourceFile.getRelativePath() + "</b> "
                    + "[" + getSelectedRangeString() + "]:</p>\n";
            content += "<pre>\n" + selectedText + "\n</pre>\n";
            content += "<p>" + dtf.format(now) + "</p>\n";
            findings.setContent(content);
            findings.save();
        }
    }

    private String getSummary() {
        final TextInputDialog inputDlg = new TextInputDialog(summary);
        inputDlg.initOwner(getScene().getWindow());
        inputDlg.setTitle("Summary");
        inputDlg.setHeaderText("Summary");

        inputDlg.getDialogPane().setMinWidth(500);

        inputDlg.setWidth(600);

        Optional<String> optional = inputDlg.showAndWait();

        if (optional.isPresent()) {
            summary = optional.get();
            return summary;
        }

        return null;
    }

    private boolean updatePauseValue() {
        TextInputDialog dialog = new TextInputDialog(autoScrollPause + "");
        dialog.setTitle("Scroll Tempo");
        dialog.setHeaderText("Scrolling Step Pause");
        dialog.setContentText("Please enter scrolling step pause (ms):");

        Optional<String> result = dialog.showAndWait();
        boolean ok[] = {false};

        result.ifPresent(strPause -> {
            if (NumberUtils.isParsable(strPause)) {
                autoScrollPause = Integer.parseInt(strPause);
                ok[0] = true;
            } else {
                ok[0] = false;
            }
        });

        return ok[0];
    }

    private void animate() {
        if (autoScroll.isSelected()) {
            int line[] = {0};
            if (running) {
                running = false;
                sleep(autoScrollPause + 100);
            }
            Executors.newCachedThreadPool().execute(() -> {
                int step = 50;
                running = true;
                for (int i = 0; running && i < linesOfCode / step + 1; i++) {
                    Platform.runLater(() -> {
                        goToLine(line[0]);
                        line[0] += step;
                    });
                    sleep(autoScrollPause);
                }
                running = false;
            });
        } else {
            running = false;
        }
    }

    private void sleep(long period) {
        try {
            Thread.sleep(period);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setEditorValue(SourceFile sourceFile, String info) {
        infoLabel.setText(info);
        this.sourceFile = sourceFile;
        this.info = info;
        String content;
        if (contentCleaningOptions.getSelectionModel().getSelectedIndex() == 1) {
            content = LanguageAnalyzerFactory.getInstance().getLanguageAnalyzer(sourceFile).cleanForLinesOfCodeCalculations(sourceFile).getCleanedContent();
        } else if (contentCleaningOptions.getSelectionModel().getSelectedIndex() == 2) {
            content = LanguageAnalyzerFactory.getInstance().getLanguageAnalyzer(sourceFile).cleanForDuplicationCalculations(sourceFile).getCleanedContent();
        } else {
            content = sourceFile.getContent();
        }
        linesOfCode = StringUtils.countMatches(content, "\n") + 1;
        editorWebView.getEngine().executeScript("setValue(\"" + StringEscapeUtils.escapeEcmaScript(content) + "\", \"" + sourceFile.getFile().getPath() + "\")");
        animate();
    }

    public void clear() {
        this.info = "";
        infoLabel.setText("");
        this.sourceFile = null;
        editorWebView.getEngine().executeScript("setValue(\"\", \"\")");
    }

    public String getEditorValue() {
        return (String) editorWebView.getEngine().executeScript("getValue()");
    }

    public String getSelectedText() {
        return (String) editorWebView.getEngine().executeScript("getSelectedText()");
    }

    public String getSelectedRangeString() {
        Object range = editorWebView.getEngine().executeScript("getSelectedRange()");

        return range.toString().replace(",", ":");
    }

    private void reload() {
        setEditorValue(sourceFile, info);
    }

    public void goToLine(int lineIndex) {
        editorWebView.getEngine().executeScript("gotoLine(" + lineIndex + ")");
    }
}
