package nl.obren.sokrates.sourcecode.analysis.files;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.duplication.DuplicatedFileBlock;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class DuplicationAnalyzerTest {

    @Test
    void analyze() {
    }

    @Test
    void getPairKey() {
        CodeAnalysisResults analysisResults = new CodeAnalysisResults();
        analysisResults.setCodeConfiguration(new CodeConfiguration());
        DuplicationAnalyzer analyzer = new DuplicationAnalyzer(analysisResults);

        DuplicatedFileBlock block1 = new DuplicatedFileBlock();
        DuplicatedFileBlock block2 = new DuplicatedFileBlock();
        DuplicatedFileBlock block1b = new DuplicatedFileBlock();

        SourceFile sourceFile1 = new SourceFile(new File("path1.txt"));
        sourceFile1.setRelativePath("path1.txt");
        block1.setSourceFile(sourceFile1);
        block1.setStartLine(1);
        block1.setCleanedStartLine(1);

        SourceFile sourceFile2 = new SourceFile(new File("path2.txt"));
        sourceFile2.setRelativePath("path2.txt");
        block2.setSourceFile(sourceFile2);
        block2.setStartLine(11);
        block2.setCleanedStartLine(11);

        SourceFile sourceFile1b = new SourceFile(new File("path1.txt"));
        sourceFile1b.setRelativePath("path1.txt");
        block1b.setSourceFile(sourceFile1b);
        block1b.setStartLine(20);
        block1b.setCleanedStartLine(20);

        assertEquals(analyzer.getPairKey(block1, block2, 0), "path1.txt:1::path2.txt:11");
        assertEquals(analyzer.getPairKey(block1, block2, 1), "path1.txt:2::path2.txt:12");
        assertEquals(analyzer.getPairKey(block1, block2, 10), "path1.txt:11::path2.txt:21");

        assertEquals(analyzer.getPairKey(block2, block1, 0), "path1.txt:1::path2.txt:11");
        assertEquals(analyzer.getPairKey(block2, block1, 1), "path1.txt:2::path2.txt:12");
        assertEquals(analyzer.getPairKey(block2, block1, 10), "path1.txt:11::path2.txt:21");

        assertEquals(analyzer.getPairKey(block1, block1b, 0), "path1.txt:1::path1.txt:20");
        assertEquals(analyzer.getPairKey(block1, block1b, 10), "path1.txt:11::path1.txt:30");

        assertEquals(analyzer.getPairKey(block1b, block1, 0), "path1.txt:1::path1.txt:20");
        assertEquals(analyzer.getPairKey(block1b, block1, 10), "path1.txt:11::path1.txt:30");
    }
}