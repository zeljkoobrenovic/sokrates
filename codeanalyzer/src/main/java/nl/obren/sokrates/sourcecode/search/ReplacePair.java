/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.search;

import nl.obren.sokrates.common.utils.RegexUtils;
import org.apache.commons.lang3.StringUtils;

public class ReplacePair {
    private String replace = "";
    private String with = "";

    public ReplacePair(String replace, String with) {
        this.replace = replace;
        this.with = with;
    }

    public String getReplace() {
        return replace;
    }

    public void setReplace(String replace) {
        this.replace = replace;
    }

    public String getWith() {
        return with;
    }

    public void setWith(String with) {
        this.with = with;
    }

    public String replaceIn(String text) {
        String result = text;
        while (StringUtils.isNotBlank(replace) && StringUtils.isNotBlank(RegexUtils.getMatchedRegex(result, replace))) {
            String foundText = RegexUtils.getMatchedRegex(result, replace);
            result = text.replace(foundText, with);
        }
        return result;
    }
}
