/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.filehistory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DateUtils {
    public static final String ENV_SOKRATES_SOURCE_CODE_DATE = "SOKRATES_SOURCE_CODE_DATE";

    public static boolean isDateWithinRange(String date, int rangeInDays) {
        if (StringUtils.isBlank(date)) {
            return true;
        }

        Calendar cal = getCalendar();

        cal.add(Calendar.DATE, -rangeInDays);

        String thresholdDate = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());

        return date.compareTo(thresholdDate) > 0;
    }

    public static Calendar getCalendar() {
        Calendar cal = Calendar.getInstance();

        String sourceCodeDate = System.getenv(ENV_SOKRATES_SOURCE_CODE_DATE);
        if (StringUtils.isNotBlank(sourceCodeDate)) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            try {
                cal.setTime(sdf.parse(sourceCodeDate));// all done
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return cal;
    }


}
