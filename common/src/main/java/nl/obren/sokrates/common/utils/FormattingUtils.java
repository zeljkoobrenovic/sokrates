/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

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

    public static String getSmallTextForNumber(int number) {
        if (number < 1000) {
            return "<b>" + number + "</b>" + "";
        } else if (number < 10000) {
            return "<b>" + removeZeroDecimalIfNecessary(String.format("%.1f", number / 1000f)) + "</b>" + "K";
        } else if (number < 1000000) {
            return "<b>" + Math.round(number / 1000f) + "</b>" + "K";
        } else if (number < 10000000) {
            return "<b>" + removeZeroDecimalIfNecessary(String.format("%.1f", number / 1000000f)) + "</b>" + "M";
        } else {
            return "<b>" + Math.round(number / 1000000f) + "</b>" + "M";
        }
    }

    private static String removeZeroDecimalIfNecessary(String formattedNumber) {
        if (formattedNumber.endsWith(".0")) {
            formattedNumber = formattedNumber.substring(0, formattedNumber.length() - 2);
        }
        return formattedNumber;
    }


}
