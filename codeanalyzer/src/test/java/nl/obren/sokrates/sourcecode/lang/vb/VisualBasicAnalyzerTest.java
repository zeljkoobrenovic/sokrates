/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.vb;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.Test;

import java.io.File;
import java.util.List;

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

    @Test
    public void extractUnits() {
        VisualBasicAnalyzer analyzer = new VisualBasicAnalyzer();

        SourceFile sourceFile = new SourceFile(new File("a.vb"));
        sourceFile.setContent("Imports A.B\n" +
                "Imports B.C\n" +
                "\n" +
                "\n" +
                "' Comment 1\n" +
                " ' Comment 2\n" +
                "\n" +
                "Sub A(a, b)\n" +
                " If a > b Then\n" +
                "   c(a, b, 0)\n" +
                " End If\n" +
                " ' comment 3\n" +
                "End Sub\n" +
                "Sub B()\n" +
                " ' comment 4\n" +
                " ' comment 4\n" +
                " REM  comment 5\n" +
                "End Sub");

        List<UnitInfo> units = analyzer.extractUnits(sourceFile);

        assertEquals(2, units.size());
        assertEquals("Sub A()", units.get(0).getShortName());
        assertEquals(3, units.get(0).getLinesOfCode());
        assertEquals(2, units.get(0).getMcCabeIndex());
        assertEquals(2, units.get(0).getNumberOfParameters());
        assertEquals("Sub B()", units.get(1).getShortName());
        assertEquals(1, units.get(1).getLinesOfCode());
        assertEquals(1, units.get(1).getMcCabeIndex());
        assertEquals(0, units.get(1).getNumberOfParameters());

    }

}
