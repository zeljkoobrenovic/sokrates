/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication_alt;

import nl.obren.sokrates.sourcecode.duplication.impl.Block;
import nl.obren.sokrates.sourcecode.duplication.impl.FileInfoForDuplication;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class FileInfoForDuplicationTest {
    @Test
    public void extractBlocks() throws Exception {
        FileInfoForDuplication fileInfoForDuplication = new FileInfoForDuplication();
        fileInfoForDuplication.setLineIDs(Arrays.asList(1, 2, 3, 4, 5));

        List<Block> blocks = fileInfoForDuplication.extractBlocks(6);
        assertEquals(blocks.size(), 0);

        fileInfoForDuplication.setLineIDs(Arrays.asList(1, 2, 3, 4, 5, 6));
        blocks = fileInfoForDuplication.extractBlocks(6);
        assertEquals(blocks.size(), 1);

        fileInfoForDuplication.setLineIDs(Arrays.asList(1, 2, 3, -1, 4, 5, 6));
        blocks = fileInfoForDuplication.extractBlocks(6);
        assertEquals(blocks.size(), 0);

        fileInfoForDuplication.setLineIDs(Arrays.asList(1, 2, 3, 4, 5, 6, 7, -1, 4, 5, 6));
        blocks = fileInfoForDuplication.extractBlocks(6);
        assertEquals(blocks.size(), 1);

        fileInfoForDuplication.setLineIDs(Arrays.asList(1, 2, 3, -1, 4, 5, 6, 7, 8, 9, 10));
        blocks = fileInfoForDuplication.extractBlocks(6);
        assertEquals(blocks.size(), 1);

        fileInfoForDuplication.setLineIDs(Arrays.asList(1, 2, 3, 4, 5, 6, 7, -1, 4, 5, 6, 7, 8, 9, 10, -1, 2, -1, -1, 11, 11, 12, 12, 13, 14, 15));
        blocks = fileInfoForDuplication.extractBlocks(6);
        assertEquals(blocks.size(), 3);
        assertEquals(blocks.get(0).getStringKey(), "1;2;3;4;5;6;7;");
        assertEquals(blocks.get(1).getStringKey(), "4;5;6;7;8;9;10;");
        assertEquals(blocks.get(2).getStringKey(), "11;11;12;12;13;14;15;");
    }

    @Test
    public void indexesOf() throws Exception {
        FileInfoForDuplication fileInfoForDuplication = new FileInfoForDuplication();
        fileInfoForDuplication.setLineIDs(Arrays.asList(1, 2, 3, 4));

        List<Integer> indexesOf = fileInfoForDuplication.indexesOf(new Block(Arrays.asList(1, 2, 3, 4, 5)));
        assertEquals(indexesOf.size(), 0);

        fileInfoForDuplication.setLineIDs(Arrays.asList(1, 2, 3, 4, 5));
        indexesOf = fileInfoForDuplication.indexesOf(new Block(Arrays.asList(1, 2, 3, 4, 5)));
        assertEquals(indexesOf.size(), 1);
        assertEquals(indexesOf.get(0).intValue(), 0);

        fileInfoForDuplication.setLineIDs(Arrays.asList(1, 2, 3, 4, 5, -1, 1, 2, 3, 4, 5, 6));
        indexesOf = fileInfoForDuplication.indexesOf(new Block(Arrays.asList(1, 2, 3, 4, 5)));
        assertEquals(indexesOf.size(), 2);
        assertEquals(indexesOf.get(0).intValue(), 0);
        assertEquals(indexesOf.get(1).intValue(), 6);

        fileInfoForDuplication.setLineIDs(Arrays.asList(12, 23, 25, 1, 2, 3, 4, 5, -1, 1, 2, 3, 4, 5, 6, 7, 9, -1, -1, 1, 2, 3, 4, 5, 7, 8));
        indexesOf = fileInfoForDuplication.indexesOf(new Block(Arrays.asList(1, 2, 3, 4, 5)));
        assertEquals(indexesOf.size(), 3);
        assertEquals(indexesOf.get(0).intValue(), 3);
        assertEquals(indexesOf.get(1).intValue(), 9);
        assertEquals(indexesOf.get(2).intValue(), 19);
    }

    @Test
    public void clearSubBlock() throws Exception {
        FileInfoForDuplication fileInfoForDuplication = new FileInfoForDuplication();

        fileInfoForDuplication.setLineIDs(Arrays.asList(1, 2, 3, 4, 5));
        assertEquals(fileInfoForDuplication.toString(), "1;2;3;4;5;");

        fileInfoForDuplication.clearSubBlock(new Block(Arrays.asList(1, 2, 3, 4, 5)));
        assertEquals(fileInfoForDuplication.toString(), "-1;-1;-1;-1;-1;");

        fileInfoForDuplication.setLineIDs(Arrays.asList(1, 2, 3, 4, 5, 6));
        fileInfoForDuplication.clearSubBlock(new Block(Arrays.asList(1, 2, 3, 4, 5)));
        assertEquals(fileInfoForDuplication.toString(), "-1;-1;-1;-1;-1;6;");

        fileInfoForDuplication.setLineIDs(Arrays.asList(10, 11, 12, 13, 1, 2, 3, 4, 5, 6));
        fileInfoForDuplication.clearSubBlock(new Block(Arrays.asList(1, 2, 3, 4, 5)));
        assertEquals(fileInfoForDuplication.toString(), "10;11;12;13;-1;-1;-1;-1;-1;6;");

        fileInfoForDuplication.setLineIDs(Arrays.asList(10, 11, 12, 13, 1, 2, 3, 4, 5, 6, 10, 11, 12, 13, 1, 2, 3, 4, 5, 6));
        fileInfoForDuplication.clearSubBlock(new Block(Arrays.asList(1, 2, 3, 4, 5)));
        assertEquals(fileInfoForDuplication.toString(), "10;11;12;13;-1;-1;-1;-1;-1;6;10;11;12;13;-1;-1;-1;-1;-1;6;");
    }

}
