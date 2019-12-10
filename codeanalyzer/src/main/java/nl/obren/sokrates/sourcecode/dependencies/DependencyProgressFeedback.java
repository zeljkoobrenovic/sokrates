/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.dependencies;

import nl.obren.sokrates.common.utils.ProgressFeedback;

import java.util.ArrayList;
import java.util.List;

public class DependencyProgressFeedback extends ProgressFeedback {
    List<Dependency> currentDependencies = new ArrayList<>();

    public List<Dependency> getCurrentDependencies() {
        return currentDependencies;
    }

    public void setCurrentDependencies(List<Dependency> currentDependencies) {
        this.currentDependencies = new ArrayList<>(currentDependencies);
    }
}
