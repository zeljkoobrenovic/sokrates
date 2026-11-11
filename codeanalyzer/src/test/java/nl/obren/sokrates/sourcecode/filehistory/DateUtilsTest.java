package nl.obren.sokrates.sourcecode.filehistory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilsTest {

    @BeforeEach
    @AfterEach
    void resetState() {
        // Memoization caches and the analysis-date override are static; isolate each test.
        DateUtils.dateParam = null;
        DateUtils.reset();
    }

    @Test
    void isCommittedBetween() {
        DateUtils.dateParam = "2020-10-01";

        assertFalse(DateUtils.isCommittedBetween("2020-09-10", 10, 20));
        assertTrue(DateUtils.isCommittedBetween("2020-09-11", 10, 20));
        assertTrue(DateUtils.isCommittedBetween("2020-09-12", 10, 20));
        assertTrue(DateUtils.isCommittedBetween("2020-09-20", 10, 20));
        assertTrue(DateUtils.isCommittedBetween("2020-09-21", 10, 20));
        assertFalse(DateUtils.isCommittedBetween("2020-09-22", 10, 20));
        assertTrue(DateUtils.isCommittedBetween("2020-09-22", 0, 10));

        assertTrue(DateUtils.isCommittedBetween("2020-10-01", 0, 30));
    }

    @Test
    void isCommittedLessThanDaysAgo() {
        DateUtils.dateParam = "2020-10-01";
        assertTrue(DateUtils.isCommittedLessThanDaysAgo("2020-09-22", 10));
        assertTrue(DateUtils.isCommittedLessThanDaysAgo("2020-09-21", 10));
        assertFalse(DateUtils.isCommittedLessThanDaysAgo("2020-09-19", 10));
        assertFalse(DateUtils.isCommittedLessThanDaysAgo("2020-09-18", 10));
    }

    @Test
    void isDateWithinRange() {
        DateUtils.dateParam = "2020-10-01";

        assertTrue(DateUtils.isDateWithinRange("2020-10-01", 30));
        assertTrue(DateUtils.isDateWithinRange("2020-09-15", 30));
        assertFalse(DateUtils.isDateWithinRange("2020-08-01", 30));
    }

    @Test
    void isDateWithinRangeTreatsBlankAsInRange() {
        DateUtils.dateParam = "2020-10-01";
        assertTrue(DateUtils.isDateWithinRange("", 30));
        assertTrue(DateUtils.isDateWithinRange(null, 30));
    }

    @Test
    void isAnyDateCommittedBetween() {
        DateUtils.dateParam = "2020-10-01";

        assertTrue(DateUtils.isAnyDateCommittedBetween(List.of("2020-01-01", "2020-09-25"), 0, 10));
        assertFalse(DateUtils.isAnyDateCommittedBetween(List.of("2020-01-01", "2020-02-01"), 0, 10));
    }

    @Test
    void getMonthAndYearExtractPrefixes() {
        assertEquals("2021-06", DateUtils.getMonth("2021-06-15"));
        assertEquals("2021", DateUtils.getYear("2021-06-15"));
        assertEquals("", DateUtils.getYear("21"));
    }

    @Test
    void getWeekMondayReturnsMondayOfThatWeek() {
        // 2021-06-16 is a Wednesday; its week's Monday is 2021-06-14.
        assertEquals("2021-06-14", DateUtils.getWeekMonday("2021-06-16"));
        // A Monday maps to itself.
        assertEquals("2021-06-14", DateUtils.getWeekMonday("2021-06-14"));
    }

    @Test
    void getPastDaysCountsBackInclusivelyFromGivenDate() {
        List<String> days = DateUtils.getPastDays(3, "2021-06-15");

        assertEquals(4, days.size(), "0..numberOfDays inclusive");
        assertEquals("2021-06-15", days.get(0));
        assertEquals("2021-06-12", days.get(3));
    }

    @Test
    void getAnalysisDateHonoursDateParam() {
        DateUtils.dateParam = "2019-05-20";
        assertEquals("2019-05-20", DateUtils.getAnalysisDate());
        assertEquals(2019, DateUtils.getAnalysisYear());
    }
}