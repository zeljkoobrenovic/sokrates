package nl.obren.sokrates.sourcecode.search;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class ReplacePairTest {
    @Test
    public void replaceIn() throws Exception {
        assertEquals(new ReplacePair("", "").replaceIn(""), "");
        assertEquals(new ReplacePair("A", "B").replaceIn("ABC"), "BBC");
        assertEquals(new ReplacePair("A.*", "_").replaceIn("QWERTYABCDEABC"), "QWERTY_");
        assertEquals(new ReplacePair("A.*C", "_").replaceIn("QWERTYABCDEABC"), "QWERTY_");
        assertEquals(new ReplacePair("A.*?C", "_").replaceIn("QWERTYABCDEABC"), "QWERTY_DE_");
    }

}