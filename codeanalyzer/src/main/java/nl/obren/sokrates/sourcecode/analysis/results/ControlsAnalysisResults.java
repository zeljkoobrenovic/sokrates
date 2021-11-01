/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.results;

import java.util.ArrayList;
import java.util.List;

public class ControlsAnalysisResults {
    private List<GoalsAnalysisResults> goalsAnalysisResults = new ArrayList<>();

    public List<GoalsAnalysisResults> getGoalsAnalysisResults() {
        return goalsAnalysisResults;
    }

    public void setGoalsAnalysisResults(List<GoalsAnalysisResults> goalsAnalysisResults) {
        this.goalsAnalysisResults = goalsAnalysisResults;
    }
}
