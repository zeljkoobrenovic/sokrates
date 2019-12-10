/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.cleaners;

import org.apache.commons.lang3.StringUtils;

public class CommentsCleanerUtils {
    public static String cleanLineComments(String content, String lineCommentStart) {
        StringBuilder cleanedContent = new StringBuilder(SourceCodeCleanerUtils.normalizeLineEnds(content));

        while (true) {
            int commentStartIndex = cleanedContent.indexOf(lineCommentStart);
            if (commentStartIndex >= 0) {
                int commentEndIndex = cleanedContent.indexOf("\n", commentStartIndex + 1);
                if (commentEndIndex < 0) {
                    commentEndIndex = cleanedContent.length();
                }
                cleanedContent.replace(commentStartIndex, commentEndIndex, "");
            } else {
                break;
            }
        }

        return cleanedContent.toString();
    }

    public static String cleanBlockComments(String content, String commentBlockStart, String commentBlockEnd) {
        StringBuilder cleanedContent = new StringBuilder(
                content.replace(getReplaceForRegex(commentBlockStart) + ".*?" + getReplaceForRegex(commentBlockEnd), ""));

        int commentStartIndex = 0;
        while (true) {
            commentStartIndex = cleanedContent.indexOf(commentBlockStart, commentStartIndex);
            if (commentStartIndex >= 0) {
                int indexOfNewLineBefore = cleanedContent.substring(0, commentStartIndex).lastIndexOf("\n");
                int indexOfQuoteBefore = cleanedContent.substring(0, commentStartIndex).indexOf("\"");
                int indexOfNewLineAfter = cleanedContent.indexOf("\n", commentStartIndex + 1);
                int indexOfQuoteAfter = cleanedContent.indexOf("\"", commentStartIndex + 1);
                if (indexOfNewLineBefore != -1 && indexOfNewLineBefore < indexOfQuoteBefore && indexOfQuoteAfter != -1
                        && indexOfQuoteAfter < indexOfNewLineAfter) {
                    commentStartIndex = indexOfNewLineAfter + 1;
                    continue;
                }
                int commentEndIndex = cleanedContent.indexOf(commentBlockEnd, commentStartIndex + commentBlockStart.length());
                if (commentEndIndex > commentStartIndex) {
                    String comment = cleanedContent.substring(commentStartIndex, commentEndIndex + commentBlockEnd.length());
                    String replacement = StringUtils.repeat("\n", StringUtils.countMatches(comment, "\n"));
                    cleanedContent.replace(commentStartIndex, commentEndIndex + commentBlockEnd.length(), replacement);
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        return cleanedContent.toString();
    }

    private static String getReplaceForRegex(String commentBlockStart) {
        return commentBlockStart.replace("*", "[*]").replace(".", "[.]").replace("%", "[%]").replace("-", "[-]");
    }


}
