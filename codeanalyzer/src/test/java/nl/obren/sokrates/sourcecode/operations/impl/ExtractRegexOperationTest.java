package nl.obren.sokrates.sourcecode.operations.impl;

import nl.obren.sokrates.sourcecode.operations.StringOperation;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class ExtractRegexOperationTest {
    @Test
    public void exec() {
        String input = "import org.package.Class.*";

        StringOperation op1 = new ExtractRegexOperation(Arrays.asList(".*"));
        StringOperation op2 = new ExtractRegexOperation(Arrays.asList(" .*"));
        StringOperation op3 = new ExtractRegexOperation(Arrays.asList("import"));
        StringOperation op4 = new ExtractRegexOperation(Arrays.asList("[A-Z][a-z]+"));
        StringOperation op5 = new ExtractRegexOperation(Arrays.asList("[A-Z][a-z]+", "[a-z]+"));
        StringOperation op6 = new ExtractRegexOperation(Arrays.asList("non existing pattern"));

        Assert.assertEquals(input, op1.exec(input));
        Assert.assertEquals(" org.package.Class.*", op2.exec(input));
        Assert.assertEquals("import", op3.exec(input));
        Assert.assertEquals("Class", op4.exec(input));
        Assert.assertEquals("lass", op5.exec(input));
        Assert.assertEquals("", op6.exec(input));
    }
}
