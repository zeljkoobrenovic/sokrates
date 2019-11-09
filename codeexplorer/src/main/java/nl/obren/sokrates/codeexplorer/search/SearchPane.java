package nl.obren.sokrates.codeexplorer.search;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import nl.obren.sokrates.codeexplorer.codebrowser.CodeBrowserPane;
import nl.obren.sokrates.codeexplorer.common.SvgIcons;
import nl.obren.sokrates.codeexplorer.common.UXUtils;
import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.codeexplorer.common.ProgressFeedbackPane;
import nl.obren.sokrates.sourcecode.SearcheableFilesCache;
import nl.obren.sokrates.sourcecode.SourceFileFilter;
import nl.obren.sokrates.sourcecode.SourceFileWithSearchData;
import nl.obren.sokrates.sourcecode.aspects.SourceCodeAspect;
import nl.obren.sokrates.sourcecode.search.SearchExpression;
import nl.obren.sokrates.sourcecode.search.SearchRequest;
import nl.obren.sokrates.sourcecode.search.SearchResult;

import java.util.concurrent.Executors;


public class SearchPane extends BorderPane {
    private final Button moreButton;
    private final ProgressFeedbackPane progressFeedbackPane;
    private TextField pathFilterField = new TextField();
    private TextField contentFilterField = new TextField();
    private GridPane gridPane = new GridPane();
    private Accordion accordion = new Accordion();
    private SearcheableFilesCache searcheableFilesCache;
    private SearchResult searchResult = new SearchResult(new SearchRequest());
    private CodeBrowserPane codeBrowserPane;
    private SourceCodeAspect aspect;

    public SearchPane(CodeBrowserPane codeBrowserPane) {
        this.codeBrowserPane = codeBrowserPane;

        addPathPane();
        addContentPane();
        moreButton = UXUtils.getButtonWithoutStyle(SvgIcons.SVG_MORE);
        moreButton.setPadding(new Insets(0, 0, 0, 0));
        gridPane.add(moreButton, 2, 0);

        accordion.getPanes().add(new TitledPane("search", gridPane));
        setCenter(accordion);
        progressFeedbackPane = new ProgressFeedbackPane();

        setColumnConstraints();

        pathFilterField.setOnAction(event -> search());
        contentFilterField.setOnAction(event -> search());
    }

    protected void setColumnConstraints() {
        ColumnConstraints col1 = new ColumnConstraints();
        ColumnConstraints col2 = new ColumnConstraints();

        col2.setHgrow(Priority.ALWAYS);

        gridPane.getColumnConstraints().addAll(col1, col2);
    }

    public void setPredefinedFilter(SourceFileFilter filter) {
        pathFilterField.setText(filter.getPathPattern());
        contentFilterField.setText(filter.getContentPattern());
        search();
    }

    protected void addPathPane() {
        gridPane.add(new Label("path: "), 0, 0);
        gridPane.add(pathFilterField, 1, 0);
    }

    protected void addContentPane() {
        gridPane.add(new Label("content: "), 0, 1);
        gridPane.add(contentFilterField, 1, 1);
    }

    protected void search() {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setPathSearchExpression(new SearchExpression(pathFilterField.getText()));
        searchRequest.setContentSearchExpression(new SearchExpression(contentFilterField.getText()));

        search(searchRequest, progressFeedbackPane.getProgressFeedback());
    }

    public void filter(SourceFileFilter sourceFileFilter) {
        pathFilterField.setText(sourceFileFilter.getPathPattern());
        contentFilterField.setText(sourceFileFilter.getContentPattern());
        search();
    }

    public void setAspect(SourceCodeAspect aspect) {
        pathFilterField.setText("");
        String firstPredefinedContentFilter = aspect.getSourceFileFilters().size() > 0 ? aspect.getSourceFileFilters().get(0).getContentPattern() : "";
        contentFilterField.setText(firstPredefinedContentFilter);
        setBottom(null);
        this.aspect = aspect;
        if (!firstPredefinedContentFilter.isEmpty()) {
            new PredefinedFiltersMenuPopulator().populate(moreButton, aspect.getSourceFileFilters(), this);
            accordion.setExpandedPane(accordion.getPanes().get(0));
            search();
        }
    }

    public TextField getPathFilterField() {
        return pathFilterField;
    }

    public TextField getContentFilterField() {
        return contentFilterField;
    }

    public SearcheableFilesCache createSearcheableFilesCache(SourceCodeAspect aspect) {
        if (searcheableFilesCache != null) {
            searcheableFilesCache.search().clear();
        }

        searcheableFilesCache = SearcheableFilesCache.getInstance(aspect.getSourceFiles());

        ObservableList<SourceFileWithSearchData> items = FXCollections.observableArrayList();
        searcheableFilesCache.search().entrySet().stream().forEach(item -> items.add(item.getValue()));

        Platform.runLater(() -> {
            codeBrowserPane.getAspectFilesBrowserPane().setItems(aspect.getName(), searcheableFilesCache.getSourceFiles(), searchResult, items);
        });

        setAspect(aspect);

        return searcheableFilesCache;
    }

    public void search(SearchRequest searchRequest, ProgressFeedback progressFeedback) {
        if (searcheableFilesCache != null) {
            setBottom(progressFeedbackPane);
            Executors.newCachedThreadPool().execute(() -> {
                ObservableList<SourceFileWithSearchData> cachedFiles = getCachedFiles(searchRequest, progressFeedback);
                Platform.runLater(() -> {
                    codeBrowserPane.getAspectFilesBrowserPane().setItems(aspect.getName(), searcheableFilesCache.getSourceFiles(), searchResult, cachedFiles);
                    setBottom(new SearchSummaryPane(codeBrowserPane, aspect, cachedFiles));
                });
            });
        }
    }

    protected ObservableList<SourceFileWithSearchData> getCachedFiles(SearchRequest searchRequest, ProgressFeedback progressFeedback) {
        ObservableList<SourceFileWithSearchData> items = FXCollections.observableArrayList();
        if (searcheableFilesCache != null) {
            searchResult = searcheableFilesCache.search(searchRequest, progressFeedback);
            searchResult.getFoundFiles().entrySet().stream().forEach(item -> items.add(item.getValue()));
        }
        return items;
    }

    public void clear() {
        pathFilterField.setText("");
        contentFilterField.setText("");
        setBottom(null);
    }

    public SearchResult getSearchResult() {
        return searchResult;
    }
}
