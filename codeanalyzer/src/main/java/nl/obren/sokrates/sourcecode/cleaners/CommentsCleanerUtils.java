/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.cleaners;

import org.apache.commons.lang3.StringUtils;

public class CommentsCleanerUtils {
    public static String cleanLineComments(String content, String lineCommentStart) {
        StringBuilder cleanedContent = new StringBuilder(SourceCodeCleanerUtils.normalizeLineEnds(content));

        // Resume each search from the previous comment's position rather than restarting at 0: the
        // newline that ended the removed comment stays in place, so no earlier comment marker can
        // appear before it.
        int searchFrom = 0;
        while (true) {
            int commentStartIndex = cleanedContent.indexOf(lineCommentStart, searchFrom);
            if (commentStartIndex >= 0) {
                int commentEndIndex = cleanedContent.indexOf("\n", commentStartIndex + 1);
                if (commentEndIndex < 0) {
                    commentEndIndex = cleanedContent.length();
                }
                cleanedContent.replace(commentStartIndex, commentEndIndex, "");
                searchFrom = commentStartIndex;
            } else {
                break;
            }
        }

        return cleanedContent.toString();
    }

    public static String cleanBlockComments(String content, String commentBlockStart, String commentBlockEnd) {
        // Note: the actual comment removal is done by the loop below. (A previous literal
        // String.replace of a regex-shaped string here never matched and has been removed.)
        StringBuilder cleanedContent = new StringBuilder(content);

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

}
