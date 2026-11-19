/*
 * Copyright (c) 2026 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.perl;

import nl.obren.sokrates.sourcecode.cleaners.CodeBlockParser;

// A # comment, except the # of $#array, $#{…} and $#$ref (the last index of an array), which is code.
public class PerlCommentParser extends CodeBlockParser {
    public PerlCommentParser() {
        super("#", "\n", "", true);
    }

    @Override
    public int getStringStartIndex(String content, int fromIndex) {
        int index = content.indexOf('#', fromIndex);
        while (index > 0 && content.charAt(index - 1) == '$') {
            index = content.indexOf('#', index + 1);
        }
        return index;
    }
}
