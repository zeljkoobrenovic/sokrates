package nl.obren.sokrates.sourcecode.lang.plsql;

import junit.framework.TestCase;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

public class PlSqlAnalyzerTest {

    PlSqlAnalyzer analyzer;

    @Before
    public void init() {
        analyzer = new PlSqlAnalyzer();
    }

    @Test
    public void cleanForLinesOfCodeCalculations() {

        SourceFile sourceFile = new SourceFile(new File("test_lines.pls"), PlSqlExamples.CONTENT_1);

        CleanedContent cleanedContent = analyzer.cleanForLinesOfCodeCalculations(sourceFile);

        assertEquals(PlSqlExamples.CONTENT_1_CLEANED, cleanedContent.getCleanedContent());
    }

    @Test
    public void cleanForDuplicationCalculations() {

        SourceFile sourceFile = new SourceFile(new File("test_duplicate.pls"), PlSqlExamples.CONTENT_2);

        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(sourceFile);

        assertEquals(PlSqlExamples.CONTENT_2_CLEANED_FOR_DUPLICATION, cleanedContent.getCleanedContent());

    }

    @Test
    public void extractUnits1() {
        PlSqlAnalyzer analyzer = new PlSqlAnalyzer();
        SourceFile sourceFile = new SourceFile(new File("test_units1.pls"), PlSqlExamples.CONTENT_3);

        List<UnitInfo> unitInfos = analyzer.extractUnits(sourceFile);
        assertEquals(1, unitInfos.size());
        assertEquals("create_email_address", unitInfos.get(0).getShortName());
        assertEquals(14, unitInfos.get(0).getLinesOfCode());
        assertEquals(2, unitInfos.get(0).getMcCabeIndex());
        assertEquals(4, unitInfos.get(0).getNumberOfParameters());
    }

    @Test
    public void extractUnits2() {
        PlSqlAnalyzer analyzer = new PlSqlAnalyzer();
        SourceFile sourceFile = new SourceFile(new File("test_units2.pls"), PlSqlExamples.CONTENT_4);

        List<UnitInfo> unitInfos = analyzer.extractUnits(sourceFile);
        assertEquals(2, unitInfos.size());
        assertEquals("print_aa", unitInfos.get(1).getShortName());
        assertEquals(8, unitInfos.get(1).getLinesOfCode());
        assertEquals(2, unitInfos.get(1).getMcCabeIndex());
        assertEquals(1, unitInfos.get(1).getNumberOfParameters());
    }

    @Test
    public void extractUnitsFromPackage() {
        PlSqlAnalyzer analyzer = new PlSqlAnalyzer();
        SourceFile sourceFile = new SourceFile(new File("test_units3.pls"), PlSqlExamples.CONTENT_9);

        List<UnitInfo> unitInfos = analyzer.extractUnits(sourceFile);
        assertEquals(3, unitInfos.size());
        assertEquals("addCustomer", unitInfos.get(0).getShortName());
        assertEquals("delCustomer", unitInfos.get(1).getShortName());
        assertEquals("listCustomer", unitInfos.get(2).getShortName());
        assertEquals(10, unitInfos.get(0).getLinesOfCode());
        assertEquals(1, unitInfos.get(0).getMcCabeIndex());
        assertEquals(5, unitInfos.get(0).getNumberOfParameters());
        assertEquals(5, unitInfos.get(1).getLinesOfCode());
        assertEquals(1, unitInfos.get(1).getMcCabeIndex());
        assertEquals(1, unitInfos.get(1).getNumberOfParameters());
        assertEquals(14, unitInfos.get(2).getLinesOfCode());
        assertEquals(2, unitInfos.get(2).getMcCabeIndex());
        assertEquals(0, unitInfos.get(2).getNumberOfParameters());
    }

    @Test
    public void extractUnitsWithoutParameters() {
        PlSqlAnalyzer analyzer = new PlSqlAnalyzer();
        SourceFile sourceFile = new SourceFile(new File("test_units4.pls"), PlSqlExamples.CONTENT_10);

        List<UnitInfo> unitInfos = analyzer.extractUnits(sourceFile);
        assertEquals(1, unitInfos.size());
        assertEquals("get_p1100_date", unitInfos.get(0).getShortName());
        assertEquals(4, unitInfos.get(0).getLinesOfCode());
        assertEquals(1, unitInfos.get(0).getMcCabeIndex());
        assertEquals(0, unitInfos.get(0).getNumberOfParameters());
    }

    /**
     * A backslash carries no special meaning in a PL/SQL string literal, so a path ending in one must
     * not be read as an escaped quote. When it was, every literal after that point paired with the
     * wrong partner and the tail of the file was dropped from the cleaned content - here the last two
     * lines, so the file measured 10 lines of code instead of 12.
     */
    @Test
    public void cleanForLinesOfCodeCalculations_keepsTheTailAfterALiteralEndingInABackslash() {
        SourceFile sourceFile = new SourceFile(new File("test_backslash.pls"),
                PlSqlExamples.CONTENT_BACKSLASH_PATH);

        CleanedContent cleanedContent = analyzer.cleanForLinesOfCodeCalculations(sourceFile);

        assertTrue(cleanedContent.getCleanedContent().contains("END export_pkg;"),
                () -> "the end of the package must survive cleaning, got:\n" + cleanedContent.getCleanedContent());
        assertEquals(12, cleanedContent.getCleanedLinesCount());
    }

    /**
     * The same defect measured on the other consumer of the cleaner. Asserting only the line count
     * would leave unit boundaries untested, and they move too: the last procedure lost its END line.
     */
    @Test
    public void extractUnits_aLiteralEndingInABackslashDoesNotShortenTheUnitsAfterIt() {
        SourceFile sourceFile = new SourceFile(new File("test_backslash_units.pls"),
                PlSqlExamples.CONTENT_BACKSLASH_PATH);

        List<UnitInfo> unitInfos = analyzer.extractUnits(sourceFile);

        assertEquals(2, unitInfos.size());
        assertEquals("write_report", unitInfos.get(0).getShortName());
        assertEquals("archive", unitInfos.get(1).getShortName());
        // Runs to its own END on line 11, not to the line before it.
        assertEquals(4, unitInfos.get(1).getLinesOfCode());
    }

    /**
     * PL/SQL's actual escape, which the fix relies on: a quote is doubled inside a literal. Passes
     * against either escape marker, so this pins the semantics rather than guarding the defect.
     */
    @Test
    public void cleanForLinesOfCodeCalculations_handlesADoubledQuote() {
        SourceFile sourceFile = new SourceFile(new File("test_doubled.pls"),
                "BEGIN\n  introduction := ' Hello! I''m John Smith.';\n  choice := 'y';\nEND;\n");

        CleanedContent cleanedContent = analyzer.cleanForLinesOfCodeCalculations(sourceFile);

        assertTrue(cleanedContent.getCleanedContent().contains("choice := 'y'"),
                () -> "the statement after the doubled quote must survive, got:\n"
                        + cleanedContent.getCleanedContent());
    }

}