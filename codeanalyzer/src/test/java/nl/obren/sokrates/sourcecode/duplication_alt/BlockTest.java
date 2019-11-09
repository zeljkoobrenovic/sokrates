package nl.obren.sokrates.sourcecode.duplication_alt;

import nl.obren.sokrates.sourcecode.duplication.impl.Block;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class BlockTest {
    @Test
    public void extractAllPossibleSubBlocks() throws Exception {
        Block block = new Block(Arrays.asList(1, 2, 3, 4));
        List<Block> subBlocks = block.extractAllPossibleSubBlocks(6);
        assertEquals(subBlocks.size(), 0);

        subBlocks = block.extractAllPossibleSubBlocks(5);
        assertEquals(subBlocks.size(), 0);

        subBlocks = block.extractAllPossibleSubBlocks(4);
        assertEquals(subBlocks.size(), 1);
        assertEquals(subBlocks.get(0).getStringKey(), "1;2;3;4;");

        subBlocks = block.extractAllPossibleSubBlocks(3);
        assertEquals(subBlocks.size(), 2);
        assertEquals(subBlocks.get(0).getStringKey(), "1;2;3;");
        assertEquals(subBlocks.get(1).getStringKey(), "2;3;4;");

        subBlocks = block.extractAllPossibleSubBlocks(2);
        assertEquals(subBlocks.size(), 3);
        assertEquals(subBlocks.get(0).getStringKey(), "1;2;");
        assertEquals(subBlocks.get(1).getStringKey(), "2;3;");
        assertEquals(subBlocks.get(2).getStringKey(), "3;4;");

        subBlocks = block.extractAllPossibleSubBlocks(1);
        assertEquals(subBlocks.size(), 4);
        assertEquals(subBlocks.get(0).getStringKey(), "1;");
        assertEquals(subBlocks.get(1).getStringKey(), "2;");
        assertEquals(subBlocks.get(2).getStringKey(), "3;");
        assertEquals(subBlocks.get(3).getStringKey(), "4;");
    }

    @Test(expected = IllegalArgumentException.class)
    public void extractAllPossibleSubBlocksIllegal() throws Exception {
        Block block = new Block(Arrays.asList(1, 2, 3, 4));

        block.extractAllPossibleSubBlocks(0);
    }

    @Test
    public void equals() throws Exception {
        assertTrue(new Block().equals(new Block()));
        assertTrue(new Block(Arrays.asList(1, 2, 3, 4)).equals(new Block(Arrays.asList(1, 2, 3, 4))));

        assertFalse(new Block(Arrays.asList(1, 2, 3, 4)).equals(new Block()));
        assertFalse(new Block(Arrays.asList(1, 2, 3, 4)).equals(new Block(Arrays.asList(1, 2, 3, 4, 5, 6))));
        assertFalse(new Block(Arrays.asList(1, 2, 3, 4)).equals(new Block(Arrays.asList(2, 3, 4))));
        assertFalse(new Block(Arrays.asList(1, 2, 3, 4)).equals(new Block(Arrays.asList(1, 2, 3))));
        assertFalse(new Block(Arrays.asList(1, 2, 3, 4)).equals(new Block(Arrays.asList(5, 6, 7, 8, 9))));
    }

    @Test
    public void getStringKey() throws Exception {
        assertEquals(new Block().getStringKey(), "");
        assertEquals(new Block(Arrays.asList(1)).getStringKey(), "1;");
        Block block = new Block(Arrays.asList(1, 2, 3, 4));
        assertEquals(block.getStringKey(), "1;2;3;4;");
        block.setLineIndexes(Arrays.asList(5, 6, 7));
        assertEquals(block.getStringKey(), "5;6;7;");
    }

}