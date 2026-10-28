/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.rust;

import nl.obren.sokrates.sourcecode.units.CStyleHeuristicUnitsExtractor;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RustHeuristicUnitsExtractor extends CStyleHeuristicUnitsExtractor {
    // fn with optional modifiers: pub, pub(crate), default, const, async, unsafe, extern "C"
    private static final Pattern FN_SIGNATURE = Pattern.compile(
            "^(pub(\\([^)]*\\))?\\s+)?(default\\s+)?(const\\s+)?(async\\s+)?(unsafe\\s+)?(extern\\s+(\"[^\"]*\"\\s+)?)?fn\\s.*");
    private static final Pattern FN_NAME = Pattern.compile("\\bfn\\s+([\\p{L}\\p{N}_]+)");
    // "||" that starts a closure without parameters (after "(" the search body has no space before it)
    private static final Pattern EMPTY_CLOSURE = Pattern.compile("([,=]|=>|\\bmove|\\breturn)\\s+\\|\\|\\s");

    public RustHeuristicUnitsExtractor() {
        // match arms are counted as "=>", one per match is subtracted in getMcCabeIndex;
        // && and || need spaces around them, so &&x (a reference) and |&&x| (a closure pattern) don't count
        super.setMcCabeIndexLiterals(Arrays.asList(
                " if ",
                " while ",
                " for ",
                "=>",
                " && ",
                " || "));
    }

    @Override
    protected int getMcCabeIndex(String body) {
        String bodyForSearch = getBodyForMcCabeSearch(body);
        int mcCabeIndex = super.getMcCabeIndex(body);
        // a match with n arms is n - 1 decisions
        mcCabeIndex -= StringUtils.countMatches(bodyForSearch, " match ");
        Matcher matcher = EMPTY_CLOSURE.matcher(bodyForSearch);
        while (matcher.find()) {
            mcCabeIndex--;
        }
        return Math.max(1, mcCabeIndex);
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
        // the "for" of a higher-ranked bound (for<'a> Fn) and of "impl Trait for Type" is not a loop
        cleanedContent = cleanedContent.replaceAll("\\bfor\\s*<", "<");
        cleanedContent = cleanedContent.replaceAll("(\\bimpl\\b[^{;]*?\\s)for\\s", "$1");
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
