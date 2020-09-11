/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.codeexplorer.codebrowser;

import com.fasterxml.jackson.core.JsonProcessingException;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Side;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import nl.obren.sokrates.codeexplorer.dependencies.DependenciesPane;
import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.renderingutils.VisualizationItem;
import nl.obren.sokrates.common.renderingutils.VisualizationTemplate;
import nl.obren.sokrates.codeexplorer.common.NumericBarCellFactory;
import nl.obren.sokrates.common.utils.SystemUtils;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.aspects.Concern;
import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.aspects.SourceCodeAspectUtils;
import nl.obren.sokrates.sourcecode.catalogs.StandardSecurityAspects;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AspectsTablePane extends BorderPane {
    private TableView<NamedSourceCodeAspect> table = new TableView<>();
    private CodeBrowserPane codeBrowserPane;
    private int maxFileLinesOfCode;
    private int maxFileCount;
    private String title;
    private boolean sortEnabled = true;
    private ComboBox selector;
    private NamedSourceCodeAspect main;

    public AspectsTablePane(String title, boolean sortEnabled) {
        this.title = title;
        this.sortEnabled = sortEnabled;
        configureTable();
        table.setOnSort(null);
        TableViewUtils.addCopyToClipboardContextMenu(table);
        setCenter(table);
    }

    public void setAspectSelections(List<Pair<String, List<NamedSourceCodeAspect>>> aspectSelections) {
        if (aspectSelections.size() > 0) {
            selector = new ComboBox<>();
            List<String> keys = new ArrayList<>();
            aspectSelections.forEach(aspectSelection -> keys.add(aspectSelection.getLeft()));
            selector.setItems(FXCollections.observableArrayList(keys));
            selector.getSelectionModel().select(0);
            selector.prefWidthProperty().bind(this.widthProperty());
            selector.setOnAction(event -> refresh(aspectSelections.get(selector.getSelectionModel().getSelectedIndex()).getRight(), main));
            BorderPane topPane = new BorderPane();
            topPane.setCenter(selector);
            Button dependenciesButton = new Button("visualize...");
            dependenciesButton.setId("visualize");
            dependenciesButton.setOnAction(event -> visualizeButton(dependenciesButton));
            topPane.setRight(dependenciesButton);
            setTop(topPane);
        } else {
            setTop(null);
        }
    }

    public void setConcernsSelections(List<Pair<String, List<Concern>>> concernSelections) {
        if (concernSelections.size() > 0) {
            selector = new ComboBox<>();
            List<String> keys = new ArrayList<>();
            concernSelections.forEach(aspectSelection -> keys.add(aspectSelection.getLeft()));
            selector.setItems(FXCollections.observableArrayList(keys));
            selector.getSelectionModel().select(0);
            selector.prefWidthProperty().bind(this.widthProperty());
            selector.setOnAction(event -> refresh(concernSelections.get(selector.getSelectionModel().getSelectedIndex()).getRight(), main));
            BorderPane topPane = new BorderPane();
            topPane.setCenter(selector);
            setTop(topPane);
        } else {
            setTop(null);
        }
    }

    private void visualizeButton(Button dependenciesButton) {
        String group = (String) selector.getSelectionModel().getSelectedItem();

        ContextMenu menu = new ContextMenu();
        MenuItem showBubbleChartMenuItem = new MenuItem("bubble chart...");
        showBubbleChartMenuItem.setOnAction(e -> renderBubbleChart());
        MenuItem showTreeMapMenuItem = new MenuItem("tree map...");
        showTreeMapMenuItem.setOnAction(e -> renderTreeMap());
        LogicalDecomposition logicalDecomposition = codeBrowserPane.getCodeConfiguration().getLogicalDecompositions().get(selector.getSelectionModel().getSelectedIndex());
        MenuItem showMeasuredDependenciesMenuItem = new MenuItem("measured dependencies...");
        showMeasuredDependenciesMenuItem.setId("measured_dependencies");
        showMeasuredDependenciesMenuItem.setOnAction(e -> {
            List<String> componentNames = new ArrayList<>();
            logicalDecomposition.getComponents().forEach(c -> componentNames.add(c.getName()));
            DependenciesPane.openInWindow(codeBrowserPane.getCodeConfiguration().getMain(), logicalDecomposition, componentNames);
        });
        menu.getItems().addAll(showMeasuredDependenciesMenuItem, new SeparatorMenuItem(), showBubbleChartMenuItem, showTreeMapMenuItem);

        menu.show(dependenciesButton, Side.BOTTOM, 0, 0);


    }

    private void addConcernButton(Button dependenciesButton) {
        String group = (String) selector.getSelectionModel().getSelectedItem();

        ContextMenu menu = new ContextMenu();
        MenuItem securityItem = new MenuItem("Security");
        securityItem.setOnAction(e -> {
            StandardSecurityAspects aspects = new StandardSecurityAspects();
            CodeConfiguration codeConfiguration = codeBrowserPane.getCodeConfigurationView().getConfigurationFromEditor();
            codeConfiguration.getConcernGroups().add(aspects);
            try {
                String configurationContent = new JsonGenerator().generate(codeConfiguration);
                codeBrowserPane.getCodeConfigurationView().setEditorValue(configurationContent);
                codeBrowserPane.getCodeConfigurationView().completeAndSave();
            } catch (JsonProcessingException e3) {
                e3.printStackTrace();
            }
        });

        menu.getItems().addAll(securityItem);

        menu.show(dependenciesButton, Side.BOTTOM, 0, 0);


    }

    private void renderBubbleChart() {
        LogicalDecomposition logicalDecomposition = codeBrowserPane.getCodeConfiguration().getLogicalDecompositions().get(selector.getSelectionModel().getSelectedIndex());
        List<VisualizationItem> items = new ArrayList<>();
        logicalDecomposition.getComponents().forEach(component -> {
            items.add(new VisualizationItem(component.getName(), component.getLinesOfCode()));
        });
        SystemUtils.createAndOpenTempFile(new VisualizationTemplate().renderBubbleChart(items));
    }

    private void renderTreeMap() {
        LogicalDecomposition logicalDecomposition = codeBrowserPane.getCodeConfiguration().getLogicalDecompositions().get(selector.getSelectionModel().getSelectedIndex());
        List<VisualizationItem> items = new ArrayList<>();
        logicalDecomposition.getComponents().forEach(component -> {
            items.add(new VisualizationItem(component.getName(), component.getLinesOfCode()));
        });
        SystemUtils.createAndOpenTempFile(new VisualizationTemplate().renderTreeMap(items));
    }

    public TableView<NamedSourceCodeAspect> getTable() {
        return table;
    }

    public void refresh(List<? extends NamedSourceCodeAspect> aspects, NamedSourceCodeAspect main) {
        calculateMax(aspects);
        setItems(aspects, main);
    }

    private void setItems(List<? extends NamedSourceCodeAspect> aspects, NamedSourceCodeAspect main) {
        if (main != null && aspects != null) {
            this.main = main;
            table.layout();
            ArrayList<? extends NamedSourceCodeAspect> filtered = aspects.stream()
                    .filter(i -> !i.getName().contains("Multiple Classification")
                            && !i.getName().contains("Unclassified"))
                    .collect(Collectors.toCollection(ArrayList::new));
            table.setItems(FXCollections.observableArrayList(filtered));

            refreshBarChartColumns();

            table.refresh();
        }
    }

    private void refreshBarChartColumns() {
        table.getColumns().get(1).setVisible(false);
        table.getColumns().get(1).setVisible(true);
        table.getColumns().get(2).setVisible(false);
        table.getColumns().get(2).setVisible(true);
    }

    private void configureTable() {
        TableColumn<NamedSourceCodeAspect, String> nameColumn = getNameTableColumn();
        TableColumn locColumn = getLocTableColumn();
        TableColumn filesColumn = getFileCountTableColumn();

        table.getColumns().addAll(nameColumn, locColumn, filesColumn);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                codeBrowserPane.load(newSelection);
            }
        });

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private TableColumn<NamedSourceCodeAspect, String> getNameTableColumn() {
        TableColumn<NamedSourceCodeAspect, String> nameColumn = new TableColumn<>(title);
        nameColumn.setSortable(sortEnabled);
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setPrefWidth(400);
        return nameColumn;
    }

    private TableColumn getFileCountTableColumn() {
        TableColumn<NamedSourceCodeAspect, Number> column = new TableColumn<>("# files");
        column.setMinWidth(150);
        column.setPrefWidth(150);
        column.setMaxWidth(200);
        column.setSortable(sortEnabled);
        column.setCellValueFactory(p -> new SimpleIntegerProperty(p.getValue().getSourceFiles().size()));
        column.setCellFactory(new NumericBarCellFactory(p -> getMaxFilesCount(), p -> main.getSourceFiles().size()));
        return column;
    }

    private TableColumn getLocTableColumn() {
        TableColumn<NamedSourceCodeAspect, Number> column = new TableColumn<>("# lines");
        column.setMinWidth(150);
        column.setPrefWidth(150);
        column.setMaxWidth(200);
        column.setSortable(sortEnabled);
        column.setCellValueFactory(p -> new SimpleIntegerProperty(p.getValue().getLinesOfCode()));
        column.setCellFactory(new NumericBarCellFactory<>(p -> getMaxLoc(), p -> main.getLinesOfCode()));
        return column;
    }

    private Integer getMaxFilesCount() {
        return maxFileCount;
    }

    private Integer getMaxLoc() {
        return maxFileLinesOfCode;
    }

    private void calculateMax(List<? extends NamedSourceCodeAspect> aspects) {
        maxFileLinesOfCode = SourceCodeAspectUtils.getMaxLinesOfCode(aspects);
        maxFileCount = SourceCodeAspectUtils.getMaxFileCount(aspects);

    }

    public void setCodeBrowserPane(CodeBrowserPane codeBrowserPane) {
        this.codeBrowserPane = codeBrowserPane;
    }
}
