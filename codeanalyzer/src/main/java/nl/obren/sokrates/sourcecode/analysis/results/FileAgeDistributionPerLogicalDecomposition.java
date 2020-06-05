/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.results;

import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;

import java.util.ArrayList;
import java.util.List;

public class FileAgeDistributionPerLogicalDecomposition {
    private String name = "";
    private List<RiskDistributionStats> distributionPerComponent = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<RiskDistributionStats> getDistributionPerComponent() {
        return distributionPerComponent;
    }

    public void setDistributionPerComponent(List<RiskDistributionStats> distributionPerComponent) {
        this.distributionPerComponent = distributionPerComponent;
    }
}
