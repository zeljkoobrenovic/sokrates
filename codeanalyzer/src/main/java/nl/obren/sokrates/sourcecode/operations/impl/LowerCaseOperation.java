/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.operations.impl;

import nl.obren.sokrates.sourcecode.operations.StringOperation;

import java.util.List;

public class LowerCaseOperation extends StringOperation {
    public LowerCaseOperation() {
        super("lowercase");
    }

    public LowerCaseOperation(List<String> params) {
        this();
        this.setParams(params);
    }

    @Override
    public String exec(String input) {
        return input.toLowerCase();
    }
}
