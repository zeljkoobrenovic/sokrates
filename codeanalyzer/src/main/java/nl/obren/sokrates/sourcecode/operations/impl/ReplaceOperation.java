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
        if (getParams().size() != 2) {
            return input;
        }

        String regex = getParams().get(0);
        String replacement = getParams().get(1);

        if (regex.equals(".*"))
            return replacement;
        else
            return input.replaceAll(regex, replacement);
    }
}
