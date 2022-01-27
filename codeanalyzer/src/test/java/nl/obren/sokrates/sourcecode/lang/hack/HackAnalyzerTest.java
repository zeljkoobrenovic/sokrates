package nl.obren.sokrates.sourcecode.lang.hack;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static nl.obren.sokrates.sourcecode.lang.hack.HackSamples.*;
import static org.junit.jupiter.api.Assertions.*;

class HackAnalyzerTest {

    @Test
    void cleanForLinesOfCodeCalculations() {
        HackAnalyzer analyzer = new HackAnalyzer();

        SourceFile sourceFile = new SourceFile();
        sourceFile.setContent(SAMPLE_1);

        CleanedContent cleanedContent = analyzer.cleanForLinesOfCodeCalculations(sourceFile);

        assertEquals(SAMPLE_1_CLEANED_LOC, cleanedContent.getCleanedContent());
    }

    @Test
    void cleanForDuplicationCalculations() {
        HackAnalyzer analyzer = new HackAnalyzer();

        SourceFile sourceFile = new SourceFile();
        sourceFile.setContent(SAMPLE_1);

        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(sourceFile);

        assertEquals(SAMPLE_1_CLEANED_DUPLICATION, cleanedContent.getCleanedContent());
    }

    @Test
    void extractUnits() {
        HackAnalyzer analyzer = new HackAnalyzer();

        SourceFile sourceFile = new SourceFile(new File("a.hack"));
        sourceFile.setContent(SAMPLE_1);

        List<UnitInfo> units = analyzer.extractUnits(sourceFile);
        assertEquals(units.size(), 3);

        assertEquals(units.get(0).getShortName(), "final public function __construct()");
        assertEquals(units.get(0).getStartLine(), 23);
        assertEquals(units.get(0).getEndLine(), 25);
        assertEquals(units.get(0).getMcCabeIndex(), 1);
        assertEquals(units.get(0).getCleanedBody(), "  final public function __construct(protected IHackCodegenConfig $config) {\n" +
                "    $this->code = new _Private\\StrBuffer();\n  }\n");

        assertEquals(units.get(1).getShortName(), "final public function newLine()");
        assertEquals(units.get(1).getStartLine(), 35);
        assertEquals(units.get(1).getEndLine(), 39);
        assertEquals(units.get(1).getMcCabeIndex(), 1);
        assertEquals(units.get(1).getCleanedBody(), "  final public function newLine(): this {\n" +
                "    $this->code->append(\"\");\n    $this->isNewLine = true;\n    return $this;\n  }\n");

        assertEquals(units.get(2).getShortName(), "final public function ensureNewLine()");
        assertEquals(units.get(2).getStartLine(), 44);
        assertEquals(units.get(2).getEndLine(), 49);
        assertEquals(units.get(2).getMcCabeIndex(), 2);
        assertEquals(units.get(2).getCleanedBody(), "  final public function ensureNewLine(): this {\n" +
                "    if (!$this->isNewLine) {\n      $this->newLine();\n    }\n    return $this;\n  }\n");
    }
}