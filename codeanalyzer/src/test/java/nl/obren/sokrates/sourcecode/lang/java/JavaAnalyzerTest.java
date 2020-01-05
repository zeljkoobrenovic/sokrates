/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.java;

import junit.framework.TestCase;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class JavaAnalyzerTest {

    @Test
    public void cleanForLinesOfCodeCalculations() {
        String code = JavaExampleFragments.FRAGMENT_1;

        JavaAnalyzer analyzer = new JavaAnalyzer();

        CleanedContent cleanedContent = analyzer.cleanForLinesOfCodeCalculations(new SourceFile(new File(""), code));

        assertEquals(JavaExampleFragments.FRAGMENT_1_CLEANED, cleanedContent.getCleanedContent());
    }

    @Test
    public void cleanForLinesOfCodeCalculations2() {
        String code = JavaExampleFragments.FRAGMENT_2;

        JavaAnalyzer analyzer = new JavaAnalyzer();

        CleanedContent cleanedContent = analyzer.cleanForLinesOfCodeCalculations(new SourceFile(new File(""), code));

        assertEquals(JavaExampleFragments.FRAGMENT_2_CLEANED, cleanedContent.getCleanedContent());
    }

    @Test
    public void cleanForLinesOfCodeCalculations3() {
        String code = JavaExampleFragments.FRAGMENT_3;

        JavaAnalyzer analyzer = new JavaAnalyzer();

        CleanedContent cleanedContent = analyzer.cleanForLinesOfCodeCalculations(new SourceFile(new File(""), code));

        assertEquals(JavaExampleFragments.FRAGMENT_3_CLEANED, cleanedContent.getCleanedContent());
    }

    @Test
    public void cleanForDuplicationCalculations() {
        String code = JavaExampleFragments.FRAGMENT_1;

        JavaAnalyzer analyzer = new JavaAnalyzer();

        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(new SourceFile(new File(""), code));

        assertEquals(JavaExampleFragments.FRAGMENT_1_CLEANED_FOR_DUPLICATION, cleanedContent.getCleanedContent());
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
    public void extractUnitsComplex() throws Exception {
        String content = "package nl.obren.codeexplorer.common;\n" +
                "\n" +
                "import java.lang.*;\n" +
                "import nl.obren.*;\n" +
                "\n" +
                "/* This class is a generic mechanism for feedback interaction.*/\n" +
                "public class ProgressFeedback {\n" +
                "    public void start() {}\n" +
                "    public void end() {}\n" +
                "    public void setText(String text) {}\n" +
                "\n" +
                "    // should be called to check if a user canceled the process\n" +
                "    // i.e. enables bi-directional feedback\n" +
                "    public boolean canceled() {\n" +
                "        if (true) return false else return true;\n" +
                "    }\n" +
                "\n" +
                "    public void progress(int currentValue, int endValue) {\n" +
                "    }\n" +
                "}\n";

        JavaAnalyzer analyzer = new JavaAnalyzer();
        assertEquals(analyzer.extractUnits(new SourceFile(new File(""), content)).size(), 5);
        assertEquals(analyzer.extractUnits(new SourceFile(new File(""), content)).get(0).getShortName(), "public void start()");
        assertEquals(analyzer.extractUnits(new SourceFile(new File(""), content)).get(0).getLinesOfCode(), 1);
        assertEquals(analyzer.extractUnits(new SourceFile(new File(""), content)).get(0).getMcCabeIndex(), 1);
        assertEquals(analyzer.extractUnits(new SourceFile(new File(""), content)).get(1).getShortName(), "public void end()");
        assertEquals(analyzer.extractUnits(new SourceFile(new File(""), content)).get(2).getShortName(), "public void setText()");
        assertEquals(analyzer.extractUnits(new SourceFile(new File(""), content)).get(3).getShortName(), "public boolean canceled()");
        assertEquals(analyzer.extractUnits(new SourceFile(new File(""), content)).get(3).getLinesOfCode(), 3);
        assertEquals(analyzer.extractUnits(new SourceFile(new File(""), content)).get(3).getMcCabeIndex(), 2);
        assertEquals(analyzer.extractUnits(new SourceFile(new File(""), content)).get(4).getShortName(), "public void progress()");
    }

    @Test
    public void cleanForLinesOfCodeCalculationsComplex() throws Exception {
        String content = "package nl.obren.codeexplorer.common;\n" +
                "\n" +
                "import java.lang.*;\n" +
                "import nl.obren.*;\n" +
                "\n" +
                "/* This class is a generic mechanism for feedback interaction.*/\n" +
                "public class ProgressFeedback {\n" +
                "    public void start() {}\n" +
                "    public void end() {}\n" +
                "    public void setText(String text) {}\n" +
                "\n" +
                "    // should be called to check if a user canceled the process\n" +
                "    // i.e. enables bi-directional feedback\n" +
                "    public boolean canceled() {\n" +
                "        return false;\n" +
                "    }\n" +
                "\n" +
                "    public void progress(int currentValue, int endValue) {\n" +
                "    }\n" +
                "}\n";

        String cleanedContent = "package nl.obren.codeexplorer.common;\n" +
                "import java.lang.*;\n" +
                "import nl.obren.*;\n" +
                "public class ProgressFeedback {\n" +
                "    public void start() {}\n" +
                "    public void end() {}\n" +
                "    public void setText(String text) {}\n" +
                "    public boolean canceled() {\n" +
                "        return false;\n" +
                "    }\n" +
                "    public void progress(int currentValue, int endValue) {\n" +
                "    }\n" +
                "}";

        JavaAnalyzer analyzer = new JavaAnalyzer();
        assertEquals(analyzer.cleanForLinesOfCodeCalculations(new SourceFile(new File(""), content)).getCleanedContent(), cleanedContent);
    }


    @Test
    public void cleanForDuplicationCalculationsComplex() throws Exception {
        String content = "package nl.obren.codeexplorer.common;\n" +
                "\n" +
                "import java.lang.*;\n" +
                "import nl.obren.*;\n" +
                "\n" +
                "/* This class is a generic mechanism for feedback interaction.*/\n" +
                "public class ProgressFeedback {\n" +
                "    public void start() {}\n" +
                "    public void end() {}\n" +
                "    public void setText(String text) {}\n" +
                "\n" +
                "    // should be called to check if a user canceled the process\n" +
                "    // i.e. enables bi-directional feedback\n" +
                "    public boolean canceled() {\n" +
                "        return false;\n" +
                "    }\n" +
                "\n" +
                "    public void progress(int currentValue, int endValue) {\n" +
                "    }\n" +
                "}\n";

        String result = "public class ProgressFeedback {\n" +
                "public void start() {}\n" +
                "public void end() {}\n" +
                "public void setText(String text) {}\n" +
                "public boolean canceled() {\n" +
                "return false;\n" +
                "public void progress(int currentValue, int endValue) {";

        SourceFile sourceFile = new SourceFile(new File("dummy.java"), content);

        JavaAnalyzer analyzer = new JavaAnalyzer();
        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(sourceFile);

        assertEquals(cleanedContent.getCleanedContent(), result);
        TestCase.assertEquals(cleanedContent.getCleanedLinesCount(), 7);
        TestCase.assertEquals(cleanedContent.getFileLineIndexes().size(), 7);
        TestCase.assertEquals(cleanedContent.getFileLineIndexes().get(0).intValue(), 6);
        TestCase.assertEquals(cleanedContent.getFileLineIndexes().get(1).intValue(), 7);
        TestCase.assertEquals(cleanedContent.getFileLineIndexes().get(2).intValue(), 8);
        TestCase.assertEquals(cleanedContent.getFileLineIndexes().get(3).intValue(), 9);
        TestCase.assertEquals(cleanedContent.getFileLineIndexes().get(4).intValue(), 13);
        TestCase.assertEquals(cleanedContent.getFileLineIndexes().get(5).intValue(), 14);
        TestCase.assertEquals(cleanedContent.getFileLineIndexes().get(6).intValue(), 17);
    }


    @Test
    public void parseStatic() throws Exception {
        JavaAnalyzer analyzer = new JavaAnalyzer();

        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File("test"), "class A {\n" +
                "    static {\n" +
                "         MAP.put('a', 'a');\n" +
                "         if (true) {\n" +
                "             MAP.put('b', 'b');\n" +
                "         }\n" +
                "    }\n\n" +
                "    public static void a() {\n" +
                "        if (a) {\n" +
                "            doA();\n" +
                "        } else {\n" +
                "            doNotA();\n" +
                "        }\n" +
                "    }\n" +
                "}"));

        Assert.assertEquals(units.size(), 2);
        Assert.assertEquals(units.get(0).getShortName(), "static");
        Assert.assertEquals(units.get(0).getLinesOfCode(), 6);
        Assert.assertEquals(units.get(0).getMcCabeIndex(), 2);
        Assert.assertEquals(units.get(0).getNumberOfParameters(), 0);

        Assert.assertEquals(units.get(1).getShortName(), "public static void a()");
        Assert.assertEquals(units.get(1).getLinesOfCode(), 7);
        Assert.assertEquals(units.get(1).getMcCabeIndex(), 2);
        Assert.assertEquals(units.get(1).getNumberOfParameters(), 0);
    }

    @Test
    public void testUnitExtractionWithCases() {
        JavaAnalyzer analyzer = new JavaAnalyzer();

        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File("dummy.java"), JavaExampleFragments.FRAGMENT_4));

        Assert.assertEquals(units.size(), 1);
        Assert.assertEquals(units.get(0).getShortName(), "public NamedSourceCodeAspect getScope()");
        Assert.assertEquals(units.get(0).getLinesOfCode(), 13);
        Assert.assertEquals(units.get(0).getMcCabeIndex(), 5);
        Assert.assertEquals(units.get(0).getNumberOfParameters(), 1);
    }
}
