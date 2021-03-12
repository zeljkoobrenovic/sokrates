/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.codeexplorer.units;

import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import nl.obren.sokrates.codeexplorer.charts.ChartUtils;
import nl.obren.sokrates.codeexplorer.codebrowser.TableViewUtils;
import nl.obren.sokrates.codeexplorer.common.NumericBarCellFactory;
import nl.obren.sokrates.codeexplorer.common.ProgressFeedbackPane;
import nl.obren.sokrates.common.renderingutils.Threshold;
import nl.obren.sokrates.common.renderingutils.Thresholds;
import nl.obren.sokrates.common.renderingutils.x3d.Unit3D;
import nl.obren.sokrates.common.renderingutils.x3d.X3DomExporter;
import nl.obren.sokrates.common.utils.BasicColorInfo;
import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import nl.obren.sokrates.sourcecode.units.UnitUtils;
import nl.obren.sokrates.sourcecode.units.UnitsExtractor;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;

public class UnitsPane extends BorderPane {
    private static Stage stage;
    private static UnitsPane instance;
    private final TextArea unitFragmentPreview;
    private TableView<UnitInfo> table = new TableView<>();
    private BorderPane topPane = new BorderPane();
    private ProgressFeedbackPane progressFeedbackPane = new ProgressFeedbackPane();
    private Number maxLinesOfCode = 0;
    private Number maxMcCabe = 0;
    private Number maxNumberOfParameters = 0;
    private List<UnitInfo> units;

    private UnitsPane() {
        TableViewUtils.addCopyToClipboardContextMenu(table);

        setId("units_pane");
        addTableColumns();

        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.VERTICAL);
        configureTable();
        splitPane.getItems().add(table);
        unitFragmentPreview = new TextArea();
        splitPane.getItems().add(unitFragmentPreview);

        setCenter(splitPane);
        setTop(topPane);
        topPane.setBottom(progressFeedbackPane);
    }

    public static void close() {
        if (stage != null) {
            stage.close();
            stage = null;
        }
    }

    public static void openInWindow(List<SourceFile> sourceFiles) {
        close();

        stage = new Stage();
        stage.setTitle("Units");
        instance = new UnitsPane();

        Button threeDButton = new Button("generate 3d view...");
        threeDButton.setOnAction(event -> instance.open3D());

        instance.setBottom(new BorderPane(null, threeDButton, null, null, null));

        instance.loadUnits(sourceFiles);
        Scene value = new Scene(instance, 800, 600);
        stage.setScene(value);
        stage.show();
    }

    public static UnitsPane getInstance() {
        return instance;
    }

    private void open3D() {
        double divideByFactor = getDivideByFactor();
        if (divideByFactor != 0) {
            List<Unit3D> unit3Ds = new ArrayList<>();
            units.forEach(unit -> {
                BasicColorInfo color = Thresholds.getColor(Thresholds.UNIT_MCCABE, unit.getMcCabeIndex());
                unit3Ds.add(new Unit3D(unit.getLongName(), unit.getLinesOfCode(), color));
            });
            new X3DomExporter("A 3D View of All Units", "Each block is one unit. The height of the block represents the file unit size in lines of code. The color of the unit represents its conditional complexity category (green=0-5, yellow=6-10, orange=11-25, red=26+).").export(unit3Ds, false, divideByFactor);


        }
    }

    public double getDivideByFactor() {
        final TextInputDialog inputDlg = new TextInputDialog("10");
        inputDlg.initOwner(getScene().getWindow());
        inputDlg.setTitle("Divide by factor");
        inputDlg.setContentText("Divide values by");
        inputDlg.setHeaderText("Divide by factor");

        Optional<String> optional = inputDlg.showAndWait();

        if (optional.isPresent() && NumberUtils.isParsable(optional.get())) {
            return Double.parseDouble(optional.get());
        }

        return 0;
    }

    public TableView<UnitInfo> getTable() {
        return table;
    }

    private void configureTable() {
        table.setEditable(false);
        table.getColumns().get(0).setPrefWidth(400);
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                unitFragmentPreview.setText(newSelection.getBody());
            } else {
                unitFragmentPreview.setText("");
            }
        });
    }


    private PieChart getUnitSizePieChart(RiskDistributionStats riskDistributionStats, List<Threshold> thresholds,
                                         String title) {
        ObservableList<PieChart.Data> pieChartData = getCategoryData(new int[]{
                riskDistributionStats.getNegligibleRiskValue(), riskDistributionStats.getLowRiskValue(), riskDistributionStats.getMediumRiskValue(),
                riskDistributionStats.getHighRiskValue(), riskDistributionStats.getVeryHighRiskValue()
        }, thresholds);

        final PieChart chart = new PieChart(pieChartData);

        chart.setTitle(title);
        chart.setLabelsVisible(false);
        chart.setLegendVisible(true);
        chart.setLegendSide(Side.RIGHT);
        chart.setMaxHeight(150);
        chart.setStartAngle(90);

        ChartUtils.applyCustomColorSequence(chart, pieChartData, Thresholds.getColors(thresholds));
        return chart;
    }

    private ObservableList<PieChart.Data> getCategoryData(int categories[], List<Threshold> thresholds) {
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        for (int i = 0; i < thresholds.size(); i++) {
            pieChartData.add(new PieChart.Data(thresholds.get(i).getTitle(), categories[i]));
        }
        return pieChartData;
    }

    protected void addTableColumns() {
        TableColumn<UnitInfo, String> column1 = new TableColumn<>("Unit");
        column1.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getShortName()));

        TableColumn<UnitInfo, String> column2 = new TableColumn<>("File");
        column2.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getSourceFile().getFile().getName()));

        TableColumn<UnitInfo, Number> column3 = new TableColumn<>("# lines");
        column3.setMinWidth(100);
        column3.setMaxWidth(100);
        column3.setCellValueFactory(p -> new SimpleIntegerProperty(p.getValue().getLinesOfCode()));
        column3.setCellFactory(new NumericBarCellFactory(param -> getMaxLines(), Thresholds.UNIT_LINES));

        TableColumn<UnitInfo, Number> column4 = new TableColumn<>("McCabe index");
        column4.setMinWidth(100);
        column4.setMaxWidth(100);
        column4.setCellValueFactory(p -> new SimpleIntegerProperty(p.getValue().getMcCabeIndex()));
        column4.setCellFactory(new NumericBarCellFactory(param -> getMaxMcCabe(), Thresholds.UNIT_MCCABE));

        TableColumn<UnitInfo, Number> column5 = new TableColumn<>("# params");
        column5.setMinWidth(100);
        column5.setMaxWidth(100);
        column5.setCellValueFactory(p -> new SimpleIntegerProperty(p.getValue().getNumberOfParameters()));
        column5.setCellFactory(new NumericBarCellFactory(param -> getMaxNumberOfParameters(), Thresholds.UNIT_PARAMS));

        table.getColumns().addAll(column1, column2, column3, column4, column5);
    }

    private Number getMaxLines() {
        return maxLinesOfCode;
    }

    private Number getMaxMcCabe() {
        return maxMcCabe;
    }

    private Number getMaxNumberOfParameters() {
        return maxNumberOfParameters;
    }

    private void loadUnits(List<SourceFile> sourceFiles) {
        if (sourceFiles != null) {
            Executors.newCachedThreadPool().execute(() -> {
                ProgressFeedback progressFeedback = progressFeedbackPane.getProgressFeedback();
                progressFeedback.start();

                units = new UnitsExtractor().getUnits(sourceFiles, progressFeedback);
                Platform.runLater(() -> updateCharts(units));
                calculateMaxValues(units);

                ObservableList<UnitInfo> items = FXCollections.observableArrayList(units);
                Platform.runLater(() -> {
                    table.setItems(items);
                    TableColumn locColumn = table.getColumns().get(2);
                    table.getSortOrder().add(locColumn);
                    locColumn.setSortType(TableColumn.SortType.DESCENDING);
                    locColumn.setSortable(true);
                });
                progressFeedback.end();
            });
        }
    }

    private void updateCharts(List<UnitInfo> units) {
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.getItems().add(getUnitSizePieChart(UnitUtils.getUnitSizeDistribution(units), Thresholds.UNIT_LINES,
                "Lines of Code"));
        splitPane.getItems().add(getUnitSizePieChart(UnitUtils.getConditionalComplexityDistribution(units), Thresholds
                .UNIT_MCCABE, "Conditional Complexity"));
        topPane.setCenter(splitPane);
    }

    private void calculateMaxValues(List<UnitInfo> units) {
        maxLinesOfCode = 0;
        maxMcCabe = 0;
        maxNumberOfParameters = 0;
        units.forEach(unit -> {
            maxLinesOfCode = Math.max(maxLinesOfCode.intValue(), unit.getLinesOfCode());
            maxMcCabe = Math.max(maxMcCabe.intValue(), unit.getMcCabeIndex());
            maxNumberOfParameters = Math.max(maxNumberOfParameters.intValue(), unit.getNumberOfParameters());
        });
    }
}
