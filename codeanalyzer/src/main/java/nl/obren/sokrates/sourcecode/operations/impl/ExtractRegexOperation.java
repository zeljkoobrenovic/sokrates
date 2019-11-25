package nl.obren.sokrates.sourcecode.operations.impl;

import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.sourcecode.operations.StringOperation;

import java.util.List;

public class ExtractRegexOperation extends StringOperation {
    public ExtractRegexOperation() {
        super("extract");
    }

    public ExtractRegexOperation(List<String> params) {
        this();
        this.setParams(params);
    }

    @Override
    public String exec(String input) {
        final String[] result = {input};

        getParams().forEach(regex -> {
            String matchedRegex = RegexUtils.getMatchedRegex(result[0], regex);
            if (matchedRegex != null) {
                result[0] = matchedRegex;
            } else {
                result[0] = "";
            }
        });

        return result[0];
    }
}
