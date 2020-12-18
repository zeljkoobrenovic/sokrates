/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.codeexplorer.search;

import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import nl.obren.sokrates.codeexplorer.codebrowser.CodeBrowserPane;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.sourcecode.SourceFileWithSearchData;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.search.SearchResult;
import org.apache.commons.lang3.StringUtils;

public class SearchSummaryPane extends BorderPane {

    public SearchSummaryPane(CodeBrowserPane codeBrowserPane, NamedSourceCodeAspect aspect, ObservableList<SourceFileWithSearchData> cachedFiles) {
        int foundFilesCount = cachedFiles.size();
        int foundLoc = getLoc(cachedFiles);
        double percentage = 100.0 * foundLoc / aspect.getLinesOfCode();
        TextField label = new TextField(" " + FormattingUtils.formatCount(foundFilesCount) + " files and "
                + FormattingUtils.formatCount(foundLoc) + " LOC ("
                + FormattingUtils.getFormattedPercentage(percentage) + "%)");
        label.setEditable(false);

        setCenter(label);
        setLeft(getPercentageCanvas(percentage));

        SearchResult searchResult = codeBrowserPane.getAspectFilesBrowserPane().getSearchPane().getSearchResult();
        if (StringUtils.isNotBlank(searchResult.getSearchRequest().getContentSearchExpression().getExpression())) {
            Button stats = new Button("stats");
            stats.setOnAction(event -> new SearchStatsPane(codeBrowserPane).show());
            Button viz = new Button("viz");
            viz.setOnAction(event -> new SearchVizPane(codeBrowserPane).show());

            setRight(new BorderPane(null, null, viz, null, stats));
        }
    }

    private Pane getPercentageCanvas(double percentage) {
        int rectWidth = (int) percentage;
        if (rectWidth == 0 && percentage > 0) {
            rectWidth = 1;
        }

        FlowPane canvas = new FlowPane();
        canvas.setPrefSize(100, 20);
        Rectangle rectangle1 = new Rectangle(rectWidth, 30, Color.SKYBLUE);
        Rectangle rectangle2 = new Rectangle(100 - rectWidth, 30, Color.GREY);
        canvas.getChildren().addAll(rectangle1, rectangle2);

        return canvas;
    }

    private int getLoc(ObservableList<SourceFileWithSearchData> cachedFiles) {
        int loc = 0;
        for (SourceFileWithSearchData sourceFileWithSearchData : cachedFiles) {
            loc += sourceFileWithSearchData.getLineCount();
        }

        return loc;
    }


}
