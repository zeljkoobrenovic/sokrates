/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.results;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.SourceFileWithSearchData;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import nl.obren.sokrates.sourcecode.search.FoundText;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AspectAnalysisResults {
    private String name;
    private int filesCount;
    private int linesOfCode;
    private int numberOfRegexLineMatches;
    private List<NumericMetric> fileCountPerExtension = new ArrayList<>();
    private List<NumericMetric> linesOfCodePerExtension = new ArrayList<>();
    @JsonIgnore
    private NamedSourceCodeAspect aspect;
    @JsonIgnore
    private List<FoundText> foundTextList = new ArrayList<>();
    @JsonIgnore
    private Map<File, SourceFileWithSearchData> foundFiles = new HashMap<>();


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


    @JsonIgnore
    public void setAspect(NamedSourceCodeAspect aspect) {
        this.aspect = aspect;
    }

    @JsonIgnore
    public NamedSourceCodeAspect getAspect() {
        return aspect;
    }

    public int getNumberOfRegexLineMatches() {
        return numberOfRegexLineMatches;
    }

    public void setNumberOfRegexLineMatches(int numberOfRegexLineMatches) {
        this.numberOfRegexLineMatches = numberOfRegexLineMatches;
    }

    @JsonIgnore
    public void setFoundTextList(List<FoundText> foundTextList) {
        this.foundTextList = foundTextList;
    }

    @JsonIgnore
    public List<FoundText> getFoundTextList() {
        return foundTextList;
    }

    @JsonIgnore
    public void setFoundFiles(Map<File, SourceFileWithSearchData> foundFiles) {
        this.foundFiles = foundFiles;
    }

    @JsonIgnore
    public Map<File, SourceFileWithSearchData> getFoundFiles() {
        return foundFiles;
    }
}
