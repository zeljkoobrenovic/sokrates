package nl.obren.sokrates.sourcecode.landscape;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.common.utils.RegexUtils;

import java.util.ArrayList;
import java.util.List;

public class ProjectTag {
    private String tag = "";
    private String color = "";
    private List<String> patterns = new ArrayList<>();
    private List<String> excludePatterns = new ArrayList<>();
    private List<String> mainExtensions = new ArrayList<>();
    private List<String> excludeExtensions = new ArrayList<>();

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

    public List<String> getMainExtensions() {
        return mainExtensions;
    }

    public void setMainExtensions(List<String> mainExtensions) {
        this.mainExtensions = mainExtensions;
    }

    public List<String> getExcludePatterns() {
        return excludePatterns;
    }

    public void setExcludePatterns(List<String> excludePatterns) {
        this.excludePatterns = excludePatterns;
    }

    public List<String> getExcludeExtensions() {
        return excludeExtensions;
    }

    public void setExcludeExtensions(List<String> excludeExtensions) {
        this.excludeExtensions = excludeExtensions;
    }

    @JsonIgnore
    public boolean exclude(String name) {
        for (String pattern : excludePatterns) {
            if (RegexUtils.matchesEntirely(pattern, name)) {
                return true;
            }
        }

        return false;
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

    @JsonIgnore
    public boolean matchesMainTechnology(String mainTech) {
        for (String tech : mainExtensions) {
            if (tech.equalsIgnoreCase(mainTech)) {
                return true;
            }
        }
        return false;
    }

    @JsonIgnore
    public boolean excludesMainTechnology(String mainTech) {
        for (String tech : excludeExtensions) {
            if (tech.equalsIgnoreCase(mainTech)) {
                return true;
            }
        }
        return false;
    }
}
