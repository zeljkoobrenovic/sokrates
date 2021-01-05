/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.filehistory;

import nl.obren.sokrates.sourcecode.githistory.AuthorCommit;
import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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

    public static List<String> getPastDays(int numberOfDays) {
        List<String> dates = new ArrayList<>();


        for (int i = 0; i <= numberOfDays; i++) {
            Calendar cal = getCalendar();
            cal.add(Calendar.DATE, -i);
            dates.add(new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime()));
        }

        return dates;
    }

    public static List<String> getPastWeeks(int numberOfWeeks) {
        List<String> dates = new ArrayList<>();


        for (int i = 0; i <= numberOfWeeks; i++) {
            Calendar cal = getCalendar();
            cal.add(Calendar.DATE, -(i * 7));
            String stringDate = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
            dates.add(new AuthorCommit(stringDate, "").getWeekOfYear());
        }

        return dates;
    }

    public static boolean isCommittedLessThanDaysAgo(String date, int daysAgo) {
        return isCommittedBetween(date, 0, daysAgo);
    }

    public static boolean isAnyDateCommittedBetween(List<String> dates, int daysAgo1, int daysAgo2) {
        for (String date : dates) {
            if (isCommittedBetween(date, daysAgo1, daysAgo2)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCommittedBetween(String date, int daysAgo1, int daysAgo2) {
        Calendar cal1 = DateUtils.getCalendar();
        cal1.add(Calendar.DATE, -daysAgo1);
        String thresholdDate1 = new SimpleDateFormat("yyyy-MM-dd").format(cal1.getTime());

        Calendar cal2 = DateUtils.getCalendar();
        cal2.add(Calendar.DATE, -daysAgo2);
        String thresholdDate2 = new SimpleDateFormat("yyyy-MM-dd").format(cal2.getTime());

        return date.compareTo(thresholdDate2) >= 0 && date.compareTo(thresholdDate1) <= 0;
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
