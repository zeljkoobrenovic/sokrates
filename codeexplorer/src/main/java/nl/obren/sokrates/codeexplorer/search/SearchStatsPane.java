/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.codeexplorer.search;

import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import nl.obren.sokrates.codeexplorer.codebrowser.CodeBrowserPane;

public class SearchStatsPane {
    private Stage stage;

    private CodeBrowserPane codeBrowserPane;
    private SearchResultsDistributionTablePane searchResultsDistributionTablePane;

    public SearchStatsPane(CodeBrowserPane codeBrowserPane) {
        this.codeBrowserPane = codeBrowserPane;
    }


    public void show() {
        if (stage == null) {
            createStage();
        }
        stage.show();
    }

    private void createStage() {
        stage = new Stage();
        stage.setTitle("Found content instances distribution");
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        searchResultsDistributionTablePane = codeBrowserPane.getAspectFilesBrowserPane().getSearchResultsDistributionTablePane();
        tabs.getTabs().add(new Tab("Found Content", searchResultsDistributionTablePane));
        searchResultsDistributionTablePane.getSearchResult();
        Scene value = new Scene(tabs, 800, 600);
        stage.setScene(value);
    }
}
