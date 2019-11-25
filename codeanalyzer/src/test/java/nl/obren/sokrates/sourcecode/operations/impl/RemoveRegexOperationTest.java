package nl.obren.sokrates.sourcecode.operations.impl;

import nl.obren.sokrates.sourcecode.operations.StringOperation;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class RemoveRegexOperationTest {
    @Test
    public void exec() {
        String input = "import org.package.Class.*";

        StringOperation op1 = new RemoveRegexOperation(Arrays.asList(".*"));
        StringOperation op2 = new RemoveRegexOperation(Arrays.asList(" .*"));
        StringOperation op3 = new RemoveRegexOperation(Arrays.asList("import"));
        StringOperation op4 = new RemoveRegexOperation(Arrays.asList("[A-Z][a-z]+"));
        StringOperation op5 = new RemoveRegexOperation(Arrays.asList("[A-Z][a-z]+", "[a-z]+"));

        Assert.assertEquals("", op1.exec(input));
        Assert.assertEquals("import", op2.exec(input));
        Assert.assertEquals(" org.package.Class.*", op3.exec(input));
        Assert.assertEquals("import org.package..*", op4.exec(input));
        Assert.assertEquals(" ...*", op5.exec(input));
    }
}
