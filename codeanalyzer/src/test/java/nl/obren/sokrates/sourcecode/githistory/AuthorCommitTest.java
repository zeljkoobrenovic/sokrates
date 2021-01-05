package nl.obren.sokrates.sourcecode.githistory;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class AuthorCommitTest {

    @Test
    public void getMonth() {
        AuthorCommit authorCommit = new AuthorCommit("2020-12-12 12:10:11", "email@host.com");
        assertEquals("2020-12", authorCommit.getMonth());
    }

    @Test
    public void getDate() {
        AuthorCommit authorCommit = new AuthorCommit("2020-12-12", "email@host.com");
        assertEquals("2020-12-12", authorCommit.getDate());
    }

    @Test
    public void getDateObject() {
        AuthorCommit authorCommit = new AuthorCommit("2020-12-12", "email@host.com");
        Calendar calendar = authorCommit.getCalendar();
        assertEquals(2020, calendar.get(Calendar.YEAR));
        assertEquals(11, calendar.get(Calendar.MONTH));
        assertEquals(12, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals(50, calendar.get(Calendar.WEEK_OF_YEAR));
    }
    @Test
    public void getWeekOfYear() {
        AuthorCommit authorCommit = new AuthorCommit("2020-12-12", "email@host.com");
        String weekOfYear = authorCommit.getWeekOfYear();
        assertEquals("2020-12-07", weekOfYear);
    }
}