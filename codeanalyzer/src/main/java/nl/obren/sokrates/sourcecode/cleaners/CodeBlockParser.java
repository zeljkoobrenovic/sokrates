/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.cleaners;

import nl.obren.sokrates.sourcecode.CleaningResult;
import org.apache.commons.lang3.StringUtils;

public class CodeBlockParser {
    private String startMarker = "\"";
    private String endMarker = "\"";
    private String escapeMarker = "\\";
    private boolean removeWhenCleaning = true;

    public CodeBlockParser() {
    }

    public CodeBlockParser(String start, String endMarker, String escapeMarker, boolean removeWhenCleaning) {
        this.startMarker = start;
        this.endMarker = endMarker;
        this.escapeMarker = escapeMarker;
        this.removeWhenCleaning = removeWhenCleaning;
    }

    public int getStringStartIndex(String content, int fromIndex) {
        return content.indexOf(startMarker, fromIndex);
    }

    public int getStringEndIndex(String content, int fromIndex) {
        int endIndex = content.indexOf(endMarker, fromIndex);
        if (StringUtils.isNotBlank(escapeMarker)) {
            String escapedEndMarker = escapeMarker + endMarker;
            String escapedEscapeMarker = escapeMarker + escapeMarker;
            boolean ignoreEscapedEscapeMarker = escapedEndMarker.equals(escapedEscapeMarker);

            int index1 = content.indexOf(escapedEndMarker, fromIndex);
            int index2 = ignoreEscapedEscapeMarker ? -1 : content.indexOf(escapedEscapeMarker, fromIndex);

            while (true) {
                while (index2 >= 0 && index1 >= 0 && index1 <= endIndex && index2 <= index1) {
                    index1 = content.indexOf(escapedEndMarker, index2 + escapedEscapeMarker.length() * 2);
                    index2 = ignoreEscapedEscapeMarker
                            ? -1
                            : content.indexOf(escapedEscapeMarker, index2 + escapedEscapeMarker.length() * 2);
                }

                if (endIndex > 0 && index1 >= 0 && index1 <= endIndex) {
                    int continueSearchFrom = index1 + escapedEndMarker.length();
                    endIndex = content.indexOf(endMarker, continueSearchFrom);
                    index1 = content.indexOf(escapedEndMarker, continueSearchFrom);
                    index2 = ignoreEscapedEscapeMarker ? -1 : content.indexOf(escapedEscapeMarker, continueSearchFrom);
                } else {
                    break;
                }
            }
        }

        return endIndex;
    }

    public CleaningResult cleanOrSkip(String content, int startIndex) {
        int endIndex = this.getStringEndIndex(content, startIndex + 1);

        if (endIndex == -1) {
            return new CleaningResult(content.substring(0, startIndex), startIndex);
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
