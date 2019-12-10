/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.codeexplorer.search;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;
import nl.obren.sokrates.codeexplorer.codebrowser.TableViewUtils;
import nl.obren.sokrates.codeexplorer.common.NumericBarCellFactory;
import nl.obren.sokrates.sourcecode.search.FoundText;
import nl.obren.sokrates.sourcecode.search.SearchResult;

public class SearchResultsDistributionTablePane extends SplitPane {
    private final TextArea label;
    private TableView<FoundText> table = new TableView<>();
    private BorderPane centerPane;
    private int maxCountPerText;
    private SearchResult searchResult;
    private int totalCount;

    public SearchResultsDistributionTablePane() {
        TableViewUtils.addCopyToClipboardContextMenu(table);
        centerPane = new BorderPane();

        label = new TextArea();
        label.setEditable(false);
        label.setMaxHeight(24);
        centerPane.setBottom(label);
        centerPane.setCenter(table);

        addTableColumns();

        this.setOrientation(Orientation.VERTICAL);
        this.getItems().add(centerPane);
        this.setDividerPositions(0.7);
    }

    private void addTableColumns() {
        TableColumn<FoundText, String> instanceColumn = new TableColumn<>("Instance");
        instanceColumn.setPrefWidth(400);
        instanceColumn.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getText()));

        TableColumn countColumn = new TableColumn<>("Count");
        countColumn.setCellFactory(new NumericBarCellFactory(p -> maxCountPerText, p -> totalCount));
        countColumn.setMinWidth(120);
        countColumn.setMaxWidth(120);
        countColumn.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<FoundText, Integer>, ObservableValue<Integer>>() {
            public ObservableValue<Integer> call(TableColumn.CellDataFeatures<FoundText, Integer> p) {
                return new ReadOnlyObjectWrapper(p.getValue().getCount());
            }
        });

        table.getColumns().addAll(instanceColumn, countColumn);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    public void setItems(SearchResult searchResult) {
        this.searchResult = searchResult;
        if (searchResult != null) {
            maxCountPerText = searchResult.getMaxCountPerText();
            table.setItems(FXCollections.observableArrayList(searchResult.getFoundTextList()));
            if (table.getItems().size() > 0) {
                table.getSelectionModel().selectFirst();
            }
            table.getSortOrder().clear();
            TableColumn<FoundText, ?> tableColumn = table.getColumns().get(1);
            tableColumn.setSortType(TableColumn.SortType.DESCENDING);
            table.getSortOrder().add(tableColumn);
            table.refresh();

            table.getColumns().get(1).setVisible(false);
            table.getColumns().get(1).setVisible(true);

            setLabel(searchResult);
        }
    }

    private void setLabel(SearchResult searchResult) {
        int numberOfFiles = searchResult.getFoundFiles().size();
        totalCount = searchResult.getTotalCountPerText();
        label.setText(totalCount + " instances (" + searchResult.getFoundTextList().size() + " unique), in " + numberOfFiles + " files");
    }

    public SearchResult getSearchResult() {
        return searchResult;
    }

    public void setSearchResult(SearchResult searchResult) {
        this.searchResult = searchResult;
    }
}
