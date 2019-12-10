/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.php;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.dependencies.Dependency;
import nl.obren.sokrates.sourcecode.dependencies.DependencyAnchor;
import nl.obren.sokrates.sourcecode.lang.php.PhpAnalyzer;
import nl.obren.sokrates.sourcecode.lang.php.PhpHeuristicDependenciesExtractor;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class PhpAnalyzerTest {
    @Test
    public void cleanForLinesOfCodeCalculations() throws Exception {
        PhpAnalyzer analyzer = new PhpAnalyzer();
        String code = "<?php\n" +
                "echo \"Hello World!\"; // This will print out Hello World!\n" +
                "echo \"<br />Psst...You can't see my PHP comments!\"; // echo \"nothing\";\n" +
                "// echo \"My name is Humperdinkle!\";\n" +
                "# echo \"I don't do anything either\";\n" +
                "/* This Echo statement will print out my message to the\n" +
                "the place in which I reside on.  In other words, the World. */\n" +
                "echo \"Hello World!\"; \n" +
                "/* echo \"My name is Humperdinkle!\";\n" +
                "echo \"No way! My name is Uber PHP Programmer!\";\n" +
                "*/" +
                "?>";
        assertEquals(analyzer.cleanForLinesOfCodeCalculations(new SourceFile(new File("dummy.php"), code)).getCleanedContent(), "<?php\n" +
                "echo \"Hello World!\"; \n" +
                "echo \"<br />Psst...You can't see my PHP comments!\"; \n" +
                "echo \"Hello World!\"; \n" +
                "?>");
    }

    @Test
    public void cleanForDuplicationCalculations() throws Exception {
        PhpAnalyzer analyzer = new PhpAnalyzer();
        String code = "<?php\n" +
                "echo \"Hello World!\"; // This will print out Hello World!\n" +
                "echo \"<br />Psst...You can't see my PHP comments!\"; // echo  \"nothing\";\n" +
                "// echo \"My name is Humperdinkle!\";\n" +
                "# echo \"I don't do anything either\";\n" +
                "/* This Echo statement will print out my message to the\n" +
                "the place in which I reside on.  In other words, the World. */\n" +
                "echo \"Hello World!\"; \n" +
                "/* echo \"My name is Humperdinkle!\";\n" +
                "echo \"No way! My name is Uber PHP Programmer!\";\n" +
                "*/" +
                "?>";
        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(new SourceFile(new File("dummy.php"), code));
        assertEquals(cleanedContent.getCleanedContent(), "echo \"Hello World!\";\n" +
                "echo \"<br />Psst...You can't see my PHP comments!\";\n" +
                "echo \"Hello World!\";");

        assertEquals(cleanedContent.getCleanedLinesCount(), 3);
        assertEquals(cleanedContent.getFileLineIndexes().size(), 3);
        assertEquals(cleanedContent.getFileLineIndexes().get(0).intValue(), 1);
        assertEquals(cleanedContent.getFileLineIndexes().get(1).intValue(), 2);
        assertEquals(cleanedContent.getFileLineIndexes().get(2).intValue(), 7);

    }

    @Test
    public void extractUnits1() throws Exception {
        PhpAnalyzer analyzer = new PhpAnalyzer();
        String code = "function sum($array,$max){   //For Reference, use:  \"&$array\"\n" +
                "    $sum=0;\n" +
                "    for ($i=0; $i<2; $i++){\n" +
                "        #$array[$i]++;        //Uncomment this line to modify the array within the function.\n" +
                "        $sum += $array[$i];  \n" +
                "    }\n" +
                "    return ($sum);\n" +
                "}\n";
        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File("dummy.php"), code));
        assertEquals(units.size(), 1);
        assertEquals(units.get(0).getShortName(), "function sum()");
        assertEquals(units.get(0).getLinesOfCode(), 7);
        assertEquals(units.get(0).getMcCabeIndex(), 2);
        assertEquals(units.get(0).getNumberOfParameters(), 2);
    }

    @Test
    public void extractUnits2() throws Exception {
        PhpAnalyzer analyzer = new PhpAnalyzer();
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
        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File("dummy.php"), code));
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
        PhpAnalyzer analyzer = new PhpAnalyzer();
        String code1 = "namespace a;\n" +
                "use b;\n" +
                "";
        String code2 = "namespace b;\n" +
                "\n" +
                "//...";

        SourceFile sourceFile1 = new SourceFile(new File("a.php"), code1);
        sourceFile1.getLogicalComponents().add(new NamedSourceCodeAspect("CompA"));

        SourceFile sourceFile2 = new SourceFile(new File("b.h"), code2);
        sourceFile2.getLogicalComponents().add(new NamedSourceCodeAspect("CompB"));

        List<DependencyAnchor> dependencyAnchors = new PhpHeuristicDependenciesExtractor().extractDependencyAnchors(sourceFile2);
        List<Dependency> dependencies = analyzer.extractDependencies(Arrays.asList(sourceFile1, sourceFile2), new ProgressFeedback()).getDependencies();

        assertEquals(dependencies.size(), 1);
        assertEquals(dependencies.get(0).getDependencyString(), "a -> b");
        assertEquals(dependencies.get(0).getComponentDependency(""), "CompA -> CompB");
    }
}
