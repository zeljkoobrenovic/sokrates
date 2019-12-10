/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.codeexplorer.dependencies;

import javafx.geometry.Side;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.util.Callback;
import nl.obren.sokrates.codeexplorer.common.UXUtils;

public class DependenciesView extends BorderPane {
    private final TextArea graphvizTextArea = new TextArea();
    private final TextArea htmlTextArea = new TextArea();
    protected WebView webView = new WebView();
    private int lastSelectedTabIndex = 0;
    private boolean renderGraphviz;

    public DependenciesView(Callback<String, Void> reloadCallback, boolean renderGraphviz) {
        this.renderGraphviz = renderGraphviz;
        webView.setZoom(0.9);

        UXUtils.addPasteHandler(webView);
        UXUtils.addCopyHandler(webView);

        TabPane tabs = getTabPane(reloadCallback);
        if (renderGraphviz) {
            tabs.getTabs().add(new Tab("diagram", webView));
        }
        tabs.getTabs().add(new Tab("graphviz code", graphvizTextArea));
        if (renderGraphviz) {
            tabs.getTabs().add(new Tab("html+svg code", htmlTextArea));
        }

        setCenter(tabs);
    }

    private TabPane getTabPane(Callback<String, Void> reloadCallback) {
        TabPane tabs = new TabPane();

        tabs.getSelectionModel().selectedItemProperty().addListener(event -> {
            if (tabs.getSelectionModel().getSelectedIndex() == 0 && lastSelectedTabIndex > 0) {
                reloadCallback.call(graphvizTextArea.getText());
            }
            lastSelectedTabIndex = tabs.getSelectionModel().getSelectedIndex();
        });
        tabs.setSide(Side.BOTTOM);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        return tabs;
    }

    public void load(String html, String graphviz) {
        if (renderGraphviz) {
            webView.getEngine().loadContent(html);
        }
        graphvizTextArea.setText(graphviz);
        if (renderGraphviz) {
            htmlTextArea.setText(html);
        }
    }
}
