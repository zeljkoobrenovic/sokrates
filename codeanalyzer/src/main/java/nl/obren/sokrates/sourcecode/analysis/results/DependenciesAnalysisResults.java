/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.results;

import java.util.ArrayList;
import java.util.List;

public class DependenciesAnalysisResults {
    private List<ControlStatus> controlStatuses = new ArrayList<>();

    public List<ControlStatus> getControlStatuses() {
        return controlStatuses;
    }

    public void setControlStatuses(List<ControlStatus> controlStatuses) {
        this.controlStatuses = controlStatuses;
    }
}
