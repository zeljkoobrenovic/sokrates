package nl.obren.sokrates.codeexplorer.search;

import javafx.scene.layout.BorderPane;
import nl.obren.sokrates.codeexplorer.dependencies.DependenciesPane;

public class SearchDependenciesPane extends BorderPane {
    private final DependenciesPane dependenciesPane;

    public SearchDependenciesPane() {
        dependenciesPane = new DependenciesPane("", true);
        setCenter(dependenciesPane);
    }

    public DependenciesPane getDependenciesPane() {
        return dependenciesPane;
    }
}
