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
        assertEquals(13, unitInfos.get(0).getLinesOfCode());
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
        assertEquals(10, unitInfos.get(1).getLinesOfCode());
        assertEquals(2, unitInfos.get(1).getMcCabeIndex());
        assertEquals(1, unitInfos.get(1).getNumberOfParameters());
    }

}