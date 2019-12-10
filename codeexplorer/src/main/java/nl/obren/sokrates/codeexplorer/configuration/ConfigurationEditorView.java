/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.codeexplorer.configuration;

import javafx.application.Platform;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import nl.obren.sokrates.codeexplorer.common.UXUtils;
import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.utils.Templates;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.concurrent.Executors;

public class ConfigurationEditorView extends BorderPane {
    public static final String FIND_AFTER_SEPARATOR = "::";
    private static final Log LOG = LogFactory.getLog(ConfigurationEditorView.class);
    protected WebView editorWebView = new WebView();
    private boolean finallySuccess = false;

    public ConfigurationEditorView() {
        editorWebView.setZoom(0.9);
        initEngine();

        UXUtils.addCopyHandler(editorWebView);

        setCenter(editorWebView);
    }

    protected void initEngine() {
        editorWebView.getEngine().load(Templates.EDITOR_TEMPLATE_PATH);
    }

    public void executeScript(Object object) {
        if (object != null) {
            try {
                setEditorValue(new JsonGenerator().generate(object));
            } catch (IOException e) {
                LOG.error(e);
            }
        }
    }

    public String getText() {
        return (String) editorWebView.getEngine().executeScript("editor.getValue()");
    }

    public void find(String text) {
        if (text.contains(FIND_AFTER_SEPARATOR)) {
            findAfter(text.substring(0, text.indexOf(FIND_AFTER_SEPARATOR)), text.substring(text.indexOf(FIND_AFTER_SEPARATOR) + FIND_AFTER_SEPARATOR.length()));
        } else {
            editorWebView.getEngine().executeScript("find(\"" + StringEscapeUtils.escapeEcmaScript(text) + "\")");
        }
    }

    public void findAfter(String textAfterWhichToFind, String text) {
        String script = "findAfter(\"" + StringEscapeUtils.escapeEcmaScript(textAfterWhichToFind) + "\", \"" + StringEscapeUtils.escapeEcmaScript(text) + "\")";
        editorWebView.getEngine().executeScript(script);
    }

    public void setEditorValue(String result) {
        executeScriptWaitUntilReady("setValue(\"" + StringEscapeUtils.escapeEcmaScript(result) + "\")");
    }

    public void executeScriptWaitUntilReady(String script) {
        if (finallySuccess) {
            executeScript(script);
        } else {
            Executors.newCachedThreadPool().execute(() -> {
                boolean success[] = {false};
                while (!success[0]) {
                    Platform.runLater(() -> {
                        success[0] = executeScript(script);
                    });
                    sleep(1000);
                }
                finallySuccess = true;
            });
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            LOG.fatal(e);
        }
    }

    private boolean executeScript(String script) {
        try {
            editorWebView.getEngine().executeScript(script);
            return true;
        } catch (Throwable e) {
        }
        return false;
    }

    public void openEditorFindDialog() {
        executeScriptWaitUntilReady("openEditorFindDialog()");
    }

    public void openEditorReplaceDialog() {
        executeScriptWaitUntilReady("openEditorReplaceDialog()");
    }

    public void goToLine(int lineIndex) {
        executeScriptWaitUntilReady("gotoLine(" + lineIndex + ")");
    }
}
