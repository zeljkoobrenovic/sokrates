package nl.obren.sokrates.sourcecode.cleaners;

import nl.obren.sokrates.common.utils.RegexUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SourceCodeCleanerUtils {
    public static String normalizeLineEnds(String content) {
        return content
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replace("\t", "    ")
                .replace("\b", "")
                .replace("\f", "");
    }

    public static String cleanEmptyLines(String content) {
        StringBuilder cleanedContent = new StringBuilder();

        SourceCodeCleanerUtils.splitInLines(content).stream().filter(line -> isNotEmptyLine(line)).forEach(line -> {
            appendNewLineIfNotEmpty(cleanedContent);
            cleanedContent.append(line);
        });

        return cleanedContent.toString();
    }

    private static void appendNewLineIfNotEmpty(StringBuilder stringBuilder) {
        if (stringBuilder.length() > 0) {
            stringBuilder.append("\n");
        }
    }

    public static CleanedContent cleanEmptyLinesWithLineIndexes(String content) {
        CleanedContent cleanedContent = new CleanedContent();
        StringBuilder cleanedContentString = new StringBuilder();

        List<String> lines = SourceCodeCleanerUtils.splitInLines(content);
        int i = 0;
        for (String line : lines) {
            if (isNotEmptyLine(line)) {
                appendNewLineIfNotEmpty(cleanedContentString);
                cleanedContentString.append(line);
                cleanedContent.getFileLineIndexes().add(i);
            }
            i++;
        }

        cleanedContent.setCleanedContent(cleanedContentString.toString());

        return cleanedContent;
    }

    private static boolean isNotEmptyLine(String line) {
        return !replaceTabs(line).trim().isEmpty();
    }

    public static String trimLines(String content) {
        StringBuilder cleanedContent = new StringBuilder();

        List<String> lines = SourceCodeCleanerUtils.splitInLines(content);
        for (String line : lines) {
            line = replaceTabs(line).trim();
            while (line.contains("  ")) {
                line = line.replace("  ", " ");
            }
            cleanedContent.append(line.trim());
            cleanedContent.append("\n");
        }

        return StringUtils.removeEnd(cleanedContent.toString(), "\n");
    }

    public static List<String> splitInLines(String content) {
        return Arrays.asList(SourceCodeCleanerUtils.normalizeLineEnds(content).split("\n"));
    }

    private static String replaceTabs(String line) {
        return line.replace("\t", " ");
    }

    public static String emptyLinesMatchingPattern(String pattern, String content) {
        return emptyLinesMatchingPattern(pattern, SourceCodeCleanerUtils.splitInLines(content));

    }

    public static String emptyLinesMatchingPattern(String pattern, List<String> lines) {
        StringBuilder cleanedContent = new StringBuilder();

        for (String line : lines) {
            if (!RegexUtils.matchesEntirely(pattern, line)) {
                cleanedContent.append(line);
            }
            cleanedContent.append("\n");
        }

        return cleanedContent.toString();

    }

    public static CleanedContent cleanCommentsAndEmptyLines(String content, String singleLineCommentStart, String
            blockCommentStart, String blockCommentEnd) {
        return cleanCommentsAndEmptyLines(content, singleLineCommentStart, blockCommentStart, blockCommentEnd, "\"", "\\");
    }

    public static CleanedContent cleanCommentsAndEmptyLines(String content, String singleLineCommentStart, String
            blockCommentStart, String blockCommentEnd, String stringDelimiter, String stringEscapeMarker) {
        CommentsAndEmptyLinesCleaner cleaner = new CommentsAndEmptyLinesCleaner(singleLineCommentStart, blockCommentStart, blockCommentEnd, stringDelimiter, stringEscapeMarker);
        return cleaner.clean(content);
    }

    public static CleanedContent cleanSingeLineCommentsAndEmptyLines(String content, List<String> singleLineCommentStarts) {
        content = SourceCodeCleanerUtils.normalizeLineEnds(content);
        content = SourceCodeCleanerUtils.emptyStringsLookingLikeComments(content);

        for (String singleLineCommentStart : singleLineCommentStarts) {
            content = CommentsCleanerUtils.cleanLineComments(content, singleLineCommentStart);
        }

        return SourceCodeCleanerUtils.cleanEmptyLinesWithLineIndexes(content);
    }

    public static CleanedContent emptyComments(String content, String singleLineCommentStart,
                                               String blockCommentStart, String blockCommentEnd) {
        content = SourceCodeCleanerUtils.normalizeLineEnds(content);
        content = SourceCodeCleanerUtils.emptyStringsLookingLikeComments(content);

        content = CommentsCleanerUtils.cleanBlockComments(content, blockCommentStart, blockCommentEnd);
        if (singleLineCommentStart != null) {
            content = CommentsCleanerUtils.cleanLineComments(content, singleLineCommentStart);
        }

        CleanedContent cleanedContent = new CleanedContent();
        cleanedContent.setCleanedContent(content);
        cleanedContent.setFileLineIndexes(new ArrayList<>());

        List<String> lines = SourceCodeCleanerUtils.splitInLines(content);
        for (int i = 0; i < lines.size(); i++) {
            cleanedContent.getFileLineIndexes().add(i);
        }

        return cleanedContent;
    }

    private static String emptyStringsLookingLikeComments(String content) {
        content = content.replace("\\\"", "");
        content = content.replace("\\'", "");
        content = content.replace("\"**/*\"", "\"\"");
        content = content.replace("\"/*\"", "\"\"");
        content = content.replace("\"*/\"", "\"\"");
        content = content.replace("\"//\"", "\"\"");
        content = content.replace("'**/*'", "''");
        content = content.replace("'/*'", "''");
        content = content.replace("'*/'", "''");
        content = content.replace("'//'", "''");

        return content;
    }
}
