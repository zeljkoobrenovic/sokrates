package nl.obren.sokrates.sourcecode.analysis.results;

import java.util.ArrayList;
import java.util.List;

public class ControlsAnalysisResults {
    private List<ControlStatus> controlStatuses = new ArrayList<>();

    public List<ControlStatus> getControlStatuses() {
        return controlStatuses;
    }

    public void setControlStatuses(List<ControlStatus> controlStatuses) {
        this.controlStatuses = controlStatuses;
    }
}
