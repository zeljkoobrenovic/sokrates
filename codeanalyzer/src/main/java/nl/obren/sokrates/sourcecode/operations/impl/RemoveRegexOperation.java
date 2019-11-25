package nl.obren.sokrates.sourcecode.operations.impl;

import nl.obren.sokrates.sourcecode.operations.StringOperation;

import java.util.List;

public class RemoveRegexOperation extends StringOperation {
    public RemoveRegexOperation() {
        super("remove");
    }

    public RemoveRegexOperation(List<String> params) {
        this();
        this.setParams(params);
    }

    @Override
    public String exec(String input) {
        final String[] result = {input};

        getParams().forEach(regex -> {
            result[0] = result[0].replaceAll(regex, "");
        });

        return result[0];
    }
}
