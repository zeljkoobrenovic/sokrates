package nl.obren.sokrates.sourcecode.cleaners;

import nl.obren.sokrates.sourcecode.CleaningResult;
import org.apache.commons.lang3.StringUtils;

public class StringParsingHelper {
    private String startMarker = "\"";
    private String end = "\"";
    private String escape = "\\";
    private boolean removeWhenCleaning = true;

    public StringParsingHelper() {
    }

    public StringParsingHelper(String start, String end, String escape, boolean removeWhenCleaning) {
        this.startMarker = start;
        this.end = end;
        this.escape = escape;
        this.removeWhenCleaning = removeWhenCleaning;
    }

    public int getStringStartIndex(String content, int startIndex) {
        return content.indexOf(startMarker, startIndex);
    }

    public int getStringEndIndex(String content, int startIndex) {
        int endIndex = content.indexOf(end, startIndex);
        if (StringUtils.isNotBlank(escape)) {
            if (!escape.equals(end)) {
                while (endIndex >= escape.length() && content.substring(endIndex - escape.length(), endIndex).equals(escape)) {
                    endIndex = content.indexOf(end, endIndex + 1);
                }
            } else {
                while (endIndex >= escape.length() && content.length() > endIndex + 1 && content.substring(endIndex + 1, endIndex + 2).equals(escape)) {
                    endIndex = content.indexOf(end, endIndex + 2);
                }
            }
        }

        return endIndex;
    }

    public CleaningResult cleanOrSkip(String content, int startIndex) {
        int endIndex = this.getStringEndIndex(content, startIndex);

        if (endIndex == -1) {
            return new CleaningResult(content, content.length() - 1);
        }

        if (this.removeWhenCleaning) {
            String contentBefore = content.substring(0, startIndex);
            String contentAfter = content.substring(endIndex + end.length());

            String contentInMiddle = content.substring(startIndex, endIndex);
            String replacement = StringUtils.repeat("\n", StringUtils.countMatches(contentInMiddle, "\n"));

            return new CleaningResult(contentBefore + contentAfter, startIndex);
        } else {
            return new CleaningResult(content, endIndex + end.length());
        }
    }

    public String getStartMarker() {
        return startMarker;
    }

    public void setStartMarker(String startMarker) {
        this.startMarker = startMarker;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public String getEscape() {
        return escape;
    }

    public void setEscape(String escape) {
        this.escape = escape;
    }

    public boolean isRemoveWhenCleaning() {
        return removeWhenCleaning;
    }

    public void setRemoveWhenCleaning(boolean removeWhenCleaning) {
        this.removeWhenCleaning = removeWhenCleaning;
    }
}
