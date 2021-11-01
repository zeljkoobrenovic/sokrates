/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.aspects;

import java.util.ArrayList;
import java.util.List;

public class DependenciesFinder {
    // If true, a built-in, language specific dependency finders are used (does not exist for all languages)
    private boolean useBuiltInDependencyFinders = true;

    // A list of regex-based rules use to identify dependency to an explicitly target component based on a file path or content
    private List<DependencyFinderPattern> rules = new ArrayList<>();

    // A list of regex-based meta-rules use to identify dependency to target components based on a file path or content
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
