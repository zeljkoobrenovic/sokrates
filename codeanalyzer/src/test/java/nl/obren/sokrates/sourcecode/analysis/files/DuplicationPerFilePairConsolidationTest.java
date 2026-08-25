package nl.obren.sokrates.sourcecode.analysis.files;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.duplication.DuplicatedFileBlock;
import nl.obren.sokrates.sourcecode.duplication.DuplicationInstance;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
        // Carried through the copy: duplication exports report a block's share of its file.
        assertEquals(1000, first.getSourceFileCleanedLinesOfCode());
        assertEquals(1000, second.getSourceFileCleanedLinesOfCode());
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
    void reproducesTheOutputOfTheMapBasedImplementation() throws Exception {
        // Golden values recorded from the previous merge()+consolidate() implementation over this corpus,
        // before it was removed. They are what makes this a behaviour-preserving change rather than a
        // plausible rewrite, so they must not be re-recorded from the current code to make a failure pass.
        // signatures() keeps each instance's blocks in their own order, so these pin the order too.
        List<String> signatures = signatures(analyzer().mergeAndConsolidatePerFilePair(pseudoRandomCorpus()));

        assertEquals(577, signatures.size());
        assertEquals("6#f0.java|10-20|5-10~f5.java|44-54|22-27", signatures.get(0));
        assertEquals("8#f2.java|46-60|23-30~f5.java|48-62|24-31", signatures.get(signatures.size() - 1));
        assertEquals("eb336aef38c2c16b0d1ccc6f7967fbb4f5a23f921e372f5ac0e872a4d0db8db2", sha256(signatures));
    }

    @Test
    void reproducesTheOutputOfTheMapBasedImplementationForPairsInsideOneFile() throws Exception {
        // Pairs inside one file are the shape where the two implementations could order an instance's two
        // blocks differently, and the corpus above holds only 70 of them. These golden values come from the
        // same recording of the previous implementation over a corpus that is 381 of them.
        List<String> signatures = signatures(analyzer().mergeAndConsolidatePerFilePair(sameFileCorpus()));

        assertEquals(466, signatures.size());
        assertEquals("6#other0.java|14-24|7-12~s0.java|14-24|7-12", signatures.get(0));
        assertEquals("9#s1.java|12-28|6-14~s1.java|26-42|13-21", signatures.get(signatures.size() - 1));
        assertEquals("21fb055570926e8b44f5c3bf31324cde698f369729916ecc2436e6d27422de91", sha256(signatures));
    }

    // Deterministic pseudo-random corpus: overlapping runs, shared blocks, intra-file pairs and repeats,
    // so agreement over it is agreement over the shapes the engine actually emits.
    List<DuplicationInstance> pseudoRandomCorpus() {
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
            sortAsTheEngineWould(blocks);
            duplicates.add(instance(BLOCK, blocks.toArray(new DuplicatedFileBlock[0])));
        }
        return duplicates;
    }


    private String sha256(List<String> signatures) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(String.join("\n", signatures).getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : digest.digest()) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
    // A corpus dominated by pairs inside ONE file, which the other corpus does not contain at all.
    // Blocks are added in ascending cleaned-start order because that is the only order the engine can
    // produce: FileInfoForDuplication.indexesOf is a forward scan, so occurrences come out ascending.
    List<DuplicationInstance> sameFileCorpus() {
        List<DuplicationInstance> duplicates = new ArrayList<>();
        int state = 987654321;
        for (int i = 0; i < 300; i++) {
            state = (state * 1103515245 + 12345) & 0x7fffffff;
            String file = "s" + (state % 5) + ".java";
            int first = 1 + (state / 5) % 30;
            int second = first + 1 + (state / 150) % 25;
            List<DuplicatedFileBlock> blocks = new ArrayList<>();
            blocks.add(block(file, first, BLOCK));
            blocks.add(block(file, second, BLOCK));
            if (state % 4 == 0) {
                blocks.add(block(file, second + 1 + (state / 3750) % 10, BLOCK));
            }
            if (state % 5 == 0) {
                blocks.add(block("other" + (state % 3) + ".java", first, BLOCK));
            }
            sortAsTheEngineWould(blocks);
            duplicates.add(instance(BLOCK, blocks.toArray(new DuplicatedFileBlock[0])));
        }
        return duplicates;
    }


    // The engine never emits two blocks of one file out of ascending start-line order: indexesOf is a
    // forward scan, so occurrences arrive ascending. Fixtures respect that, or they describe inputs that
    // cannot occur.
    private void sortAsTheEngineWould(List<DuplicatedFileBlock> blocks) {
        blocks.sort((a, b) -> a.getSourceFile().getRelativePath().equals(b.getSourceFile().getRelativePath())
                ? Integer.compare(a.getCleanedStartLine(), b.getCleanedStartLine())
                : a.getSourceFile().getRelativePath().compareTo(b.getSourceFile().getRelativePath()));
    }
}
