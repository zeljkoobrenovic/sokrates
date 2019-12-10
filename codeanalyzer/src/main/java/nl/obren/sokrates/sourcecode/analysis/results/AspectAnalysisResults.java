/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.results;

import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;

import java.util.ArrayList;
import java.util.List;

public class AspectAnalysisResults {
    private String name;
    private int filesCount;
    private int linesOfCode;
    private int numberOfRegexLineMatches;
    private List<NumericMetric> fileCountPerExtension = new ArrayList<>();
    private List<NumericMetric> linesOfCodePerExtension = new ArrayList<>();
    private NamedSourceCodeAspect aspect;


    public AspectAnalysisResults() {
    }

    public AspectAnalysisResults(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getFilesCount() {
        return filesCount;
    }

    public void setFilesCount(int filesCount) {
        this.filesCount = filesCount;
    }

    public int getLinesOfCode() {
        return linesOfCode;
    }

    public void setLinesOfCode(int linesOfCode) {
        this.linesOfCode = linesOfCode;
    }

    public List<NumericMetric> getFileCountPerExtension() {
        return fileCountPerExtension;
    }

    public void setFileCountPerExtension(List<NumericMetric> fileCountPerExtension) {
        this.fileCountPerExtension = fileCountPerExtension;
    }

    public List<NumericMetric> getLinesOfCodePerExtension() {
        return linesOfCodePerExtension;
    }

    public void setLinesOfCodePerExtension(List<NumericMetric> linesOfCodePerExtension) {
        this.linesOfCodePerExtension = linesOfCodePerExtension;
    }


    public void setAspect(NamedSourceCodeAspect aspect) {
        this.aspect = aspect;
    }

    public NamedSourceCodeAspect getAspect() {
        return aspect;
    }

    public int getNumberOfRegexLineMatches() {
        return numberOfRegexLineMatches;
    }

    public void setNumberOfRegexLineMatches(int numberOfRegexLineMatches) {
        this.numberOfRegexLineMatches = numberOfRegexLineMatches;
    }
}
