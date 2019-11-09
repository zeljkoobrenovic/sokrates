package nl.obren.sokrates.sourcecode.aspects;

import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class RangeTest {

    private int value = 10;

    @Test
    public void testIsInRangeForInvalidRangeValues() throws Exception {
        assertTrue(new Range().isInRange(value));

        assertFalse(new Range("invalidNumber", "200").isInRange(value));
        assertFalse(new Range("invalidNumber", "100").isInRange(value));
        assertFalse(new Range("11", "invalid").isInRange(value));
    }

    @Test
    public void testIsInRangeForAbsoluteValues() throws Exception {
        assertTrue(new Range().isInRange(value));

        assertTrue(new Range("0", "200").isInRange(value));
        assertTrue(new Range("10", "100").isInRange(value));
        assertFalse(new Range("11", "9").isInRange(value));
    }

    @Test
    public void testIsInRangeForAbsoluteValuesWithTolerance() throws Exception {
        assertTrue(new Range().isInRange(value));

        assertFalse(new Range("10", "100", "10").isInRange(0));
        assertTrue(new Range("10", "100", "10").isInRangeWithTolerance(0));
        assertFalse(new Range("10", "100", "10").isInRangeWithTolerance(-1));
        assertFalse(new Range("10", "100", "10").isInRange(110));
        assertTrue(new Range("10", "100", "10").isInRangeWithTolerance(110));
        assertFalse(new Range("10", "100", "10").isInRangeWithTolerance(111));
    }

    @Test
    public void testIsInRangeForEmptyMinMax() throws Exception {
        assertTrue(new Range().isInRange(value));

        assertTrue(new Range("", "200").isInRange(value));
        assertTrue(new Range("", "100").isInRange(value));
        assertFalse(new Range("", "9").isInRange(value));

        assertTrue(new Range("1", "").isInRange(value));
        assertTrue(new Range("5", "").isInRange(value));
        assertFalse(new Range("11", "").isInRange(value));
    }

}