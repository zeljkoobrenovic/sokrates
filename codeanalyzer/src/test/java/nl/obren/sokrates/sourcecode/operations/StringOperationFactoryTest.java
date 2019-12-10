/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.operations;

import nl.obren.sokrates.sourcecode.operations.impl.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

public class StringOperationFactoryTest {

    @Test
    public void getOperation() {
        StringOperationFactory factory = new StringOperationFactory();

        Assert.assertNotNull(factory.getOperation(new OperationStatement("extract", new ArrayList<>())));
        Assert.assertNotNull(factory.getOperation(new OperationStatement("remove", new ArrayList<>())));
        Assert.assertNotNull(factory.getOperation(new OperationStatement("replace", new ArrayList<>())));
        Assert.assertNotNull(factory.getOperation(new OperationStatement("trim", new ArrayList<>())));
        Assert.assertNotNull(factory.getOperation(new OperationStatement("uppercase", new ArrayList<>())));
        Assert.assertNotNull(factory.getOperation(new OperationStatement("lowercase", new ArrayList<>())));
        Assert.assertNotNull(factory.getOperation(new OperationStatement("append", new ArrayList<>())));
        Assert.assertNotNull(factory.getOperation(new OperationStatement("prepend", new ArrayList<>())));

        Assert.assertTrue(factory.getOperation(new OperationStatement("extract", new ArrayList<>())) instanceof ExtractRegexOperation);
        Assert.assertTrue(factory.getOperation(new OperationStatement("remove", new ArrayList<>())) instanceof RemoveRegexOperation);
        Assert.assertTrue(factory.getOperation(new OperationStatement("replace", new ArrayList<>())) instanceof ReplaceOperation);
        Assert.assertTrue(factory.getOperation(new OperationStatement("trim", new ArrayList<>())) instanceof TrimOperation);
        Assert.assertTrue(factory.getOperation(new OperationStatement("uppercase", new ArrayList<>())) instanceof UpperCaseOperation);
        Assert.assertTrue(factory.getOperation(new OperationStatement("lowercase", new ArrayList<>())) instanceof LowerCaseOperation);
        Assert.assertTrue(factory.getOperation(new OperationStatement("append", new ArrayList<>())) instanceof AppendOperation);
        Assert.assertTrue(factory.getOperation(new OperationStatement("prepend", new ArrayList<>())) instanceof PrependOperation);


        Assert.assertNotNull(factory.getOperation(new OperationStatement("non existing operation", new ArrayList<>())));
        Assert.assertTrue(factory.getOperation(new OperationStatement("non existing operation", new ArrayList<>())) instanceof NoOpsOperation);
    }
}
