/*
 * Copyright (c) 2026 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.stats;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.threshold.Thresholds;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * File complexity: files classified by the sum of the McCabe indexes of their units, weighted by their
 * lines of code. Only files with units count — a file of a language without unit analysis (or without
 * functions) has no complexity to measure, and counting it as "simple" would flatter the profile.
 */
public class SourceFileComplexityDistribution {
    private SourceFileComplexityDistribution() {
    }

    public static List<SourceFile> filesWithUnits(List<SourceFile> files) {
        if (files == null) {
            return new ArrayList<>();
        }
        return files.stream().filter(file -> file.getUnitsCount() > 0).collect(Collectors.toList());
    }

    public static RiskDistributionStats overall(List<SourceFile> files, Thresholds thresholds) {
        RiskDistributionStats distribution = new RiskDistributionStats(thresholds);
        distribution.setKey("system");
        filesWithUnits(files).forEach(file -> add(distribution, file));
        return distribution;
    }

    public static List<RiskDistributionStats> perExtension(List<SourceFile> files, Thresholds thresholds) {
        return perKey(filesWithUnits(files), SourceFile::getExtension, thresholds);
    }

    /** One distribution per key, in the order the keys are first met. */
    public static List<RiskDistributionStats> perKey(List<SourceFile> files, Function<SourceFile, String> key, Thresholds thresholds) {
        Map<String, RiskDistributionStats> map = new LinkedHashMap<>();
        filesWithUnits(files).forEach(file -> add(map.computeIfAbsent(key.apply(file), k -> {
            RiskDistributionStats distribution = new RiskDistributionStats(thresholds);
            distribution.setKey(k);
            return distribution;
        }), file));
        return new ArrayList<>(map.values());
    }

    private static void add(RiskDistributionStats distribution, SourceFile file) {
        distribution.update(file.getUnitsMcCabeIndexSum(), file.getLinesOfCode());
    }
}
