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
}