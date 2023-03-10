package nl.obren.sokrates.sourcecode.lang.cobol;

import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;

import java.util.List;

public class CobolReformatter {

    public static String getReformattedCobol(String content) {
        List<String> originalLines = SourceCodeCleanerUtils.splitInLines(content);

        StringBuilder cleanedContent = originalLines.stream().collect(StringBuilder::new, (x, y) -> mergeStrings(x, y), (a, b) -> mergeStrings(a, b));

        return cleanedContent.toString();
    }

    private static StringBuilder mergeStrings(StringBuilder a, String b) {
        if (b.length() < 26) {
            b = ""; }
        else {
            b = b.substring(25, Math.min(102, b.length()));
        }

        return a.append('\n').append(b);
    }

    private static StringBuilder mergeStrings(StringBuilder a, StringBuilder b) {
        if (b.length() < 26) {
            b = new StringBuilder();
        }
        else {
            b = new StringBuilder(b.substring(25, Math.min(101, b.length())));
        }

        return a.append('\n').append(b);
    }
}
