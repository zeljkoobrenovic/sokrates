/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.operations;

import java.util.ArrayList;
import java.util.List;

public class OperationStatement {
    // A string transformation operation. Valid options include "extract", "remove", "replace", "trim", "uppercase", "lowercase", "append", "prepend"
    private String op = "";

    // An optional list of parameters used by the operation
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
