/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.codeexplorer.common;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.input.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.StringEscapeUtils;

public class UXUtils {
    public static final String DEFAULT_HTML_HEADER = "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <style type=\"text/css\" media=\"screen\">\n" +
            "        body { font-family: \"Ubuntu\",\"Helvetica Neue Light\",\"Helvetica Neue\",Helvetica,Roboto,Arial,sans-serif; }\n" +
            "    </style>\n" +
            "</head>\n";
    public static final Color ICON_COLOR = Color.rgb(0, 24, 50);
    private static final Log LOG = LogFactory.getLog(UXUtils.class);

    public static Button getButton(String svgContent) {
        Button button = new Button();
        button.setStyle("-fx-background-color: " + SvgIcons.ICON_COLOR_STRING
                + "; -fx-pref-width: 40px; -fx-pref-height: 32px");
        button.setGraphic(getIcon(svgContent));

        return button;
    }

    public static Button getButton(String svgContent, String toolTipText, EventHandler<ActionEvent> actionEventEventHandler) {
        Button button = getButton(svgContent);
        button.setOnAction(actionEventEventHandler);
        button.setTooltip(new Tooltip(toolTipText));
        return button;
    }

    public static SVGPath getIcon(String content) {
        SVGPath completeIcon = new SVGPath();
        completeIcon.setFill(SvgIcons.ICON_COLOR);
        completeIcon.setStroke(Color.WHITE);
        completeIcon.setContent(content);
        return completeIcon;
    }

    public static String toRGBCode(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }

    public static Button getButtonWithoutStyle(String svgContent) {
        Button button = new Button();
        button.setGraphic(getIcon(svgContent));

        return button;
    }

    public static Button getButton(String svgContent, EventHandler<ActionEvent> actionEventEventHandler, String tooltipText) {
        Button button = getButton(svgContent);
        button.setId(tooltipText.toLowerCase().replace(" ", "_").replace("-", "_"));
        button.setOnAction(actionEventEventHandler);
        button.setTooltip(new Tooltip(tooltipText));
        return button;
    }

    public static Button getButtonWithoutStyle(String svgContent, EventHandler<ActionEvent> actionEventEventHandler) {
        Button button = getButtonWithoutStyle(svgContent);
        button.setOnAction(actionEventEventHandler);
        return button;
    }

    public static void addCopyHandler(WebView webView) {
        webView.getEngine().setOnAlert((WebEvent<String> we) -> {
            if (we.getData() != null && we.getData().startsWith("copy: ")) {
                String string = we.getData().substring(6);
                StringSelection stringSelection = new StringSelection(string);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);
            }
        });
    }

    public static void addPasteHandler(WebView webView) {
        webView.addEventHandler(KeyEvent.KEY_PRESSED, keyEvent -> {
            boolean pasteKey = (keyEvent.isMetaDown() || keyEvent.isControlDown()) && keyEvent.getCode() == KeyCode.V;
            if (pasteKey) {
                final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                String content = null;
                try {
                    content = (String) clipboard.getData(DataFlavor.stringFlavor);
                    String script = "pasteContent(\"" + StringEscapeUtils.escapeEcmaScript(content) + "\") ";
                    webView.getEngine().executeScript(script);
                } catch (UnsupportedFlavorException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    public static Button getButton(String svgContent, String toolTipText) {
        Button button = getButton(svgContent);
        button.setTooltip(new Tooltip(toolTipText));
        return button;
    }
}
