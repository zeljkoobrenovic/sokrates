/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.utils;

import junit.framework.TestCase;
import nl.obren.sokrates.common.utils.FormattingUtils;
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
        assertEquals(FormattingUtils.getFormattedCount(100), "100");
        assertEquals(FormattingUtils.getFormattedCount(1000), "1,000");
        assertEquals(FormattingUtils.getFormattedCount(10000), "10,000");
        assertEquals(FormattingUtils.getFormattedCount(100000), "100,000");
        assertEquals(FormattingUtils.getFormattedCount(1000000), "1,000,000");
        assertEquals(FormattingUtils.getFormattedCount(10000000), "10,000,000");
        assertEquals(FormattingUtils.getFormattedCount(10), "10");
        assertEquals(FormattingUtils.getFormattedCount(1), "1");
        assertEquals(FormattingUtils.getFormattedCount(0), "0");
    }

}
