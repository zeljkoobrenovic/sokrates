/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.operations.impl;

import nl.obren.sokrates.sourcecode.operations.StringOperation;

import java.util.List;

public class UpperCaseOperation extends StringOperation {
    public UpperCaseOperation() {
        super("uppercase");
    }

    public UpperCaseOperation(List<String> params) {
        this();
        this.setParams(params);
    }

    @Override
    public String exec(String input) {
        return input.toUpperCase();
    }
}
