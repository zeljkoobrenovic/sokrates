package nl.obren.sokrates.sourcecode.githistory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class AuthorCommit {
    private String date = "";
    private String authorEmail = "";

    public AuthorCommit() {
    }

    public AuthorCommit(String date, String authorEmail) {
        this.date = date;
        this.authorEmail = authorEmail;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    @JsonIgnore
    public String getYear() {
        return getDate().length() >= 4 ? getDate().substring(0, 4) : "";
    }

    @JsonIgnore
    public String getMonth() {
        return getDate().length() >= 7 ? getDate().substring(0, 7) : "";
    }

    @JsonIgnore
    public Calendar getCalendar() {
        Calendar calendar = Calendar.getInstance();
        if (this.date.length() >= 10) {
            String string = date.substring(0, 10);
            String elements[] = string.split("-");
            if (elements.length == 3) {
                if (StringUtils.isNumeric(elements[0]) && StringUtils.isNumeric(elements[1]) && StringUtils.isNumeric(elements[2])) {
                    calendar.set(Calendar.YEAR, Integer.parseInt(elements[0]));
                    calendar.set(Calendar.MONTH, Integer.parseInt(elements[1]) - 1);
                    calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(elements[2]));

                    return calendar;
                }
            }
        }
        return null;
    }

    public String getWeekOfYear() {
        Calendar calendar = getCalendar();

        if (calendar != null) {
            while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                calendar.add(Calendar.DATE, -1);
            }
            return new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime());
        }

        return "";
    }
}
