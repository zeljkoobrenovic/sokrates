/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.filehistory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class FileHistoryUtils {
    public static Date getDateFromString(String dateString) {
        if (dateString.length() >= 10) {
            try {
                return new SimpleDateFormat("yyyy-MM-dd").parse(dateString.substring(0, 10));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public static int daysBetween(Date date1, Date date2) {
        return 1 + (int) TimeUnit.DAYS.convert(Math.abs(date1.getTime() - date2.getTime()), TimeUnit.MILLISECONDS);
    }

    public static int daysFromToday(String dateString) {
        Date today = new Date();

        Date fileDate = getDateFromString(dateString);
        if (fileDate != null) {
            return daysBetween(today, fileDate);
        }

        return 0;
    }
}
