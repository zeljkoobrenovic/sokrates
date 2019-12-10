/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.codeexplorer.search;

import javafx.scene.layout.BorderPane;
import nl.obren.sokrates.codeexplorer.dependencies.DependenciesPane;
import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;

public class SearchDependenciesPane extends BorderPane {
    private final DependenciesPane dependenciesPane;

    public SearchDependenciesPane() {
        dependenciesPane = new DependenciesPane(new LogicalDecomposition(), true);
        setCenter(dependenciesPane);
    }

    public DependenciesPane getDependenciesPane() {
        return dependenciesPane;
    }
}
