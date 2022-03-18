package nl.obren.sokrates.sourcecode.lang.adabasnatural;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

class AdabasNaturalAnalyzerTest {
    @Test
    void cleanForLinesOfCodeCalculations() {
        AdabasNaturalAnalyzer analyzer = new AdabasNaturalAnalyzer();

        SourceFile sourceFile = new SourceFile(new File("a.nsp"), AdabasExamples.CONTENT);
        CleanedContent cleanedContent = analyzer.cleanForLinesOfCodeCalculations(sourceFile);

        assertEquals(cleanedContent.getCleanedContent(), AdabasExamples.CLEANED);
    }

    @Test
    void cleanForDuplicationCalculations() {
        AdabasNaturalAnalyzer analyzer = new AdabasNaturalAnalyzer();

        SourceFile sourceFile = new SourceFile(new File("a.nsp"), AdabasExamples.CONTENT);
        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(sourceFile);

        assertEquals(cleanedContent.getCleanedContent(), AdabasExamples.CLEANED_FOR_DUPLICATION);
    }

    @Test
    void extractUnits() {
        AdabasNaturalAnalyzer analyzer = new AdabasNaturalAnalyzer();

        SourceFile sourceFile = new SourceFile(new File("a.nsp"), AdabasExamples.CONTENT);
        sourceFile.setLinesOfCode(10);
        List<UnitInfo> unitInfos = analyzer.extractUnits(sourceFile);

        assertEquals(unitInfos.size(), 1);
        assertEquals(unitInfos.get(0).getShortName(), "a.nsp");
        assertEquals(unitInfos.get(0).getLinesOfCode(), 10);
        assertEquals(unitInfos.get(0).getMcCabeIndex(), 8);
        assertEquals(unitInfos.get(0).getNumberOfParameters(), 14);
    }

    @Test
    void extractMultipleUnits() {
        AdabasNaturalAnalyzer analyzer = new AdabasNaturalAnalyzer();

        SourceFile sourceFile = new SourceFile(new File("a.nsp"), AdabasExamples.UNITCONTENT);
        
        List<UnitInfo> unitInfos = analyzer.extractUnits(sourceFile);

        assertEquals(unitInfos.size(), 3);
        assertEquals(unitInfos.get(0).getShortName(), "F#MULTI");
        assertEquals(unitInfos.get(0).getLinesOfCode(), 11);
        assertEquals(unitInfos.get(0).getMcCabeIndex(), 2);
        assertEquals(unitInfos.get(0).getNumberOfParameters(), 2);

        assertEquals(unitInfos.get(1).getShortName(), "F2#MULTI");
        assertEquals(unitInfos.get(1).getLinesOfCode(), 11);
        assertEquals(unitInfos.get(1).getMcCabeIndex(), 2);
        assertEquals(unitInfos.get(1).getNumberOfParameters(), 2);

        assertEquals(unitInfos.get(2).getShortName(), "F3#MULTI");
        assertEquals(unitInfos.get(2).getLinesOfCode(), 11);
        assertEquals(unitInfos.get(2).getMcCabeIndex(), 2);
        assertEquals(unitInfos.get(2).getNumberOfParameters(), 2);

    }

}