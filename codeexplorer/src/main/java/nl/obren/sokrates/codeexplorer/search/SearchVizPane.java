/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.codeexplorer.search;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import nl.obren.sokrates.codeexplorer.codebrowser.CodeBrowserPane;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.dependencies.Dependency;
import nl.obren.sokrates.sourcecode.search.SearchResultCleaner;
import nl.obren.sokrates.sourcecode.search.SearchResultDependencies;

import java.util.ArrayList;
import java.util.List;

public class SearchVizPane {
    private Stage stage;

    private SearchDependenciesPane searchDependenciesPane;
    private SearchCleanerPane searchCleanerPane;
    private SearchResultCleaner searchResultCleaner = new SearchResultCleaner();
    private ComboBox logicalDecompositions = new ComboBox<>();
    private SearchResultDependencies dependenciesExtractor;

    private CodeBrowserPane codeBrowserPane;

    public SearchVizPane(CodeBrowserPane codeBrowserPane) {
        this.codeBrowserPane = codeBrowserPane;
    }

    public void show() {
        if (stage == null) {
            createDependenciesStage();
        }
        dependenciesExtractor = new SearchResultDependencies(codeBrowserPane.getAspectFilesBrowserPane().getFileListPane().getSearchResult(), searchResultCleaner);
        searchCleanerPane.connect(dependenciesExtractor.getSearchResult(), searchResultCleaner, param -> {
            // updateDependecies();
            return null;
        });
        // updateDependecies();
        stage.show();
    }

    private void createDependenciesStage() {
        stage = new Stage();
        stage.setTitle("Found content instances distribution");
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        searchCleanerPane = new SearchCleanerPane();
        tabs.getTabs().add(new Tab("Cleaning", searchCleanerPane));
        searchDependenciesPane = new SearchDependenciesPane();
        loadLogicalDecompositions();
        tabs.getTabs().add(new Tab("Dependencies", new BorderPane(searchDependenciesPane,
                logicalDecompositions, null, null, null)));

        tabs.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
            if (newTab.getText().equalsIgnoreCase("Dependencies")) {
                updateDependecies();
            }
        });

        Scene value = new Scene(tabs, 800, 600);
        stage.setScene(value);
    }

    private void loadLogicalDecompositions() {
        ObservableList<Object> items = FXCollections.observableArrayList();
        codeBrowserPane.getCodeConfiguration().getLogicalDecompositions().forEach(logicalDecomposition -> {
            items.add(logicalDecomposition.getName() + " logical decomposition");
        });
        logicalDecompositions.setPrefWidth(800);
        logicalDecompositions.setItems(items);

        if (items.size() > 0) {
            logicalDecompositions.getSelectionModel().select(0);
        }

        logicalDecompositions.setOnAction(e -> updateDependecies());
    }

    private void updateDependecies() {
        List<Dependency> dependencies = dependenciesExtractor.getDependencies();
        int index = logicalDecompositions.getSelectionModel().getSelectedIndex();
        List<ComponentDependency> componentDependencies = dependenciesExtractor.getComponentDependencies(dependencies,
                codeBrowserPane.getCodeConfiguration().getLogicalDecompositions().get(index).getName());

        searchDependenciesPane.getDependenciesPane().showDependencies(dependencies, componentDependencies, new ArrayList<>(), "LR");
        searchCleanerPane.updateTable();
    }

}
