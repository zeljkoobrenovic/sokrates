/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

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

    @Test
    public void execReturnsFirstCapturingGroupWhenPresent() {
        String line = "        #[cfg(target_os = \"windows\")]";

        // With a group: only the group's text, not the whole (indented) line.
        Assert.assertEquals("windows", new ExtractRegexOperation(Arrays.asList(".*target_os = \"([a-z]+)\".*")).exec(line));
        // Only group 1 is used, even with several groups.
        Assert.assertEquals("target_os", new ExtractRegexOperation(Arrays.asList("(target_os) = \"([a-z]+)\"")).exec(line));
        // Non-capturing groups don't count as a group: whole match is returned.
        Assert.assertEquals("target_os = \"windows\"", new ExtractRegexOperation(Arrays.asList("(?:target_os) = \"[a-z]+\"")).exec(line));
        // An optional group that did not participate yields "".
        Assert.assertEquals("", new ExtractRegexOperation(Arrays.asList("target_os(XYZ)? = ")).exec(line));
        // Chained: the group result feeds the next regex.
        Assert.assertEquals("win", new ExtractRegexOperation(Arrays.asList("\"([a-z]+)\"", "^[a-z]{3}")).exec(line));
        // Invalid regex yields "" rather than throwing.
        Assert.assertEquals("", new ExtractRegexOperation(Arrays.asList("(unbalanced")).exec(line));
    }
}
