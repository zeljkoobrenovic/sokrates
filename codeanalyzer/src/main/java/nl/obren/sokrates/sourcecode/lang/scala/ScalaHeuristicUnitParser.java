package nl.obren.sokrates.sourcecode.lang.scala;

import nl.obren.sokrates.sourcecode.units.CStyleHeuristicUnitParser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScalaHeuristicUnitParser extends CStyleHeuristicUnitParser {
    @Override
    public boolean isUnitSignature(String line) {
        line = extraCleanContent(line);
        if (line.contains("(") && !line.contains(";") && !line.contains("new ") && !line.trim().startsWith("else ") && !line.contains("return ")) {
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
}
