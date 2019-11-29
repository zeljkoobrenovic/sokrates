package nl.obren.sokrates.sourcecode.lang;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.dependencies.Dependency;
import nl.obren.sokrates.sourcecode.lang.csharp.CSharpAnalyzer;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class CSharpAnalyzerTest {
    @Test
    public void cleanForLinesOfCodeCalculations() throws Exception {
        CSharpAnalyzer analyzer = new CSharpAnalyzer();
        String code = "namespace PC\n" +
                "{\n" +
                "    // Define an alias for the nested namespace.\n" +
                "    using Project = PC.MyCompany.Project;\n" +
                "    class A\n" +
                "    {\n" +
                "        void M()\n" +
                "        {\n" +
                "            // Use the alias\n" +
                "            Project.MyClass mc = new Project.MyClass();\n" +
                "        }\n" +
                "    }\n" +
                "    namespace MyCompany\n" +
                "    {\n" +
                "        namespace Project\n" +
                "        {\n" +
                "            public class MyClass { }\n" +
                "        }\n" +
                "        /*namespace Project\n" +
                "        {\n" +
                "            public class MyClass { }\n" +
                "        }*/\n" +
                "    }\n" +
                "}";
        assertEquals(analyzer.cleanForLinesOfCodeCalculations(new SourceFile(new File("dummy.cs"), code)).getCleanedContent(), "namespace PC\n" +
                "{\n" +
                "    using Project = PC.MyCompany.Project;\n" +
                "    class A\n" +
                "    {\n" +
                "        void M()\n" +
                "        {\n" +
                "            Project.MyClass mc = new Project.MyClass();\n" +
                "        }\n" +
                "    }\n" +
                "    namespace MyCompany\n" +
                "    {\n" +
                "        namespace Project\n" +
                "        {\n" +
                "            public class MyClass { }\n" +
                "        }\n" +
                "    }\n" +
                "}");
    }

    @Test
    public void cleanForDuplicationCalculations() throws Exception {
        CSharpAnalyzer analyzer = new CSharpAnalyzer();
        String code = "namespace PC\n" +
                "{\n" +
                "    // Define an alias for the nested namespace.\n" +
                "    using Project = PC.MyCompany.Project;\n" +
                "    class A\n" +
                "    {\n" +
                "        void M()\n" +
                "        {\n" +
                "            // Use the alias\n" +
                "            Project.MyClass mc = new Project.MyClass();\n" +
                "        }\n" +
                "    }\n" +
                "    namespace MyCompany\n" +
                "    {\n" +
                "        namespace Project\n" +
                "        {\n" +
                "            public class MyClass { }\n" +
                "        }\n" +
                "        /*namespace Project\n" +
                "        {\n" +
                "            public class MyClass { }\n" +
                "        }*/\n" +
                "    }\n" +
                "}";
        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(new SourceFile(new File("dummy.cs"), code));
        assertEquals(cleanedContent.getCleanedContent(), "class A\n" +
                "void M()\n" +
                "Project.MyClass mc = new Project.MyClass();\n" +
                "public class MyClass { }");
        assertEquals(cleanedContent.getCleanedLinesCount(), 4);
        assertEquals(cleanedContent.getFileLineIndexes().size(), 4);
        assertEquals(cleanedContent.getFileLineIndexes().get(0).intValue(), 4);
        assertEquals(cleanedContent.getFileLineIndexes().get(1).intValue(), 6);
        assertEquals(cleanedContent.getFileLineIndexes().get(2).intValue(), 9);
        assertEquals(cleanedContent.getFileLineIndexes().get(3).intValue(), 16);
    }

    @Test
    public void extractUnits1() throws Exception {
        CSharpAnalyzer analyzer = new CSharpAnalyzer();
        String code = "#include \"add.h\"\n" +
                "\n" +
                "int triple(int x)\n" +
                "{\n" +
                "    // comment\n" +
                "    return add(x, add(x,x));\n" +
                "}";
        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File("dummy.cs"), code));
        assertEquals(units.size(), 1);
        assertEquals(units.get(0).getShortName(), "int triple()");
        assertEquals(units.get(0).getLinesOfCode(), 4);
        assertEquals(units.get(0).getMcCabeIndex(), 1);
        assertEquals(units.get(0).getNumberOfParameters(), 1);
    }

    @Test
    public void extractUnits2() throws Exception {
        CSharpAnalyzer analyzer = new CSharpAnalyzer();
        String code = "public void Caller()\n" +
                "{\n" +
                "    int numA = 4;\n" +
                "    // Call with an int variable.\n" +
                "    int productA = Square(numA);\n" +
                "\n" +
                "    int numB = 32;\n" +
                "    // Call with another int variable.\n" +
                "    int productB = Square(numB);\n" +
                "\n" +
                "    // Call with an integer literal.\n" +
                "    int productC = Square(12);\n" +
                "\n" +
                "    // Call with an expression that evaulates to int.\n" +
                "    productC = Square(productA * 3);\n" +
                "}\n" +
                "\n" +
                "int Square(int i)\n" +
                "{\n" +
                "    // Store input argument in a local variable.\n" +
                "    while (true) if (true) int input = i;\n" +
                "    return input * input;\n" +
                "}";
        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File("dummy.cs"), code));
        assertEquals(units.size(), 2);
        assertEquals(units.get(0).getShortName(), "public void Caller()");
        assertEquals(units.get(0).getLinesOfCode(), 9);
        assertEquals(units.get(0).getMcCabeIndex(), 1);
        assertEquals(units.get(0).getNumberOfParameters(), 0);
        assertEquals(units.get(1).getShortName(), "int Square()");
        assertEquals(units.get(1).getLinesOfCode(), 5);
        assertEquals(units.get(1).getMcCabeIndex(), 3);
        assertEquals(units.get(1).getNumberOfParameters(), 1);
    }

    @Test
    public void extractDependencies() throws Exception {
        CSharpAnalyzer analyzer = new CSharpAnalyzer();
        String code1 = "namespace A {\n" +
                "{\n" +
                "   using B;\n" +
                "}\n" +
                "}";
        String code2 = "namespace B {\n" +
                "\n" +
                "int triple(int x)\n" +
                "{\n" +
                "    // comment\n" +
                "    return add(x, add(x,x));\n" +
                "}\n" +
                "}";
        SourceFile sourceFile1 = new SourceFile(new File("a.cs"), code1);
        sourceFile1.getLogicalComponents().add(new NamedSourceCodeAspect("CompA"));
        SourceFile sourceFile2 = new SourceFile(new File("b.cs"), code2);
        sourceFile2.getLogicalComponents().add(new NamedSourceCodeAspect("CompB"));
        List<Dependency> dependencies = analyzer.extractDependencies(Arrays.asList(sourceFile1, sourceFile2), new ProgressFeedback()).getDependencies();
        assertEquals(dependencies.size(), 1);
        assertEquals(dependencies.get(0).getDependencyString(), "A -> B");
        assertEquals(dependencies.get(0).getComponentDependency(""), "CompA -> CompB");
    }
}
