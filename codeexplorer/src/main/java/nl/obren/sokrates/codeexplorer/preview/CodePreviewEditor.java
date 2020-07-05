/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.codeexplorer.preview;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import nl.obren.sokrates.codeexplorer.search.SearchResultCellRenderer;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.SourceFileWithSearchData;
import nl.obren.sokrates.sourcecode.findings.Findings;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzerFactory;
import nl.obren.sokrates.sourcecode.search.FoundLine;
import org.apache.commons.lang3.StringUtils;

public class CodePreviewEditor extends SplitPane {
    private CodePreviewView codeArea;

    public CodePreviewEditor(Findings findings) {
        codeArea = new CodePreviewView(findings);
        setOrientation(Orientation.VERTICAL);
        getItems().clear();
        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(codeArea);
        getItems().add(borderPane);
    }

    private static void copyListContentToClipboard(ListView<FoundLine> list) {
        final ClipboardContent content = new ClipboardContent();
        StringBuilder clipboardString = new StringBuilder();
        clipboardString.append("Line number\tFound text\tLine content\n");
        list.getItems().forEach(listItem -> {
            clipboardString.append(listItem.getLineNumber() + "\t" + listItem.getFoundText() + "\t" + listItem.getLine() + "\n");
        });
        content.putString(StringUtils.removeEnd(clipboardString.toString(), "\n"));

        Clipboard.getSystemClipboard().setContent(content);
    }

    private void addCopyToClipboardContextMenu(ListView<FoundLine> list) {
        MenuItem item = new MenuItem("copy list content to clipboard");
        item.setOnAction(event -> copyListContentToClipboard(list));

        ContextMenu menu = new ContextMenu();
        menu.getItems().add(item);

        list.setContextMenu(menu);
    }

    public CodePreviewView getCodeArea() {
        return codeArea;
    }

    public void setText(SourceFileWithSearchData sourceFileWithSearchData) {
        if (sourceFileWithSearchData != null) {
            SourceFile sourceFile = sourceFileWithSearchData.getSourceFile();
            StringBuilder infoBuilder = new StringBuilder();

            infoBuilder.append(LanguageAnalyzerFactory.getInstance().getLanguageAnalyzer(sourceFile).getClass().getSimpleName()).append("; ");

            sourceFile.getLogicalComponents().forEach(aspect -> infoBuilder.append(aspect.getName() + "; "));
            infoBuilder.append(" / ");
            sourceFile.getConcerns().forEach(aspect -> infoBuilder.append(aspect.getName() + "; "));

            codeArea.setEditorValue(sourceFile, infoBuilder.toString());
            getItems().clear();
            BorderPane borderPane = new BorderPane();

            borderPane.setCenter(codeArea);
            getItems().add(borderPane);

            if (sourceFileWithSearchData.getLinesWithSearchedContent().size() > 0) {
                ListView<FoundLine> list = createList();
                setListItems(sourceFileWithSearchData, list);
                getItems().add(list);
                list.getSelectionModel().selectFirst();
            }
        }
    }

    protected void setListItems(SourceFileWithSearchData sourceFileWithSearchData, ListView<FoundLine> list) {
        ObservableList<FoundLine> items = FXCollections.observableArrayList(sourceFileWithSearchData.getLinesWithSearchedContent());
        list.setItems(items);
    }

    protected ListView<FoundLine> createList() {
        ListView<FoundLine> list = new ListView<>();
        addCopyToClipboardContextMenu(list);

        list.getSelectionModel().selectedItemProperty().addListener((ov, old_val, new_val) -> {
            codeArea.goToLine(new_val.getLineNumber());
        });
        list.setCellFactory(listCell -> new SearchResultCellRenderer());
        return list;
    }

    public void clear() {
        codeArea.clear();

        getItems().clear();

        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(codeArea);
        getItems().add(borderPane);
    }
}
