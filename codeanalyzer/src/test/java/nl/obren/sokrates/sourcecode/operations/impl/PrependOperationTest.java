/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.operations.impl;

import nl.obren.sokrates.sourcecode.operations.StringOperation;
import nl.obren.sokrates.sourcecode.operations.impl.PrependOperation;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

public class PrependOperationTest {

    @Test
    public void exec() {
        StringOperation op1 = new PrependOperation(new ArrayList<>());
        StringOperation op2 = new PrependOperation(Arrays.asList("", "", ""));
        StringOperation op3 = new PrependOperation(Arrays.asList("abc"));
        StringOperation op4 = new PrependOperation(Arrays.asList("abc", " ", "def", " ", "+/-[.]"));

        Assert.assertEquals("", op1.exec(""));
        Assert.assertEquals("", op2.exec(""));
        Assert.assertEquals("test 1", op1.exec("test 1"));
        Assert.assertEquals("test 2", op2.exec("test 2"));
        Assert.assertEquals("abc", op3.exec(""));
        Assert.assertEquals("abcxyz", op3.exec("xyz"));
        Assert.assertEquals("abc def +/-[.]", op4.exec(""));
        Assert.assertEquals("abc def +/-[.] Done.", op4.exec(" Done."));
    }
}
