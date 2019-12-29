/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.cpp;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class CStyleAnalyzerTest {
    @Test
    public void cleanForLinesOfCodeCalculations() throws Exception {
        CStyleAnalyzer analyzer = new CStyleAnalyzer();
        String code = "if (opt.equals (\"e\"))\n" +
                "   opt_enabled = true;\n" +
                "   // opt_enabled = true;\n" +
                "   a(); // opt_enabled = false;\n" +
                " /*\n" +
                "  if (opt.equals (\"d\"))\n" +
                "   opt_debug = true;\n" +
                " // */\n";
        assertEquals(analyzer.cleanForLinesOfCodeCalculations(new SourceFile(new File("dummy.c"), code)).getCleanedContent(), "if (opt.equals (\"e\"))\n" +
                "   opt_enabled = true;\n" +
                "   a(); ");
    }

    @Test
    public void cleanForDuplicationCalculations() throws Exception {
        CStyleAnalyzer analyzer = new CStyleAnalyzer();
        String code = "#include \"add.h\"\n" +
                "\n" +
                "int triple(int x)\n" +
                "{\n" +
                "    // comment\n" +
                "    return add(x, add(x,x));\n" +
                "}";
        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(new SourceFile(new File("dummy.c"), code));
        assertEquals(cleanedContent.getCleanedContent(), "int triple(int x)\n" +
                "return add(x, add(x,x));");
        assertEquals(cleanedContent.getCleanedLinesCount(), 2);
        assertEquals(cleanedContent.getFileLineIndexes().size(), 2);
        assertEquals(cleanedContent.getFileLineIndexes().get(0).intValue(), 2);
        assertEquals(cleanedContent.getFileLineIndexes().get(1).intValue(), 5);
    }

    @Test
    public void extractUnits1() throws Exception {
        CStyleAnalyzer analyzer = new CStyleAnalyzer();
        String code = "#include \"add.h\"\n" +
                "\n" +
                "int triple(int x)\n" +
                "{\n" +
                "    // comment\n" +
                "    return add(x, add(x,x));\n" +
                "}";
        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File("dummy.c"), code));
        assertEquals(units.size(), 1);
        assertEquals(units.get(0).getShortName(), "int triple()");
        assertEquals(units.get(0).getLinesOfCode(), 4);
        assertEquals(units.get(0).getMcCabeIndex(), 1);
        assertEquals(units.get(0).getNumberOfParameters(), 1);
    }

    @Test
    public void extractUnitsMultiLine() throws Exception {
        CStyleAnalyzer analyzer = new CStyleAnalyzer();
        String code = "#include \"add.h\"\n" +
                "\n" +
                "int triple(int x)\n" +
                "{\n" +
                "    // comment\n" +
                "    return add(x, add(x,x));\n" +
                "}";
        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File("dummy.c"), code));
        assertEquals(units.size(), 1);
        assertEquals(units.get(0).getShortName(), "int triple()");
        assertEquals(units.get(0).getCleanedBody(), "int triple(int x)\n" +
                "{\n    return add(x, add(x,x));\n}\n");
        assertEquals(units.get(0).getLinesOfCode(), 4);
        assertEquals(units.get(0).getMcCabeIndex(), 1);
        assertEquals(units.get(0).getNumberOfParameters(), 1);
    }

    @Test
    public void extractUnits2() throws Exception {
        CStyleAnalyzer analyzer = new CStyleAnalyzer();
        String code = "#include <stdio.h>\n" +
                " \n" +
                "/* function declaration */\n" +
                "int max(int num1, int num2);\n" +
                " \n" +
                "int main () {\n" +
                "\n" +
                "   /* local variable definition */\n" +
                "   int a = 100;\n" +
                "   int b = 200;\n" +
                "   int ret;\n" +
                " \n" +
                "   /* calling a function to get max value */\n" +
                "   ret = max(a, b);\n" +
                " \n" +
                "   printf( \"Max value is : %d\\n\", ret );\n" +
                " \n" +
                "   return 0;\n" +
                "}\n" +
                " \n" +
                "/* function returning the max between two numbers */\n" +
                "int max(int num1, int num2) {\n" +
                "\n" +
                "   /* local variable declaration */\n" +
                "   int result;\n" +
                " \n" +
                "   if (num1 > num2)\n" +
                "      result = num1;\n" +
                "   else\n" +
                "      result = num2;\n" +
                " \n" +
                "   return result; \n" +
                "}";
        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File("dummy.c"), code));
        assertEquals(units.size(), 2);
        assertEquals(units.get(0).getShortName(), "int main()");
        assertEquals(units.get(0).getLinesOfCode(), 8);
        assertEquals(units.get(0).getMcCabeIndex(), 1);
        assertEquals(units.get(0).getNumberOfParameters(), 0);
        assertEquals(units.get(1).getShortName(), "int max()");
        assertEquals(units.get(1).getLinesOfCode(), 8);
        assertEquals(units.get(1).getMcCabeIndex(), 2);
        assertEquals(units.get(1).getNumberOfParameters(), 2);
    }

    @Test
    public void extractDependencies() throws Exception {

    }
}
