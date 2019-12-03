package nl.obren.sokrates.sourcecode.lang.java;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

public class JavaAnalyzerTest {

    @Test
    public void cleanForLinesOfCodeCalculations() {
        String code = JavaExampleFragments.FRAGMENT_1;

        JavaAnalyzer analyzer = new JavaAnalyzer();

        CleanedContent cleanedContent = analyzer.cleanForLinesOfCodeCalculations(new SourceFile(new File(""), code));

        assertEquals(JavaExampleFragments.FRAMGENT_1_CLEANED, cleanedContent.getCleanedContent());
    }

    @Test
    public void cleanForDuplicationCalculations() {
        String code = JavaExampleFragments.FRAGMENT_1;

        JavaAnalyzer analyzer = new JavaAnalyzer();

        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(new SourceFile(new File(""), code));

        assertEquals(JavaExampleFragments.FRAMGENT_1_CLEANED_FOR_DUPLICATION, cleanedContent.getCleanedContent());
    }

    @Test
    public void extractUnits() {
        String code = JavaExampleFragments.FRAGMENT_1;

        JavaAnalyzer analyzer = new JavaAnalyzer();

        List<UnitInfo> unitInfos = analyzer.extractUnits(new SourceFile(new File(""), code));

        assertEquals(unitInfos.size(), 2);
        assertEquals(unitInfos.get(0).getShortName(), "protected String getRelativePath()");
        assertEquals(unitInfos.get(0).getLinesOfCode(), 3);
        assertEquals(unitInfos.get(0).getMcCabeIndex(), 1);
        assertEquals(unitInfos.get(1).getShortName(), "protected String getRelativePath()");
        assertEquals(unitInfos.get(1).getLinesOfCode(), 28);
        assertEquals(unitInfos.get(1).getMcCabeIndex(), 6);
    }

    @Test
    public void extractDependencies() {
    }

    @Test
    public void getFeaturesDescription() {
    }
}
