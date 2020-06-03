/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.results;

import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;

import java.util.ArrayList;
import java.util.List;

public class FileAgeDistributionPerLogicalDecomposition {
    private String name = "";
    private List<RiskDistributionStats> fileAgeDistributionPerComponent = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<RiskDistributionStats> getFileAgeDistributionPerComponent() {
        return fileAgeDistributionPerComponent;
    }

    public void setFileAgeDistributionPerComponent(List<RiskDistributionStats> fileAgeDistributionPerComponent) {
        this.fileAgeDistributionPerComponent = fileAgeDistributionPerComponent;
    }
}
