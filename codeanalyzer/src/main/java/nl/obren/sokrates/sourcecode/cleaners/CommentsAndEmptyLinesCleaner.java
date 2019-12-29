/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.cleaners;

import nl.obren.sokrates.sourcecode.CleaningResult;

import java.util.ArrayList;
import java.util.List;

public class CommentsAndEmptyLinesCleaner {
    private List<CodeBlockParser> codeBlockParsers = new ArrayList<>();
    private String content;
    private CodeBlockParser activeHelper = null;

    private int currentIndex = 0;

    public CommentsAndEmptyLinesCleaner() {
    }

    public CommentsAndEmptyLinesCleaner(String singleLineCommentStart, String blockCommentStart, String blockCommentEnd) {
        addCommentBlockHelper(singleLineCommentStart, "\n", "");
        addCommentBlockHelper(blockCommentStart, blockCommentEnd, "");
    }

    public CommentsAndEmptyLinesCleaner(String singleLineCommentStart, String blockCommentStart, String blockCommentEnd, String stringDelimiter, String stringEscapeMarker) {
        addCommentBlockHelper(singleLineCommentStart, "\n", "");
        addCommentBlockHelper(blockCommentStart, blockCommentEnd, "");
        addStringBlockHelper(stringDelimiter, stringDelimiter, stringEscapeMarker);
    }

    public void addStringBlockHelper(String marker, String escapeMarker) {
        codeBlockParsers.add(new CodeBlockParser(marker, marker, escapeMarker, false));
    }

    public void addStringBlockHelper(String startMarker, String endMarker, String escapeMarker) {
        codeBlockParsers.add(new CodeBlockParser(startMarker, endMarker, escapeMarker, false));
    }

    public void addCommentBlockHelper(String startMarker, String endMarker) {
        codeBlockParsers.add(new CodeBlockParser(startMarker, endMarker, "", true));
    }

    public void addCommentBlockHelper(String startMarker, String endMarker, String escapeMarker) {
        codeBlockParsers.add(new CodeBlockParser(startMarker, endMarker, escapeMarker, true));
    }

    public String cleanKeepEmptyLines(String originalContent) {
        this.content = SourceCodeCleanerUtils.normalizeLineEnds(originalContent);

        while (true) {
            activeHelper = null;
            final int index[] = {-1};
            this.codeBlockParsers.forEach(helper -> {
                int helperIndex = helper.getStringStartIndex(content, currentIndex);

                if (helperIndex >= 0 && (index[0] == -1 || helperIndex < index[0])) {
                    index[0] = helperIndex;
                    activeHelper = helper;
                }
            });

            if (activeHelper != null) {
                CleaningResult cleaningResult = activeHelper.cleanOrSkip(content, index[0]);
                content = cleaningResult.getContent();
                currentIndex = cleaningResult.getCurrentIndex();
            } else {
                break;
            }
        }

        return content;
    }

    public CleanedContent clean(String originalContent) {
        String rawContent = cleanKeepEmptyLines(originalContent);
        return SourceCodeCleanerUtils.cleanEmptyLinesWithLineIndexes(rawContent);
    }

    public List<CodeBlockParser> getCodeBlockParsers() {
        return codeBlockParsers;
    }

    public void setCodeBlockParsers(List<CodeBlockParser> codeBlockParsers) {
        this.codeBlockParsers = codeBlockParsers;
    }
}
