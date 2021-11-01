/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.scala;

import nl.obren.sokrates.sourcecode.units.CStyleHeuristicUnitsExtractor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScalaHeuristicUnitsExtractor extends CStyleHeuristicUnitsExtractor {
    @Override
    public boolean isUnitSignature(String line) {
        line = extraCleanContent(line);
        if (hasMinimalRequirementsForUnitStart(line)) {
            line = line.substring(0, line.indexOf("(") + 1);
            String startUnitRegex = "(.* |)def .*";
            Pattern pattern = Pattern.compile(startUnitRegex);
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                return true;
            }

        }
        return false;
    }

    private boolean hasMinimalRequirementsForUnitStart(String line) {
        return line.contains("(") && !line.contains(";") && !line.contains("new ") && !line.trim().startsWith("else ") && !line.contains("return ");
    }
}
