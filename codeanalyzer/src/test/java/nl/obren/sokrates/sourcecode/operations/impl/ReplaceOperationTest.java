/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.operations.impl;

import nl.obren.sokrates.sourcecode.operations.StringOperation;
import nl.obren.sokrates.sourcecode.operations.impl.ReplaceOperation;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class ReplaceOperationTest {

    @Test
    public void exec() {
        String input = "ABC def 123 \\path1\\path2\\file - data...";

        StringOperation op1 = new ReplaceOperation(Arrays.asList(".*", ""));
        StringOperation op2 = new ReplaceOperation(Arrays.asList(".*", "ABC"));
        StringOperation op3 = new ReplaceOperation(Arrays.asList("\\\\", "/"));
        StringOperation op4 = new ReplaceOperation(Arrays.asList("(\\-|\\\\|/)", " "));

        Assert.assertEquals("", op1.exec(input));
        Assert.assertEquals("ABC", op2.exec(input));
        Assert.assertEquals("ABC def 123 /path1/path2/file - data...", op3.exec(input));
        Assert.assertEquals("ABC def 123  path1 path2 file   data...", op4.exec(input));
    }

    @Test
    public void execWithWrongParameters() {
        String input = "ABC def 123 \\path1\\path2\\file - data...";

        StringOperation op1 = new ReplaceOperation(Arrays.asList());
        StringOperation op2 = new ReplaceOperation(Arrays.asList(".*"));
        StringOperation op3 = new ReplaceOperation(Arrays.asList("", "/", ";"));
        StringOperation op4 = new ReplaceOperation(Arrays.asList("", "/", ";", ";"));

        Assert.assertEquals(input, op1.exec(input));
        Assert.assertEquals(input, op2.exec(input));
        Assert.assertEquals(input, op3.exec(input));
        Assert.assertEquals(input, op4.exec(input));
    }
}
