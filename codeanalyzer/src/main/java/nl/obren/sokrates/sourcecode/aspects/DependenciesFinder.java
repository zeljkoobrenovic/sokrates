/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.aspects;

import java.util.ArrayList;
import java.util.List;

public class DependenciesFinder {
    private boolean useBuiltInDependencyFinders = true;
    private List<DependencyFinderPattern> rules = new ArrayList<>();
    private List<MetaDependencyRule> metaRules = new ArrayList<>();

    public boolean isUseBuiltInDependencyFinders() {
        return useBuiltInDependencyFinders;
    }

    public void setUseBuiltInDependencyFinders(boolean useBuiltInDependencyFinders) {
        this.useBuiltInDependencyFinders = useBuiltInDependencyFinders;
    }

    public List<DependencyFinderPattern> getRules() {
        return rules;
    }

    public void setRules(List<DependencyFinderPattern> rules) {
        this.rules = rules;
    }

    public List<MetaDependencyRule> getMetaRules() {
        return metaRules;
    }

    public void setMetaRules(List<MetaDependencyRule> metaRules) {
        this.metaRules = metaRules;
    }
}
