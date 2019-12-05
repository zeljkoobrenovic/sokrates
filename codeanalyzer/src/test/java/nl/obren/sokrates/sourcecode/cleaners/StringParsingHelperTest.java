package nl.obren.sokrates.sourcecode.cleaners;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StringParsingHelperTest {

    @Test
    public void getStringStartIndex() {
        StringParsingHelper stringParsing = new StringParsingHelper("'", "'", "\\", false);

        assertEquals(10, stringParsing.getStringEndIndex("Line with 'string content' ", 0));
        assertEquals(10, stringParsing.getStringEndIndex("Line with 'string content' ", 2));
        assertEquals(10, stringParsing.getStringEndIndex("Line with 'string content' ", 10));
        assertEquals(25, stringParsing.getStringEndIndex("Line with 'string content' ", 11));
    }

    @Test
    public void getStringEndIndex() {
        StringParsingHelper stringParsing = new StringParsingHelper("'", "'", "\\", false);

        assertEquals(10, stringParsing.getStringEndIndex("Line with 'string \\'content\\'' ", 0));
        assertEquals(29, stringParsing.getStringEndIndex("Line with 'string \\'content\\'' ", 11));
        assertEquals(29, stringParsing.getStringEndIndex("Line with 'string \\'content\\'' ", 12));
        assertEquals(31, stringParsing.getStringEndIndex("Line with 'string \n \\'content\\'' ", 13));

        stringParsing = new StringParsingHelper("\"\"\"", "\"\"\"", "", false);

        String content = "Line with \"\"\"string\n\"content\" \"\"\" ";

        assertEquals(10, stringParsing.getStringEndIndex(content, 0));
        assertEquals(30, stringParsing.getStringEndIndex(content, 11));
        assertEquals(30, stringParsing.getStringEndIndex(content, 12));
        assertEquals(30, stringParsing.getStringEndIndex(content, 13));

        stringParsing = new StringParsingHelper("@\"", "\"", "\"", false);

        content = "Line with @\"string\n\"\"content\"\" \" ";

        assertEquals(11, stringParsing.getStringEndIndex(content, 0));
        assertEquals(11, stringParsing.getStringEndIndex(content, 11));
        assertEquals(31, stringParsing.getStringEndIndex(content, 12));
        assertEquals(31, stringParsing.getStringEndIndex(content, 13));
    }
}
