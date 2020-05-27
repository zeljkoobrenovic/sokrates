/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.common.utils.RegexUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SourceFileFilter {
    private static final Log LOG = LogFactory.getLog(SourceFileFilter.class);

    private String pathPattern = "";
    private String contentPattern = "";
    private Boolean include = true;
    private String note = "";

    @JsonIgnore
    private int maxLinesForContentSearch = -1;

    public SourceFileFilter() {
    }

    public SourceFileFilter(String pathPattern, String contentPattern) {
        this.pathPattern = pathPattern;
        this.contentPattern = contentPattern;
    }

    public static boolean matchesAnyLine(List<String> lines, String patternString) {
        for (String text : lines) {
            try {
                Pattern pattern = Pattern.compile(patternString);
                Matcher matcher = pattern.matcher(text);
                if (matcher.matches()) {
                    return true;
                }
            } catch (PatternSyntaxException e) {
                LOG.debug(e);
            }
        }

        return false;
    }

    public static int getMatchingLinesCount(List<String> lines, String patternString) {
        if (StringUtils.isBlank(patternString)) {
            return 1;
        }

        int count = 0;
        try {
            Pattern pattern = Pattern.compile(patternString);

            for (String text : lines) {
                Matcher matcher = pattern.matcher(text);
                if (matcher.matches()) {
                    count++;
                }
            }
        } catch (PatternSyntaxException e) {
            LOG.debug(e);
        }


        return count;
    }

    public String getPathPattern() {
        return pathPattern;
    }

    public void setPathPattern(String pathPattern) {
        this.pathPattern = pathPattern;
    }

    public String getContentPattern() {
        return contentPattern;
    }

    public void setContentPattern(String contentPattern) {
        this.contentPattern = contentPattern;
    }

    public Boolean getInclude() {
        return include;
    }

    public void setInclude(Boolean include) {
        this.include = include;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean matches(SourceFile sourceFile) {
        return pathMatches(sourceFile.getFile().getPath()) &&
                (StringUtils.isBlank(contentPattern) || contentMatches(sourceFile.getLines()));
    }

    public boolean pathMatches(String path) {
        boolean pathMatches;
        if (StringUtils.isNotBlank(pathPattern)) {
            pathMatches = RegexUtils.matchesEntirely(pathPattern, path)
                    || RegexUtils.matchesEntirely(pathPattern, path.replace("\\", "/"))
                    || RegexUtils.matchesEntirely(pathPattern, path.replace("/", "\\"))
                    || RegexUtils.matchesEntirely(pathPattern.replace("\\", "/"), path.replace("\\", "/"))
                    || RegexUtils.matchesEntirely(pathPattern.replace("\\", "/"), path.replace("/", "\\"));
        } else {
            pathMatches = true;
        }
        return pathMatches;
    }

    boolean contentMatches(List<String> lines) {
        if (StringUtils.isBlank(contentPattern)) {
            return true;
        } else {
            return SourceFileFilter.matchesAnyLine(getMaxLines(lines), contentPattern);
        }
    }

    private List<String> getMaxLines(List<String> lines) {
        if (maxLinesForContentSearch < 0 || maxLinesForContentSearch > lines.size()) {
            return lines;
        } else {
            return lines.subList(0, maxLinesForContentSearch);
        }
    }

    @Override
    public String toString() {
        String string = "";
        if (StringUtils.isNotBlank(pathPattern)) {
            string += "path like \"" + pathPattern + "\"";
        }
        if (StringUtils.isNotBlank(contentPattern)) {
            if (StringUtils.isNotBlank(string)) {
                string += " AND ";
            }
            string += "content like \"" + contentPattern + "\"";
        }
        return string;
    }

    @JsonIgnore
    public int getMaxLinesForContentSearch() {
        return maxLinesForContentSearch;
    }

    @JsonIgnore
    public void setMaxLinesForContentSearch(int maxLinesForContentSearch) {
        this.maxLinesForContentSearch = maxLinesForContentSearch;
    }
}
