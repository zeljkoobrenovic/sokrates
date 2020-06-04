/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.results;

import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;

import java.util.ArrayList;
import java.util.List;

public class FileAgeDistributionPerLogicalDecomposition {
    private String name = "";
    private List<RiskDistributionStats> firstModifiedDistributionPerComponent = new ArrayList<>();
    private List<RiskDistributionStats> lastModifiedDistributionPerComponent = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<RiskDistributionStats> getFirstModifiedDistributionPerComponent() {
        return firstModifiedDistributionPerComponent;
    }

    public void setFirstModifiedDistributionPerComponent(List<RiskDistributionStats> firstModifiedDistributionPerComponent) {
        this.firstModifiedDistributionPerComponent = firstModifiedDistributionPerComponent;
    }

    public List<RiskDistributionStats> getLastModifiedDistributionPerComponent() {
        return lastModifiedDistributionPerComponent;
    }

    public void setLastModifiedDistributionPerComponent(List<RiskDistributionStats> lastModifiedDistributionPerComponent) {
        this.lastModifiedDistributionPerComponent = lastModifiedDistributionPerComponent;
    }
}
