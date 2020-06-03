/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.SourceFileFilter;

import java.util.ArrayList;
import java.util.List;

public class AnalyzerOverride {
    private String analyzer = "";
    private List<SourceFileFilter> filters = new ArrayList<>();

    public String getAnalyzer() {
        return analyzer;
    }

    public void setAnalyzer(String analyzer) {
        this.analyzer = analyzer;
    }

    public List<SourceFileFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<SourceFileFilter> filters) {
        this.filters = filters;
    }

    @JsonIgnore
    public boolean isOverridden(SourceFile sourceFile) {
        boolean overridden = false;
        for (SourceFileFilter sourceFileFilter : filters) {
            if (sourceFileFilter.matches(sourceFile) && !sourceFileFilter.getException()) {
                overridden = true;
            } else if (sourceFileFilter.matches(sourceFile) && sourceFileFilter.getException()) {
                overridden = false;
                break;
            }
        }

        return overridden;
    }
}
