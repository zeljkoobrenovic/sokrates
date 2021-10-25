package nl.obren.sokrates.sourcecode.lang.abap;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;


public class AbapAnalyzerTest {
    @Test
    public void cleanForLinesOfCodeCalculations() {
        AbapAnalyzer analyzer = new AbapAnalyzer();

        SourceFile sourceFile = new SourceFile(new File("a.abap"), AbapExamples.CONTENT);
        CleanedContent cleanedContent = analyzer.cleanForLinesOfCodeCalculations(sourceFile);

        assertEquals(cleanedContent.getCleanedContent(), AbapExamples.CLEANED);
    }

    @Test
    public void cleanForDuplicationCalculations() {
        AbapAnalyzer analyzer = new AbapAnalyzer();

        SourceFile sourceFile = new SourceFile(new File("a.abap"), AbapExamples.CONTENT);
        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(sourceFile);

        assertEquals(cleanedContent.getCleanedContent(), AbapExamples.CLEANED_FOR_DUPLICATION);
    }

    @Test
    public void extractUnits() {
        AbapAnalyzer analyzer = new AbapAnalyzer();

        SourceFile sourceFile = new SourceFile(new File("a.abap"), AbapExamples.CONTENT);
        sourceFile.setLinesOfCode(10);
        List<UnitInfo> unitInfos = analyzer.extractUnits(sourceFile);
        assertEquals(unitInfos.size(), 4);
        assertEquals(unitInfos.get(0).getShortName(), "calculate_flight_price");
        assertEquals(unitInfos.get(0).getLinesOfCode(), 11);
        assertEquals(unitInfos.get(0).getMcCabeIndex(), 5);
        assertEquals(unitInfos.get(0).getNumberOfParameters(), 0);
    }
}