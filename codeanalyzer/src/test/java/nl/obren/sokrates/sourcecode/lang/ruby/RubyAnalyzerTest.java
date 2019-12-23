/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.ruby;

import junit.framework.TestCase;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.lang.java.JavaAnalyzer;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class RubyAnalyzerTest {
    private final static String SIMPLE_CODE = "# sample program showing special characters like comments\n" +
            "# I'm a comment line\n" +
            "a = 1  #notice no semicolon and no type declaration\n" +
            "b = 2; c = 3 #notice two statements on one line\n" +
            "name = \"Abraham \\\n" +
            "Lincoln\"   # a line continued by trailing \\\n" +
            "puts \"#{name}\"\n" +
            "=begin\n" +
            "I'm ignored.\n" +
            "So am I.\n" +
            "=end\n" +
            "puts \"goodbye\"\n" +
            "__END__\n" +
            "1\n" +
            "2\n" +
            "3\n" +
            "4";
    private static final String CODE_WITH_UNIT = SIMPLE_CODE + "\n\n" + "def multiply(a,b)\n" +
            "  product = a * b\n" +
            "  return product\n" +
            "end";
    @Test
    @Ignore
    public void extractUnits() throws Exception {
        RubyAnalyzer analyzer = new RubyAnalyzer();
        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File("dummy.rb"), CODE_WITH_UNIT));

        assertEquals(units.size(), 1);
        assertEquals(units.get(0).getCleanedBody(), "def multiply(a,b)\n" +
                "  product = a * b\n" +
                "  return product\n" +
                "end\n");
        assertEquals(units.get(0).getShortName(), "multiply");
        assertEquals(units.get(0).getLinesOfCode(), 4);
        assertEquals(units.get(0).getMcCabeIndex(), 1);
    }

    @Test
    public void cleanForLinesOfCodeCalculations() throws Exception {
        RubyAnalyzer analyzer = new RubyAnalyzer();
        SourceFile sourceFile = new SourceFile(new File("dummy.rb"), SIMPLE_CODE);

        assertEquals(analyzer.cleanForLinesOfCodeCalculations(sourceFile).getCleanedContent(), "a = 1  \n" +
                "b = 2; c = 3 \n" +
                "name = \"Abraham \\\n" +
                "Lincoln\"   \n" +
                "puts \"#{name}\"\n" +
                "puts \"goodbye\"");
    }


    @Test
    public void cleanForDuplicationCalculations() throws Exception {
        RubyAnalyzer analyzer = new RubyAnalyzer();
        SourceFile sourceFile = new SourceFile(new File("dummy.rb"), SIMPLE_CODE);

        assertEquals(analyzer.cleanForDuplicationCalculations(sourceFile).getCleanedContent(), "a = 1\n" +
                "b = 2; c = 3\n" +
                "name = \"Abraham \\\n" +
                "Lincoln\"\n" +
                "puts \"#{name}\"\n" +
                "puts \"goodbye\"");
    }

}
