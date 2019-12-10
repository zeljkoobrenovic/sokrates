/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.codeexplorer.charts;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.Set;

public class ChartUtils {
    public static void applyCustomColorSequence(PieChart chart, ObservableList<PieChart.Data> pieChartData, String... pieColors) {
        int i = 0;
        for (PieChart.Data data : pieChartData) {
            data.getNode().setStyle("-fx-pie-color: " + pieColors[i % pieColors.length] + ";");
            i++;
        }

        Set<Node> items = chart.lookupAll("Label.chart-legend-item");

        i = 0;
        for (Node item : items) {
            Label label = (Label) item;
            Circle rectangle = new Circle(5, 5, 5, Color.web(pieColors[i % pieColors.length]));
            label.setGraphic(rectangle);
            i++;
        }
    }

}
