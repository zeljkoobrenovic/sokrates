package nl.obren.sokrates.codeexplorer.duplication;

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
import nl.obren.sokrates.codeexplorer.preview.SimpleCodePreviewView;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.codeexplorer.common.ProgressFeedbackPane;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.duplication.DuplicatedFileBlock;
import nl.obren.sokrates.sourcecode.duplication.DuplicationEngine;
import nl.obren.sokrates.sourcecode.duplication.DuplicationInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class DuplicatesPane extends BorderPane {
    private static Stage stage;
    private static DuplicatesPane instance;
    private final TextArea duplicationFragmentPreview;
    private final SimpleCodePreviewView codePreview1 = new SimpleCodePreviewView();
    private final SimpleCodePreviewView codePreview2 = new SimpleCodePreviewView();
    private ComboBox filesForPreview1 = new ComboBox();
    private ComboBox filesForPreview2 = new ComboBox();
    private TableView<DuplicationInstance> table = new TableView<>();
    private BorderPane topPane = new BorderPane();
    private ProgressFeedbackPane progressFeedbackPane = new ProgressFeedbackPane();
    private DuplicationInstance selection;

    private DuplicatesPane() {
        TableViewUtils.addCopyToClipboardContextMenu(table);

        setId("duplicates_pane");
        addTableColumns();

        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.VERTICAL);
        configureTable();
        splitPane.getItems().add(table);
        duplicationFragmentPreview = new TextArea();

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        SplitPane filesSplit = new SplitPane();
        filesSplit.getItems().add(new BorderPane(codePreview1, filesForPreview1, null, null, null));
        filesSplit.getItems().add(new BorderPane(codePreview2, filesForPreview2, null, null, null));
        tabs.getTabs().add(new Tab("files", filesSplit));
        tabs.getTabs().add(new Tab("fragment", duplicationFragmentPreview));
        splitPane.getItems().add(tabs);

        setCenter(splitPane);
        setTop(topPane);
        topPane.setBottom(progressFeedbackPane);
    }

    public static void openInWindow(List<SourceFile> sourceFiles) {
        stage = new Stage();
        stage.setTitle("Duplicates");
        instance = new DuplicatesPane();
        instance.loadDuplicates(sourceFiles);

        Scene value = new Scene(instance, 800, 600);
        stage.setScene(value);
        stage.show();
    }

    public static DuplicatesPane getInstance() {
        return instance;
    }

    public static void close() {
        if (stage != null) {
            stage.close();
            stage = null;
        }
    }

    private void configureTable() {
        table.setEditable(false);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        filesForPreview1.setOnAction(event -> {
            int selectedIndex = filesForPreview1.getSelectionModel().getSelectedIndex();
            if (selection != null && selectedIndex >= 0) {
                DuplicatedFileBlock duplicatedFileBlock = selection.getDuplicatedFileBlocks().get(selectedIndex);
                codePreview1.setEditorValue(duplicatedFileBlock.getSourceFile());
                codePreview1.select(duplicatedFileBlock.getStartLine(), duplicatedFileBlock.getEndLine() - 1);
                codePreview1.goToLine(duplicatedFileBlock.getStartLine());
            }
        });
        filesForPreview2.setOnAction(event -> {
            int selectedIndex = filesForPreview2.getSelectionModel().getSelectedIndex();
            if (selection != null && selectedIndex >= 0) {
                DuplicatedFileBlock duplicatedFileBlock = selection.getDuplicatedFileBlocks().get(selectedIndex);
                codePreview2.setEditorValue(duplicatedFileBlock.getSourceFile());
                codePreview2.select(duplicatedFileBlock.getStartLine(), duplicatedFileBlock.getEndLine() - 1);
                codePreview2.goToLine(duplicatedFileBlock.getStartLine());
            }
        });
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            selection = newSelection;
            if (newSelection != null) {
                duplicationFragmentPreview.setText(newSelection.getDisplayContent());
                List<String> items = new ArrayList<>();
                newSelection.getDuplicatedFileBlocks().forEach(duplicatedFileBlock -> {
                    items.add(duplicatedFileBlock.getSourceFile().getRelativePath() + " ("
                            + duplicatedFileBlock.getStartLine() + ":" + duplicatedFileBlock.getEndLine() + ")");
                });
                filesForPreview1.setItems(FXCollections.observableArrayList(items));
                filesForPreview1.getSelectionModel().select(0);
                filesForPreview2.setItems(FXCollections.observableArrayList(items));
                filesForPreview2.getSelectionModel().select(1);
            }
        });
    }

    protected void addTableColumns() {
        TableColumn<DuplicationInstance, Number> column1 = new TableColumn<>("Block Size");
        column1.setMinWidth(50);
        column1.setPrefWidth(50);
        column1.setMaxWidth(50);
        column1.setCellValueFactory(p -> new SimpleIntegerProperty(p.getValue().getBlockSize()));

        TableColumn<DuplicationInstance, Number> column1b = new TableColumn<>("# Instances");
        column1b.setMinWidth(50);
        column1b.setPrefWidth(50);
        column1b.setMaxWidth(50);
        column1b.setCellValueFactory(p -> new SimpleIntegerProperty(p.getValue().getDuplicatedFileBlocks().size()));

        TableColumn<DuplicationInstance, String> column2 = new TableColumn<>("Sample");
        column2.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getSampleDisplayString()));

        TableColumn<DuplicationInstance, String> column3 = getFoldersColumn();

        TableColumn<DuplicationInstance, String> column4 = getFilesColumn();

        TableColumn<DuplicationInstance, String> column5 = getLinesColumn();

        table.getColumns().addAll(column1, column1b, column3, column4, column5);
    }

    private TableColumn<DuplicationInstance, String> getFoldersColumn() {
        final TableColumn<DuplicationInstance, String> column = new TableColumn<>("Folders");
        column.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getFoldersDisplayString()));
        return column;
    }

    private TableColumn<DuplicationInstance, String> getFilesColumn() {
        TableColumn<DuplicationInstance, String> column = new TableColumn<>("Files");
        column.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getFilesDisplayString(false)));
        return column;
    }

    private TableColumn<DuplicationInstance, String> getLinesColumn() {
        TableColumn<DuplicationInstance, String> column = new TableColumn<>("Lines");
        column.setMinWidth(150);
        column.setPrefWidth(150);
        column.setMaxWidth(150);
        column.setCellValueFactory(p -> {
            return new SimpleStringProperty(p.getValue().getLinesDisplayString());
        });
        return column;
    }

    private PieChart getPieChart(int duplicatedLinesOfCode, int totalCleanedLinesOfCode) {
        double percentage = 100.0 * duplicatedLinesOfCode / totalCleanedLinesOfCode;
        int notDuplicatedLinesOfCode = totalCleanedLinesOfCode - duplicatedLinesOfCode;
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                new PieChart.Data("duplicated (" + FormattingUtils.getFormattedPercentage(percentage) + "%, " + duplicatedLinesOfCode + " cleaned lines)", duplicatedLinesOfCode),
                new PieChart.Data("not duplicated (" + FormattingUtils.getFormattedRemainderPercentage(percentage) + "%, " + notDuplicatedLinesOfCode + " cleaned lines)", notDuplicatedLinesOfCode));
        final PieChart chart = new PieChart(pieChartData);

        chart.setLabelsVisible(true);
        chart.setLegendVisible(false);
        chart.setLegendSide(Side.RIGHT);
        chart.setMaxHeight(150);

        ChartUtils.applyCustomColorSequence(chart, pieChartData, "coral", "lightgrey", "yellow", "red");
        return chart;
    }

    private void loadDuplicates(List<SourceFile> sourceFiles) {
        if (sourceFiles != null) {
            Executors.newCachedThreadPool().execute(() -> {
                ProgressFeedback progressFeedback = progressFeedbackPane.getProgressFeedback();
                progressFeedback.start();
                DuplicationEngine duplicationEngine = new DuplicationEngine();
                //DuplicationEngine duplicationEngine = new DuplicationEngine();
                List<DuplicationInstance> duplicates = duplicationEngine.findDuplicates(sourceFiles, progressFeedback);
                int duplicatedLinesOfCode = duplicationEngine.getNumberOfDuplicatedLines();
                int totalCleanedLinesOfCode = duplicationEngine.getTotalCleanedLinesOfCode();
                Platform.runLater(() -> setTop(getPieChart(duplicatedLinesOfCode, totalCleanedLinesOfCode)));
                ObservableList<DuplicationInstance> items = FXCollections.observableArrayList();
                duplicates.forEach(items::add);
                Platform.runLater(() -> {
                    table.setItems(items);
                });
                progressFeedback.end();
            });
        }
    }

    public TableView<DuplicationInstance> getTable() {
        return table;
    }
}
