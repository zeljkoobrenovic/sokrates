/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.units;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzerFactory;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit line numbers are 1-based file lines, and this holds every language's extractor to that.
 *
 * <p>The convention was previously folklore: most extractors followed it, a few did not, and nothing
 * said which was right. Reports link source fragments by these numbers, so an extractor that reports
 * a 0-based index sends readers to the line above the unit, and one that reports nothing sends them
 * to line 0.
 *
 * <p>Each fixture puts a <b>comment on line 1</b>. That is the whole trick: a correct extractor can
 * never report a first unit starting at line 1, so an off-by-one and a missing line number both show
 * up as a failure here rather than as a slightly wrong link in a report.
 */
public class UnitLineNumbersTest {

    @Test
    public void javaUnitsAreOnOneBasedFileLines() {
        assertFirstUnit("a.java", "// comment\nclass A {\n  int one() {\n    return 1;\n  }\n}\n", 3, 5);
    }

    @Test
    public void javaScriptUnitsAreOnOneBasedFileLines() {
        assertFirstUnit("a.js", "// comment\nfunction one() {\n  return 1;\n}\n", 2, 4);
    }

    @Test
    public void pythonUnitsAreOnOneBasedFileLines() {
        assertFirstUnit("a.py", "# comment\ndef alpha(x):\n    return x\n", 2, 3);
    }

    @Test
    public void rubyUnitsAreOnOneBasedFileLines() {
        assertFirstUnit("a.rb", "# comment\nclass Foo\n  def bar(x)\n    x + 1\n  end\nend\n", 3, 5);
    }

    @Test
    public void visualBasicUnitsAreOnOneBasedFileLines() {
        assertFirstUnit("a.vb", "' comment\nModule M\n    Sub Alpha()\n        Dim x = 1\n    End Sub\nEnd Module\n", 3, 5);
    }

    @Test
    public void luaUnitsAreOnOneBasedFileLines() {
        assertFirstUnit("a.lua", "-- comment\nfunction alpha(x)\n  return x\nend\n", 2, 4);
    }

    @Test
    public void juliaUnitsAreOnOneBasedFileLines() {
        assertFirstUnit("a.jl", "# comment\nfunction alpha(x)\n    return x\nend\n", 2, 4);
    }

    /**
     * The property every extractor must satisfy, stated without naming a specific line: whatever the
     * language, a unit that follows a comment cannot begin on line 1, and cannot end before it starts.
     * A new extractor added without its own case above is still worth holding to this.
     */
    @Test
    public void noExtractorPlacesAUnitBeforeTheFileStarts() {
        String[][] fixtures = {
                {"a.java", "// comment\nclass A {\n  int one() {\n    return 1;\n  }\n}\n"},
                {"a.js", "// comment\nfunction one() {\n  return 1;\n}\n"},
                {"a.py", "# comment\ndef alpha(x):\n    return x\n"},
                {"a.rb", "# comment\nclass Foo\n  def bar(x)\n    x + 1\n  end\nend\n"},
                {"a.vb", "' comment\nModule M\n    Sub Alpha()\n        Dim x = 1\n    End Sub\nEnd Module\n"},
                {"a.lua", "-- comment\nfunction alpha(x)\n  return x\nend\n"},
                {"a.jl", "# comment\nfunction alpha(x)\n    return x\nend\n"},
        };

        for (String[] fixture : fixtures) {
            List<UnitInfo> units = extractUnits(fixture[0], fixture[1]);
            assertFalse(fixture[0] + ": expected at least one unit", units.isEmpty());
            for (UnitInfo unit : units) {
                assertTrue(fixture[0] + ": unit \"" + unit.getShortName().trim() + "\" starts at line "
                                + unit.getStartLine() + ", but line 1 is a comment",
                        unit.getStartLine() >= 2);
                assertTrue(fixture[0] + ": unit \"" + unit.getShortName().trim() + "\" ends at line "
                                + unit.getEndLine() + ", before it starts at " + unit.getStartLine(),
                        unit.getEndLine() >= unit.getStartLine());
            }
        }
    }

    private void assertFirstUnit(String fileName, String content, int expectedStartLine, int expectedEndLine) {
        List<UnitInfo> units = extractUnits(fileName, content);

        assertFalse(fileName + ": expected at least one unit", units.isEmpty());
        UnitInfo first = units.get(0);
        assertEquals(fileName + ": start line", expectedStartLine, first.getStartLine());
        assertEquals(fileName + ": end line", expectedEndLine, first.getEndLine());
    }

    private List<UnitInfo> extractUnits(String fileName, String content) {
        SourceFile sourceFile = new SourceFile(new File(fileName), content);
        return LanguageAnalyzerFactory.getInstance().getLanguageAnalyzer(sourceFile).extractUnits(sourceFile);
    }
}
