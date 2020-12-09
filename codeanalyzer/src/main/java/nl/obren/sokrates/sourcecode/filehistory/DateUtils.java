/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.filehistory;

import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DateUtils {
    public static final String ENV_SOKRATES_SOURCE_CODE_DATE = "SOKRATES_SOURCE_CODE_DATE";
    public static String dateParam = null;

    public static boolean isDateWithinRange(String date, int rangeInDays) {
        if (StringUtils.isBlank(date)) {
            return true;
        }

        Calendar cal = getCalendar();

        cal.add(Calendar.DATE, -rangeInDays);

        String thresholdDate = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());

        return date.compareTo(thresholdDate) > 0;
    }

    public static boolean isCommittedLessThanDaysAgo(String date, int daysAgo) {
        Calendar cal = DateUtils.getCalendar();
        cal.add(Calendar.DATE, -daysAgo);

        String thresholdDate = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());

        return date.compareTo(thresholdDate) > 0;
    }


    public static Calendar getCalendar() {
        Calendar cal = Calendar.getInstance();

        updateWithDateParams(cal);

        return cal;
    }

    private static void updateWithDateParams(Calendar cal) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            if (StringUtils.isNotBlank(DateUtils.dateParam)) {
                cal.setTime(sdf.parse(DateUtils.dateParam));
            } else {
                String sourceCodeDate = System.getenv(ENV_SOKRATES_SOURCE_CODE_DATE);
                if (StringUtils.isNotBlank(sourceCodeDate)) {
                    cal.setTime(sdf.parse(sourceCodeDate));
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public static String getDateParam() {
        return dateParam;
    }

    public static void setDateParam(String dateParam) {
        DateUtils.dateParam = dateParam;
    }
}
