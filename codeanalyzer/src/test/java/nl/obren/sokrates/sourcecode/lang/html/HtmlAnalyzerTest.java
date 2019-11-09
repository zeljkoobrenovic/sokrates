package nl.obren.sokrates.sourcecode.lang.html;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.assertEquals;

public class HtmlAnalyzerTest {
    @Test
    public void cleanForLinesOfCodeCalculations() throws Exception {
        HtmlAnalyzer analyzer = new HtmlAnalyzer();
        String code = "You will be able to see this text.\n" +
                "\n" +
                "<!-- You will not be able to see this text. -->\n" +
                "\n" +
                "You can even comment out things in <!-- the middle of --> a sentence.\n" +
                "\n" +
                "<!--\n" +
                "Or you can\n" +
                "comment out\n" +
                "a large number of lines.\n" +
                "-->\n" +
                "\n" +
                "<div class=\"example-class\">\n" +
                "Another thing you can do is put comments after closing tags, to help you find where a particular element ends. <br>\n" +
                "(This can be helpful if you have a lot of nested elements.)\n" +
                "</div> <!-- /.example-class -->";
        assertEquals(analyzer.cleanForLinesOfCodeCalculations(new SourceFile(new File("dummy.html"), code)).getCleanedContent(),
                "You will be able to see this text.\n" +
                        "You can even comment out things in  a sentence.\n" +
                        "<div class=\"example-class\">\n" +
                        "Another thing you can do is put comments after closing tags, to help you find where a particular element ends. <br>\n" +
                        "(This can be helpful if you have a lot of nested elements.)\n" +
                        "</div> ");
    }

    @Test
    public void cleanForDuplicationCalculations() throws Exception {
        HtmlAnalyzer analyzer = new HtmlAnalyzer();
        String code = "You will be able to see this text.\n" +
                "\n" +
                "<!-- You will not be able to see this text. -->\n" +
                "\n" +
                "You can even comment out things in <!-- the middle of --> a sentence.\n" +
                "\n" +
                "<!--\n" +
                "Or you can\n" +
                "comment out\n" +
                "a large number of lines.\n" +
                "-->\n" +
                "\n" +
                "<div class=\"example-class\">\n" +
                "Another thing you can do is put comments after closing tags, to help you find where a particular element ends. <br>\n" +
                "(This can be helpful if you have a lot of nested elements.)\n" +
                "</div> <!-- /.example-class -->";
        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(new SourceFile(new File("dummy.html"), code));
        assertEquals(cleanedContent.getCleanedContent(), "You will be able to see this text.\n" +
                "You can even comment out things in a sentence.\n" +
                "<div class=\"example-class\">\n" +
                "Another thing you can do is put comments after closing tags, to help you find where a particular element ends. <br>\n" +
                "(This can be helpful if you have a lot of nested elements.)");

        assertEquals(cleanedContent.getCleanedLinesCount(), 5);
        assertEquals(cleanedContent.getFileLineIndexes().size(), 5);
        assertEquals(cleanedContent.getFileLineIndexes().get(0).intValue(), 0);
        assertEquals(cleanedContent.getFileLineIndexes().get(1).intValue(), 4);
        assertEquals(cleanedContent.getFileLineIndexes().get(2).intValue(), 12);

    }

}