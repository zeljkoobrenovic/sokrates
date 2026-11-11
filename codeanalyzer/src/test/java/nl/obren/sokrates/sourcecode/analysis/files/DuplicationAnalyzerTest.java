package nl.obren.sokrates.sourcecode.analysis.files;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.duplication.DuplicatedFileBlock;
import nl.obren.sokrates.sourcecode.duplication.DuplicationInstance;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DuplicationAnalyzerTest {

    @Test
    void analyze() {
    }

    private DuplicationInstance instanceWithBlocks(int numberOfBlocks) {
        DuplicationInstance instance = new DuplicationInstance();
        for (int i = 0; i < numberOfBlocks; i++) {
            DuplicatedFileBlock block = new DuplicatedFileBlock();
            SourceFile sourceFile = new SourceFile(new File("file" + i + ".txt"));
            sourceFile.setRelativePath("file" + i + ".txt");
            block.setSourceFile(sourceFile);
            instance.getDuplicatedFileBlocks().add(block);
        }
        return instance;
    }

    @Test
    void addMostFrequentDuplicatesIsOrderedByFrequency() {
        CodeAnalysisResults analysisResults = new CodeAnalysisResults();
        analysisResults.setCodeConfiguration(new CodeConfiguration());
        DuplicationAnalyzer analyzer = new DuplicationAnalyzer(analysisResults);

        // Deliberately unsorted input; instances with >2 blocks must come out ordered by descending block count.
        List<DuplicationInstance> duplicates = new ArrayList<>();
        duplicates.add(instanceWithBlocks(3));
        duplicates.add(instanceWithBlocks(7));
        duplicates.add(instanceWithBlocks(2)); // filtered out (not > 2)
        duplicates.add(instanceWithBlocks(5));
        duplicates.add(instanceWithBlocks(2)); // filtered out

        analyzer.addMostFrequentDuplicates(duplicates);

        List<DuplicationInstance> mostFrequent = analysisResults.getDuplicationAnalysisResults().getMostFrequentDuplicates();

        // Only the three instances with > 2 blocks, ordered by descending block count.
        assertEquals(3, mostFrequent.size());
        assertEquals(7, mostFrequent.get(0).getDuplicatedFileBlocks().size());
        assertEquals(5, mostFrequent.get(1).getDuplicatedFileBlocks().size());
        assertEquals(3, mostFrequent.get(2).getDuplicatedFileBlocks().size());
    }

}