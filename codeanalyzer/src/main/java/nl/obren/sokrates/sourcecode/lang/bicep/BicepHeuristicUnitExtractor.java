package nl.obren.sokrates.sourcecode.lang.bicep;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzerFactory;
import nl.obren.sokrates.sourcecode.units.CStyleHeuristicUnitsExtractor;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BicepHeuristicUnitExtractor extends CStyleHeuristicUnitsExtractor{

    private List<String> mcCabeIndexLiterals = Arrays.asList(
            " if ",
            " for ",
            "&&",
            "||",
            " ? ",
            "??");

    @Override
    public boolean isUnitSignature(String line) {
        line = extraCleanContent(line);
        if (hasMinimalRequirementsForUnitStart(line)) {
            line = line.substring(0, line.indexOf("=") + 1);
            String startUnitRegex = "(.* |)(resource|module) .*";
            Pattern pattern = Pattern.compile(startUnitRegex);
            Matcher matcher = pattern.matcher(line);
            if (matcher.matches()) {
                return true;
            }

        }
        return false;
    }

    private boolean hasMinimalRequirementsForUnitStart(String line) {
        return line.contains("=");
    }

    protected String extraCleanContent(String content) {
        String cleanedContent = emptyStrings(content);
        cleanedContent = SourceCodeCleanerUtils.normalizeLineEnds(cleanedContent);

        return cleanedContent;
    }

    private String emptyStrings(String cleanedContent) {
        cleanedContent = cleanedContent.replaceAll("\".*?\"", "\"\"");
        cleanedContent = cleanedContent.replaceAll("'.*?'", "''");
        return cleanedContent;
    }

    private CleanedContent getCleanContent(SourceFile sourceFile) {
        CleanedContent normallyCleanedContent = LanguageAnalyzerFactory.getInstance().getLanguageAnalyzer(sourceFile)
                .cleanForLinesOfCodeCalculations(sourceFile);
        normallyCleanedContent.setCleanedContent(extraCleanContent(normallyCleanedContent.getCleanedContent()));
        return normallyCleanedContent;
    }
}
