package nl.obren.sokrates.sourcecode.cleaners;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommentsAndEmptyLinesCleaner {
    private String singleLineCommentStart = "//";
    private String blockCommentStart = "/*";
    private String blockCommentEnd = "*/";
    private List<StringParsingHelper> stringParsing = new ArrayList<>();
    private String stringDelimiter = "\"";
    private char stringEscapeChar = '\\';
    private String stringDelimiterAlt = "'";
    private char stringEscapeCharAlt = '\\';

    private int indexSingleLineCommentStart;
    private int indexBlockCommentStart;
    private int indexStringStart;
    private int indexStringStartAlt;
    private String content;

    public CommentsAndEmptyLinesCleaner() {
        stringParsing.add(new StringParsingHelper("\"", "\"", "\\", false));
        stringParsing.add(new StringParsingHelper("'", "'", "\\", false));
        stringParsing.add(new StringParsingHelper("`", "`", "\\", false));
        stringParsing.add(new StringParsingHelper("@\"", "\"", "\"", false));
        stringParsing.add(new StringParsingHelper("\"\"\"", "\"\"\"", "", false));
    }

    public CommentsAndEmptyLinesCleaner(String singleLineCommentStart, String blockCommentStart, String blockCommentEnd) {
        this();
        this.singleLineCommentStart = singleLineCommentStart;
        this.blockCommentStart = blockCommentStart;
        this.blockCommentEnd = blockCommentEnd;
    }

    public CommentsAndEmptyLinesCleaner(String singleLineCommentStart, String blockCommentStart, String blockCommentEnd, String stringDelimiter, char stringEscapeChar) {
        this();
        this.singleLineCommentStart = singleLineCommentStart;
        this.blockCommentStart = blockCommentStart;
        this.blockCommentEnd = blockCommentEnd;
        this.stringDelimiter = stringDelimiter;
        this.stringEscapeChar = stringEscapeChar;
    }

    public String getSingleLineCommentStart() {
        return singleLineCommentStart;
    }

    public void setSingleLineCommentStart(String singleLineCommentStart) {
        this.singleLineCommentStart = singleLineCommentStart;
    }

    public String getBlockCommentStart() {
        return blockCommentStart;
    }

    public void setBlockCommentStart(String blockCommentStart) {
        this.blockCommentStart = blockCommentStart;
    }

    public String getBlockCommentEnd() {
        return blockCommentEnd;
    }

    public void setBlockCommentEnd(String blockCommentEnd) {
        this.blockCommentEnd = blockCommentEnd;
    }

    public String getStringDelimiter() {
        return stringDelimiter;
    }

    public void setStringDelimiter(String stringDelimiter) {
        this.stringDelimiter = stringDelimiter;
    }

    public char getStringEscapeChar() {
        return stringEscapeChar;
    }

    public void setStringEscapeChar(char stringEscapeChar) {
        this.stringEscapeChar = stringEscapeChar;
    }

    public String getStringDelimiterAlt() {
        return stringDelimiterAlt;
    }

    public void setStringDelimiterAlt(String stringDelimiterAlt) {
        this.stringDelimiterAlt = stringDelimiterAlt;
    }

    public char getStringEscapeCharAlt() {
        return stringEscapeCharAlt;
    }

    public void setStringEscapeCharAlt(char stringEscapeCharAlt) {
        this.stringEscapeCharAlt = stringEscapeCharAlt;
    }

    public CleanedContent clean(String originalContent) {
        this.content = SourceCodeCleanerUtils.normalizeLineEnds(originalContent);

        initIndexes();

        int index;

        while ((index = getFirstIndex(Arrays.asList(indexSingleLineCommentStart, indexBlockCommentStart, indexStringStart))) != -1) {
            String contentBefore = content.substring(0, index);

            int startIndex = removeCommentInstance(index, contentBefore);

            if (startIndex == -1) break;

            updateIndexes(startIndex);
        }

        return SourceCodeCleanerUtils.cleanEmptyLinesWithLineIndexes(content);
    }

    private int removeCommentInstance(int index, String contentBefore) {
        int startIndex = -1;
        if (index == indexSingleLineCommentStart) {
            startIndex = removeSingleLineComment(index, contentBefore);
        } else if (index == indexBlockCommentStart) {
            startIndex = removeBlockComment(index, contentBefore);
        } else if (index == indexStringStart) {
            startIndex = skipString(index, contentBefore, stringDelimiter, stringEscapeChar);
        } else if (index == indexStringStartAlt) {
            startIndex = skipString(index, contentBefore, stringDelimiterAlt, stringEscapeCharAlt);
        }
        return startIndex;
    }

    private int skipString(int index, String contentBefore, String delimiter, char escapeChar) {
        int endIndex = content.indexOf(delimiter, index + 1);
        while (endIndex > 0 && content.charAt(endIndex - 1) == escapeChar) {
            endIndex = content.indexOf(delimiter, endIndex + 1);
        }
        if (endIndex == -1) {
            content = contentBefore;
            return -1;
        } else {
            contentBefore += content.substring(index, endIndex + 1);
        }
        return contentBefore.length();
    }

    private void updateIndexes(int startIndex) {
        indexSingleLineCommentStart = content.indexOf(singleLineCommentStart, startIndex);
        indexBlockCommentStart = content.indexOf(blockCommentStart, startIndex);
        indexStringStart = content.indexOf(stringDelimiter, startIndex);
        while (indexStringStart > 0 && content.charAt(indexStringStart - 1) == stringEscapeChar) {
            indexStringStart = content.indexOf(stringDelimiter, indexStringStart + 1);
        }
        if (StringUtils.isBlank(stringDelimiterAlt)) {
            indexStringStartAlt = -1;
        } else {
            indexStringStartAlt = content.indexOf(indexStringStartAlt, startIndex);
            while (indexStringStartAlt > 0 && content.charAt(indexStringStartAlt - 1) == stringEscapeCharAlt) {
                indexStringStartAlt = content.indexOf(indexStringStartAlt, indexStringStartAlt + 1);
            }
        }
    }

    private int removeBlockComment(int index, String contentBefore) {
        int endIndex = content.indexOf(blockCommentEnd, index + blockCommentStart.length());
        if (endIndex == -1) {
            content = contentBefore;
            return -1;
        } else {
            contentBefore += StringUtils.repeat("\n", StringUtils.countMatches(content.substring(index, endIndex), "\n"));
            content = contentBefore + content.substring(endIndex + blockCommentEnd.length());
        }
        return contentBefore.length();
    }

    private int removeSingleLineComment(int index, String contentBefore) {
        int endIndex = content.indexOf("\n", index + singleLineCommentStart.length());
        if (endIndex == -1) {
            content = contentBefore;
            return -1;
        } else {
            content = contentBefore + content.substring(endIndex);
        }
        return contentBefore.length();
    }

    private void initIndexes() {
        indexSingleLineCommentStart = content.indexOf(singleLineCommentStart);
        indexBlockCommentStart = content.indexOf(blockCommentStart);
        indexStringStart = content.indexOf(stringDelimiter);
        while (indexStringStart > 0 && content.charAt(indexStringStart - 1) == stringEscapeChar) {
            indexStringStart = content.indexOf(stringDelimiter, indexStringStart + 1);
        }
        if (StringUtils.isBlank(stringDelimiterAlt)) {
            indexStringStartAlt = -1;
        } else {
            indexStringStartAlt = content.indexOf(stringDelimiterAlt);
            while (indexStringStartAlt > 0 && content.charAt(indexStringStartAlt - 1) == stringEscapeCharAlt) {
                indexStringStartAlt = content.indexOf(stringDelimiterAlt, indexStringStartAlt + 1);
            }
        }
    }

    private int getFirstIndex(List<Integer> indexes) {
        int index = -1;
        for (Integer i : indexes) {
            if (i >= 0 && (index == -1 || i < index)) {
                index = i;
            }
        }
        return index;
    }

}
