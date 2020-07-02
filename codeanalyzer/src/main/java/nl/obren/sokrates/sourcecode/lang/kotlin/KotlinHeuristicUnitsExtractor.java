/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.kotlin;

import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.sourcecode.units.CStyleHeuristicUnitsExtractor;

public class KotlinHeuristicUnitsExtractor extends CStyleHeuristicUnitsExtractor {
    public KotlinHeuristicUnitsExtractor() {
        super.setExtractRecursively(true);
    }

    @Override
    public boolean isUnitSignature(String line) {
        return isFunction(line) || isInitBlock(line);
    }

    private boolean isFunction(String line) {
        String idRegex = "[a-zA-Z_$][a-zA-Z_$0-9]*";
        String prefixes = "override |open |protected |public | private |suspend ";
        return !line.contains(";") && !line.contains("=") && (RegexUtils.matchesEntirely("[ ]*(" + prefixes + ")*[ ]*fun[ ]*" + idRegex + "[(].*", line));
    }

    private boolean isInitBlock(String line) {
        return RegexUtils.matchesEntirely("[ ]*init[ ]*{[ ]*", line);
    }


}
