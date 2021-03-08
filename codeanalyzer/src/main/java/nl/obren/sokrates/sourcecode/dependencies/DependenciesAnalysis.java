/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.dependencies;

import com.kitfox.svg.A;
import nl.obren.sokrates.sourcecode.aspects.MetaDependencyRule;

import java.util.ArrayList;
import java.util.List;

public class DependenciesAnalysis {
    private List<Dependency> dependencies = new ArrayList<>();
    private List<DependencyError> errors = new ArrayList<>();
    private List<MetaDependencyRule> metaRules = new ArrayList<>();

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }

    public List<DependencyError> getErrors() {
        return errors;
    }

    public void setErrors(List<DependencyError> errors) {
        this.errors = errors;
    }

    public List<MetaDependencyRule> getMetaRules() {
        return metaRules;
    }

    public void setMetaRules(List<MetaDependencyRule> metaRules) {
        this.metaRules = metaRules;
    }
}
