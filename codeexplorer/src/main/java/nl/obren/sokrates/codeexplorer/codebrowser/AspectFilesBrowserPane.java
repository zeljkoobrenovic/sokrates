/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.codeexplorer.codebrowser;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import nl.obren.sokrates.codeexplorer.charts.FileSizeDistributionChartPanel;
import nl.obren.sokrates.codeexplorer.common.SvgIcons;
import nl.obren.sokrates.codeexplorer.common.UXUtils;
import nl.obren.sokrates.codeexplorer.duplication.DuplicatesPane;
import nl.obren.sokrates.codeexplorer.preview.CodePreviewEditor;
import nl.obren.sokrates.codeexplorer.search.SearchPane;
import nl.obren.sokrates.codeexplorer.search.SearchResultsDistributionTablePane;
import nl.obren.sokrates.codeexplorer.units.UnitsPane;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.SourceFileFilter;
import nl.obren.sokrates.sourcecode.SourceFileWithSearchData;
import nl.obren.sokrates.sourcecode.findings.Findings;
import nl.obren.sokrates.sourcecode.search.SearchRequest;
import nl.obren.sokrates.sourcecode.search.SearchResult;

import java.util.List;

public class AspectFilesBrowserPane extends BorderPane {
    private CodePreviewEditor codePreviewEditor;
    private CodeBrowserPane codeBrowserPane;
    private SearchResultsDistributionTablePane searchResultsDistributionTablePane = new SearchResultsDistributionTablePane();
    private FileSizeDistributionChartPanel fileSizeDistributionChartPanel = new FileSizeDistributionChartPanel();
    private FileListPane fileListPane;
    private SearchPane searchPane;
    private SplitPane splitPane = new SplitPane();
    private TabPane previewPane;
    private List<SourceFile> sourceFiles;
    private BorderPane fileListTabsPane;
    private BorderPane fileListTabsPaneTop;
    private Button unitsButton;
    private Button duplicatesButton;

    public AspectFilesBrowserPane(CodeBrowserPane codeBrowserPane, Findings findings) {
        this.codeBrowserPane = codeBrowserPane;
        this.codePreviewEditor = new CodePreviewEditor(findings);
        searchPane = new SearchPane(codeBrowserPane);
        fileListPane = new FileListPane(this);

        configureSplitPane();

        setCenter(splitPane);

        enableButtons();
    }

    public CodePreviewEditor getCodePreviewEditor() {
        return codePreviewEditor;
    }

    protected void configureSplitPane() {
        splitPane.setOrientation(Orientation.VERTICAL);
        searchPane.setMaxHeight(90);
        fileListPane.setTop(searchPane);

        SplitPane tableSplitPane = new SplitPane();
        tableSplitPane.setDividerPositions(0.4);
        tableSplitPane.setOrientation(Orientation.VERTICAL);

        tableSplitPane.getItems().add(getAspectsListTabs());
        TabPane tabs = getListsTabPane();
        fileListTabsPane = new BorderPane(tabs);
        fileListTabsPaneTop = new BorderPane();
        fileListTabsPaneTop.setRight(getToolBar());
        fileListTabsPane.setTop(fileListTabsPaneTop);

        tableSplitPane.getItems().add(fileListTabsPane);

        splitPane.getItems().add(tableSplitPane);

        splitPane.getItems().add(getDetailsPane());
        splitPane.setDividerPositions(0.7);
        splitPane.setOrientation(Orientation.HORIZONTAL);

    }

    private TabPane getListsTabPane() {
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        BorderPane filesPane = new BorderPane(fileListPane);
        tabs.getTabs().add(new Tab("files", filesPane));
        tabs.getTabs().add(new Tab("files size distribution", fileSizeDistributionChartPanel));
        return tabs;
    }

    private Pane getAspectsListTabs() {
        AspectsTablePane scopeAspectsTablePane = new AspectsTablePane("Scope", false);
        scopeAspectsTablePane.setCodeBrowserPane(codeBrowserPane);
        codeBrowserPane.setScopeAspectsTablePane(scopeAspectsTablePane);

        AspectsTablePane concernAspectsTablePane = new AspectsTablePane("Concern", true);
        concernAspectsTablePane.setCodeBrowserPane(codeBrowserPane);
        codeBrowserPane.setConcernAspectsTablePane(concernAspectsTablePane);

        AspectsTablePane logicalComponentsTablePane = new AspectsTablePane("Component", true);
        logicalComponentsTablePane.setCodeBrowserPane(codeBrowserPane);
        codeBrowserPane.setLogicalComponentsTablePane(logicalComponentsTablePane);

        TabPane aspectsTabPane = new TabPane();
        aspectsTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Tab scope = new Tab("scope", scopeAspectsTablePane);
        scope.setGraphic(UXUtils.getIcon(SvgIcons.SVG_SCOPE));
        scope.setText("scope");
        aspectsTabPane.getTabs().add(scope);
        Tab tabLogicalComponents = new Tab("components", logicalComponentsTablePane);
        tabLogicalComponents.setGraphic(UXUtils.getIcon(SvgIcons.SVG_COMPONENT));
        tabLogicalComponents.setText("components");
        tabLogicalComponents.setId("logical_components_tab");
        aspectsTabPane.getTabs().add(tabLogicalComponents);
        Tab concerns = new Tab("concerns", concernAspectsTablePane);
        concerns.setGraphic(UXUtils.getIcon(SvgIcons.SVG_CONCERN));
        concerns.setText("concerns");
        aspectsTabPane.getTabs().add(concerns);
        BorderPane pane = new BorderPane();

        pane.setCenter(aspectsTabPane);

        return pane;
    }


    private TabPane getDetailsPane() {
        previewPane = new TabPane();
        previewPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab preview = new Tab("file preview", codePreviewEditor);
        preview.setGraphic(UXUtils.getIcon(SvgIcons.SVG_PREVIEW));
        preview.setText("file preview");
        previewPane.getTabs().add(preview);
        return previewPane;
    }

    void previewFile(SourceFileWithSearchData sourceFileWithSearchData) {
        if (sourceFileWithSearchData != null) {
            codePreviewEditor.setText(sourceFileWithSearchData);
            previewPane.getTabs().get(0).setText(sourceFileWithSearchData.getSourceFile().getFile().getName());
        }
    }

    public FileListPane getFileListPane() {
        return fileListPane;
    }

    public void filter(SourceFileFilter sourceFileFilter) {
        searchPane.filter(sourceFileFilter);
    }


    public void setItems(String title, List<SourceFile> sourceFiles, SearchResult searchResult, ObservableList<SourceFileWithSearchData> items) {
        updateTitle(title, sourceFiles);

        this.sourceFiles = sourceFiles;
        enableButtons();
        fileSizeDistributionChartPanel.setData(sourceFiles);
        if (items != null) {
            fileListPane.setItems(searchResult, items);
            searchResultsDistributionTablePane.setItems(searchResult);
        }

        if (items == null || items.size() == 0) {
            getCodePreviewEditor().clear();
            previewPane.getTabs().get(0).setText("file preview");
        }
    }

    private void enableButtons() {
        duplicatesButton.setDisable(sourceFiles == null || sourceFiles.size() == 0);
        unitsButton.setDisable(sourceFiles == null || sourceFiles.size() == 0);
    }

    private void updateTitle(String title, List<SourceFile> sourceFiles) {
        String titleText = " " + title.replace("-", "").trim().toUpperCase() + " files (" + sourceFiles.size() + ")";
        updateTitle(titleText);
    }

    private void updateTitle(String titleText) {
        Text text = new Text(titleText);
        Font font = text.getFont();
        text.setFont(Font.font(font.getFamily(), FontWeight.BOLD, font.getSize() + 1));
        fileListTabsPaneTop.setLeft(new BorderPane(text));
    }

    public SearchResultsDistributionTablePane getSearchResultsDistributionTablePane() {
        return searchResultsDistributionTablePane;
    }

    public SearchPane getSearchPane() {
        return searchPane;
    }

    public Node getToolBar() {
        BorderPane toolBar = new BorderPane();
        duplicatesButton = new Button("duplicates...");
        duplicatesButton.setOnAction(event -> DuplicatesPane.openInWindow(sourceFiles));
        duplicatesButton.setId("duplicates");
        toolBar.setLeft(duplicatesButton);
        unitsButton = new Button("units...");
        unitsButton.setId("units");
        unitsButton.setOnAction(event -> UnitsPane.openInWindow(sourceFiles));
        toolBar.setRight(unitsButton);
        return toolBar;
    }

    public void clear() {
        updateTitle("");
        searchPane.clear();
        codePreviewEditor.clear();
        fileListPane.setItems(new SearchResult(new SearchRequest()), FXCollections.observableArrayList());
        previewPane.getTabs().get(0).setText("file preview");
    }
}
