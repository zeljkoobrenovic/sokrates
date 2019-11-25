package nl.obren.sokrates.sourcecode.operations;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class ComplexOperationTest {

    @Test
    public void exec() {
        String input = "import org.package.subpackage.Class.*;";

        List<OperationStatement> operations = Arrays.asList(new OperationStatement[]{
                new OperationStatement("extract", Arrays.asList(new String[]{" .*"})),
                new OperationStatement("remove", Arrays.asList(new String[]{"[.][*][;]"})),
                new OperationStatement("remove", Arrays.asList(new String[]{"org[.]"})),
                new OperationStatement("replace", Arrays.asList(new String[]{"[.]", "/"})),
                new OperationStatement("trim", Arrays.asList()),
                new OperationStatement("uppercase", Arrays.asList()),
                new OperationStatement("append", Arrays.asList(new String[]{"]", "}", ")"})),
                new OperationStatement("prepend", Arrays.asList(new String[]{"(", "{", "["})),
        });

        StringOperation op = new ComplexOperation(operations);

        Assert.assertEquals("({[PACKAGE/SUBPACKAGE/CLASS]})", op.exec(input));
    }
}
