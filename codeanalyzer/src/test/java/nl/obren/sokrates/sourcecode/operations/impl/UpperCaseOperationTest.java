/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.operations.impl;

import nl.obren.sokrates.sourcecode.operations.StringOperation;
import nl.obren.sokrates.sourcecode.operations.impl.UpperCaseOperation;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

public class UpperCaseOperationTest {
    @Test
    public void exec() {
        StringOperation op = new UpperCaseOperation(new ArrayList<>());

        Assert.assertEquals("", op.exec(""));
        Assert.assertEquals("ABC/DEF - GHI", op.exec("ABC/DEF - GHI"));
        Assert.assertEquals("ABC/DEF - GHI", op.exec("Abc/DeF - Ghi"));
        Assert.assertEquals("ABC/DEF - GHI", op.exec("abc/def - ghi"));
    }
}
