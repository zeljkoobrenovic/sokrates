package nl.obren.sokrates.codeexplorer.search;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;
import nl.obren.sokrates.codeexplorer.codebrowser.TableViewUtils;
import nl.obren.sokrates.sourcecode.search.CleanedFoundText;
import nl.obren.sokrates.sourcecode.search.ReplacePair;
import nl.obren.sokrates.sourcecode.search.SearchResult;
import nl.obren.sokrates.sourcecode.search.SearchResultCleaner;

import static javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY;

public class SearchCleanerPane extends BorderPane {
    private TextField removeFromStart;
    private TextField removeFromEnd;
    private CheckBox trim;
    private Button reload;
    private GridPane top = new GridPane();
    private TableView<ReplacePair> replacePairTableView = new TableView<>();
    private TableView<CleanedFoundText> cleanedFoundTextTableView = new TableView<>();
    private SearchResult searchResult;
    private SearchResultCleaner searchResultCleaner;

    public SearchCleanerPane() {
        TableViewUtils.addCopyToClipboardContextMenu(replacePairTableView);
        TableViewUtils.addCopyToClipboardContextMenu(cleanedFoundTextTableView);

        removeFromStart = new TextField();
        removeFromStart.setPrefWidth(400);
        removeFromEnd = new TextField();
        removeFromEnd.setPrefWidth(400);
        trim = new CheckBox();
        reload = new Button("reload");
        reload.setDefaultButton(true);

        setTop();
        addReplaceWithTable();
        addCleanedFoundTextTableView();
    }

    private void addReplaceWithTable() {
        TableColumn<ReplacePair, String> replaceColumn = new TableColumn<>("replace");
        replaceColumn.setCellValueFactory(new PropertyValueFactory<>("replace"));
        replaceColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        replaceColumn.setOnEditCommit(t -> t.getTableView().getItems().get(t.getTablePosition().getRow()).setReplace(t.getNewValue()));

        TableColumn<ReplacePair, String> withColumn = new TableColumn<>("with");
        withColumn.setCellValueFactory(new PropertyValueFactory<>("with"));
        withColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        withColumn.setOnEditCommit(t -> t.getTableView().getItems().get(t.getTablePosition().getRow()).setWith(t.getNewValue()));

        replacePairTableView.getColumns().addAll(replaceColumn, withColumn);

        replacePairTableView.setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY);
        replacePairTableView.setPrefHeight(100);
        replacePairTableView.setEditable(true);

        top.add(replacePairTableView, 1, 3);
    }

    private void addCleanedFoundTextTableView() {
        TableColumn<CleanedFoundText, String> originalColumn = new TableColumn<>("Original text");
        originalColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getFoundText().getText()));
        TableColumn<CleanedFoundText, String> cleanedColumn = new TableColumn<>("Cleaned text");
        cleanedColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getCleanedText()));

        cleanedFoundTextTableView.getColumns().addAll(originalColumn, cleanedColumn);

        cleanedFoundTextTableView.setColumnResizePolicy(CONSTRAINED_RESIZE_POLICY);
        cleanedFoundTextTableView.setPrefHeight(100);
        setCenter(cleanedFoundTextTableView);
    }

    private void setTop() {
        top.add(new Label("Remove from start pattern: "), 0, 0);
        top.add(removeFromStart, 1, 0);

        top.add(new Label("Remove from end pattern: "), 0, 1);
        top.add(removeFromEnd, 1, 1);

        top.add(new Label("Trim "), 0, 2);
        top.add(trim, 1, 2);

        top.add(reload, 2, 2);

        setTop(new TitledPane("Clean", top));
    }

    public void connect(SearchResult searchResult, SearchResultCleaner searchResultCleaner, Callback<Void, Void> callback) {
        this.searchResult = searchResult;
        this.searchResultCleaner = searchResultCleaner;
        removeFromStart.textProperty().addListener(event -> searchResultCleaner.setStartCleaningPattern(removeFromStart.getText()));
        removeFromEnd.textProperty().addListener(event -> searchResultCleaner.setEndCleaningPattern(removeFromEnd.getText()));
        trim.selectedProperty().addListener(event -> searchResultCleaner.setTrim(trim.isSelected()));
        reload.setOnAction(event -> callback.call(null));
        replacePairTableView.setItems(FXCollections.observableArrayList(searchResultCleaner.getReplacePairs()));
        cleanedFoundTextTableView.setItems(FXCollections.observableArrayList(searchResultCleaner.getCleanedTextList(searchResult.getFoundTextList())));
    }

    public void updateTable() {
        cleanedFoundTextTableView.setItems(FXCollections.observableArrayList(searchResultCleaner.getCleanedTextList(searchResult.getFoundTextList())));
    }
}
