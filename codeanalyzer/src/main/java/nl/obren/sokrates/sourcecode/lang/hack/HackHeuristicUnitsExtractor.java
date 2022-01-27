/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.hack;

import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.sourcecode.units.CStyleHeuristicUnitsExtractor;

public class HackHeuristicUnitsExtractor extends CStyleHeuristicUnitsExtractor {
    public HackHeuristicUnitsExtractor() {
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
                && (RegexUtils.matchesEntirely("[ ]*function[ ]*" + idRegex + "[(].*", line)
                || RegexUtils.matchesEntirely("[ ]*[a-zA-Z_]+[ ]*function[ ]*" + idRegex + "[(].*", line));
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
