/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.perl;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.assertEquals;

public class PerlAnalyzerTest {
    @Test
    public void cleanForLinesOfCodeCalculations() throws Exception {
        PerlAnalyzer analyzer = new PerlAnalyzer();
        String code = "#!/usr/bin/perl\n" +
                "\n" +
                "# This is a single line comment\n" +
                "print \"Hello, world\\n\";\n" +
                "\n" +
                "=begin comment\n" +
                "This is all part of multiline comment.\n" +
                "You can use as many lines as you like\n" +
                "These comments will be ignored by the \n" +
                "compiler until the next =cut is encountered.\n" +
                "=cut";
        assertEquals(analyzer.cleanForLinesOfCodeCalculations(new SourceFile(new File("dummy.pl"), code)).getCleanedContent(),
                "print \"Hello, world\\n\";");
    }

    @Test
    public void cleanForDuplicationCalculations() throws Exception {
        PerlAnalyzer analyzer = new PerlAnalyzer();
        String code = "#!/usr/bin/perl\n" +
                "\n" +
                "# This is a single line comment\n" +
                "print \"Hello, world\\n\";\n" +
                "\n" +
                "=begin comment\n" +
                "This is all part of multiline comment.\n" +
                "You can use as many lines as you like\n" +
                "These comments will be ignored by the \n" +
                "compiler until the next =cut is encountered.\n" +
                "=cut";
        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(new SourceFile(new File("dummy.html"), code));
        assertEquals(cleanedContent.getCleanedContent(), "print \"Hello, world\\n\";");

        assertEquals(cleanedContent.getCleanedLinesCount(), 1);
        assertEquals(cleanedContent.getFileLineIndexes().size(), 1);
        assertEquals(cleanedContent.getFileLineIndexes().get(0).intValue(), 3);
    }
}
