package nl.obren.sokrates.sourcecode.landscape;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.common.utils.RegexUtils;

import java.util.ArrayList;
import java.util.List;

public class ProjectTag {
    private String tag = "";
    private String color = "";
    private List<String> patterns = new ArrayList<>();

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

    public List<String> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<String> patterns) {
        this.patterns = patterns;
    }

    @JsonIgnore
    public boolean matches(String name) {
        for (String pattern : patterns) {
            if (RegexUtils.matchesEntirely(pattern, name)) {
                return true;
            }
        }

        return false;
    }
}
