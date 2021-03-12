/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.renderingutils;

import nl.obren.sokrates.common.utils.BasicColorInfo;

import java.util.Arrays;
import java.util.List;

public class Thresholds {
    public static final BasicColorInfo RISK_GREEN = getBasicColorInfo(0.02, 0.40, 0.03);
    public static final BasicColorInfo RISK_YELLOW = getBasicColorInfo(0.855, 1, 0);
    public static final BasicColorInfo RISK_ORANGE = getBasicColorInfo(1, 0.647, 0);
    public static final BasicColorInfo RISK_RED = getBasicColorInfo(0.863, 0.078, 0.235);

    public static final List<Threshold> UNIT_LINES = Arrays.asList(
            new Threshold("0-10", 10, RISK_GREEN),
            new Threshold("0-20", 20, RISK_GREEN),
            new Threshold("21-50", 50, RISK_YELLOW),
            new Threshold("51-100", 100, RISK_ORANGE),
            new Threshold("101+", Integer.MAX_VALUE, RISK_RED)
    );

    public static final List<Threshold> UNIT_MCCABE = Arrays.asList(
            new Threshold("1-5", 5, RISK_GREEN),
            new Threshold("6-10", 10, RISK_YELLOW),
            new Threshold("11-25", 25, RISK_ORANGE),
            new Threshold("26-50", 50, RISK_RED),
            new Threshold("51+", Integer.MAX_VALUE, RISK_RED)
    );

    public static final List<Threshold> UNIT_PARAMS = Arrays.asList(
            new Threshold("0-2", 2, RISK_GREEN),
            new Threshold("3-5", 5, RISK_GREEN),
            new Threshold("6-10", 10, RISK_YELLOW),
            new Threshold("11-15", 15, RISK_ORANGE),
            new Threshold("16+", Integer.MAX_VALUE, RISK_RED)
    );


    private static BasicColorInfo getBasicColorInfo(double r, double g, double b) {
        return new BasicColorInfo(r, g, b);
    }

    public static String[] getColors(List<Threshold> thresholds) {
        String colors[] = new String[thresholds.size()];
        for (int i = 0; i < thresholds.size(); i++) {
            BasicColorInfo color = thresholds.get(i).getColor();
            colors[i] = String.format("#%02X%02X%02X",
                    (int) (color.getRed() * 255),
                    (int) (color.getGreen() * 255),
                    (int) (color.getBlue() * 255));
        }

        return colors;
    }

    public static BasicColorInfo getColor(List<Threshold> thresholds, Number value) {
        Number prevValue = 0;
        for (Threshold threshold : thresholds) {
            if (value.doubleValue() >= prevValue.doubleValue()
                    && value.doubleValue() <= threshold.getThreshold().doubleValue()) {
                return threshold.getColor();
            }
            prevValue = threshold.getThreshold();
        }
        return new BasicColorInfo(0.0, 0.0, 0.0);
    }
}
