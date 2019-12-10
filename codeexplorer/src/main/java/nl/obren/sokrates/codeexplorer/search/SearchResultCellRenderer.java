/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.codeexplorer.search;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.text.TextFlow;
import nl.obren.sokrates.sourcecode.search.FoundLine;
import org.apache.commons.lang3.StringUtils;

public class SearchResultCellRenderer extends ListCell<FoundLine> {
    public SearchResultCellRenderer() {
    }

    @Override
    public void updateItem(FoundLine item, boolean empty) {
        super.updateItem(item, empty);
        if (item != null && !empty) {
            TextFlow textFlow = new TextFlow();
            String text = item.toString();
            if (StringUtils.isBlank(item.getFoundText()) || !text.contains(item.getFoundText())) {
                textFlow.getChildren().add(new Label(text));
            } else {
                createHighlightingTextFlow(textFlow, text, item.getFoundText());
            }
            setGraphic(textFlow);
        } else {
            setGraphic(null);
        }
    }

    protected void createHighlightingTextFlow(TextFlow textFlow, String text, String highlightedText) {
        int index = text.indexOf(highlightedText);
        textFlow.getChildren().add(new Label(text.substring(0, index)));
        Label contentLabel = new Label(highlightedText);
        contentLabel.setStyle("-fx-font-weight: bold");
        textFlow.getChildren().add(contentLabel);
        textFlow.getChildren().add(new Label(text.substring(index + highlightedText.length())));
    }
}
