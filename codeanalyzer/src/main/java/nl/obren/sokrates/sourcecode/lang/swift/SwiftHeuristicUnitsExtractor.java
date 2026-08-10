/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.swift;

import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.sourcecode.units.CStyleHeuristicUnitsExtractor;

public class SwiftHeuristicUnitsExtractor extends CStyleHeuristicUnitsExtractor {
    public SwiftHeuristicUnitsExtractor() {
        super.setExtractRecursively(true);
    }

    @Override
    public boolean isUnitSignature(String line) {
        return isFunction(line) || isInitBlock(line);
    }

    private boolean isFunction(String line) {
        String idRegex = "[\\p{L}_$][\\p{L}\\p{M}\\p{N}_$]*";
        String prefixes = "mutating ";
        return !line.contains(";") && !line.contains("=") && (RegexUtils.matchesEntirely("[ ]*(" + prefixes + ")*[ ]*func[ ]*" + idRegex + "[(].*", line));
    }

    private boolean isInitBlock(String line) {
        return RegexUtils.matchesEntirely("[ ]*init[ ]*.*\\{[ ]*", line);
    }

}
