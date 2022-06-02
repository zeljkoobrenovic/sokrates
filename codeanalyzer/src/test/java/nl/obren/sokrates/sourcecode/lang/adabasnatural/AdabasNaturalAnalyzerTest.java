package nl.obren.sokrates.sourcecode.lang.adabasnatural;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class AdabasNaturalAnalyzerTest {
    @Test
    public void cleanForLinesOfCodeCalculations() {
        AdabasNaturalAnalyzer analyzer = new AdabasNaturalAnalyzer();

        SourceFile sourceFile = new SourceFile(new File("a.nsp"), AdabasExamples.CONTENT);
        CleanedContent cleanedContent = analyzer.cleanForLinesOfCodeCalculations(sourceFile);

        assertEquals(cleanedContent.getCleanedContent(), AdabasExamples.CLEANED);
    }

    @Test
    public void cleanForDuplicationCalculations() {
        AdabasNaturalAnalyzer analyzer = new AdabasNaturalAnalyzer();

        SourceFile sourceFile = new SourceFile(new File("a.nsp"), AdabasExamples.CONTENT);
        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(sourceFile);

        assertEquals(cleanedContent.getCleanedContent(), AdabasExamples.CLEANED_FOR_DUPLICATION);
    }

    @Test
    public void extractUnits() {
        AdabasNaturalAnalyzer analyzer = new AdabasNaturalAnalyzer();

        SourceFile sourceFile = new SourceFile(new File("a.nsp"), AdabasExamples.CONTENT);
        sourceFile.setLinesOfCode(10);
        List<UnitInfo> unitInfos = analyzer.extractUnits(sourceFile);
        assertEquals(unitInfos.size(), 1);
        
        assertEquals(unitInfos.get(0).getShortName(), "EXAMPLE");
        assertEquals(unitInfos.get(0).getLinesOfCode(), 96);
        assertEquals(unitInfos.get(0).getMcCabeIndex(), 6);
        assertEquals(unitInfos.get(0).getNumberOfParameters(), 0);
        
    }

    @Test
    public void extractMultipleUnits() {
        AdabasNaturalAnalyzer analyzer = new AdabasNaturalAnalyzer();

        SourceFile sourceFile = new SourceFile(new File("u.nsp"), AdabasExamples.UNITCONTENT);
        
        List<UnitInfo> unitInfos = analyzer.extractUnits(sourceFile);

        assertEquals(unitInfos.size(), 3);
        assertEquals(unitInfos.get(0).getShortName(), "F#MULTI");
        assertEquals(unitInfos.get(0).getLinesOfCode(), 7);
        assertEquals(unitInfos.get(0).getMcCabeIndex(), 1);
        assertEquals(unitInfos.get(0).getNumberOfParameters(), 0);

        assertEquals(unitInfos.get(1).getShortName(), "F2#MULTI");
        assertEquals(unitInfos.get(1).getLinesOfCode(), 6);
        assertEquals(unitInfos.get(1).getMcCabeIndex(), 1);
        assertEquals(unitInfos.get(1).getNumberOfParameters(), 0);

        assertEquals(unitInfos.get(2).getShortName(), "SUBR01");
        assertEquals(unitInfos.get(2).getLinesOfCode(), 4);
        assertEquals(unitInfos.get(2).getMcCabeIndex(), 1);
        assertEquals(unitInfos.get(2).getNumberOfParameters(), 0);

    }

}