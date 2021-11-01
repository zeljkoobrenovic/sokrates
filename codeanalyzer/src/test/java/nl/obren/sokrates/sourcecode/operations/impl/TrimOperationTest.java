/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.operations.impl;

import nl.obren.sokrates.sourcecode.operations.StringOperation;
import nl.obren.sokrates.sourcecode.operations.impl.TrimOperation;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

public class TrimOperationTest {
    @Test
    public void exec() {
        StringOperation op = new TrimOperation(new ArrayList<>());

        Assert.assertEquals("", op.exec(" "));
        Assert.assertEquals("", op.exec(" "));
        Assert.assertEquals("", op.exec("  "));
        Assert.assertEquals("abc", op.exec("abc "));
        Assert.assertEquals("abc", op.exec(" abc"));
        Assert.assertEquals("abc", op.exec(" abc "));
    }
}
