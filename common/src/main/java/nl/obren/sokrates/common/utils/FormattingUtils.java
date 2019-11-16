package nl.obren.sokrates.common.utils;

import java.text.DecimalFormat;

public class FormattingUtils {
    public static String getFormattedPercentage(double percentage) {
        return percentage < 0.0000000000000000000001
                ? "0"
                : percentage < 1.0
                ? "<1"
                : ("" + (int) percentage);
    }

    public static String getFormattedRemainderPercentage(double percentage) {
        return percentage == 0 ? "100" : percentage < 1 ? ">99" : "" + (100 - (int) percentage);
    }

    public static String getFormattedCount(int value) {
        return new DecimalFormat("#,###").format(value);
    }
}
