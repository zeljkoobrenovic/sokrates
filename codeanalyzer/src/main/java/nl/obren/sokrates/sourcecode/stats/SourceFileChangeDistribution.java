/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.stats;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.threshold.Thresholds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SourceFileChangeDistribution extends RiskDistributionStats {
    private Thresholds thresholds;

    public SourceFileChangeDistribution() {
    }

    public SourceFileChangeDistribution(Thresholds thresholds) {
        super(thresholds);
        this.thresholds = thresholds;
    }

    public List<RiskDistributionStats> getDistributionPerExtension(List<SourceFile> files) {
        ArrayList<RiskDistributionStats> distributions = new ArrayList<>();

        Map<String, SourceFileChangeDistribution> map = new HashMap<>();

        if (files != null) {
            files.forEach(sourceFile -> {
                SourceFileChangeDistribution distribution = map.get(sourceFile.getExtension());
                if (distribution == null) {
                    distribution = new SourceFileChangeDistribution(thresholds);
                    distribution.setKey(sourceFile.getExtension());
                    distributions.add(distribution);
                    map.put(distribution.getKey(), distribution);
                }

                if (sourceFile.getFileModificationHistory() != null) {
                    distribution.update(getNumberOfChanges(sourceFile), sourceFile.getLinesOfCode());
                }
            });
        }

        return distributions;
    }

    ;

    public List<RiskDistributionStats> getRiskDistributionPerComponent(LogicalDecomposition logicalDecomposition) {
        ArrayList<RiskDistributionStats> distributions = new ArrayList<>();

        logicalDecomposition.getComponents().forEach(component -> {
            SourceFileChangeDistribution distribution = new SourceFileChangeDistribution(thresholds);
            distribution.setKey(component.getName());
            distributions.add(distribution);
            component.getSourceFiles().stream().filter(f -> f.getFileModificationHistory() != null).forEach(sourceFile -> {
                distribution.update(getNumberOfChanges(sourceFile), sourceFile.getLinesOfCode());
            });
        });

        return distributions;
    }

    public SourceFileChangeDistribution getOverallDistribution(List<SourceFile> files) {
        reset();
        if (files != null) {
            files.stream().filter(f -> f.getFileModificationHistory() != null).forEach(sourceFile -> {
                update(getNumberOfChanges(sourceFile), sourceFile.getLinesOfCode());
            });
        }
        return this;
    }

    public SourceFileChangeDistribution getOverallContributorsCountDistribution(List<SourceFile> files) {
        reset();
        if (files != null) {
            files.stream().filter(f -> f.getFileModificationHistory() != null).forEach(sourceFile -> {
                update(sourceFile.getFileModificationHistory() != null ? sourceFile.getFileModificationHistory().countContributors() : 0,
                        sourceFile.getLinesOfCode());
            });
        }
        return this;
    }

    private int getNumberOfChanges(SourceFile sourceFile) {
        return sourceFile.getFileModificationHistory().getDates().size();
    }
}

