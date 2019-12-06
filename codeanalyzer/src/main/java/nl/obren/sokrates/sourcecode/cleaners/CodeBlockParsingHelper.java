package nl.obren.sokrates.sourcecode.cleaners;

import nl.obren.sokrates.sourcecode.CleaningResult;
import org.apache.commons.lang3.StringUtils;

public class CodeBlockParsingHelper {
    private String startMarker = "\"";
    private String endMarker = "\"";
    private String escapeMarker = "\\";
    private boolean removeWhenCleaning = true;

    public CodeBlockParsingHelper() {
    }

    public CodeBlockParsingHelper(String start, String endMarker, String escapeMarker, boolean removeWhenCleaning) {
        this.startMarker = start;
        this.endMarker = endMarker;
        this.escapeMarker = escapeMarker;
        this.removeWhenCleaning = removeWhenCleaning;
    }

    public int getStringStartIndex(String content, int fromIndex) {
        return content.indexOf(startMarker, fromIndex);
    }

    public int getStringEndIndex(String content, int startIndex) {
        int endIndex = content.indexOf(endMarker, startIndex);
        if (StringUtils.isNotBlank(escapeMarker)) {
            if (!escapeMarker.equals(endMarker)) {
                while (endIndex >= escapeMarker.length() && content.substring(endIndex - escapeMarker.length(), endIndex).equals(escapeMarker)) {
                    endIndex = content.indexOf(endMarker, endIndex + 1);
                }
            } else {
                while (endIndex >= escapeMarker.length() && content.length() > endIndex + 1 && content.substring(endIndex + 1, endIndex + 2).equals(escapeMarker)) {
                    endIndex = content.indexOf(endMarker, endIndex + 2);
                }
            }
        }

        return endIndex;
    }

    public CleaningResult cleanOrSkip(String content, int startIndex) {
        int endIndex = this.getStringEndIndex(content, startIndex + 1);

        if (endIndex == -1) {
            return new CleaningResult(content, content.length() - 1);
        }

        if (this.removeWhenCleaning) {
            String contentBefore = content.substring(0, startIndex);
            String contentAfter = content.substring(endIndex + endMarker.length());

            String contentInMiddle = content.substring(startIndex, endIndex);
            String replacement = StringUtils.repeat("\n", StringUtils.countMatches(contentInMiddle, "\n"));

            if (endMarker == "\n") replacement += "\n";

            String cleanedContent = contentBefore + replacement + contentAfter;
            return new CleaningResult(cleanedContent, (contentBefore + replacement).length());
        } else {
            return new CleaningResult(content, endIndex + endMarker.length());
        }
    }

    public String getStartMarker() {
        return startMarker;
    }

    public void setStartMarker(String startMarker) {
        this.startMarker = startMarker;
    }

    public String getEndMarker() {
        return endMarker;
    }

    public void setEndMarker(String endMarker) {
        this.endMarker = endMarker;
    }

    public String getEscapeMarker() {
        return escapeMarker;
    }

    public void setEscapeMarker(String escapeMarker) {
        this.escapeMarker = escapeMarker;
    }

    public boolean isRemoveWhenCleaning() {
        return removeWhenCleaning;
    }

    public void setRemoveWhenCleaning(boolean removeWhenCleaning) {
        this.removeWhenCleaning = removeWhenCleaning;
    }
}
