package nl.obren.sokrates.sourcecode.analysis.results;

import nl.obren.sokrates.sourcecode.aspects.CrossCuttingConcernsGroup;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;

import java.util.ArrayList;
import java.util.List;

public class CrossCuttingConcernsAnalysisResults {
    private String key;
    private List<AspectAnalysisResults> crossCuttingConcerns = new ArrayList<>();
    private CrossCuttingConcernsGroup crossCuttingConcernsGroup;

    public CrossCuttingConcernsAnalysisResults() {
    }

    public CrossCuttingConcernsAnalysisResults(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<NumericMetric> getFileCountPerCrossCuttingConcern() {
        List<NumericMetric> fileCount = new ArrayList<>();
        crossCuttingConcerns.forEach(component -> fileCount.add(new NumericMetric(component.getName(), component.getFilesCount())));
        return fileCount;
    }

    public List<NumericMetric> getLinesOfCodePerCrossCuttingConcern() {
        List<NumericMetric> fileCount = new ArrayList<>();
        crossCuttingConcerns.forEach(component -> fileCount.add(new NumericMetric(component.getName(), component.getLinesOfCode())));
        return fileCount;
    }

    public List<AspectAnalysisResults> getCrossCuttingConcerns() {
        return crossCuttingConcerns;
    }

    public void setCrossCuttingConcerns(List<AspectAnalysisResults> crossCuttingConcerns) {
        this.crossCuttingConcerns = crossCuttingConcerns;
    }

    public CrossCuttingConcernsGroup getCrossCuttingConcernsGroup() {
        return crossCuttingConcernsGroup;
    }

    public void setCrossCuttingConcernsGroup(CrossCuttingConcernsGroup crossCuttingConcernsGroup) {
        this.crossCuttingConcernsGroup = crossCuttingConcernsGroup;
    }
}
