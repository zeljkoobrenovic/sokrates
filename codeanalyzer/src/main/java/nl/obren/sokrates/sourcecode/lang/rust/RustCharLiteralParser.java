/*
 * Copyright (c) 2026 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.rust;

import nl.obren.sokrates.sourcecode.cleaners.CodeBlockParser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Rust uses ' both for char literals ('x', '\n', b'x', unicode escapes) and for lifetimes ('a, 'static).
// Only a ' that starts a complete char literal opens a block, so a lifetime is left as code
// (a plain ' delimiter would read it as an unterminated string and drop the rest of the file).
public class RustCharLiteralParser extends CodeBlockParser {
    static final Pattern CHAR_LITERAL = Pattern.compile(
            "'(\\\\(x[0-9a-fA-F]{2}|u\\{[0-9a-fA-F]{1,6}\\}|.)|[^\\\\'\\n])'");

    public RustCharLiteralParser() {
        super("'", "'", "\\", false);
    }

    @Override
    public int getStringStartIndex(String content, int fromIndex) {
        Matcher matcher = CHAR_LITERAL.matcher(content);
        int index = content.indexOf('\'', fromIndex);
        while (index >= 0) {
            if (matcher.region(index, content.length()).lookingAt()) {
                return index;
            }
            index = content.indexOf('\'', index + 1);
        }
        return -1;
    }

    @Override
    public int getStringEndIndex(String content, int fromIndex) {
        Matcher matcher = CHAR_LITERAL.matcher(content);
        if (fromIndex > 0 && matcher.region(fromIndex - 1, content.length()).lookingAt()) {
            return matcher.end() - 1;
        }
        return -1;
    }
}
