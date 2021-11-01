/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.search;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class SearchResultCleanerTest {

    @Test
    public void clean() throws Exception {
        SearchResultCleaner cleaner = new SearchResultCleaner();

        // test no cleaning by default
        assertEquals(cleaner.clean("ABC"), "ABC");
        assertEquals(cleaner.clean(""), "");

        cleaner.setStartCleaningPattern("import ");
        cleaner.setEndCleaningPattern(";");

        cleaner.setReplacePairs(Arrays.asList(new ReplacePair("java[.].*", "JAVA")));

        // test cleaning
        assertEquals(cleaner.clean("ABC"), "ABC");
        assertEquals(cleaner.clean("import nl.obren.test.Test;"), "nl.obren.test.Test");
        assertEquals(cleaner.clean("import java.util.*;"), "JAVA");
    }

    @Test
    public void getCleanedTextList() throws Exception {
        SearchResultCleaner cleaner = new SearchResultCleaner();
        cleaner.setStartCleaningPattern("A");
        cleaner.setEndCleaningPattern(";");

        List<CleanedFoundText> cleanedFoundTextList = cleaner.getCleanedTextList(Arrays.asList(new FoundText("ABC;", 1), new FoundText("DEF;", 1)));

        assertEquals(cleanedFoundTextList.size(), 2);
        assertEquals(cleanedFoundTextList.get(0).getFoundText().getText(), "ABC;");
        assertEquals(cleanedFoundTextList.get(0).getCleanedText(), "BC");
        assertEquals(cleanedFoundTextList.get(1).getFoundText().getText(), "DEF;");
        assertEquals(cleanedFoundTextList.get(1).getCleanedText(), "DEF");
    }
}
