/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.stats;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SourceFileAgeDistribution extends RiskDistributionStats {
    public SourceFileAgeDistribution() {
        super(30, 90, 180);
        setLowRiskLabel("1-30 days");
        setMediumRiskLabel("31-60 days");
        setHighRiskLabel("61-180 days");
        setVeryHighRiskLabel("181+");
    }

    public static List<RiskDistributionStats> getFileAgeRiskDistributionPerExtension(List<SourceFile> files) {
        ArrayList<RiskDistributionStats> distributions = new ArrayList<>();

        Map<String, SourceFileAgeDistribution> map = new HashMap<>();

        if (files != null) {
            files.forEach(sourceFile -> {
                SourceFileAgeDistribution distribution = map.get(sourceFile.getExtension());
                if (distribution == null) {
                    distribution = new SourceFileAgeDistribution();
                    distribution.setKey(sourceFile.getExtension());
                    distributions.add(distribution);
                    map.put(distribution.getKey(), distribution);
                }

                distribution.update(sourceFile.getAgeInDays(), sourceFile.getAgeInDays());
            });
        }

        return distributions;
    }

    public static List<RiskDistributionStats> getFileAgeRiskDistributionPerComponent(List<SourceFile> files, LogicalDecomposition logicalDecomposition) {
        ArrayList<RiskDistributionStats> distributions = new ArrayList<>();

        logicalDecomposition.getComponents().forEach(component -> {
            SourceFileAgeDistribution distribution = new SourceFileAgeDistribution();
            distribution.setKey(component.getName());
            distributions.add(distribution);
            component.getSourceFiles().forEach(sourceFile -> {
                distribution.update(sourceFile.getAgeInDays(), sourceFile.getAgeInDays());
            });
        });

        return distributions;
    }

    public SourceFileAgeDistribution getOverallDistribution(List<SourceFile> files) {
        reset();
        if (files != null) {
            files.forEach(sourceFile -> {
                update(sourceFile.getAgeInDays(), sourceFile.getAgeInDays());
            });
        }
        return this;
    }
}

