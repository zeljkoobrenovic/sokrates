package nl.obren.sokrates.codeexplorer.common;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import nl.obren.sokrates.common.utils.ProgressFeedback;
import org.apache.commons.lang3.StringUtils;

public class ProgressFeedbackPane extends BorderPane {
    private final Text label = new Text();
    private BorderPane pane = new BorderPane();
    private ProgressBar progressBar = new ProgressBar();
    private boolean cancel = false;
    private Button cancelButton = UXUtils.getButtonWithoutStyle(SvgIcons.SVG_CANCEL);

    public ProgressFeedbackPane() {
        progressBar.prefWidthProperty().bind(widthProperty());
        cancelButton.setOnAction(event -> cancel = true);
        StackPane stackPane = new StackPane();
        stackPane.getChildren().setAll(progressBar, label);
        pane.setCenter(stackPane);
        pane.setRight(cancelButton);
        cancelButton.setPadding(new Insets(0, 0, 0, 0));
    }

    public ProgressFeedback getProgressFeedback() {
        return new ProgressFeedback() {
            @Override
            public void start() {
                cancel = false;
                Platform.runLater(() -> {
                    progressBar.setProgress(-1);
                    ProgressFeedbackPane.this.setCenter(pane);
                });
            }

            @Override
            public void setText(String text) {
                Platform.runLater(() -> {
                    label.setText(StringUtils.abbreviateMiddle(text, " ... ", 40));
                });
            }

            @Override
            public void progress(int currentValue, int endValue) {
                Platform.runLater(() -> {
                    if (endValue <= 0) {
                        progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
                    } else {
                        progressBar.setProgress((double) currentValue / endValue);
                    }
                });
            }

            @Override
            public boolean canceled() {
                return cancel;
            }

            @Override
            public void end() {
                Platform.runLater(() -> {
                    ProgressFeedbackPane.this.setCenter(null);
                });
            }
        };
    }


}
