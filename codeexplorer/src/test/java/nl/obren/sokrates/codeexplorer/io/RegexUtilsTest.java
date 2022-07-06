/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.codeexplorer.io;

import nl.obren.sokrates.common.utils.RegexUtils;
import org.junit.Test;

import java.util.Arrays;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class RegexUtilsTest {
    @Test
    public void matchesAnyPattern() throws Exception {
        assertTrue(RegexUtils.matchesAnyPattern("abcd", Arrays.asList("a.*", "b.*", "c.*")));
        assertTrue(RegexUtils.matchesAnyPattern("abcd", Arrays.asList("d.*", ".*b.*", "c.*")));

        assertFalse(RegexUtils.matchesAnyPattern("abcd", Arrays.asList("b.*", "c.*", "d.*")));
    }

    @Test
    public void matchesEntirely() throws Exception {
        assertTrue(RegexUtils.matchesEntirely("A.*", "ABC"));
        assertTrue(RegexUtils.matchesEntirely(".*B.*", "ABC"));
        assertTrue(RegexUtils.matchesEntirely(".*C", "ABC"));

        assertFalse(RegexUtils.matchesEntirely("A.*D", "ABC"));
        assertFalse(RegexUtils.matchesEntirely("B.*", "ABC"));

        assertTrue(RegexUtils.matchesEntirely("A.*", "ABC DG"));
        assertTrue(RegexUtils.matchesEntirely(".*B.*", "ABC DG"));
        assertTrue(RegexUtils.matchesEntirely(".*DG", "ABC DG"));
        assertTrue(RegexUtils.matchesEntirely(".*", "ABC DG"));

        assertFalse(RegexUtils.matchesEntirely("B.*", "ABC DG"));
        assertFalse(RegexUtils.matchesEntirely(".*Z.*", "ABC DG"));
        assertFalse(RegexUtils.matchesEntirely(".*Z","ABC DG"));

        assertTrue(RegexUtils.matchesEntirely(".*", "GHH7k/Repos/DFF/Source/Prg_999.xml"));
    }

}
