package nl.obren.sokrates.sourcecode.operations.impl;

import nl.obren.sokrates.sourcecode.operations.StringOperation;

import java.util.List;

public class TrimOperation extends StringOperation {
    public TrimOperation() {
        super("trim");
    }

    public TrimOperation(List<String> params) {
        this();
        this.setParams(params);
    }

    @Override
    public String exec(String input) {
        return input.trim();
    }
}
