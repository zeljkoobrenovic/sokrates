package nl.obren.sokrates.codeexplorer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import nl.obren.sokrates.codeexplorer.codebrowser.CodeBrowserPane;
import nl.obren.sokrates.sourcecode.findings.Findings;
import org.apache.log4j.BasicConfigurator;

public class CodeExplorerLauncher extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        BasicConfigurator.configure();

        primaryStage.setTitle("Sokrates Code Explorer");
        CodeBrowserPane codeBrowserPane = new CodeBrowserPane(primaryStage);
        primaryStage.setScene(new Scene(codeBrowserPane, 800, 600));
        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
        primaryStage.show();
    }
}
