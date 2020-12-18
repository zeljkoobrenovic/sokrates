package nl.obren.sokrates.sourcecode.filehistory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DateUtilsTest {

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
}