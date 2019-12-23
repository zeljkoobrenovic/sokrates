/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.codeexplorer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import nl.obren.sokrates.codeexplorer.codebrowser.CodeBrowserPane;
import org.apache.log4j.BasicConfigurator;

import java.awt.*;

public class CodeExplorerLauncher extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        BasicConfigurator.configure();

        primaryStage.setTitle("Sokrates Code Explorer");
        CodeBrowserPane codeBrowserPane = new CodeBrowserPane(primaryStage);

        VBox vBox = new VBox(codeBrowserPane.getMenuBar(), codeBrowserPane);

        primaryStage.setScene(new Scene(vBox, 800, 600));
        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });

        primaryStage.show();
        primaryStage.requestFocus();
    }
}
