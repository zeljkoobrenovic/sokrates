package nl.obren.sokrates.reports.landscape.utils;

import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.landscape.MergeExtension;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LandscapeGeneratorUtils {
    public static List<NumericMetric> getLinesOfCodePerExtension(LandscapeAnalysisResults landscapeAnalysisResults, List<NumericMetric> linesOfCodePerExtension) {
        LandscapeConfiguration config = landscapeAnalysisResults.getConfiguration();
        int threshold = config.getExtensionThresholdLoc();
        List<NumericMetric> mergedLinesOfCodePerExtension = new ArrayList<>();

        Map<String,NumericMetric> map = new HashMap<>();
        linesOfCodePerExtension.forEach(ext -> map.put(ext.getName().replaceAll(".*[.]", ""), ext));

        Map<String, MergeExtension> mapMerge = new HashMap<>();
        config.getMergeExtensions().forEach(merge -> mapMerge.put(merge.getSecondary(), merge));

        linesOfCodePerExtension.forEach(ext -> {
            String key = ext.getName().replaceAll(".*[.]", "");
            if (mapMerge.containsKey(key) && map.containsKey(mapMerge.get(key).getPrimary())) {
                NumericMetric primary = map.get(mapMerge.get(key).getPrimary());
                NumericMetric secondary = map.get(key);
                primary.setValue(primary.getValue().intValue() + secondary.getValue().intValue());
                primary.getDescription().addAll(secondary.getDescription());
            } else {
                mergedLinesOfCodePerExtension.add(ext);
            }
        });

        return mergedLinesOfCodePerExtension.stream()
                .filter(e -> !ignoreExtension(landscapeAnalysisResults, e.getName().replaceAll(".*[.]", "")))
                .filter(e -> !e.getName().endsWith("="))
                .filter(e -> !e.getName().startsWith("h-"))
                .filter(e -> e.getValue().intValue() >= threshold)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static boolean ignoreExtension(LandscapeAnalysisResults landscapeAnalysisResults, String extension) {
        return landscapeAnalysisResults.getConfiguration().getIgnoreExtensions().contains(extension);
    }
}
