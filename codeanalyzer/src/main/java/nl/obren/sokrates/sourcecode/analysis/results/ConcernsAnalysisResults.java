/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.results;

import nl.obren.sokrates.sourcecode.aspects.ConcernsGroup;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;

import java.util.ArrayList;
import java.util.List;

public class ConcernsAnalysisResults {
    private String key;
    private List<AspectAnalysisResults> concerns = new ArrayList<>();
    private ConcernsGroup concernsGroup;

    public ConcernsAnalysisResults() {
    }

    public ConcernsAnalysisResults(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<NumericMetric> getFileCountPerConcern() {
        List<NumericMetric> fileCount = new ArrayList<>();
        concerns.forEach(component -> fileCount.add(new NumericMetric(component.getName(), component.getFilesCount())));
        return fileCount;
    }

    public List<NumericMetric> getLinesOfCodePerConcern() {
        List<NumericMetric> fileCount = new ArrayList<>();
        concerns.forEach(component -> fileCount.add(new NumericMetric(component.getName(), component.getLinesOfCode())));
        return fileCount;
    }

    public List<AspectAnalysisResults> getConcerns() {
        return concerns;
    }

    public void setConcerns(List<AspectAnalysisResults> concerns) {
        this.concerns = concerns;
    }

    public ConcernsGroup getConcernsGroup() {
        return concernsGroup;
    }

    public void setConcernsGroup(ConcernsGroup concernsGroup) {
        this.concernsGroup = concernsGroup;
    }
}
