package nl.obren.sokrates.sourcecode.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.common.utils.RegexUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TagRule {
    // A tag name
    private String tag = "";

    // A tag color
    private String color = "";

    // A list of regex path patterns to tag projects. Any project with at least one file matching any of the regex patterns will be tagged with this tag.
    private List<String> pathPatterns = new ArrayList<>();

    // A list of regex path patterns ignore for tagging.
    private List<String> excludePathPatterns = new ArrayList<>();

    public TagRule() {
    }

    public TagRule(String tag, String color, List<String> pathPatterns) {
        this.tag = tag;
        this.color = color;
        this.pathPatterns = pathPatterns;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public List<String> getPathPatterns() {
        return pathPatterns;
    }

    public void setPathPatterns(List<String> pathPatterns) {
        this.pathPatterns = pathPatterns;
    }

    public List<String> getExcludePathPatterns() {
        return excludePathPatterns;
    }

    public void setExcludePathPatterns(List<String> excludePathPatterns) {
        this.excludePathPatterns = excludePathPatterns;
    }

    @JsonIgnore
    public String matchesPath(List<String> paths) {
        List<String> foundPaths = new ArrayList<>();
        for (String path : paths) {
            for (String pattern : pathPatterns) {
                if (RegexUtils.matchesEntirely(pattern, path) && !excludePath(path)) {
                    foundPaths.add(path);
                }
            }
        }

        int limit = 20;
        String evidence = foundPaths.stream().sorted().limit(limit).collect(Collectors.joining("\n"));

        if (foundPaths.size() > limit) {
            evidence += "\n...\n(found " + (foundPaths.size() - limit) + " more files)";
        }


        return evidence;
    }

    @JsonIgnore
    public boolean excludePath(String path) {
        for (String pattern : excludePathPatterns) {
            if (RegexUtils.matchesEntirely(pattern, path)) {
                return true;
            }
        }

        return false;
    }
}
