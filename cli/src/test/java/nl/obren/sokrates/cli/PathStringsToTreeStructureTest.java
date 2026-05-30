/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.cli;

import nl.obren.sokrates.common.renderingutils.charts.Palette;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.threshold.Thresholds;
import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.*;

public class PathStringsToTreeStructureTest {

    private static SourceFile sourceFileAt(String relativePath) {
        SourceFile sourceFile = new SourceFile();
        sourceFile.setRelativePath(relativePath);
        return sourceFile;
    }

    @Test
    public void createDirectoryTreeRootsEverythingUnderRoot() {
        DirectoryNode tree = PathStringsToTreeStructure.createDirectoryTree(
                List.of(sourceFileAt("src/Main.java")));

        assertEquals("ROOT", tree.getValue());
        assertEquals(1, tree.getChildren().size());
        assertEquals("src", tree.getChildren().iterator().next().getValue());
    }

    @Test
    public void createDirectoryTreeMergesSharedPrefixes() {
        DirectoryNode tree = PathStringsToTreeStructure.createDirectoryTree(List.of(
                sourceFileAt("src/a/A.java"),
                sourceFileAt("src/b/B.java")));

        DirectoryNode src = tree.getChildren().iterator().next();
        assertEquals("src", src.getValue());
        // src must merge to a single node holding both subtrees, not duplicate
        assertEquals(2, src.getChildren().size());
    }

    @Test
    public void leafNodeCarriesItsSourceFile() {
        DirectoryNode tree = PathStringsToTreeStructure.createDirectoryTree(
                List.of(sourceFileAt("src/Main.java")));

        DirectoryNode src = tree.getChildren().iterator().next();
        DirectoryNode leaf = src.getChildren().iterator().next();
        assertTrue(leaf.isLeaf());
        assertNotNull(leaf.getSourceFile());
        assertEquals("src/Main.java", leaf.getSourceFile().getRelativePath());
    }

    @Test
    public void getColorSelectsBucketByThreshold() {
        Thresholds thresholds = new Thresholds(10, 20, 30, 40);
        Palette palette = Palette.getRiskPalette();
        List<String> colors = palette.getColors();

        // size within each band maps to colors indexed 4,3,2,1,0 (low value -> "good" end)
        assertEquals(colors.get(4), PathStringsToTreeStructure.getColor(thresholds, palette, 5));
        assertEquals(colors.get(3), PathStringsToTreeStructure.getColor(thresholds, palette, 15));
        assertEquals(colors.get(2), PathStringsToTreeStructure.getColor(thresholds, palette, 25));
        assertEquals(colors.get(1), PathStringsToTreeStructure.getColor(thresholds, palette, 35));
        assertEquals(colors.get(0), PathStringsToTreeStructure.getColor(thresholds, palette, 100));
    }

    @Test
    public void getColorReturnsEmptyForTooSmallPalette() {
        Thresholds thresholds = new Thresholds(10, 20, 30, 40);
        // the duplication palette has only 2 colors, fewer than the 5 getColor requires
        Palette palette = Palette.getDuplicationPalette();
        assertTrue(palette.getColors().size() < 5);

        assertEquals("", PathStringsToTreeStructure.getColor(thresholds, palette, 5));
    }
}
