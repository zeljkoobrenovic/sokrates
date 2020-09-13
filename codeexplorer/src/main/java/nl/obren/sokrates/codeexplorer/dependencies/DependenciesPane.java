/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.codeexplorer.dependencies;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Callback;
import nl.obren.sokrates.codeexplorer.codebrowser.TableViewUtils;
import nl.obren.sokrates.codeexplorer.common.NumericBarCellFactory;
import nl.obren.sokrates.codeexplorer.common.ProgressFeedbackPane;
import nl.obren.sokrates.codeexplorer.common.UXUtils;
import nl.obren.sokrates.common.renderingutils.GraphvizUtil;
import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.reports.utils.GraphvizDependencyRenderer;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.aspects.SourceCodeAspectUtils;
import nl.obren.sokrates.sourcecode.dependencies.*;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzer;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzerFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class DependenciesPane extends BorderPane {
    private static final Log LOG = LogFactory.getLog(DependenciesPane.class);
    private static DependenciesPane instance;
    private static Stage stage;
    private final WebView info;
    private final DependenciesView dependenciesView;
    private final DependenciesMatrix dependenciesMatrix;
    private LogicalDecomposition logicalDecomposition;
    private TableView<Dependency> table = new TableView<>();
    private BorderPane topPane = new BorderPane();
    private ProgressFeedbackPane progressFeedbackPane = new ProgressFeedbackPane();
    private Number maxCount;
    private List<ComponentDependency> componentDependencies;
    private boolean renderGraphviz;

    public DependenciesPane(LogicalDecomposition logicalDecomposition, boolean renderGraphviz) {
        this.renderGraphviz = renderGraphviz;
        TableViewUtils.addCopyToClipboardContextMenu(table);

        this.logicalDecomposition = logicalDecomposition;
        setId("dependencies_pane");
        addTableColumns();

        dependenciesView = new DependenciesView(graphvizContent -> {
            reloadGrapvizContent(graphvizContent);
            return null;
        }, renderGraphviz);

        dependenciesMatrix = new DependenciesMatrix();

        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.VERTICAL);
        configureTable();
        splitPane.getItems().add(table);
        info = new WebView();
        splitPane.getItems().add(info);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().add(new Tab(renderGraphviz ? "diagram" : "graphviz dot", dependenciesView));
        tabs.getTabs().add(new Tab("matrix", dependenciesMatrix));
        tabs.getTabs().add(new Tab("list", splitPane));
        setCenter(tabs);
        setTop(topPane);
        topPane.setBottom(progressFeedbackPane);
    }

    public static void close() {
        if (stage != null) {
            stage.close();
            stage = null;
        }
    }

    public static void openInWindow(NamedSourceCodeAspect namedSourceCodeAspect, LogicalDecomposition group, List<String> componentNames) {
        if (stage != null) {
            stage.close();
        }
        stage = new Stage();
        stage.setTitle("Logical Component Dependencies");
        instance = new DependenciesPane(group, true);
        instance.loadDependencies(namedSourceCodeAspect, componentNames);

        Scene value = new Scene(instance, 800, 600);
        stage.setScene(value);
        stage.show();
    }

    public static DependenciesPane getInstance() {
        return instance;
    }

    private void reloadGrapvizContent(String graphvizContent) {
        if (renderGraphviz) {
            GraphvizDependencyRenderer renderer = new GraphvizDependencyRenderer();
            renderer.setOrientation(logicalDecomposition.getRenderingOptions().getOrientation());
            renderer.append(GraphvizUtil.getSvgFromDot(graphvizContent));
            renderer.setMaxNumberOfDependencies(100);
            dependenciesView.load(renderer.getHtmlContent(), graphvizContent);
        } else {
            dependenciesView.load("", graphvizContent);
        }
    }

    private void configureTable() {
        table.setEditable(false);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                setInfo(newSelection);
            } else {
                info.getEngine().loadContent("");
            }
        });
    }

    private void setInfo(Dependency dependency) {
        StringBuilder text = new StringBuilder();

        text.append("<b><i>" + dependency.getDependencyString() + " (");
        dependency.getFromComponents(logicalDecomposition.getName()).forEach(component -> {
            text.append("" + component.getName() + " ");
        });

        text.append(" ->  ");

        dependency.getToComponents(logicalDecomposition.getName()).forEach(component -> {
            text.append(component.getName() + " ");
        });
        text.append(")</i></b><br/>\n<br/>\n");

        text.append("From files:<ol>");
        dependency.getFromFiles().forEach(sourceFileDependency -> {
            text.append("<li><b>" + sourceFileDependency.getSourceFile().getRelativePath() + "</b><br/>\n");
            text.append("contains <i>\"" + sourceFileDependency.getCodeFragment() + "\"</i></li>\n");
        });
        text.append("</ol>\n");

        if (dependency.getTo().getCodeFragment() != null) {
            text.append("<br/>Possible target files (containing <i>\"" + dependency.getTo().getCodeFragment() + "\"</i>):<ol>\n");
            dependency.getTo().getSourceFiles().forEach(sourceFile -> {
                text.append("<li><b>" + sourceFile.getRelativePath() + "</b></li>");
            });
            text.append("</ol>\n");
        }

        info.getEngine().loadContent(UXUtils.DEFAULT_HTML_HEADER + "<body>" + text.toString() + "</body>");
    }

    protected void addTableColumns() {
        TableColumn<Dependency, String> column1 = new TableColumn<>("Component Dependency");
        column1.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getComponentDependency(logicalDecomposition.getName())));

        TableColumn<Dependency, String> column2 = new TableColumn<>("From");
        column2.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getFrom().getAnchor()));

        TableColumn<Dependency, String> column3 = new TableColumn<>("To");
        column3.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().getTo().getAnchor()));

        TableColumn column4 = new TableColumn<>("Count");
        column4.setMinWidth(100);
        column4.setMaxWidth(100);
        column4.setCellValueFactory(new Callback<TableColumn.CellDataFeatures<Dependency, Integer>, ObservableValue<Integer>>() {
            public ObservableValue<Integer> call(TableColumn.CellDataFeatures<Dependency, Integer> p) {
                return new ReadOnlyObjectWrapper(p.getValue().getFromFiles().size());
            }
        });
        column4.setCellFactory(new NumericBarCellFactory(param -> getMaxCount()));

        table.getColumns().addAll(column1, column2, column3, column4);
    }

    private Number getMaxCount() {
        return maxCount;
    }

    private void loadDependencies(NamedSourceCodeAspect namedSourceCodeAspect, List<String> componentNames) {
        if (namedSourceCodeAspect != null) {
            Executors.newCachedThreadPool().execute(() -> {
                ObservableList<Dependency> items = FXCollections.observableArrayList();
                List<Dependency> allDependencies = new ArrayList<>();

                if (logicalDecomposition.getDependenciesFinder().isUseBuiltInDependencyFinders()) {
                    ProgressFeedback progressFeedback = getProgressFeedback(componentNames, items);
                    progressFeedback.start();

                    SourceCodeAspectUtils.getAspectsPerExtensions(namedSourceCodeAspect).forEach(langAspect -> {
                        if (langAspect.getSourceFiles().size() > 0) {
                            SourceFile sourceFileSample = langAspect.getSourceFiles().get(0);
                            LanguageAnalyzer languageAnalyzer = LanguageAnalyzerFactory.getInstance().getLanguageAnalyzer(sourceFileSample);
                            List<Dependency> dependencies = languageAnalyzer.extractDependencies(langAspect.getSourceFiles(), progressFeedback).getDependencies();
                            allDependencies.addAll(dependencies);

                            calculateMaxValues(dependencies);
                            dependencies.forEach(items::add);
                        }
                    });
                    progressFeedback.end();
                }

                DependenciesFinderExtractor finder = new DependenciesFinderExtractor(logicalDecomposition);
                List<ComponentDependency> finderDependencies = finder.findComponentDependencies(namedSourceCodeAspect);

                Platform.runLater(() -> {
                    showDependencyDiagram(allDependencies, finderDependencies, logicalDecomposition.getName(), componentNames);
                    table.setItems(items);
                });

            });
        } else {
            LOG.error("Load dependencies: the source code aspect is null.");
        }
    }

    private ProgressFeedback getProgressFeedback(List<String> componentNames, ObservableList<Dependency> items) {
        ProgressFeedback progressFeedbackInPane = progressFeedbackPane.getProgressFeedback();
        DependencyProgressFeedback progressFeedback = new DependencyProgressFeedback() {
            private long lastUpdate = System.currentTimeMillis();

            public void start() {
                progressFeedbackInPane.start();
            }

            public void end() {
                progressFeedbackInPane.end();
            }

            public void setText(String text) {
                progressFeedbackInPane.setText(text);
            }

            public boolean canceled() {
                return progressFeedbackInPane.canceled();
            }

            public void progress(int currentValue, int endValue) {
                if (System.currentTimeMillis() - lastUpdate > 100) {
                    lastUpdate = System.currentTimeMillis();
                    Platform.runLater(() -> {
                        showDependencyDiagram(getCurrentDependencies(), new ArrayList<>(), logicalDecomposition.getName(), componentNames);
                        table.setItems(items);
                    });
                }
                progressFeedbackInPane.progress(currentValue, endValue);
            }

        };
        return progressFeedback;
    }

    public void showDependencyDiagram(List<Dependency> dependencies, List<ComponentDependency> finderDependencies, String group, List<String> componentNames) {
        dependenciesView.load("Please wait...", "");
        componentDependencies = DependencyUtils.getComponentDependencies(dependencies, group);
        componentDependencies.addAll(finderDependencies);
        String graphvizContent = new GraphvizDependencyRenderer().getGraphvizContent(componentNames, componentDependencies);

        String htmlContent = "";
        if (this.renderGraphviz) {
            try {
                GraphvizDependencyRenderer renderer = new GraphvizDependencyRenderer();
                renderer.setOrientation(logicalDecomposition.getRenderingOptions().getOrientation());
                String svg = GraphvizUtil.getSvgFromDot(graphvizContent);
                renderer.append(svg != null ? svg : "Could not render dependencies. Try to increase memory limits.");
                htmlContent = renderer.getHtmlContent();
            } catch (Exception e) {
                htmlContent = "";
            }

        }
        dependenciesMatrix.load(componentDependencies);
        dependenciesView.load(htmlContent, graphvizContent);
    }

    public void showDependencies(List<Dependency> dependencies, List<ComponentDependency> componentDependencies, List<String> componentNames, String orientation) {
        dependenciesView.load("Please wait...", "");
        String graphvizContent = new GraphvizDependencyRenderer().orientation(orientation).getGraphvizContent(componentNames, componentDependencies);
        String htmlContent = "";
        if (this.renderGraphviz) {
            GraphvizDependencyRenderer renderer = new GraphvizDependencyRenderer();
            renderer.setOrientation(logicalDecomposition.getRenderingOptions().getOrientation());
            String svg = GraphvizUtil.getSvgFromDot(graphvizContent);
            renderer.append(svg != null ? svg : "Could not render dependencies. Try to increase memory limits.");
            htmlContent = renderer.getHtmlContent();
        }
        dependenciesView.load(htmlContent, graphvizContent);
        dependenciesMatrix.load(componentDependencies);
        table.setItems(FXCollections.observableArrayList(dependencies));
    }

    private void calculateMaxValues(List<Dependency> dependencies) {
        maxCount = 0;
        dependencies.forEach(dependency -> {
            maxCount = Math.max((Integer) maxCount, dependency.getFromFiles().size());
        });
    }

    public TableView<Dependency> getTable() {
        return table;
    }
}
