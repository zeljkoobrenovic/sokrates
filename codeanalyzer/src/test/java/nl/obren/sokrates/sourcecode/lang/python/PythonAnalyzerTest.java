/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.python;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.dependencies.Dependency;
import nl.obren.sokrates.sourcecode.lang.python.PythonAnalyzer;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class PythonAnalyzerTest {
    @Test
    public void cleanForLinesOfCodeCalculations() throws Exception {
        PythonAnalyzer analyzer = new PythonAnalyzer();
        String code = "# this is the first comment\n" +
                "spam = 1  # and this is the second comment\n" +
                "          # ... and now a third!\n" +
                "text = \"# This is not a comment because it's inside quotes.\"\n" +
                "if __name__ == \"__main__\":\n" +
                "    import sys\n" +
                "    fib(int(sys.argv[1]))";
        assertEquals(analyzer.cleanForLinesOfCodeCalculations(new SourceFile(new File("dummy.py"), code)).getCleanedContent(), "spam = 1  \n" +
                "text = \"# This is not a comment because it's inside quotes.\"\n" +
                "if __name__ == \"__main__\":\n" +
                "    import sys\n" +
                "    fib(int(sys.argv[1]))");
    }

    @Test
    public void cleanForLinesOfCodeCalculationsWithBlockComments() throws Exception {
        PythonAnalyzer analyzer = new PythonAnalyzer();
        String code = "\"\"\"\n" +
                "and this is the second comment\n" +
                " ... and now a third!\n" +
                "\"\"\"\n" +
                "if __name__ == \"__main__\":\n" +
                "    import sys\n" +
                "    fib(int(sys.argv[1]))";
        assertEquals(analyzer.cleanForLinesOfCodeCalculations(new SourceFile(new File("dummy.py"), code)).getCleanedContent(),
                "if __name__ == \"__main__\":\n" +
                "    import sys\n" +
                "    fib(int(sys.argv[1]))");
    }

    @Test
    public void cleanForDuplicationCalculations() throws Exception {
        PythonAnalyzer analyzer = new PythonAnalyzer();
        String code = "# this is the first comment\n" +
                "spam = 1  # and this is the second comment\n" +
                "          # ... and now a third!\n" +
                "text = \"# This is not a comment because it's inside quotes.\"\n" +
                "if __name__ == \"__main__\":\n" +
                "    import sys\n" +
                "    fib(int(sys.argv[1]))";
        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(new SourceFile(new File("dummy.py"), code));
        assertEquals(cleanedContent.getCleanedContent(), "spam = 1\n" +
                "text = \"# This is not a comment because it's inside quotes.\"\n" +
                "if __name__ == \"__main__\":\n" +
                "fib(int(sys.argv[1]))");
        assertEquals(cleanedContent.getCleanedLinesCount(), 4);
        assertEquals(cleanedContent.getFileLineIndexes().size(), 4);
        assertEquals(cleanedContent.getFileLineIndexes().get(0).intValue(), 1);
        assertEquals(cleanedContent.getFileLineIndexes().get(1).intValue(), 3);
        assertEquals(cleanedContent.getFileLineIndexes().get(2).intValue(), 4);
        assertEquals(cleanedContent.getFileLineIndexes().get(3).intValue(), 6);
    }

    @Test
    public void extractUnits1() throws Exception {
        PythonAnalyzer analyzer = new PythonAnalyzer();
        String code = "def printme( str ):\n" +
                "   \"This prints a passed string into this function\"\n" +
                "   print str\n" +
                "   return";
        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File("dummy.py"), code));
        assertEquals(units.size(), 1);
        assertEquals(units.get(0).getShortName(), "def printme()");
        assertEquals(units.get(0).getLinesOfCode(), 4);
        assertEquals(units.get(0).getMcCabeIndex(), 1);
        assertEquals(units.get(0).getNumberOfParameters(), 1);
    }

    @Test
    public void extractUnits2() throws Exception {
        PythonAnalyzer analyzer = new PythonAnalyzer();
        String code = "# Fibonacci numbers module\n" +
                "\n" +
                "def fib(n):    # write Fibonacci series up to n\n" +
                "    a, b = 0, 1\n" +
                "    while b < n:\n" +
                "        print b,\n" +
                "        a, b = b, a+b\n" +
                "\n" +
                "def fib2(n):   # return Fibonacci series up to n\n" +
                "    result = []\n" +
                "    a, b = 0, 1\n" +
                "    while b < n:\n" +
                "        result.append(b)\n" +
                "        a, b = b, a+b\n" +
                "    return result\n";
        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File("dummy.py"), code));
        assertEquals(units.size(), 2);
        assertEquals(units.get(0).getShortName(), "def fib()");
        assertEquals(units.get(0).getLinesOfCode(), 5);
        assertEquals(units.get(0).getMcCabeIndex(), 2);
        assertEquals(units.get(0).getNumberOfParameters(), 1);
        assertEquals(units.get(1).getShortName(), "def fib2()");
        assertEquals(units.get(1).getLinesOfCode(), 7);
        assertEquals(units.get(1).getMcCabeIndex(), 2);
        assertEquals(units.get(1).getNumberOfParameters(), 1);
    }

    @Test
    public void extractDependencies() throws Exception {
    }
}
