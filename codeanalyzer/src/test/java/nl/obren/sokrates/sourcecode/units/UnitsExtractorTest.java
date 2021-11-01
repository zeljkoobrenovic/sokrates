/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.units;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzerFactory;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class UnitsExtractorTest {
    @Test
    public void getUnits() throws Exception {
        String code1 = "public class ExampleMinNumber {\n" +
                "   \n" +
                "   public static void main(String[] args) {\n" +
                "      int a = 11;\n" +
                "      int b = 6;\n" +
                "      int c = minFunction(a, b);\n" +
                "      System.out.println(\"Minimum Value = \" + c);\n" +
                "   }\n" +
                "\n" +
                "   /** returns the minimum of two numbers */\n" +
                "   public static int minFunction(int n1, int n2) {\n" +
                "      int min;\n" +
                "      if (n1 > n2)\n" +
                "         min = n2;\n" +
                "      else\n" +
                "         min = n1;\n" +
                "\n" +
                "      return min; \n" +
                "   }\n" +
                "}";

        String code2 = "public class ExampleVoid {\n" +
                "\n" +
                "   public static void main(String[] args) {\n" +
                "      methodRankPoints(255.7);\n" +
                "   }\n" +
                "\n" +
                "   public static void methodRankPoints(double points) {\n" +
                "      if (points >= 202.5) {\n" +
                "         System.out.println(\"Rank:A1\");\n" +
                "      }else if (points >= 122.4) {\n" +
                "         System.out.println(\"Rank:A2\");\n" +
                "      }else {\n" +
                "         System.out.println(\"Rank:A3\");\n" +
                "      }\n" +
                "   }\n" +
                "}";

        LanguageAnalyzerFactory factory = LanguageAnalyzerFactory.getInstance();
        List<SourceFile> sourceFiles = Arrays.asList(new SourceFile(new File("file1.java"), code1), new SourceFile(new File("file2.java"), code2));
        List<UnitInfo> units = new UnitsExtractor().getUnits(sourceFiles, new ProgressFeedback());

        units = new UnitsExtractor().getUnits(sourceFiles, new ProgressFeedback());

        assertEquals(units.size(), 4);
        assertEquals(units.get(0).getShortName(), "public static void main()");
        assertEquals(units.get(1).getShortName(), "public static int minFunction()");
        assertEquals(units.get(2).getShortName(), "public static void main()");
        assertEquals(units.get(3).getShortName(), "public static void methodRankPoints()");
    }

}
