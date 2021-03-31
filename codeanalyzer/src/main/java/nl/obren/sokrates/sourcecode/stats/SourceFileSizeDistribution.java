/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.stats;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.threshold.Thresholds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SourceFileSizeDistribution extends RiskDistributionStats {
    public SourceFileSizeDistribution() {
    }

    public SourceFileSizeDistribution(Thresholds thresholds) {
        super(thresholds);
    }

    public static List<RiskDistributionStats> getFileSizeRiskDistributionPerExtension(List<SourceFile> files, Thresholds thresholds) {
        ArrayList<RiskDistributionStats> distributions = new ArrayList<>();

        Map<String, SourceFileSizeDistribution> map = new HashMap<>();

        if (files != null) {
            files.forEach(sourceFile -> {
                SourceFileSizeDistribution distribution = map.get(sourceFile.getExtension());
                if (distribution == null) {
                    distribution = new SourceFileSizeDistribution(thresholds);
                    distribution.setKey(sourceFile.getExtension());
                    distributions.add(distribution);
                    map.put(distribution.getKey(), distribution);
                }

                distribution.update(sourceFile.getLinesOfCode(), sourceFile.getLinesOfCode());
            });
        }

        return distributions;
    }

    public static List<RiskDistributionStats> getFileSizeRiskDistributionPerComponent(LogicalDecomposition logicalDecomposition, Thresholds thresholds) {
        ArrayList<RiskDistributionStats> distributions = new ArrayList<>();

        logicalDecomposition.getComponents().forEach(component -> {
            SourceFileSizeDistribution distribution = new SourceFileSizeDistribution(thresholds);
            distribution.setKey(component.getName());
            distributions.add(distribution);
            component.getSourceFiles().forEach(sourceFile -> {
                distribution.update(sourceFile.getLinesOfCode(), sourceFile.getLinesOfCode());
            });
        });

        return distributions;
    }

    public SourceFileSizeDistribution getOverallDistribution(List<SourceFile> files) {
        reset();
        if (files != null) {
            files.forEach(sourceFile -> {
                update(sourceFile.getLinesOfCode(), sourceFile.getLinesOfCode());
            });
        }
        return this;
    }
}

