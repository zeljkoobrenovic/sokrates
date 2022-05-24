package nl.obren.sokrates.sourcecode.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.common.utils.RegexUtils;

import java.util.ArrayList;
import java.util.List;

public class TagRule {
    // A tag name
    private String tag = "";

    // A tag color
    private String color = "";

    // A list of regex path patterns to tag projects. Any project with at least one file matching any of the regex patterns will be tagged with this tag.
    private List<String> pathPatterns = new ArrayList<>();

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

    @JsonIgnore
    public boolean matchesPath(List<String> paths) {
        for (String path : paths) {
            for (String pattern : pathPatterns) {
                if (RegexUtils.matchesEntirely(pattern, path)) {
                    return true;
                }
            }
        }

        return false;
    }
}
