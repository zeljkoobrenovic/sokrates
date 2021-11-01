/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.dependencies;

import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.SourceFileFilter;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;

import java.util.ArrayList;
import java.util.List;

public class DependencyAnchor {
    public static final int MAX_REGEX_LINE_SEARCH_DEPTH = 1000;
    private String anchor;
    private String codeFragment;
    private List<String> dependencyPatterns = new ArrayList<>();
    private List<SourceFile> sourceFiles = new ArrayList<>();
    private List<SourceFileFilter> filters = new ArrayList<>();

    public DependencyAnchor() {
    }

    public DependencyAnchor(String anchor) {
        this.anchor = anchor;
    }

    public String getAnchor() {
        return anchor;
    }

    public void setAnchor(String anchor) {
        this.anchor = anchor;
    }

    public String getCodeFragment() {
        return codeFragment;
    }

    public void setCodeFragment(String codeFragment) {
        this.codeFragment = codeFragment;
    }

    public List<String> getDependencyPatterns() {
        return dependencyPatterns;
    }

    public void setDependencyPatterns(List<String> dependencyPatterns) {
        this.dependencyPatterns = dependencyPatterns;
    }


    public List<SourceFile> getSourceFiles() {
        return sourceFiles;
    }

    public void setSourceFiles(List<SourceFile> sourceFiles) {
        this.sourceFiles = sourceFiles;
    }

    public boolean isLinkedFrom(String content) {
        return getDependencyCodeFragment(content) != null;
    }

    public String getDependencyCodeFragment(String content) {
        List<String> lines = SourceCodeCleanerUtils.splitInLines(content);
        for (String line : lines) {
            if (matchesLine(line)) {
                return line;
            }
        }

        return null;
    }

    public boolean matchesLine(String line) {
        line = line.replace("\t", " ");
        if (line.length() > MAX_REGEX_LINE_SEARCH_DEPTH) {
            line = line.substring(0, MAX_REGEX_LINE_SEARCH_DEPTH);
        }
        for (String regexPattern : dependencyPatterns) {
            if (RegexUtils.matchesEntirely(regexPattern, line)) {
                return true;
            }
        }

        return false;
    }

    public List<SourceFileFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<SourceFileFilter> filters) {
        this.filters = filters;
    }
}
