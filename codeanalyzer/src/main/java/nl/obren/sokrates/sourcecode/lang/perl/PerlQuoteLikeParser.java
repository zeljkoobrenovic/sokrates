/*
 * Copyright (c) 2026 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.perl;

import nl.obren.sokrates.sourcecode.CleaningResult;
import nl.obren.sokrates.sourcecode.cleaners.CodeBlockParser;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Perl's quote-like operators (m//, s///, tr///, y///, q//, qq//, qw//, qr//) and bare /…/ regex literals
// (after =~, !~, split, if, …). Their delimiters can be any character, and their contents often hold a
// bare " or ' (s/"/&quot;/g) or a # (m/#/, s#a#b#), which plain string and comment helpers would read as
// a string that never closes (dropping the rest of the file) or as a comment (issue #131).
// A recognised construct is skipped as one block: kept as code, or removed when removeWhenCleaning is set.
// Here-docs (<<EOT, <<"EOT", <<'EOT', <<~EOT) are skipped the same way, from the << to the terminator line,
// as their text (You'll, $1[s.i]) would otherwise open strings or quote-like operators.
// Not handled: the old ' package separator ($main'var).
public class PerlQuoteLikeParser extends CodeBlockParser {
    private static final Set<String> OPERATORS = new HashSet<>(Arrays.asList("m", "q", "qq", "qw", "qr", "s", "tr", "y"));
    private static final Set<String> TWO_PART_OPERATORS = new HashSet<>(Arrays.asList("s", "tr", "y"));
    // words after which a / starts a regex, not a division
    private static final Set<String> REGEX_AFTER_WORDS = new HashSet<>(Arrays.asList(
            "split", "grep", "if", "unless", "and", "or", "not", "return", "when", "while", "until"));
    private static final Pattern HERE_DOC = Pattern.compile("<<(~?)(?:\"([^\"\\n]+)\"|'([^'\\n]+)'|([A-Za-z_][A-Za-z0-9_]*))");
    private static final String OPENERS = "{([<";
    private static final String CLOSERS = "})]>";
    // a construct longer than this is not one (keeps false starts cheap on large files)
    private static final int MAX_LENGTH = 10000;

    private String cachedContent = null;
    private int cachedFromIndex = -1;
    private int cachedStart = -1;
    private int cachedEnd = -1;

    public PerlQuoteLikeParser() {
        super("", "", "", false);
    }

    @Override
    public int getStringStartIndex(String content, int fromIndex) {
        if (content == cachedContent && fromIndex >= cachedFromIndex && (cachedStart == -1 || fromIndex <= cachedStart)) {
            return cachedStart;
        }
        cachedContent = content;
        cachedFromIndex = fromIndex;
        cachedStart = -1;
        cachedEnd = -1;
        for (int i = Math.max(0, fromIndex); i < content.length(); i++) {
            int end = matchAt(content, i);
            if (end > i) {
                cachedStart = i;
                cachedEnd = end;
                break;
            }
        }
        return cachedStart;
    }

    @Override
    public CleaningResult cleanOrSkip(String content, int startIndex) {
        int endIndex = content == cachedContent && startIndex == cachedStart ? cachedEnd : matchAt(content, startIndex);
        if (endIndex <= startIndex) {
            return new CleaningResult(content, startIndex + 1);
        }
        if (isRemoveWhenCleaning()) {
            String middle = content.substring(startIndex, endIndex);
            String before = content.substring(0, startIndex) + StringUtils.repeat("\n", StringUtils.countMatches(middle, "\n"));
            return new CleaningResult(before + content.substring(endIndex), before.length());
        }
        return new CleaningResult(content, endIndex);
    }

    // the index just after the quote-like construct starting at index, or -1 when none starts there
    static int matchAt(String content, int index) {
        char c = content.charAt(index);
        if (c == '/') {
            return isRegexContext(content, index) ? endOfPart(content, index, false) : -1;
        }
        if (c == '<') {
            return endOfHereDoc(content, index);
        }
        if (!Character.isLetter(c) || !isOperatorBoundary(content, index)) {
            return -1;
        }
        int wordEnd = index;
        while (wordEnd < content.length() && isWordChar(content.charAt(wordEnd))) {
            wordEnd++;
        }
        String word = content.substring(index, wordEnd);
        if (!OPERATORS.contains(word)) {
            return -1;
        }
        int delimiterIndex = wordEnd;
        while (delimiterIndex < content.length() && (content.charAt(delimiterIndex) == ' ' || content.charAt(delimiterIndex) == '\t')) {
            delimiterIndex++;
        }
        if (delimiterIndex >= content.length() || !isDelimiter(content, delimiterIndex, delimiterIndex > wordEnd)) {
            return -1;
        }
        int end = endOfPart(content, delimiterIndex, true);
        if (end < 0) {
            return -1;
        }
        if (TWO_PART_OPERATORS.contains(word)) {
            char delimiter = content.charAt(delimiterIndex);
            if (OPENERS.indexOf(delimiter) >= 0) {
                // s{a}{b}, s{a} (b), tr[a-z]
                //    [A-Z]
                int next = end;
                while (next < content.length() && Character.isWhitespace(content.charAt(next))) {
                    next++;
                }
                if (next >= content.length() || !isDelimiter(content, next, false)) {
                    return -1;
                }
                end = endOfPart(content, next, true);
            } else {
                // s/a/b/: the closing delimiter of the first part opens the second
                end = endOfPart(content, end - 1, true);
            }
            if (end < 0) {
                return -1;
            }
        }
        while (end < content.length() && Character.isLetter(content.charAt(end))) {
            end++;
        }
        return end;
    }

    // the operator word must start a token: not part of an identifier, a variable ($s, @y, %q),
    // an escape (\s in a regex), a file test (-s $file), a method (->s), a package name (Foo::s) or a hash key ($h{s}, s => 1)
    private static boolean isOperatorBoundary(String content, int index) {
        if (index == 0) {
            return true;
        }
        char before = content.charAt(index - 1);
        if (isWordChar(before) || "$@%&*-\\".indexOf(before) >= 0) {
            return false;
        }
        if (before == '>' && index >= 2 && content.charAt(index - 2) == '-') {
            return false;
        }
        return !(before == ':' && index >= 2 && content.charAt(index - 2) == ':');
    }

    private static boolean isDelimiter(String content, int index, boolean afterWhitespace) {
        char delimiter = content.charAt(index);
        if (Character.isWhitespace(delimiter) || isWordChar(delimiter)) {
            return false;
        }
        if (CLOSERS.indexOf(delimiter) >= 0 || ",;".indexOf(delimiter) >= 0) {
            return false;
        }
        if (delimiter == '=' && (index + 1 >= content.length() || content.charAt(index + 1) == '>'
                || content.charAt(index + 1) == '=' || content.charAt(index + 1) == '~')) {
            return false;
        }
        if (delimiter == '-' && index + 1 < content.length() && content.charAt(index + 1) == '>') {
            return false;
        }
        // after whitespace, # starts a comment; other characters are too likely to be operators (y / 2)
        return !afterWhitespace || OPENERS.indexOf(delimiter) >= 0 || delimiter == '"' || delimiter == '\'' || delimiter == '/';
    }

    private static boolean isRegexContext(String content, int index) {
        int i = index - 1;
        while (i >= 0 && (content.charAt(i) == ' ' || content.charAt(i) == '\t')) {
            i--;
        }
        if (i < 0) {
            return false;
        }
        char before = content.charAt(i);
        // operand positions, where a / cannot be a division
        if ("(!{,;=?".indexOf(before) >= 0) {
            return true;
        }
        if (i >= 1) {
            String two = content.substring(i - 1, i + 1);
            if (two.equals("=~") || two.equals("!~") || two.equals("&&") || two.equals("||")) {
                return true;
            }
        }
        if (isWordChar(before)) {
            int wordStart = i;
            while (wordStart > 0 && isWordChar(content.charAt(wordStart - 1))) {
                wordStart--;
            }
            if (wordStart > 0 && "$@%&>".indexOf(content.charAt(wordStart - 1)) >= 0) {
                return false;
            }
            return REGEX_AFTER_WORDS.contains(content.substring(wordStart, i + 1));
        }
        return false;
    }

    // the index just after the terminator line of a here-doc starting at index, or -1
    private static int endOfHereDoc(String content, int index) {
        if (index > 0 && content.charAt(index - 1) == '<') {
            return -1;
        }
        Matcher matcher = HERE_DOC.matcher(content).region(index, content.length());
        if (!matcher.lookingAt()) {
            return -1;
        }
        boolean indented = !matcher.group(1).isEmpty();
        String terminator = matcher.group(2) != null ? matcher.group(2) : matcher.group(3) != null ? matcher.group(3) : matcher.group(4);
        int lineStart = content.indexOf('\n', matcher.end());
        while (lineStart >= 0) {
            int lineEnd = content.indexOf('\n', lineStart + 1);
            String line = content.substring(lineStart + 1, lineEnd < 0 ? content.length() : lineEnd);
            if ((indented ? line.trim() : line).equals(terminator)) {
                return lineEnd < 0 ? content.length() : lineEnd;
            }
            lineStart = lineEnd;
        }
        return -1;
    }

    // the index just after the closing delimiter of the part opened at openIndex, or -1
    private static int endOfPart(String content, int openIndex, boolean multiLine) {
        char open = content.charAt(openIndex);
        int bracket = OPENERS.indexOf(open);
        char close = bracket >= 0 ? CLOSERS.charAt(bracket) : open;
        int depth = 1;
        int limit = Math.min(content.length(), openIndex + MAX_LENGTH);
        for (int i = openIndex + 1; i < limit; i++) {
            char c = content.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '\n' && !multiLine) {
                return -1;
            } else if (c == close) {
                depth--;
                if (depth == 0) {
                    return i + 1;
                }
            } else if (bracket >= 0 && c == open) {
                depth++;
            }
        }
        return -1;
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }
}
