package nl.obren.sokrates.reports.generators.explorers;

import nl.obren.sokrates.sourcecode.threshold.Thresholds;

/**
 * Small shared helpers for the files/units explorer generators.
 */
public class FilesExportUtils {

    /**
     * Serializes risk thresholds as a {@code {low, medium, high, veryHigh}} JSON object literal for
     * embedding into an explorer template (used to colour-code risk bands client-side).
     */
    public static String thresholdsJson(Thresholds thresholds) {
        return "{\"low\": " + thresholds.getLow()
                + ", \"medium\": " + thresholds.getMedium()
                + ", \"high\": " + thresholds.getHigh()
                + ", \"veryHigh\": " + thresholds.getVeryHigh() + "}";
    }
}
