/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.operations.impl;

import nl.obren.sokrates.sourcecode.operations.StringOperation;

import java.util.List;

public class AppendOperation extends StringOperation {
    public AppendOperation() {
        super("append");
    }

    public AppendOperation(List<String> params) {
        this();
        this.setParams(params);
    }

    @Override
    public String exec(String input) {
        StringBuilder result = new StringBuilder(input);

        getParams().forEach(result::append);

        return result.toString();
    }
}
