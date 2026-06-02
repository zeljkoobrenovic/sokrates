package nl.obren.sokrates.sourcecode.filehistory;

import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class FileHistoryUtilsTest {

    @Test
    void getDateFromStringParsesLeadingDate() throws Exception {
        Date expected = new SimpleDateFormat("yyyy-MM-dd").parse("2021-06-15");
        assertEquals(expected, FileHistoryUtils.getDateFromString("2021-06-15"));
        // a full timestamp is truncated to the date portion
        assertEquals(expected, FileHistoryUtils.getDateFromString("2021-06-15 12:34:56"));
    }

    @Test
    void getDateFromStringReturnsNullForTooShortInput() {
        assertNull(FileHistoryUtils.getDateFromString("2021-06"));
        assertNull(FileHistoryUtils.getDateFromString(""));
    }

    @Test
    void daysBetweenIsInclusiveAndOrderIndependent() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date d1 = sdf.parse("2021-01-01");
        Date d2 = sdf.parse("2021-01-01");
        Date d3 = sdf.parse("2021-01-02");
        Date d4 = sdf.parse("2021-01-11");

        assertEquals(1, FileHistoryUtils.daysBetween(d1, d2), "same day -> 1 (inclusive)");
        assertEquals(2, FileHistoryUtils.daysBetween(d1, d3), "one day apart -> 2");
        assertEquals(2, FileHistoryUtils.daysBetween(d3, d1), "absolute difference, order independent");
        assertEquals(11, FileHistoryUtils.daysBetween(d1, d4));
    }

    @Test
    void daysFromTodayIsPositiveForPastDateAndZeroForUnparseable() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -10);
        String tenDaysAgo = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());

        int days = FileHistoryUtils.daysFromToday(tenDaysAgo);
        // 10 calendar days ago is 11 inclusive days; allow ±1 for clock/DST edges.
        assertTrue(days >= 10 && days <= 12, "expected ~11 inclusive days, got " + days);

        assertEquals(0, FileHistoryUtils.daysFromToday("bad"), "unparseable date -> 0");
    }
}
