/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.codeexplorer.common;

import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.text.TextFlow;
import javafx.util.Callback;

import java.io.File;

public class PathCellRenderer<T> implements Callback<TableColumn<T, String>, TableCell<T, String>> {
    @Override
    public TableCell<T, String> call(TableColumn<T, String> param) {
        return new TableCell<T, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setGraphic(null);
                } else {
                    setGraphic(getTextFlow(item));
                }
            }

        };
    }

    private TextFlow getTextFlow(String item) {
        File file = new File(item);

        String prefix = getPathPrefix(file);
        Label prefixLabel = new Label(prefix);
        prefixLabel.setStyle("-fx-text-fill: darkgrey");

        String name = file.getName();
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-weight: bold");

        TextFlow textFlow = new TextFlow();
        textFlow.getChildren().add(prefixLabel);
        textFlow.getChildren().add(nameLabel);
        textFlow.setMinWidth(4000);

        textFlow.setPrefHeight(20);

        return textFlow;
    }

    String getPathPrefix(File file) {
        return file.getParent() != null ? file.getParent() + File.separator : "";
    }
}
