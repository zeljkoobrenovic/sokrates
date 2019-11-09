package nl.obren.sokrates.sourcecode.duplication_alt;

import nl.obren.sokrates.sourcecode.duplication.impl.LineIndexesExtractor;
import nl.obren.sokrates.sourcecode.duplication.impl.LineInfo;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class LineIndexesExtractorTest {
    @Test
    public void getLineIDs() throws Exception {
        LineInfo.resetCounter();
        LineIndexesExtractor extractor = new LineIndexesExtractor();

        List<Integer> lineIDs = extractor.getLineIDs(Arrays.asList("A", "B", "C"));

        assertEquals(lineIDs.size(), 3);
        assertEquals(lineIDs.get(0).intValue(), 0);
        assertEquals(lineIDs.get(1).intValue(), 1);
        assertEquals(lineIDs.get(2).intValue(), 2);

        lineIDs = extractor.getLineIDs(Arrays.asList("A", "A"));
        assertEquals(lineIDs.size(), 2);
        assertEquals(lineIDs.get(0).intValue(), 0);
        assertEquals(lineIDs.get(1).intValue(), 0);

        lineIDs = extractor.getLineIDs(Arrays.asList("B", "B"));
        assertEquals(lineIDs.size(), 2);
        assertEquals(lineIDs.get(0).intValue(), 1);
        assertEquals(lineIDs.get(1).intValue(), 1);

        lineIDs = extractor.getLineIDs(Arrays.asList("B", "C", "D", "Z", "A"));
        assertEquals(lineIDs.size(), 5);
        assertEquals(lineIDs.get(0).intValue(), 1);
        assertEquals(lineIDs.get(1).intValue(), 2);
        assertEquals(lineIDs.get(2).intValue(), 3);
        assertEquals(lineIDs.get(3).intValue(), 4);
        assertEquals(lineIDs.get(4).intValue(), 0);
    }

    @Test
    public void clearUniqueLines() throws Exception {
        LineInfo.resetCounter();
        LineIndexesExtractor extractor = new LineIndexesExtractor();

        List<Integer> lineIDs = extractor.getLineIDs(Arrays.asList("A", "B", "C", "D", "A", "D"));

        assertEquals(lineIDs.get(0).intValue(), 0);
        assertEquals(lineIDs.get(1).intValue(), 1);
        assertEquals(lineIDs.get(2).intValue(), 2);
        assertEquals(lineIDs.get(3).intValue(), 3);
        assertEquals(lineIDs.get(4).intValue(), 0);
        assertEquals(lineIDs.get(5).intValue(), 3);

        extractor.clearUniqueLines(lineIDs);
        assertEquals(lineIDs.get(0).intValue(), 0);
        assertEquals(lineIDs.get(1).intValue(), -1);
        assertEquals(lineIDs.get(2).intValue(), -1);
        assertEquals(lineIDs.get(3).intValue(), 3);
        assertEquals(lineIDs.get(4).intValue(), 0);
        assertEquals(lineIDs.get(5).intValue(), 3);
   }

}