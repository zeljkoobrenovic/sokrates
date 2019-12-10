/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.analysis;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FindingTest {

    @Test
    public void testToString() throws Exception {
        assertEquals(Finding.newNegativeFinding("This is bad.").toString(), "- This is bad.");
        assertEquals(Finding.newPositiveFinding("This is rather good.").toString(), "+ This is rather good.");
        assertEquals(Finding.newNeutralFinding("This is OK.").toString(), "± This is OK.");
    }

    @Test
    public void testToStringWithType() throws Exception {
        assertEquals(Finding.newNegativeFinding("This is bad.").setType("GROUP").toString(), "- (GROUP) This is bad.");
        assertEquals(Finding.newPositiveFinding("This is rather good.").setType("GROUP").toString(), "+ (GROUP) This is rather good.");
        assertEquals(Finding.newNeutralFinding("This is OK.").setType("GROUP").toString(), "± (GROUP) This is OK.");
    }
}
