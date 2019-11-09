package nl.obren.sokrates.sourcecode.lang.vb;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class VisualBasicAnalyzerTest {
    @Test
    public void cleanForLinesOfCodeCalculations() throws Exception {
        VisualBasicAnalyzer analyzer = new VisualBasicAnalyzer();

        SourceFile sourceFile = new SourceFile(new File("a.vb"));
        sourceFile.setContent("Imports A.B\n" +
                "Imports B.C\n" +
                "\n" +
                "\n" +
                "' Comment 1\n" +
                " ' Comment 2\n" +
                "\n" + "Sub A()\n" +
                " ' comment 2\n" +
                "End Sub");

        CleanedContent cleanedContent = analyzer.cleanForLinesOfCodeCalculations(sourceFile);

        assertEquals(cleanedContent.getCleanedContent(), "Imports A.B\n" +
                "Imports B.C\n" +
                "Sub A()\n" +
                "End Sub");
        assertEquals(cleanedContent.getFileLineIndexes().size(), 4);
        assertEquals(cleanedContent.getFileLineIndexes().get(0).intValue(), 0);
        assertEquals(cleanedContent.getFileLineIndexes().get(1).intValue(), 1);
        assertEquals(cleanedContent.getFileLineIndexes().get(2).intValue(), 7);
        assertEquals(cleanedContent.getFileLineIndexes().get(3).intValue(), 9);
    }

    @Test
    public void cleanForDuplicationCalculations() throws Exception {
        VisualBasicAnalyzer analyzer = new VisualBasicAnalyzer();

        SourceFile sourceFile = new SourceFile(new File("a.vb"));
        sourceFile.setContent("Imports A.B\n" +
                "Imports B.C\n" +
                "\n" +
                "\n" +
                "' Comment 1\n" +
                " ' Comment 2\n" +
                "\n" +
                "Sub A()\n" +
                " ' comment 3\n" +
                "End Sub\n" +
                "Sub B()\n" +
                " ' comment 4\n" +
                " REM  comment 5\n" +
                "End Sub");

        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(sourceFile);

        assertEquals(cleanedContent.getCleanedContent(), "Sub A()\n" +
                "Sub B()");
        assertEquals(cleanedContent.getFileLineIndexes().size(), 2);
        assertEquals(cleanedContent.getFileLineIndexes().get(0).intValue(), 7);
        assertEquals(cleanedContent.getFileLineIndexes().get(1).intValue(), 10);
    }

}