package nl.obren.sokrates.sourcecode.analysis.files;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.duplication.DuplicatedFileBlock;
import nl.obren.sokrates.sourcecode.duplication.DuplicationInstance;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins mergeAndConsolidatePerFilePair, which replaces merge() + consolidate(): duplicated block pairs are
 * grouped by file pair and runs of adjacent pairs are collapsed into a single instance spanning the run.
 */
class DuplicationPerFilePairConsolidationTest {

    private static final int BLOCK = 6;

    private DuplicationAnalyzer analyzer() {
        CodeAnalysisResults analysisResults = new CodeAnalysisResults();
        analysisResults.setCodeConfiguration(new CodeConfiguration());
        return new DuplicationAnalyzer(analysisResults);
    }

    // Raw lines are twice the cleaned lines, so an assertion on startLine/endLine below distinguishes the
    // raw span from the cleaned one instead of passing for either.
    private DuplicatedFileBlock block(String path, int cleanedStartLine, int blockSize) {
        SourceFile sourceFile = new SourceFile(new File(path));
        sourceFile.setRelativePath(path);
        DuplicatedFileBlock block = new DuplicatedFileBlock();
        block.setSourceFile(sourceFile);
        block.setCleanedStartLine(cleanedStartLine);
        block.setCleanedEndLine(cleanedStartLine + blockSize - 1);
        block.setStartLine(cleanedStartLine * 2);
        block.setEndLine((cleanedStartLine + blockSize - 1) * 2);
        block.setSourceFileCleanedLinesOfCode(1000);
        return block;
    }

    private DuplicationInstance instance(int blockSize, DuplicatedFileBlock... blocks) {
        DuplicationInstance instance = new DuplicationInstance();
        instance.setBlockSize(blockSize);
        instance.getDuplicatedFileBlocks().addAll(Arrays.asList(blocks));
        return instance;
    }

    private DuplicationInstance pair(String path1, int start1, String path2, int start2) {
        return instance(BLOCK, block(path1, start1, BLOCK), block(path2, start2, BLOCK));
    }

    private List<String> signatures(List<DuplicationInstance> instances) {
        List<String> signatures = new ArrayList<>();
        instances.forEach(instance -> {
            List<String> blocks = new ArrayList<>();
            instance.getDuplicatedFileBlocks().forEach(block -> blocks.add(
                    block.getSourceFile().getRelativePath() + "|" + block.getStartLine() + "-" + block.getEndLine()
                            + "|" + block.getCleanedStartLine() + "-" + block.getCleanedEndLine()));
            Collections.sort(blocks);
            signatures.add(instance.getBlockSize() + "#" + String.join("~", blocks));
        });
        Collections.sort(signatures);
        return signatures;
    }

    @Test
    void oneSharedBlockBecomesOnePairSpanningExactlyThatBlock() {
        List<DuplicationInstance> result = analyzer().mergeAndConsolidatePerFilePair(
                Collections.singletonList(pair("a.java", 10, "b.java", 20)));

        assertEquals(1, result.size());
        DuplicationInstance consolidated = result.get(0);
        assertEquals(BLOCK, consolidated.getBlockSize());
        assertEquals(2, consolidated.getDuplicatedFileBlocks().size());

        DuplicatedFileBlock first = consolidated.getDuplicatedFileBlocks().get(0);
        DuplicatedFileBlock second = consolidated.getDuplicatedFileBlocks().get(1);
        // The lower relative path comes first, matching getPairKey's ordering.
        assertEquals("a.java", first.getSourceFile().getRelativePath());
        assertEquals("b.java", second.getSourceFile().getRelativePath());
        assertEquals(10, first.getCleanedStartLine());
        assertEquals(15, first.getCleanedEndLine());
        assertEquals(20, first.getStartLine());
        assertEquals(30, first.getEndLine());
        assertEquals(20, second.getCleanedStartLine());
        assertEquals(25, second.getCleanedEndLine());
    }

    @Test
    void adjacentPairsCollapseIntoOneInstanceSpanningTheWholeRun() {
        List<DuplicationInstance> result = analyzer().mergeAndConsolidatePerFilePair(Arrays.asList(
                pair("a.java", 10, "b.java", 20),
                pair("a.java", 11, "b.java", 21),
                pair("a.java", 12, "b.java", 22)));

        assertEquals(1, result.size());
        DuplicationInstance consolidated = result.get(0);
        // Three windows of 6 starting one line apart span 8 cleaned lines, not 6 and not 18.
        assertEquals(8, consolidated.getBlockSize());

        DuplicatedFileBlock first = consolidated.getDuplicatedFileBlocks().get(0);
        assertEquals(10, first.getCleanedStartLine());
        assertEquals(17, first.getCleanedEndLine());
        assertEquals(20, first.getStartLine());
        assertEquals(34, first.getEndLine());

        DuplicatedFileBlock second = consolidated.getDuplicatedFileBlocks().get(1);
        assertEquals(20, second.getCleanedStartLine());
        assertEquals(27, second.getCleanedEndLine());
        assertEquals(40, second.getStartLine());
        assertEquals(54, second.getEndLine());
    }

    @Test
    void aRunIsOnlyAdjacentWhenBothSidesAdvanceTogether() {
        // b advances by two while a advances by one, so these are two separate duplicates rather than a run.
        List<DuplicationInstance> result = analyzer().mergeAndConsolidatePerFilePair(Arrays.asList(
                pair("a.java", 10, "b.java", 20),
                pair("a.java", 11, "b.java", 22)));

        assertEquals(2, result.size());
        result.forEach(instance -> assertEquals(BLOCK, instance.getBlockSize()));
    }

    @Test
    void separateRunsInTheSameFilePairStayApart() {
        List<DuplicationInstance> result = analyzer().mergeAndConsolidatePerFilePair(Arrays.asList(
                pair("a.java", 10, "b.java", 20),
                pair("a.java", 11, "b.java", 21),
                pair("a.java", 30, "b.java", 40)));

        assertEquals(2, result.size());
        List<Integer> blockSizes = new ArrayList<>();
        result.forEach(instance -> blockSizes.add(instance.getBlockSize()));
        Collections.sort(blockSizes);
        assertEquals(Arrays.asList(BLOCK, 7), blockSizes);
    }

    @Test
    void aRunStartingOnTheFirstLineIsNotTreatedAsAContinuation() {
        // The chain-head test probes (start - 1); at line 1 that probe addresses line 0, which must simply
        // be absent rather than wrapping or matching.
        List<DuplicationInstance> result = analyzer().mergeAndConsolidatePerFilePair(Arrays.asList(
                pair("a.java", 1, "b.java", 1),
                pair("a.java", 2, "b.java", 2)));

        assertEquals(1, result.size());
        assertEquals(7, result.get(0).getBlockSize());
        assertEquals(1, result.get(0).getDuplicatedFileBlocks().get(0).getCleanedStartLine());
    }

    @Test
    void aBlockSharedByThreeFilesBecomesOneInstancePerFilePair() {
        List<DuplicationInstance> result = analyzer().mergeAndConsolidatePerFilePair(
                Collections.singletonList(instance(BLOCK,
                        block("a.java", 10, BLOCK), block("b.java", 20, BLOCK), block("c.java", 30, BLOCK))));

        assertEquals(3, result.size());
        assertEquals(Arrays.asList(
                "6#a.java|20-30|10-15~b.java|40-50|20-25",
                "6#a.java|20-30|10-15~c.java|60-70|30-35",
                "6#b.java|40-50|20-25~c.java|60-70|30-35"), signatures(result));
    }

    @Test
    void twoBlocksInTheSameFileAreKeptAsADuplicateOfThatFileWithItself() {
        List<DuplicationInstance> result = analyzer().mergeAndConsolidatePerFilePair(
                Collections.singletonList(instance(BLOCK, block("a.java", 30, BLOCK), block("a.java", 10, BLOCK))));

        assertEquals(1, result.size());
        DuplicationInstance consolidated = result.get(0);
        assertEquals("a.java", consolidated.getDuplicatedFileBlocks().get(0).getSourceFile().getRelativePath());
        assertEquals("a.java", consolidated.getDuplicatedFileBlocks().get(1).getSourceFile().getRelativePath());
        // Within one file the lower cleaned start line comes first, whatever order the blocks arrived in.
        assertEquals(10, consolidated.getDuplicatedFileBlocks().get(0).getCleanedStartLine());
        assertEquals(30, consolidated.getDuplicatedFileBlocks().get(1).getCleanedStartLine());
    }

    @Test
    void anInstanceWithASingleBlockContributesNoPair() {
        List<DuplicationInstance> result = analyzer().mergeAndConsolidatePerFilePair(
                Collections.singletonList(instance(BLOCK, block("a.java", 10, BLOCK))));

        assertTrue(result.isEmpty());
    }

    @Test
    void noDuplicatesProduceNoInstances() {
        assertTrue(analyzer().mergeAndConsolidatePerFilePair(new ArrayList<>()).isEmpty());
    }

    @Test
    void theSamePairReportedByTwoInstancesIsEmittedOnce() {
        List<DuplicationInstance> result = analyzer().mergeAndConsolidatePerFilePair(Arrays.asList(
                pair("a.java", 10, "b.java", 20),
                pair("a.java", 10, "b.java", 20)));

        assertEquals(1, result.size());
        assertEquals(BLOCK, result.get(0).getBlockSize());
    }

    @Test
    void producesTheSameSetAsTheMapBasedImplementation() {
        // Deterministic pseudo-random corpus: overlapping runs, shared blocks, intra-file pairs and repeats,
        // so agreement here is agreement over the shapes the engine actually emits.
        List<DuplicationInstance> duplicates = new ArrayList<>();
        int state = 12345;
        for (int i = 0; i < 400; i++) {
            state = (state * 1103515245 + 12345) & 0x7fffffff;
            int fileA = state % 7;
            int fileB = (state / 7) % 7;
            int startA = 1 + (state / 49) % 40;
            int startB = 1 + (state / 2401) % 40;
            List<DuplicatedFileBlock> blocks = new ArrayList<>();
            blocks.add(block("f" + fileA + ".java", startA, BLOCK));
            blocks.add(block("f" + fileB + ".java", startB, BLOCK));
            if (state % 3 == 0) {
                blocks.add(block("f" + ((fileA + 3) % 7) + ".java", startA + 1, BLOCK));
            }
            duplicates.add(instance(BLOCK, blocks.toArray(new DuplicatedFileBlock[0])));
        }

        DuplicationAnalyzer analyzer = analyzer();
        List<String> legacy = signatures(new ArrayList<>(analyzer.consolidate(analyzer.merge(duplicates)).values()));
        List<String> bucketed = signatures(analyzer.mergeAndConsolidatePerFilePair(duplicates));

        assertFalse(legacy.isEmpty());
        assertEquals(legacy, bucketed);
    }
}
