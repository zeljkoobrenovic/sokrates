/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.codeexplorer.charts;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import nl.obren.sokrates.common.renderingutils.x3d.Unit3D;
import nl.obren.sokrates.common.renderingutils.x3d.X3DomExporter;
import nl.obren.sokrates.codeexplorer.common.UXUtils;
import nl.obren.sokrates.common.utils.BasicColorInfo;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import nl.obren.sokrates.sourcecode.stats.SourceFileSizeDistribution;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class FileSizeDistributionChartPanel extends BorderPane {
    private List<SourceFile> sourceFiles;

    public FileSizeDistributionChartPanel() {
        createCharts();
    }

    private void createCharts() {
        setCenter(null);
        setBottom(null);

        if (sourceFiles != null && sourceFiles.size() > 0) {
            SourceFileSizeDistribution distribution = new SourceFileSizeDistribution().getOverallDistribution(sourceFiles);
            if (distribution.getTotalValue() > 0) {
                setCenter(getRiskPieChart("File size distribution", getPieChartDistributionData(distribution)));
                setBottom(getToolBar());
            }
        }
    }

    private static BasicColorInfo getBasicColorInfo(Color color) {
        return new BasicColorInfo(color.getRed(), color.getGreen(), color.getBlue());
    }


    public Node getToolBar() {
        ToolBar toolBar = new ToolBar();
        Button threeDButton = new Button("generate 3d view...");
        threeDButton.setOnAction(event -> open3D());
        toolBar.getItems().add(threeDButton);
        return toolBar;
    }

    private void open3D() {
        double divideByFactor = getDivideByFactor();
        if (divideByFactor != 0) {
            List<Unit3D> units = new ArrayList<>();
            sourceFiles.forEach(file -> {
                SourceFileSizeDistribution sourceFileSizeDistribution = new SourceFileSizeDistribution();
                BasicColorInfo color = getRiskProfileColor(sourceFileSizeDistribution, file.getLinesOfCode());
                units.add(new Unit3D(file.getFile().getPath(), file.getLinesOfCode(), color));
            });
            new X3DomExporter("A 3D View of All Files", "Each block is one file. The height of the block represents the file relative size in lines of code. The color of the file represents its unit size category (green=0-200, yellow=201-500, orange=501-1000, red=1001+).").export(units, false, divideByFactor);
        }
    }


    public BasicColorInfo getRiskProfileColor(SourceFileSizeDistribution distribution, int linesOfCode) {
        if (linesOfCode <= distribution.getMediumRiskThreshold()) {
            return getBasicColorInfo(Color.GREEN.darker());
        } else if (linesOfCode <= distribution.getHighRiskThreshold()) {
            return getBasicColorInfo(Color.YELLOW.darker());
        } else if (linesOfCode <= distribution.getVeryHighRiskThreshold()) {
            return getBasicColorInfo(Color.ORANGE.darker());
        } else {
            return getBasicColorInfo(Color.RED.darker());
        }
    }


    public double getDivideByFactor() {
        final TextInputDialog inputDlg = new TextInputDialog("50");
        inputDlg.initOwner(getScene().getWindow());
        inputDlg.setTitle("Divide by factor");
        inputDlg.setContentText("50");
        inputDlg.setHeaderText("Divide by factor");

        Optional<String> optional = inputDlg.showAndWait();

        if (optional.isPresent() && NumberUtils.isParsable(optional.get())) {
            return Double.parseDouble(optional.get());
        }

        return 0;
    }


    private ObservableList<PieChart.Data> getPieChartDistributionData(RiskDistributionStats distribution) {
        return FXCollections.observableArrayList(
                new PieChart.Data(distribution.getNegligibleRiskLabel() + " (" + FormattingUtils.getFormattedPercentage(100.0 * distribution.getNegligibleRiskValue() / distribution.getTotalValue()) + "%)",
                        distribution.getNegligibleRiskValue()),
                new PieChart.Data(distribution.getLowRiskLabel() + " (" + FormattingUtils.getFormattedPercentage(100.0 * distribution.getLowRiskValue() / distribution.getTotalValue()) + "%)",
                        distribution.getLowRiskValue()),
                new PieChart.Data(distribution.getMediumRiskLabel() + " (" + FormattingUtils.getFormattedPercentage(100.0 * distribution.getMediumRiskValue() / distribution.getTotalValue()) + "%)",
                        distribution.getMediumRiskValue()),
                new PieChart.Data(distribution.getHighRiskLabel() + " (" + FormattingUtils.getFormattedPercentage(100.0 * distribution.getHighRiskValue() / distribution.getTotalValue()) + "%)",
                        distribution.getHighRiskValue()),
                new PieChart.Data(distribution.getVeryHighRiskLabel() + " (" + FormattingUtils.getFormattedPercentage(100.0 * distribution.getVeryHighRiskValue() / distribution.getTotalValue()) +
                        "%)", distribution.getVeryHighRiskValue())
        );
    }

    private PieChart getRiskPieChart(String title, ObservableList<PieChart.Data> pieChartData) {
        final PieChart chart = new PieChart(pieChartData);
        chart.setStartAngle(90);
        chart.setClockwise(true);
        chart.setLabelsVisible(true);
        chart.setTitle(title);

        ChartUtils.applyCustomColorSequence(chart, pieChartData,
                UXUtils.toRGBCode(Color.GREEN.darker()),
                UXUtils.toRGBCode(Color.YELLOW.darker()),
                UXUtils.toRGBCode(Color.ORANGE.darker()),
                UXUtils.toRGBCode(Color.RED.darker()));
        return chart;
    }

    public void setData(List<SourceFile> sourceFiles) {
        this.sourceFiles = new ArrayList<>(sourceFiles);
        Collections.sort(sourceFiles, (o1, o2) -> {
            if (o1.getLinesOfCode() > o2.getLinesOfCode()) {
                return -1;
            } else if (o2.getLinesOfCode() > o1.getLinesOfCode()) {
                return 1;
            } else {
                return 0;
            }
        });
        createCharts();
    }
}
