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

import static nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution.Types.LAST_MODIFIED;

public class SourceFileAgeDistribution extends RiskDistributionStats {
    private Thresholds thresholds;
    private Types type;

    public SourceFileAgeDistribution() {
    }

    public SourceFileAgeDistribution(Thresholds thresholds, Types type) {
        super(thresholds.getLow(), thresholds.getMedium(), thresholds.getHigh(), thresholds.getVeryHigh());
        this.thresholds = thresholds;
        this.type = type;
        setNegligibleRiskLabel(thresholds.getNegligibleRiskLabel() + " days");
        setLowRiskLabel(thresholds.getLowRiskLabel() + " days");
        setMediumRiskLabel(thresholds.getMediumRiskLabel() + " days");
        setHighRiskLabel(thresholds.getHighRiskLabel() + " days");
        setVeryHighRiskLabel(thresholds.getVeryHighRiskLabel() + " days");
    }

    public List<RiskDistributionStats> getFileAgeRiskDistributionPerExtension(List<SourceFile> files) {
        ArrayList<RiskDistributionStats> distributions = new ArrayList<>();

        Map<String, SourceFileAgeDistribution> map = new HashMap<>();

        if (files != null) {
            files.forEach(sourceFile -> {
                SourceFileAgeDistribution distribution = map.get(sourceFile.getExtension());
                if (distribution == null) {
                    distribution = new SourceFileAgeDistribution(thresholds, LAST_MODIFIED);
                    distribution.setKey(sourceFile.getExtension());
                    distributions.add(distribution);
                    map.put(distribution.getKey(), distribution);
                }

                if (sourceFile.getFileModificationHistory() != null) {
                    distribution.update(getAge(sourceFile), sourceFile.getLinesOfCode());
                }
            });
        }

        return distributions;
    }

    ;

    public List<RiskDistributionStats> getFileAgeRiskDistributionPerComponent(LogicalDecomposition logicalDecomposition) {
        ArrayList<RiskDistributionStats> distributions = new ArrayList<>();

        logicalDecomposition.getComponents().forEach(component -> {
            SourceFileAgeDistribution distribution = new SourceFileAgeDistribution(thresholds, LAST_MODIFIED);
            distribution.setKey(component.getName());
            distributions.add(distribution);
            component.getSourceFiles().stream().filter(f -> f.getFileModificationHistory() != null).forEach(sourceFile -> {
                distribution.update(getAge(sourceFile), sourceFile.getLinesOfCode());
            });
        });

        return distributions;
    }

    public SourceFileAgeDistribution getOverallLastModifiedDistribution(List<SourceFile> files) {
        reset();
        if (files != null) {
            files.stream().filter(f -> f.getFileModificationHistory() != null).forEach(sourceFile -> {
                update(getAge(sourceFile), sourceFile.getLinesOfCode());
            });
        }
        return this;
    }

    private int getAge(SourceFile sourceFile) {
        return type == LAST_MODIFIED
                ? sourceFile.getFileModificationHistory().daysSinceLatestUpdate()
                : sourceFile.getFileModificationHistory().daysSinceFirstUpdate();
    }

    public SourceFileAgeDistribution getOverallFirstModifiedDistribution(List<SourceFile> files) {
        reset();
        if (files != null) {
            files.stream().filter(f -> f.getFileModificationHistory() != null).forEach(sourceFile -> {
                update(sourceFile.getFileModificationHistory().daysSinceFirstUpdate(), sourceFile.getLinesOfCode());
            });
        }
        return this;
    }

    public static enum Types {LAST_MODIFIED, FIRST_MODIFIED}

}

