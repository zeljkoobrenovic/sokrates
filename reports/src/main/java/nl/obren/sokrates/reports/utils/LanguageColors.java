/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * Deterministic color per programming language, for color-coding repositories by their main
 * language (e.g. the landscape Overview tab's repositories circle-packing chart and its legend).
 *
 * <p>There is no curated language→color map: the hue is derived by hashing the (lowercased)
 * language name, with fixed saturation/lightness for readable, distinguishable pastels. The same
 * language always yields the same color (stable across runs and JVMs), so the chart bubbles and the
 * legend swatches always match.
 */
public class LanguageColors {

    private LanguageColors() {
    }

    /**
     * @param language a language name (e.g. "java", "python"); blank/null returns a neutral grey.
     * @return a CSS {@code hsl(...)} color string.
     */
    public static String getColor(String language) {
        if (StringUtils.isBlank(language)) {
            return "hsl(0, 0%, 80%)";
        }
        int hue = Math.floorMod(language.toLowerCase().trim().hashCode(), 360);
        // Fixed S/L → distinguishable but not garish; light enough that black labels read on top.
        return "hsl(" + hue + ", 60%, 70%)";
    }
}
