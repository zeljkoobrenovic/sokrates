package nl.obren.sokrates.sourcecode.core;

public class ReferenceAnalysisResult {
    private String label = "reference";
    private String analysisResultsPath = "reference-analyses/previous.json";

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getAnalysisResultsPath() {
        return analysisResultsPath;
    }

    public void setAnalysisResultsPath(String analysisResultsPath) {
        this.analysisResultsPath = analysisResultsPath;
    }
}
