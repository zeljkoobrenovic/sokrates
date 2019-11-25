package nl.obren.sokrates.sourcecode.operations;

import java.util.ArrayList;
import java.util.List;

public class OperationStatement {
    private String op = "";
    private List<String> params = new ArrayList<>();

    public OperationStatement() {
    }

    public OperationStatement(String op, List<String> params) {
        this.op = op;
        this.params = params;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public List<String> getParams() {
        return params;
    }

    public void setParams(List<String> params) {
        this.params = params;
    }
}
