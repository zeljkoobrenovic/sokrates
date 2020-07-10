/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CodeAnalyzerSettings {
    private static final Log LOG = LogFactory.getLog(CodeAnalyzerSettings.class);
    private boolean dataOnly = false;
    private boolean updateIndex = true;
    private boolean analyzeFilesInScope = true;
    private boolean analyzeLogicalDecomposition = true;
    private boolean analyzeConcerns = true;
    private boolean analyzeDuplication = true;
    private boolean analyzeFileSize = true;
    private boolean analyzeFileHistory = true;
    private boolean analyzeContributors = true;
    private boolean analyzeUnitSize = true;
    private boolean analyzeConditionalComplexity = true;
    private boolean createMetricsList = true;
    private boolean analyzeControls = true;
    private boolean analyzeFindings = true;

    public void selectAll() {
        analyzeFilesInScope = true;
        analyzeLogicalDecomposition = true;
        analyzeConcerns = true;
        analyzeDuplication = true;
        analyzeFileSize = true;
        analyzeFileHistory = true;
        analyzeUnitSize = true;
        analyzeConditionalComplexity = true;
        createMetricsList = true;
        analyzeControls = true;
        analyzeFindings = true;
        analyzeContributors = true;

        dataOnly = false;
        updateIndex = true;
    }

    public void deselectAll() {
        analyzeFilesInScope = false;
        analyzeLogicalDecomposition = false;
        analyzeConcerns = false;
        analyzeDuplication = false;
        analyzeFileSize = false;
        analyzeFileHistory = false;
        analyzeUnitSize = false;
        analyzeConditionalComplexity = false;
        createMetricsList = false;
        analyzeControls = false;
        analyzeFindings = false;
        analyzeContributors = false;

        dataOnly = false;
        updateIndex = false;
    }

    public boolean isDataOnly() {
        return dataOnly;
    }

    public void setDataOnly(boolean dataOnly) {
        this.dataOnly = dataOnly;
    }

    public boolean isUpdateIndex() {
        return updateIndex;
    }

    public void setUpdateIndex(boolean updateIndex) {
        this.updateIndex = updateIndex;
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

    public boolean isAnalyzeConcerns() {
        return analyzeConcerns;
    }

    public void setAnalyzeConcerns(boolean analyzeConcerns) {
        this.analyzeConcerns = analyzeConcerns;
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

    public boolean isAnalyzeFileHistory() {
        return analyzeFileHistory;
    }

    public void setAnalyzeFileHistory(boolean analyzeFileHistory) {
        this.analyzeFileHistory = analyzeFileHistory;
    }

    public boolean isAnalyzeContributors() {
        return analyzeContributors;
    }

    public void setAnalyzeContributors(boolean analyzeContributors) {
        this.analyzeContributors = analyzeContributors;
    }
}
