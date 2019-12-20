/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.codeexplorer.preview;

import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import nl.obren.sokrates.codeexplorer.common.UXUtils;
import nl.obren.sokrates.common.utils.Templates;
import nl.obren.sokrates.sourcecode.SourceFile;
import org.apache.commons.text.StringEscapeUtils;

public class SimpleCodePreviewView extends BorderPane {
    private WebView editorWebView = new WebView();

    public SimpleCodePreviewView() {
        editorWebView.setZoom(0.9);
        editorWebView.getEngine().load(Templates.CODE_PREVIEW_TEMPLATE_PATH);
        UXUtils.addCopyHandler(editorWebView);

        setCenter(editorWebView);
    }

    public void clear() {
        editorWebView.getEngine().executeScript("setValue(\"\", \"\")");
    }

    public String getEditorValue() {
        return (String) editorWebView.getEngine().executeScript("getValue()");
    }

    public void setEditorValue(SourceFile sourceFile) {
        String content = sourceFile.getContent();
        editorWebView.getEngine().executeScript("setValue(\"" + StringEscapeUtils.escapeEcmaScript(content) + "\", \"" + sourceFile.getFile().getPath() + "\")");
    }

    public void goToLine(int lineIndex) {
        editorWebView.getEngine().executeScript("gotoLine(" + lineIndex + ")");
    }

    public void select(int startLine, int endLine) {
        editorWebView.getEngine().executeScript("selectLines(" + startLine + ", " + endLine + ")");
    }
}
