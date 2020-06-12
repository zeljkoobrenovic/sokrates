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

import static nl.obren.sokrates.sourcecode.stats.SourceFileAgeDistribution.Types.LAST_MODIFIED;

public class SourceFileAgeDistribution extends RiskDistributionStats {
    private Types type;

    public SourceFileAgeDistribution() {
        super(30, 90, 180, 365);
    }

    public SourceFileAgeDistribution(Types type) {
        super(30, 90, 180, 365);
        this.type = type;
        setNegligibleRiskLabel("1-30 days");
        setLowRiskLabel("31-60 days");
        setMediumRiskLabel("61-180 days");
        setHighRiskLabel("181-365 days");
        setVeryHighRiskLabel("366+ days");
    }

    public List<RiskDistributionStats> getFileAgeRiskDistributionPerExtension(List<SourceFile> files) {
        ArrayList<RiskDistributionStats> distributions = new ArrayList<>();

        Map<String, SourceFileAgeDistribution> map = new HashMap<>();

        if (files != null) {
            files.forEach(sourceFile -> {
                SourceFileAgeDistribution distribution = map.get(sourceFile.getExtension());
                if (distribution == null) {
                    distribution = new SourceFileAgeDistribution(LAST_MODIFIED);
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
            SourceFileAgeDistribution distribution = new SourceFileAgeDistribution(LAST_MODIFIED);
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

