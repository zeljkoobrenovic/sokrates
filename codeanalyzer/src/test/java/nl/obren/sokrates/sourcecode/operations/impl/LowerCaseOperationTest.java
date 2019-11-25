package nl.obren.sokrates.sourcecode.operations.impl;

import nl.obren.sokrates.sourcecode.operations.StringOperation;
import nl.obren.sokrates.sourcecode.operations.impl.LowerCaseOperation;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

public class LowerCaseOperationTest {
    @Test
    public void exec() {
        StringOperation op = new LowerCaseOperation(new ArrayList<>());

        Assert.assertEquals("", op.exec(""));
        Assert.assertEquals("abc/def - ghi", op.exec("ABC/DEF - GHI"));
        Assert.assertEquals("abc/def - ghi", op.exec("Abc/DeF - Ghi"));
        Assert.assertEquals("abc/def - ghi", op.exec("abc/def - ghi"));
    }
}
