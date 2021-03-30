/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.operations.impl;

import nl.obren.sokrates.sourcecode.operations.StringOperation;

import java.util.List;

public class ReplaceOperation extends StringOperation {
    public ReplaceOperation() {
        super("replace");
    }

    public ReplaceOperation(List<String> params) {
        this();
        this.setParams(params);
    }

    @Override
    public String exec(String input) {
        if (getParams().size() < 2) {
            return input;
        }

        String result = input;

        for (int i = 0; i + 1 < getParams().size(); i += 2) {
            String regex = getParams().get(i);
            String replacement = getParams().get(i + 1);

            if (regex.equals(".*"))
                result = replacement;
            else
                result = result.replaceAll(regex, replacement);
        }

        return result;
    }
}
