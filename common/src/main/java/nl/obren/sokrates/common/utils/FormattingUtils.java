/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.utils;

import java.text.DecimalFormat;

public class FormattingUtils {
    public static String getFormattedPercentage(double percentage) {
        return getFormattedPercentage(percentage, "0");
    }

    public static String getFormattedPercentage(double percentage, String textForZero) {
        return percentage < 0.0000000000000000000001
                ? textForZero
                : percentage < 1.0
                ? "<1"
                : ("" + (int) percentage);
    }

    public static String getFormattedRemainderPercentage(double percentage) {
        return percentage == 0 ? "100" : percentage < 1 ? ">99" : "" + (100 - (int) percentage);
    }

    public static String formatCount(int value) {
        return new DecimalFormat("#,###").format(value);
    }

    public static String formatCount(int value, String textForZero) {
        return value == 0 ? textForZero : new DecimalFormat("#,###").format(value);
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

    public static String getPlainTextForNumber(int number) {
        return getSmallTextForNumber(number).replaceAll("<.*?>", "");
    }

    private static String removeZeroDecimalIfNecessary(String formattedNumber) {
        if (formattedNumber.endsWith(".0")) {
            formattedNumber = formattedNumber.substring(0, formattedNumber.length() - 2);
        }
        return formattedNumber;
    }


    public static String formatPeriod(int ageInDays) {
        int years = ageInDays / 365;
        int rest = ageInDays % 365;
        int months = rest / 30;


        if (years == 0 && months == 0) {
            return "less than a month";
        }

        String text = "";
        if (years == 1) {
            text += "1 year";
        } else if (years > 1) {
            text += years + " years";
        }

        if (months > 0) {
            if (text.length() > 0) {
                text += ", ";
            }
            if (months == 1) {
                text += "1 month";
            } else if (months > 1) {
                text += months + " months";
            } else {

            }
        }

        return text;
    }
}
