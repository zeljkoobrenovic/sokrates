/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.utils;

import junit.framework.TestCase;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class FormattingUtilsTest {
    @Test
    public void getFormattedPercentage() throws Exception {
        TestCase.assertEquals(FormattingUtils.getFormattedPercentage(100), "100");
        assertEquals(FormattingUtils.getFormattedPercentage(1), "1");
        assertEquals(FormattingUtils.getFormattedPercentage(0), "0");
        assertEquals(FormattingUtils.getFormattedPercentage(0.4), "<1");
        assertEquals(FormattingUtils.getFormattedPercentage(0.5), "<1");
        assertEquals(FormattingUtils.getFormattedPercentage(0.6), "<1");
        assertEquals(FormattingUtils.getFormattedPercentage(25), "25");
        assertEquals(FormattingUtils.getFormattedPercentage(25.5), "25");
        assertEquals(FormattingUtils.getFormattedPercentage(25.55467456), "25");
    }

    @Test
    public void getFormattedRemainderPercentage() throws Exception {
        assertEquals(FormattingUtils.getFormattedRemainderPercentage(100), "0");
        assertEquals(FormattingUtils.getFormattedRemainderPercentage(1), "99");
        assertEquals(FormattingUtils.getFormattedRemainderPercentage(0), "100");
        assertEquals(FormattingUtils.getFormattedRemainderPercentage(0.4), ">99");
        assertEquals(FormattingUtils.getFormattedRemainderPercentage(0.5), ">99");
        assertEquals(FormattingUtils.getFormattedRemainderPercentage(0.6), ">99");
        assertEquals(FormattingUtils.getFormattedRemainderPercentage(25), "75");
        assertEquals(FormattingUtils.getFormattedRemainderPercentage(25.5), "75");
        assertEquals(FormattingUtils.getFormattedRemainderPercentage(25.55467456), "75");
    }

    @Test
    public void getFormattedCount() throws Exception {
        assertEquals(FormattingUtils.formatCount(100), "100");
        assertEquals(FormattingUtils.formatCount(1000), "1,000");
        assertEquals(FormattingUtils.formatCount(10000), "10,000");
        assertEquals(FormattingUtils.formatCount(100000), "100,000");
        assertEquals(FormattingUtils.formatCount(1000000), "1,000,000");
        assertEquals(FormattingUtils.formatCount(10000000), "10,000,000");
        assertEquals(FormattingUtils.formatCount(10), "10");
        assertEquals(FormattingUtils.formatCount(1), "1");
        assertEquals(FormattingUtils.formatCount(0), "0");
    }

    @Test
    public void getSmallTextForNumber() {
        assertEquals(FormattingUtils.getSmallTextForNumber(100), "<b>100</b>");
        assertEquals(FormattingUtils.getSmallTextForNumber(999), "<b>999</b>");
        assertEquals(FormattingUtils.getSmallTextForNumber(1000), "<b>1</b>K");
        assertEquals(FormattingUtils.getSmallTextForNumber(1807), "<b>1.8</b>K");
        assertEquals(FormattingUtils.getSmallTextForNumber(1999), "<b>2</b>K");
        assertEquals(FormattingUtils.getSmallTextForNumber(9999), "<b>10</b>K");
        assertEquals(FormattingUtils.getSmallTextForNumber(9899), "<b>9.9</b>K");
        assertEquals(FormattingUtils.getSmallTextForNumber(10000), "<b>10</b>K");
        assertEquals(FormattingUtils.getSmallTextForNumber(18070), "<b>18</b>K");
        assertEquals(FormattingUtils.getSmallTextForNumber(19990), "<b>20</b>K");
        assertEquals(FormattingUtils.getSmallTextForNumber(89990), "<b>90</b>K");
        assertEquals(FormattingUtils.getSmallTextForNumber(99990), "<b>100</b>K");
        assertEquals(FormattingUtils.getSmallTextForNumber(1000000), "<b>1</b>M");
        assertEquals(FormattingUtils.getSmallTextForNumber(1807000), "<b>1.8</b>M");
        assertEquals(FormattingUtils.getSmallTextForNumber(1999000), "<b>2</b>M");
        assertEquals(FormattingUtils.getSmallTextForNumber(9800000), "<b>9.8</b>M");
        assertEquals(FormattingUtils.getSmallTextForNumber(19800000), "<b>20</b>M");
    }


    @Test
    public void formatPeriod() {
        assertEquals(FormattingUtils.formatPeriod(0), "less than a month");
        assertEquals(FormattingUtils.formatPeriod(10), "less than a month");
        assertEquals(FormattingUtils.formatPeriod(30), "1 month");
        assertEquals(FormattingUtils.formatPeriod(60), "2 months");
        assertEquals(FormattingUtils.formatPeriod(91), "3 months");
        assertEquals(FormattingUtils.formatPeriod(365), "1 year");
        assertEquals(FormattingUtils.formatPeriod(367), "1 year");
        assertEquals(FormattingUtils.formatPeriod(400), "1 year, 1 month");
        assertEquals(FormattingUtils.formatPeriod(440), "1 year, 2 months");
        assertEquals(FormattingUtils.formatPeriod(1440), "3 years, 11 months");
    }
}
