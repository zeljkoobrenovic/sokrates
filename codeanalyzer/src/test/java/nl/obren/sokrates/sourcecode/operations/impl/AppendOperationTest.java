/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.operations.impl;

import nl.obren.sokrates.sourcecode.operations.StringOperation;
import nl.obren.sokrates.sourcecode.operations.impl.AppendOperation;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

public class AppendOperationTest {

    @Test
    public void exec() {
        StringOperation op1 = new AppendOperation(new ArrayList<>());
        StringOperation op2 = new AppendOperation(Arrays.asList("", "", ""));
        StringOperation op3 = new AppendOperation(Arrays.asList("abc"));
        StringOperation op4 = new AppendOperation(Arrays.asList("abc", " ", "def", " ", "+/-[.]"));

        Assert.assertEquals("", op1.exec(""));
        Assert.assertEquals("", op2.exec(""));
        Assert.assertEquals("test 1", op1.exec("test 1"));
        Assert.assertEquals("test 2", op2.exec("test 2"));
        Assert.assertEquals("abc", op3.exec(""));
        Assert.assertEquals("xyzabc", op3.exec("xyz"));
        Assert.assertEquals("abc def +/-[.]", op4.exec(""));
        Assert.assertEquals("Start: abc def +/-[.]", op4.exec("Start: "));
    }
}
