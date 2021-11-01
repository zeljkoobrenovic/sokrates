/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.codeexplorer.console;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.codeexplorer.common.ProgressFeedbackPane;
import nl.obren.sokrates.common.utils.SystemUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import java.text.DecimalFormat;

public class WebViewConsole extends BorderPane implements Console {
    public final static String HTML_HEADER = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <style type=\"text/css\" media=\"all\">\n" +
            "        body {\n" +
            "            font-family: \"Helvetica, Arial, Tahoma, Menlo, 'Lucida Console', Monaco,'Courier New', monospace\";\n" +
            "            margin: 3px;\n" +
            "            font-size: 12px;\n" +
            "        }\n" +
            "    </style>" +
            "</head>\n";
    private static final String EVENT_TYPE_CLICK = "click";
    private WebView webView = new WebView();
    private ProgressFeedbackPane progressFeedbackPane = new ProgressFeedbackPane();
    private ProgressFeedback progressFeedback;
    private long startTime = System.currentTimeMillis();
    private StringBuilder lines = new StringBuilder();

    public WebViewConsole() {
        progressFeedback = createProgressFeedback();

        setTop(progressFeedbackPane);
        setCenter(webView);
        webView.getEngine().getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                EventListener listener = ev -> {
                    String domEventType = ev.getType();
                    if (domEventType.equals(EVENT_TYPE_CLICK)) {
                        String href = ((Element) ev.getTarget()).getAttribute("href");
                        SystemUtils.openFile(href);
                    }
                };

                Document doc = webView.getEngine().getDocument();
                NodeList nodeList = doc.getElementsByTagName("a");
                for (int i = 0; i < nodeList.getLength(); i++) {
                    ((EventTarget) nodeList.item(i)).addEventListener("click", listener, false);
                }
            }
        });
    }

    private String getPrefix() {
        long timeInSeconds = (System.currentTimeMillis() - startTime) / 1000;
        long minutes = timeInSeconds / 60;
        long seconds = timeInSeconds % 60;
        DecimalFormat decimalFormat = new DecimalFormat("00");
        String prefix = decimalFormat.format(minutes) + ":" + decimalFormat.format(seconds) + "&nbsp;&nbsp;&nbsp;&nbsp;";
        return "<span style='color:grey'>" + prefix + "</span>";
    }

    private ProgressFeedback createProgressFeedback() {
        return new ProgressFeedback() {
            public void clear() {
                startTime = System.currentTimeMillis();
                lines = new StringBuilder();
                Platform.runLater(() -> webView.getEngine().loadContent("<html><body>" + lines.toString() + "</body></html>"));
            }

            public void start() {
                progressFeedbackPane.getProgressFeedback().start();
            }

            public void end() {
                progressFeedbackPane.getProgressFeedback().end();
            }

            public synchronized void setText(String text) {
                String prefix;
                if (StringUtils.isBlank(text)) {
                    prefix = "";
                } else {
                    prefix = getPrefix();
                }
                Platform.runLater(() -> {
                    log(prefix + text);
                });
            }

            public void setDetailedText(String text) {
                progressFeedbackPane.getProgressFeedback().setText(text);
            }

            public boolean canceled() {
                if (progressFeedbackPane.getProgressFeedback().canceled()) {
                    setText("Cancelling processing...");
                }
                return progressFeedbackPane.getProgressFeedback().canceled();
            }

            public void progress(int currentValue, int endValue) {
                progressFeedbackPane.getProgressFeedback().progress(currentValue, endValue);
            }
        };
    }

    @Override
    public void log(String text) {
        lines.append(text).append("<br/>");
        webView.getEngine().loadContent(HTML_HEADER + "<body style='font-family: Tahoma'onload=' window.scrollTo(0,document.body.scrollHeight);'>" + lines.toString() + "</a></body></html>");
    }

    public ProgressFeedback getProgressFeedback() {
        return progressFeedback;
    }

}
