/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.codeexplorer.newproject;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.ExtensionGroup;
import nl.obren.sokrates.sourcecode.ExtensionGroupExtractor;
import nl.obren.sokrates.sourcecode.SourceCodeFiles;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.core.CodeConfigurationUtils;
import nl.obren.sokrates.sourcecode.scoping.ScopingConventions;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.controlsfx.control.CheckListView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class NewProjectDialog extends BorderPane {
    private TextField srcFolder = new TextField();
    private TextField configurationFilePath = new TextField();
    private Stage stage;
    private CheckListView<ExtensionGroup> checkListView;
    private CodeConfiguration codeConfiguration = CodeConfiguration.getDefaultConfiguration();
    private Callback<Pair<File, CodeConfiguration>, Void> onNewProjectCreated;
    private Button createProjectButton;

    public NewProjectDialog() {
        setId("new_project_dialog");
        setCenter(getCheckListViewPane());
        setTop(getSrcFolderSelectorPane());
        setBottom(getConfigurationFilePath());
    }

    private Node getSrcFolderSelectorPane() {
        BorderPane pane = new BorderPane();

        srcFolder.setEditable(false);
        Button selectButton = new Button("select...");
        selectButton.setOnAction(event -> selectFile());

        Button deselectAllButton = new Button("clear selection");
        deselectAllButton.setOnAction(event -> deselectAll());

        pane.setLeft(new Label(" Source code root folder: "));
        pane.setCenter(srcFolder);
        pane.setRight(selectButton);
        pane.setBottom(deselectAllButton);

        return pane;
    }

    private void selectFile() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select a source code folder");
        File selectedDirectory = chooser.showDialog(stage);
        if (selectedDirectory != null) {
            String selectedDirectoryPath = selectedDirectory.getPath();
            srcFolder.setText(selectedDirectoryPath);
            createProjectButton.disableProperty().setValue(!new File(selectedDirectoryPath).exists());
            File sokratesConfigFile = CodeConfigurationUtils.getDefaultSokratesConfigFile(selectedDirectory);
            configurationFilePath.setText(sokratesConfigFile.getPath());
            updateExtensions(selectedDirectory);
        }
    }

    private Node getConfigurationFilePath() {
        BorderPane pane = new BorderPane();

        pane.setLeft(new Label(" Configuration file: "));
        pane.setCenter(configurationFilePath);
        createProjectButton = new Button("Create New Project");
        createProjectButton.setDefaultButton(true);
        createProjectButton.disableProperty().setValue(true);
        createProjectButton.setOnAction(event -> createNewProject());
        pane.setBottom(new BorderPane(createProjectButton));

        return pane;
    }

    private void createNewProject() {
        File file = new File(configurationFilePath.getText());
        if (file.exists()) {
            if (userDoesNotConfirmOverwriting()) {
                return;
            }
        }
        List<String> extensions = new ArrayList<>();

        checkListView.getItems().forEach(item -> {
            if (checkListView.getItemBooleanProperty(item).getValue()) {
                extensions.add(item.getExtension());
            }
        });

        codeConfiguration.setExtensions(extensions);

        SourceCodeFiles sourceCodeFiles = new SourceCodeFiles();
        File root = new File(srcFolder.getText());
        codeConfiguration.getMetadata().setName(root.getName());
        sourceCodeFiles.load(root, new ProgressFeedback() {
            public void setText(String text) {
                System.out.println(text);
            }
        });

        sourceCodeFiles.createBroadScope(extensions, new ArrayList<>(), false, codeConfiguration.getAnalysis().getMaxLineLength());

        ScopingConventions scopingConventions = new ScopingConventions();

        scopingConventions.addConventions(codeConfiguration, sourceCodeFiles.getFilesInBroadScope());

        Platform.runLater(() -> stage.close());
        Platform.runLater(() -> onNewProjectCreated.call(new ImmutablePair<>(file, codeConfiguration)));
    }

    private boolean userDoesNotConfirmOverwriting() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Configuration File");
        alert.setHeaderText("Configuration File Already Exists. Do you want to overwrite it?");
        alert.setContentText("Choose your option.");

        ButtonType overwriteButton = new ButtonType("Overwrite");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(overwriteButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        return result.get() != overwriteButton;
    }

    private ObservableList<ExtensionGroup> getExtensionGroups(File file) {
        ExtensionGroupExtractor extractor = new ExtensionGroupExtractor();
        extractor.extractExtensionsInfo(file);

        ObservableList<ExtensionGroup> groups = FXCollections.observableArrayList();
        extractor.getExtensionsList().forEach(groups::add);

        Collections.sort(groups, (o1, o2) -> o2.getNumberOfFiles() - o1.getNumberOfFiles());

        return groups;
    }

    private void updateExtensions(File root) {
        ObservableList<ExtensionGroup> groups = getExtensionGroups(root);
        checkListView.setItems(groups);
        groups.forEach(group -> {
            checkListView.getItemBooleanProperty(group).setValue(ExtensionGroupExtractor.isKnownSourceCodeExtension(group.getExtension()));
        });
    }

    private void deselectAll() {
        checkListView.getItems().forEach(item -> checkListView.getItemBooleanProperty(item).setValue(false));
    }

    private Node getCheckListViewPane() {
        checkListView = new CheckListView<>();
        TitledPane titledPane = new TitledPane("Extensions", checkListView);
        titledPane.setExpanded(true);
        return titledPane;
    }

    public void showAndWait(Callback<Pair<File, CodeConfiguration>, Void> onNewProjectCreated) {
        this.onNewProjectCreated = onNewProjectCreated;
        stage = new Stage();
        stage.setTitle("New Project");
        stage.setScene(new Scene(this, 600, 400));
        stage.showAndWait();
    }
}

