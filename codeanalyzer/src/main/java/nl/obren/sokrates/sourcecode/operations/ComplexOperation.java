package nl.obren.sokrates.sourcecode.operations;

import java.util.ArrayList;
import java.util.List;

public class ComplexOperation extends StringOperation {
    private List<OperationStatement> operations = new ArrayList<>();
    private StringOperationFactory factory = new StringOperationFactory();

    public ComplexOperation() {
        super("sequence");
    }

    public ComplexOperation(List<OperationStatement> operations) {
        this();
        this.operations = operations;
    }

    @Override
    public String exec(String input) {
        final String[] result = {input};

        this.operations.forEach(op -> {
            System.out.println(result[0]);
            System.out.println(" => " + op.getOp());
            System.out.println("    (" + op.getParams() + "):");
            result[0] = factory.getOperation(op).exec(result[0]);
            System.out.println(result[0]);
            System.out.println();
        });

        return result[0];
    }
}
