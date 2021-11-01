/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.search;

import nl.obren.sokrates.common.utils.RegexUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SearchExpression {
    private static final Log LOG = LogFactory.getLog(SearchExpression.class);

    private String expression = "";

    public SearchExpression(String expression) {
        this.expression = expression;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public boolean matches(String content) {
        try {
            Pattern pattern = Pattern.compile(expression);
            Matcher matcher = pattern.matcher(content);
            return matcher.matches();
        } catch (PatternSyntaxException e) {
            LOG.debug(e);
        }
        return false;
    }

    public String getMatchedRegex(String text) {
        return RegexUtils.getMatchedRegex(text, expression);
    }
}
