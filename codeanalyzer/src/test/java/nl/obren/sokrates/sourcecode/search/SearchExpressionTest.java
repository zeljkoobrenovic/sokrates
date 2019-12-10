/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.search;

import org.junit.Test;

import static org.junit.Assert.*;

public class SearchExpressionTest {
    @Test
    public void getMatchedRegex() throws Exception {
        SearchExpression searchExpression = new SearchExpression("abc.*d");

        assertEquals(searchExpression.getMatchedRegex("Test abcdefghijklmnoprsguvxyz"), "abcd");
        assertEquals(searchExpression.getMatchedRegex("Test abc e fgd hijklmnoprsguvxyz"), "abc e fgd");

        assertNull(searchExpression.getMatchedRegex("Test"));
        assertNull(searchExpression.getMatchedRegex(""));
    }

}
