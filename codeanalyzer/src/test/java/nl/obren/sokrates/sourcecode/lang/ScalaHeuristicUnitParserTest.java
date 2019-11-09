package nl.obren.sokrates.sourcecode.lang;

import nl.obren.sokrates.sourcecode.lang.scala.ScalaHeuristicUnitParser;
import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class ScalaHeuristicUnitParserTest {
    @Test
    public void isUnitSignature() throws Exception {
        ScalaHeuristicUnitParser parser = new ScalaHeuristicUnitParser();

        assertTrue(parser.isUnitSignature("def method()"));

        assertFalse(parser.isUnitSignature(""));
        assertFalse(parser.isUnitSignature("return test();"));
        assertFalse(parser.isUnitSignature("new A().run();"));
    }

}