/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.codeexplorer.common;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;
import nl.obren.sokrates.common.renderingutils.Threshold;
import nl.obren.sokrates.common.utils.BasicColorInfo;

import java.text.DecimalFormat;
import java.util.List;

public class NumericBarCellFactory<T> implements Callback<TableColumn<T, Number>, TableCell<T, Number>> {
    private Callback<Void, Number> maxCountCallback;
    private Callback<Void, Number> compareToValueCallback;
    private List<Threshold> thresholds;

    public NumericBarCellFactory(Callback<Void, Number> maxCountCallback) {
        this.maxCountCallback = maxCountCallback;
    }

    public NumericBarCellFactory(Callback<Void, Number> maxCountCallback, Callback<Void, Number> compareToValueCallback) {
        this.maxCountCallback = maxCountCallback;
        this.compareToValueCallback = compareToValueCallback;
    }

    public NumericBarCellFactory(Callback<Void, Number> maxCountCallback, List<Threshold> thresholds) {
        this.maxCountCallback = maxCountCallback;
        this.thresholds = thresholds;
    }

    @Override
    public TableCell call(TableColumn column) {
        return new TableCell<T, Number>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    updateItemWithNumericBar(this, item.doubleValue());
                }
            }

        };
    }

    private void updateItemWithNumericBar(TableCell tableCell, double itemValue) {
        double max = maxCountCallback != null && maxCountCallback.call(null) != null ? maxCountCallback.call(null).doubleValue() : 1;

        if (max > 0) {
            Rectangle r = new Rectangle();
            Color color = getColor(itemValue);
            r.setFill(color);
            double value = 1 + 20 * (itemValue / max);
            r.setWidth(value);
            r.setHeight(18);
            tableCell.setGraphic(r);
        }

        String pattern = "###,###";
        DecimalFormat decimalFormat = new DecimalFormat(pattern);

        String text = decimalFormat.format(itemValue);
        if (compareToValueCallback != null && compareToValueCallback.call(null).doubleValue() > 0) {
            double percentage = 100.0 * itemValue / compareToValueCallback.call(null).doubleValue();
            text += " (" + (percentage < 1 && percentage > 0 ? "<1" : percentage == 0 ? "0" : (int) (percentage + 0.5)) + "%)";
        }
        tableCell.setText(text);
    }

    private Color getColor(double item) {
        if (thresholds != null) {
            for (Threshold treshold : thresholds) {
                if (item <= treshold.getThreshold().doubleValue()) {
                    return convertToColor(treshold.getColor());
                }
            }
        }
        return Color.LIGHTSKYBLUE;
    }

    private Color convertToColor(BasicColorInfo basicColorInfo) {
        return new Color(basicColorInfo.getRed(), basicColorInfo.getGreen(), basicColorInfo.getRed(), 1);
    }
}
