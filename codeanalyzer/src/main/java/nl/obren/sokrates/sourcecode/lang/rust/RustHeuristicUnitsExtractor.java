/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.rust;

import nl.obren.sokrates.sourcecode.units.CStyleHeuristicUnitsExtractor;
import nl.obren.sokrates.sourcecode.units.UnitInfo;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RustHeuristicUnitsExtractor extends CStyleHeuristicUnitsExtractor {
    // fn with optional modifiers: pub, pub(crate), default, const, async, unsafe, extern "C"
    private static final Pattern FN_SIGNATURE = Pattern.compile(
            "^(pub(\\([^)]*\\))?\\s+)?(default\\s+)?(const\\s+)?(async\\s+)?(unsafe\\s+)?(extern\\s+(\"[^\"]*\"\\s+)?)?fn\\s.*");
    private static final Pattern FN_NAME = Pattern.compile("\\bfn\\s+([\\p{L}\\p{N}_]+)");

    public RustHeuristicUnitsExtractor() {
        super.setMcCabeIndexLiterals(Arrays.asList(
                " if ",
                " loop ",
                " while ",
                " for ",
                "case ",
                "&&",
                "||",
                " catch "));
    }

    @Override
    public boolean isUnitSignature(String line) {
        return FN_SIGNATURE.matcher(line.replace("\t", " ").trim()).matches();
    }

    // a signature that ends with ";" before any "{" is a declaration without a body (e.g. a trait method),
    // also when it spans several lines; without this check the brace counting runs into the next block
    @Override
    protected int getEndOfUnitBodyIndex(List<String> lines, int startIndex) {
        for (int i = startIndex; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.contains("{")) {
                break;
            }
            if (line.endsWith(";")) {
                return -1;
            }
        }
        return super.getEndOfUnitBodyIndex(lines, startIndex);
    }

    // the C-style cleaning replaces '.*?' as a char literal, which pairs up lifetimes ('a ... 'b) and drops
    // the code between them (braces too); char literals become "" and the remaining ' (lifetimes) are removed
    @Override
    protected String extraCleanContent(String content) {
        String cleanedContent = content.replaceAll(RustCharLiteralParser.CHAR_LITERAL.pattern(), "\"\"");
        cleanedContent = cleanedContent.replace("'", "");
        return super.extraCleanContent(cleanedContent);
    }

    @Override
    protected void setNameAndParameters(String line, UnitInfo unit, String cleanedBody) {
        Matcher matcher = FN_NAME.matcher(cleanedBody);
        if (!matcher.find()) {
            super.setNameAndParameters(line, unit, cleanedBody);
            return;
        }
        unit.setShortName("fn " + matcher.group(1) + "()");
        unit.setNumberOfParameters(getNumberOfParameters(cleanedBody, matcher.end()));
    }

    // counts the top-level comma-separated parameters of the first parameter list after the name,
    // skipping generics (which may hold parentheses, e.g. F: Fn(u8) -> u8)
    private int getNumberOfParameters(String body, int fromIndex) {
        int angleDepth = 0;
        int start = -1;
        for (int i = fromIndex; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '<') angleDepth++;
            else if (c == '>' && body.charAt(i - 1) != '-') angleDepth--;
            else if (c == '(' && angleDepth <= 0) {
                start = i;
                break;
            } else if (c == '{' || c == ';') break;
        }
        if (start < 0) return 0;

        int depth = 0;
        int count = 0;
        boolean hasContent = false;
        for (int i = start + 1; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '(' || c == '[' || c == '<') depth++;
            else if ((c == '>' && body.charAt(i - 1) != '-') || c == ']') depth--;
            else if (c == ')') {
                if (depth == 0) break;
                depth--;
            } else if (c == ',' && depth == 0) {
                if (hasContent) count++;
                hasContent = false;
                continue;
            }
            if (!Character.isWhitespace(c)) hasContent = true;
        }
        return hasContent ? count + 1 : count;
    }
}
