package nl.obren.sokrates.sourcecode.lang;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.SourceCodeAspect;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.dependencies.Dependency;
import nl.obren.sokrates.sourcecode.lang.cpp.CppAnalyzer;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class CppAnalyzerTest {
    @Test
    public void cleanForLinesOfCodeCalculations() throws Exception {
        CppAnalyzer analyzer = new CppAnalyzer();
        String code = "// function example\n" +
                "#include <iostream>\n" +
                "using namespace std;\n" +
                "\n" +
                "int addition (int a, int b)\n" +
                "{\n" +
                "  int r;\n" +
                "  r=a+b;\n" +
                "  return r;\n" +
                "}\n" +
                "\n" +
                "int main ()\n" +
                "{\n" +
                "  int z;\n" +
                "  z = addition (5,3);\n" +
                "  cout << \"The result is \" << z;\n" +
                "}";
        assertEquals(analyzer.cleanForLinesOfCodeCalculations(new SourceFile(new File("dummy.cpp"), code)).getCleanedContent(), "#include <iostream>\n" +
                "using namespace std;\n" +
                "int addition (int a, int b)\n" +
                "{\n" +
                "  int r;\n" +
                "  r=a+b;\n" +
                "  return r;\n" +
                "}\n" +
                "int main ()\n" +
                "{\n" +
                "  int z;\n" +
                "  z = addition (5,3);\n" +
                "  cout << \"The result is \" << z;\n" +
                "}");
    }

    @Test
    public void cleanForDuplicationCalculations() throws Exception {
        CppAnalyzer analyzer = new CppAnalyzer();
        String code = "// function example\n" +
                "#include <iostream>\n" +
                "using namespace std;\n" +
                "\n" +
                "int addition (int a, int b)\n" +
                "{\n" +
                "  int r;\n" +
                "  r=a+b;\n" +
                "  return r;\n" +
                "}\n" +
                "\n" +
                "int main ()\n" +
                "{\n" +
                "  int z;\n" +
                "  z = addition (5,3);\n" +
                "  cout << \"The result is \" << z;\n" +
                "}";
        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(new SourceFile(new File("dummy.cpp"), code));
        assertEquals(cleanedContent.getCleanedContent(), "int addition (int a, int b)\n" +
                "int r;\n" +
                "r=a+b;\n" +
                "return r;\n" +
                "int main ()\n" +
                "int z;\n" +
                "z = addition (5,3);\n" +
                "cout << \"The result is \" << z;");
        assertEquals(cleanedContent.getCleanedLinesCount(), 8);
        assertEquals(cleanedContent.getFileLineIndexes().size(), 8);
        assertEquals(cleanedContent.getFileLineIndexes().get(0).intValue(), 4);
        assertEquals(cleanedContent.getFileLineIndexes().get(1).intValue(), 6);
        assertEquals(cleanedContent.getFileLineIndexes().get(2).intValue(), 7);
        assertEquals(cleanedContent.getFileLineIndexes().get(3).intValue(), 8);
        assertEquals(cleanedContent.getFileLineIndexes().get(4).intValue(), 11);
        assertEquals(cleanedContent.getFileLineIndexes().get(5).intValue(), 13);
        assertEquals(cleanedContent.getFileLineIndexes().get(6).intValue(), 14);
        assertEquals(cleanedContent.getFileLineIndexes().get(7).intValue(), 15);
    }

    @Test
    public void extractUnits1() throws Exception {
        CppAnalyzer analyzer = new CppAnalyzer();
        String code = "#include \"add.h\"\n" +
                "\n" +
                "int triple(int x)\n" +
                "{\n" +
                "    // comment\n" +
                "    return add(x, add(x,x));\n" +
                "}";
        SourceFile sourceFile = new SourceFile(new File("dummy.cpp"), code);
        List<UnitInfo> units = analyzer.extractUnits(sourceFile);
        assertEquals(units.size(), 1);
        assertEquals(units.get(0).getShortName(), "int triple()");
        assertEquals(units.get(0).getLinesOfCode(), 4);
        assertEquals(units.get(0).getMcCabeIndex(), 1);
        assertEquals(units.get(0).getNumberOfParameters(), 1);
    }

    @Test
    public void extractUnits2() throws Exception {
        CppAnalyzer analyzer = new CppAnalyzer();
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
        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File("dummy.cpp"), code));
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
        CppAnalyzer analyzer = new CppAnalyzer();
        String code1 = "#include \"b.h\"\n";
        String code2 = "#include \"add.h\"\n" +
                "\n" +
                "int triple(int x)\n" +
                "{\n" +
                "    // comment\n" +
                "    return add(x, add(x,x));\n" +
                "}";
        SourceFile sourceFile1 = new SourceFile(new File("a.cpp"), code1);
        sourceFile1.getLogicalComponents().add(new SourceCodeAspect("CompA"));
        SourceFile sourceFile2 = new SourceFile(new File("b.h"), code2);
        sourceFile2.getLogicalComponents().add(new SourceCodeAspect("CompB"));
        List<Dependency> dependencies = analyzer.extractDependencies(Arrays.asList(sourceFile1, sourceFile2), new ProgressFeedback()).getDependencies();
        assertEquals(dependencies.size(), 1);
        assertEquals(dependencies.get(0).getDependencyString(), "a.cpp -> b.h");
        assertEquals(dependencies.get(0).getComponentDependency(""), "CompA -> CompB");
    }
}