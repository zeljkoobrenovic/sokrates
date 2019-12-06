package nl.obren.sokrates.sourcecode.cleaners;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CommentsAndEmptyLinesCleanerTest {

    @Test
    public void clean() {

        CommentsAndEmptyLinesCleaner cleaner = new CommentsAndEmptyLinesCleaner("//", "/*", "*/", "\"", "\\");
        String code = "#include \"add.h\"\n" +
                "\n" +
                "int triple(int x)\n" +
                "{\n" +
                "    // comment\n" +
                "    return add(x, add(x,x));\n" +
                "}";

        assertEquals("#include \"add.h\"\n" +
                "int triple(int x)\n" +
                "{\n" +
                "    return add(x, add(x,x));\n" +
                "}", cleaner.clean(code).getCleanedContent());
    }
}
