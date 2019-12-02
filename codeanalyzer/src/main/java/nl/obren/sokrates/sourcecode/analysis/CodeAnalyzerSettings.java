package nl.obren.sokrates.sourcecode.analysis;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CodeAnalyzerSettings {
    private static final Log LOG = LogFactory.getLog(CodeAnalyzerSettings.class);
    private boolean analyzeFilesInScope = true;
    private boolean analyzeLogicalDecomposition = true;
    private boolean analyzeCrossCuttingConcerns = true;
    private boolean analyzeDuplication = true;
    private boolean analyzeFileSize = true;
    private boolean analyzeUnitSize = true;
    private boolean analyzeConditionalComplexity = true;
    private boolean createMetricsList = true;
    private boolean analyzeControls = true;
    private boolean analyzeFindings = true;

    public void selectAll() {
        analyzeFilesInScope = true;
        analyzeLogicalDecomposition = true;
        analyzeCrossCuttingConcerns = true;
        analyzeDuplication = true;
        analyzeFileSize = true;
        analyzeUnitSize = true;
        analyzeConditionalComplexity = true;
        createMetricsList = true;
        analyzeControls = true;
    }

    public void deselectAll() {
        analyzeFilesInScope = false;
        analyzeLogicalDecomposition = false;
        analyzeCrossCuttingConcerns = false;
        analyzeDuplication = false;
        analyzeFileSize = false;
        analyzeUnitSize = false;
        analyzeConditionalComplexity = false;
        createMetricsList = false;
        analyzeControls = false;
    }

    public boolean isAnalyzeFilesInScope() {
        return analyzeFilesInScope;
    }

    public void setAnalyzeFilesInScope(boolean analyzeFilesInScope) {
        this.analyzeFilesInScope = analyzeFilesInScope;
    }

    public boolean isAnalyzeLogicalDecomposition() {
        return analyzeLogicalDecomposition;
    }

    public void setAnalyzeLogicalDecomposition(boolean analyzeLogicalDecomposition) {
        this.analyzeLogicalDecomposition = analyzeLogicalDecomposition;
    }

    public boolean isAnalyzeCrossCuttingConcerns() {
        return analyzeCrossCuttingConcerns;
    }

    public void setAnalyzeCrossCuttingConcerns(boolean analyzeCrossCuttingConcerns) {
        this.analyzeCrossCuttingConcerns = analyzeCrossCuttingConcerns;
    }

    public boolean isAnalyzeDuplication() {
        return analyzeDuplication;
    }

    public void setAnalyzeDuplication(boolean analyzeDuplication) {
        this.analyzeDuplication = analyzeDuplication;
    }

    public boolean isAnalyzeFileSize() {
        return analyzeFileSize;
    }

    public void setAnalyzeFileSize(boolean analyzeFileSize) {
        this.analyzeFileSize = analyzeFileSize;
    }

    public boolean isAnalyzeUnitSize() {
        return analyzeUnitSize;
    }

    public void setAnalyzeUnitSize(boolean analyzeUnitSize) {
        this.analyzeUnitSize = analyzeUnitSize;
    }

    public boolean isAnalyzeConditionalComplexity() {
        return analyzeConditionalComplexity;
    }

    public void setAnalyzeConditionalComplexity(boolean analyzeConditionalComplexity) {
        this.analyzeConditionalComplexity = analyzeConditionalComplexity;
    }

    public boolean isCreateMetricsList() {
        return createMetricsList;
    }

    public void setCreateMetricsList(boolean createMetricsList) {
        this.createMetricsList = createMetricsList;
    }

    public boolean isAnalyzeControls() {
        return analyzeControls;
    }

    public void setAnalyzeControls(boolean analyzeControls) {
        this.analyzeControls = analyzeControls;
    }

    public boolean isAnalyzeFindings() {
        return analyzeFindings;
    }

    public void setAnalyzeFindings(boolean analyzeFindings) {
        this.analyzeFindings = analyzeFindings;
    }
}
