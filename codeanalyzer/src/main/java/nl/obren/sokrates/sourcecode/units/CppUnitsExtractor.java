package nl.obren.sokrates.sourcecode.units;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CppUnitsExtractor extends CStyleHeuristicUnitsExtractor {
    @Override
    protected int getEndOfUnitBodyIndex(List<String> lines, int startIndex) {
        StringBuilder unitBody = new StringBuilder();
        int startCount = 0;
        int endCount = 0;
        for (int i = startIndex; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            unitBody.append(line + "\n");
            startCount += StringUtils.countMatches(line, "{");
            endCount += StringUtils.countMatches(line, "}");

            boolean hasEndStatement = StringUtils.contains(line, ";");
            boolean hasNoBody = startCount == 0 && endCount == 0;
            boolean hasValidBody = startCount > 0 && startCount == endCount;
            boolean hasInlineSingleLineBody = hasValidBody && startIndex == i;

            boolean isForwardDeclaration = hasEndStatement && hasNoBody;
            boolean isOneLiner = hasInlineSingleLineBody;

            if (isOneLiner || hasValidBody) {
                return i;
            } else if (isForwardDeclaration) {
                return -1;
            }
        }

        return -1;
    }

    @Override
    protected boolean isUnitSignature(String line) {
        line = extraCleanContent(line);
        if (line.contains("(") && !line.contains("new ") && !line.trim().startsWith("else ")
                && !line.trim().startsWith("?") && !line.trim().startsWith(":")) {
            line = line.substring(0, line.indexOf("(") + 1);
            String identifierPattern = "[a-zA-Z0-9_$?:~]+";
            String startUnitRegex = "(" + identifierPattern + "[ ]+)+" + identifierPattern + "[ ]*[(]";
            Pattern pattern = Pattern.compile(startUnitRegex);
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                return true;
            }
        }
        return false;
    }
}
