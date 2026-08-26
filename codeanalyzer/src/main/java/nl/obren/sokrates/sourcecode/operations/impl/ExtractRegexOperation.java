/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.operations.impl;

import nl.obren.sokrates.sourcecode.operations.StringOperation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Extracts the first match of each regex in turn. If the regex defines a capturing group, the
 * result is group 1 rather than the whole match — so ".*target_os = \"([a-z]+)\".*" yields just the
 * OS name instead of the entire (indentation-carrying) line. Without a group the whole match is
 * returned, as before. A non-matching regex yields "".
 */
public class ExtractRegexOperation extends StringOperation {
    private static final Log LOG = LogFactory.getLog(ExtractRegexOperation.class);

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

        getParams().forEach(regex -> result[0] = extract(result[0], regex));

        return result[0];
    }

    static String extract(String text, String regex) {
        try {
            Matcher matcher = Pattern.compile(regex).matcher(text);
            if (matcher.find()) {
                if (matcher.groupCount() >= 1) {
                    String group = matcher.group(1);
                    return group != null ? group : "";
                }
                return matcher.group();
            }
        } catch (PatternSyntaxException | StackOverflowError e) {
            LOG.debug(e);
        }
        return "";
    }
}
