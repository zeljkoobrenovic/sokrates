/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.js;

import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.sourcecode.units.CStyleHeuristicUnitsExtractor;

public class JavaScriptHeuristicUnitsExtractor extends CStyleHeuristicUnitsExtractor {
    public JavaScriptHeuristicUnitsExtractor() {
        super.setExtractRecursively(true);
    }

    @Override
    public boolean isUnitSignature(String line) {
        return super.isUnitSignature(line) || isFunction(line);
    }

    private boolean isFunction(String line) {
        String idRegex = "[a-zA-Z_$][a-zA-Z_$0-9]*";
        return !line.contains(";")
                && doesNotStartWithKeyword(line)
                && (RegexUtils.matchesEntirely("[ ]*function[ ]*[(].*", line)
                || RegexUtils.matchesEntirely(".*(=|:)[ ]*function[ ]*[(].*", line)
                || RegexUtils.matchesEntirely("[ ]*[(][ ]*function[ ]*[(].*", line)
                || RegexUtils.matchesEntirely("[ ]*define[(][ ]*function[ ]*[(].*", line)
                || RegexUtils.matchesEntirely("[ ]*" + idRegex + "[(].*?[)][ ]*[{][ ]*", line));
    }

    public boolean doesNotStartWithKeyword(String line) {
        String controlFlowKeywords[] = new String[]{"if", "while", "for", "switch", "catch"};

        for (String keyword : controlFlowKeywords) {
            if (line.trim().startsWith(keyword + " ") || line.trim().startsWith(keyword + "(")) {
                return false;
            }
        }

        return true;
    }

}
