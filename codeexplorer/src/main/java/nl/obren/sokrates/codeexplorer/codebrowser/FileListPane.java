package nl.obren.sokrates.codeexplorer.codebrowser;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;
import nl.obren.sokrates.codeexplorer.common.NumericBarCellFactory;
import nl.obren.sokrates.codeexplorer.common.PathCellRenderer;
import nl.obren.sokrates.sourcecode.SourceFileWithSearchData;
import nl.obren.sokrates.sourcecode.search.SearchResult;

public class FileListPane extends BorderPane {
    private TableView<SourceFileWithSearchData> table = new TableView<>();
    private SearchResult searchResult;

    public FileListPane(final AspectFilesBrowserPane aspectFilesBrowserPane) {
        setCenter(table);
        addTableColumns();
        setContextMenu();

        table.setEditable(false);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                aspectFilesBrowserPane.previewFile(newSelection);
            } else {
                aspectFilesBrowserPane.previewFile(null);
            }
        });

    }

    private void setContextMenu() {
        ContextMenu contextMenu = TableViewUtils.addCopyToClipboardContextMenu(table);

        Menu copySelectedItemMenu = new Menu("copy selected row data");

        MenuItem fileNameItem = new MenuItem("file name");
        fileNameItem.setOnAction(event -> copyToClipboard(table.getSelectionModel().getSelectedItem().getSourceFile().getFile().getName()));
        copySelectedItemMenu.getItems().add(fileNameItem);

        MenuItem relativePathItem = new MenuItem("relative path");
        relativePathItem.setOnAction(event -> copyToClipboard(table.getSelectionModel().getSelectedItem().getSourceFile().getRelativePath()));
        copySelectedItemMenu.getItems().add(relativePathItem);

        MenuItem absolutePathItem = new MenuItem("absolute path");
        copySelectedItemMenu.getItems().add(absolutePathItem);
        absolutePathItem.setOnAction(event -> copyToClipboard(table.getSelectionModel().getSelectedItem().getSourceFile().getFile().getAbsolutePath()));

        contextMenu.getItems().add(copySelectedItemMenu);
    }

    private void copyToClipboard(String string) {
        final Clipboard clipboard = Clipboard.getSystemClipboard();
        final ClipboardContent content = new ClipboardContent();
        content.putString(string);
        clipboard.setContent(content);
    }

    public TableView<SourceFileWithSearchData> getTable() {
        return table;
    }

    public void setTable(TableView<SourceFileWithSearchData> table) {
        this.table = table;
    }

    protected void addTableColumns() {
        PathCellRenderer pathCellRenderer = new PathCellRenderer();
        TableColumn<SourceFileWithSearchData, String> column1 = new TableColumn<>("Path");
        column1.setPrefWidth(400);
        column1.setCellFactory(pathCellRenderer);
        column1.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getSourceFile().getRelativePath()));

        TableColumn column2 = new TableColumn<>("# lines");
        column2.setMinWidth(100);
        column2.setMaxWidth(100);
        column2.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<SourceFileWithSearchData, Integer>, ObservableValue<Integer>>() {
            public ObservableValue<Integer> call(TableColumn.CellDataFeatures<SourceFileWithSearchData, Integer> p) {
                return new ReadOnlyObjectWrapper(p.getValue().getLineCount());
            }
        });
        column2.setCellFactory(new NumericBarCellFactory(param -> getMaxLines()));

        TableColumn column3 = new TableColumn<>("# found content");
        column3.setMinWidth(100);
        column3.setMaxWidth(100);
        column3.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<SourceFileWithSearchData, Integer>, ObservableValue<Integer>>() {
            public ObservableValue<Integer> call(TableColumn.CellDataFeatures<SourceFileWithSearchData, Integer> p) {
                return new ReadOnlyObjectWrapper(p.getValue().getFoundInstancesCount());
            }
        });
        column3.setCellFactory(new NumericBarCellFactory(param -> searchResult.getMaxNumberOfFoundInstances()));

        table.getColumns().addAll(column1, column2, column3);
    }

    public int getMaxLines() {
        int max = 1000;

        for (SourceFileWithSearchData sourceFileWithSearchData : table.getItems()) {
            max = Math.max(sourceFileWithSearchData.getLineCount(), max);
        }

        return max;
    }

    public void setItems(SearchResult searchResult, ObservableList<SourceFileWithSearchData> items) {
        this.searchResult = searchResult;
        table.setItems(items);
        table.getSortOrder().clear();
        TableColumn<SourceFileWithSearchData, ?> tableColumn2 = table.getColumns().get(1);
        tableColumn2.setSortType(TableColumn.SortType.DESCENDING);
        TableColumn<SourceFileWithSearchData, ?> tableColumn3 = table.getColumns().get(2);
        tableColumn3.setSortType(TableColumn.SortType.DESCENDING);
        table.getSortOrder().add(tableColumn3);
        table.getSortOrder().add(tableColumn2);

        table.getColumns().get(1).setVisible(false);
        table.getColumns().get(1).setVisible(true);
        table.getColumns().get(2).setVisible(false);
        if (searchResult.getFoundTextList().size() > 0) {
            table.getColumns().get(2).setVisible(true);
        }

        if (table.getItems().size() > 0) {
            table.getSelectionModel().selectFirst();
        }

        table.refresh();
    }

    public SearchResult getSearchResult() {
        return searchResult;
    }
}
