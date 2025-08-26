/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.filehistory;

import nl.obren.sokrates.sourcecode.githistory.AuthorCommit;
import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class DateUtils {
    public static final String ENV_SOKRATES_ANALYSIS_DATE = "SOKRATES_ANALYSIS_DATE";
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static String dateParam = null;
    private static String latestCommitDate = "";

    private static Map<String, Boolean> dateInRangeCache = new HashMap<>();
    private static Map<String, Boolean> dateBetweenCache = new HashMap<>();
    private static Map<String, String> mondays = new HashMap<>();

    public static boolean isDateWithinRange(String date, int rangeInDays) {
        if (StringUtils.isBlank(date)) {
            return true;
        }

        String key = date + "[" + rangeInDays + "]";
        if (dateInRangeCache.containsKey(key)) {
            return dateInRangeCache.get(key);
        }

        Calendar cal = getCalendar();

        cal.add(Calendar.DATE, -rangeInDays);

        String thresholdDate = new SimpleDateFormat(DATE_FORMAT).format(cal.getTime());

        boolean inRange = date.compareTo(thresholdDate) >= 0;
        dateInRangeCache.put(key, inRange);

        return inRange;
    }

    public static List<String> getPastDays(int numberOfDays, String latestCommitDate) {
        List<String> dates = new ArrayList<>();

        for (int i = 0; i <= numberOfDays; i++) {
            Calendar cal = getCalendar(latestCommitDate);
            cal.add(Calendar.DATE, -i);
            dates.add(new SimpleDateFormat(DATE_FORMAT).format(cal.getTime()));
        }

        return dates;
    }

    public static List<String> getPastWeeks(int numberOfWeeks, String latestCommitDate) {
        List<String> dates = new ArrayList<>();

        if (StringUtils.isNotEmpty(latestCommitDate)) {
            for (int i = 0; i <= numberOfWeeks; i++) {
                Calendar cal = getCalendar(latestCommitDate);
                cal.add(Calendar.DATE, -(i * 7));

                String stringDate = new SimpleDateFormat(DATE_FORMAT).format(cal.getTime());
                dates.add(new AuthorCommit(stringDate, "", "", false).getWeekOfYear());
            }
        }

        return dates;
    }

    public static List<String> getPastMonths(int numberOfMonths, String latestCommitDate) {
        List<String> dates = new ArrayList<>();

        for (int i = 0; i <= numberOfMonths; i++) {
            Calendar cal = getCalendar(latestCommitDate);
            cal.add(Calendar.MONTH, -i);

            String stringDate = new SimpleDateFormat(DATE_FORMAT).format(cal.getTime());
            dates.add(new AuthorCommit(stringDate, "", "", false).getMonth());
        }

        return dates;
    }

    public static List<String> getPastYears(int numberOfYears, String latestCommitDate) {
        List<String> dates = new ArrayList<>();

        for (int i = 0; i <= numberOfYears; i++) {
            Calendar cal = getCalendar(latestCommitDate);
            cal.add(Calendar.YEAR, -i);

            String stringDate = new SimpleDateFormat(DATE_FORMAT).format(cal.getTime());
            dates.add(new AuthorCommit(stringDate, "", "", false).getYear());
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
        String key = date + "[" + daysAgo1 + ":" + daysAgo2 + "]";
        if (dateBetweenCache.containsKey(key)) {
            return dateBetweenCache.get(key);
        }

        Calendar cal1 = DateUtils.getCalendar();
        cal1.add(Calendar.DATE, -daysAgo1);
        String thresholdDate1 = new SimpleDateFormat(DATE_FORMAT).format(cal1.getTime());

        Calendar cal2 = DateUtils.getCalendar();
        cal2.add(Calendar.DATE, -daysAgo2);
        String thresholdDate2 = new SimpleDateFormat(DATE_FORMAT).format(cal2.getTime());

        boolean inRange = date.compareTo(thresholdDate2) >= 0 && date.compareTo(thresholdDate1) <= 0;
        dateBetweenCache.put(key, inRange);

        return inRange;
    }

    public static Calendar getCalendar() {
        Calendar calendar = Calendar.getInstance();

        updateWithDateParams(calendar);

        return calendar;
    }

    public static String getAnalysisDate() {
        Calendar calendar = Calendar.getInstance();

        updateWithDateParams(calendar);

        return new SimpleDateFormat(DATE_FORMAT).format(calendar.getTime());
    }

    public static int getAnalysisYear() {
        Calendar calendar = Calendar.getInstance();

        updateWithDateParams(calendar);

        return calendar.get(Calendar.YEAR);
    }

    public static Calendar getCalendar(String date) {
        Calendar calendar = Calendar.getInstance();

        if (StringUtils.isNotBlank(date)) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
                calendar.setTime(sdf.parse(date));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return calendar;
    }

    private static void updateWithDateParams(Calendar cal) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
            if (StringUtils.isNotBlank(DateUtils.dateParam)) {
                cal.setTime(sdf.parse(DateUtils.dateParam));
            } else {
                String sourceCodeDate = System.getenv(ENV_SOKRATES_ANALYSIS_DATE);
                if (StringUtils.isNotBlank(sourceCodeDate)) {
                    cal.setTime(sdf.parse(sourceCodeDate));
                } else if (StringUtils.isNotBlank(latestCommitDate)) {
                    cal.setTime(sdf.parse(latestCommitDate));
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

    public static String getLatestCommitDate() {
        return latestCommitDate;
    }

    public static void setLatestCommitDate(String latestCommitDate) {
        DateUtils.latestCommitDate = latestCommitDate;
    }


    public static String getWeekMonday(String date) {
        if (mondays.containsKey(date)) {
            return mondays.get(date);
        }
        Calendar calendar = getCalendar(date);

        if (calendar != null) {
            while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                calendar.add(Calendar.DATE, -1);
            }
            String formatedDate = new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime());
            mondays.put(date, formatedDate);
            return formatedDate;
        }

        return "";
    }

    public static String getMonth(String date) {
        return date.substring(0, 7);
    }

    public static String getYear(String date) {
        return date.length() > 4 ? date.substring(0, 4) : "";
    }

    public static void reset() {
        dateBetweenCache.clear();
        dateInRangeCache.clear();
        mondays.clear();
    }
}
