/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode;

import nl.obren.sokrates.sourcecode.search.FoundLine;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

public class SourceFileWithSearchDataTest {
    @Test
    public void getLines() throws Exception {
        SourceFileWithSearchData sourceFileWithSearchData = new SourceFileWithSearchData(new SourceFile(new File("")));

        assertNotNull(sourceFileWithSearchData.getLines());
        assertEquals(sourceFileWithSearchData.getLines().size(), 0);
    }

    @Test
    public void getLineCount() throws Exception {
        SourceFile sourceFile = new SourceFile(new File(""));
        sourceFile.setLinesOfCode(110);
        SourceFileWithSearchData sourceFileWithSearchData = new SourceFileWithSearchData(sourceFile);

        assertEquals(sourceFileWithSearchData.getLineCount(), 110);
    }

    @Test
    public void clearSearchData() throws Exception {
        SourceFile sourceFile = new SourceFile(new File(""));
        sourceFile.setLinesOfCode(110);
        SourceFileWithSearchData sourceFileWithSearchData = new SourceFileWithSearchData(sourceFile);

        sourceFileWithSearchData.setFoundInstancesCount(20);
        sourceFileWithSearchData.getLinesWithSearchedContent().add(new FoundLine());
        sourceFileWithSearchData.getLinesWithSearchedContent().add(new FoundLine());

        assertEquals(sourceFileWithSearchData.getFoundInstancesCount(), 20);
        assertEquals(sourceFileWithSearchData.getLinesWithSearchedContent().size(), 2);

        sourceFileWithSearchData.clearSearchData();

        assertEquals(sourceFileWithSearchData.getFoundInstancesCount(), 0);
        assertEquals(sourceFileWithSearchData.getLinesWithSearchedContent().size(), 0);

    }

}
