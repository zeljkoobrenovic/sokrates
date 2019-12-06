package nl.obren.sokrates.sourcecode.lang;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import static junit.framework.TestCase.assertEquals;

public class DefaultLanguageAnalyzerTest {
    @Test
    public void cleanForLinesOfCodeCalculations() throws Exception {
        DefaultLanguageAnalyzer analyzer = new DefaultLanguageAnalyzer();
        assertEquals(analyzer.cleanForLinesOfCodeCalculations(new SourceFile(new File(""), "a\nb\n\n\nc\n")).getCleanedContent(), "a\nb\nc");
    }

    @Test
    public void cleanForDuplicationCalculations() throws Exception {
        DefaultLanguageAnalyzer analyzer = new DefaultLanguageAnalyzer();
        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(new SourceFile(new File(""),
                "a\nb\n\n\nc\n"));
        assertEquals(cleanedContent.getCleanedContent(), "a\nb\nc");
        assertEquals(cleanedContent.getCleanedLinesCount(), 3);
        assertEquals(cleanedContent.getFileLineIndexes().size(), 3);
        assertEquals(cleanedContent.getFileLineIndexes().get(0).intValue(), 0);
        assertEquals(cleanedContent.getFileLineIndexes().get(1).intValue(), 1);
        assertEquals(cleanedContent.getFileLineIndexes().get(2).intValue(), 4);
    }

    @Test
    public void extractUnits() throws Exception {
        DefaultLanguageAnalyzer analyzer = new DefaultLanguageAnalyzer();
        assertEquals(analyzer.extractUnits(new SourceFile(new File(""), "a\nb\n\n\nc\n")).size(), 0);
    }

    @Test
    public void extractDependencies() throws Exception {
        DefaultLanguageAnalyzer analyzer = new DefaultLanguageAnalyzer();
        SourceFile sourceFile = new SourceFile(new File(""), "a\nb\n\n\nc\n");
        assertEquals(analyzer.extractDependencies(Arrays.asList(sourceFile), new ProgressFeedback()).getDependencies().size(), 0);
    }

}
