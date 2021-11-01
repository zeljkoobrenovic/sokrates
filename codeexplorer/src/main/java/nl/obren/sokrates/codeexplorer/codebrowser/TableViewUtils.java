/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.codeexplorer.codebrowser;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import org.apache.commons.lang3.StringUtils;

public class TableViewUtils {
    public static ContextMenu addCopyToClipboardContextMenu(TableView tableView) {
        MenuItem item = new MenuItem("copy table content to clipboard");
        item.setOnAction(event -> copyCellsToClipboard(tableView));

        ContextMenu menu = new ContextMenu();
        menu.getItems().add(item);

        tableView.setContextMenu(menu);

        return menu;
    }

    private static void copyCellsToClipboard(TableView tableView) {
        String[][] data = new String[tableView.getItems().size() + 1][tableView.getColumns().size()];
        final int[] columnIndex = {0};
        tableView.getColumns().forEach(column -> {
            data[0][columnIndex[0]] = StringUtils.defaultIfBlank(((TableColumn) column).getText(), "Column" + (columnIndex[0] + 1));
            int rowIndex = 1;
            for (Object item : tableView.getItems()) {
                data[rowIndex][columnIndex[0]] = (((TableColumn) column).getCellObservableValue(item).getValue()).toString();
                rowIndex++;
            }
            columnIndex[0]++;
        });

        copyDataToClipboard(data);
    }

    private static void copyDataToClipboard(String[][] data) {
        final ClipboardContent content = new ClipboardContent();
        content.putString(getStringFromData(data));

        Clipboard.getSystemClipboard().setContent(content);
    }

    private static String getStringFromData(String[][] data) {
        final StringBuilder clipboardString = new StringBuilder();
        for (String[] row : data) {
            final StringBuilder rowString = new StringBuilder();
            for (String cell : row) {
                appendCell(rowString, cell);
            }
            clipboardString.append(StringUtils.removeEnd(rowString.toString(), "\t"));
            clipboardString.append("\n");
        }
        return StringUtils.removeEnd(clipboardString.toString(), "\n");
    }

    private static void appendCell(StringBuilder clipboardString, String cell) {
        if (cell == null) {
            cell = "";
        }
        clipboardString.append(cell);
        clipboardString.append('\t');
    }
}
