package nl.obren.sokrates.sourcecode.landscape;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

public class ProjectTag {
    private static final Log LOG = LogFactory.getLog(ProjectTag.class);
    // A tag name
    private String tag = "";

    // A list of regex name patterns to tag projects. Any project with a name matching any of the regex patterns will be tagged with this tag.
    private List<String> patterns = new ArrayList<>();

    // A list of regex name patterns used to exclude projects (if included in the name patterns list)
    private List<String> excludePatterns = new ArrayList<>();

    // A list of extensions to include project if a project has these extensions as biggest (most line of code)
    private List<String> mainExtensions = new ArrayList<>();

    // A list of extensions to include project if a project has any file with this extensions
    private List<String> anyExtensions = new ArrayList<>();

    // A list of extensions to be excluded (if project has this extension as the biggest one)
    private List<String> excludeExtensions = new ArrayList<>();

    // A list of regex path patterns to tag projects. Any project with at least one file matching any of the regex patterns will be tagged with this tag.
    private List<String> pathPatterns = new ArrayList<>();

    // A list of regex path patterns used to exclude projects (if included in the path patterns list)
    private List<String> excludePathPatterns = new ArrayList<>();

    @JsonIgnore
    private ProjectTagGroup group;

    public ProjectTag() {
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
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

    public List<String> getAnyExtensions() {
        return anyExtensions;
    }

    public void setAnyExtensions(List<String> anyExtensions) {
        this.anyExtensions = anyExtensions;
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
    public boolean excludeName(String name) {
        for (String pattern : excludePatterns) {
            if (RegexUtils.matchesEntirely(pattern, name)) {
                return true;
            }
        }

        return false;
    }

    @JsonIgnore
    public boolean matchesName(String name) {
        for (String pattern : patterns) {
            if (RegexUtils.matchesEntirely(pattern, name)) {
                return true;
            }
        }

        return false;
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

    @JsonIgnore
    public boolean matchesPath(List<String> paths) {
        for (String path : paths) {
            for (String pattern : pathPatterns) {
                if (RegexUtils.matchesEntirely(pattern, path) && !excludePath(path)) {
                    return true;
                }
            }
        }

        return false;
    }

    @JsonIgnore
    public boolean matchesMainTechnology(String mainTech) {
        for (String extension : mainExtensions) {
            if (extension.equalsIgnoreCase(mainTech)) {
                return true;
            }
        }
        return false;
    }
    @JsonIgnore
    public boolean matchesAnyTechnology(List<NumericMetric> technologies) {
        for (NumericMetric tech : technologies) {
            for (String extension : anyExtensions) {
                if (extension.equalsIgnoreCase(tech.getName().replaceAll(".*[.]", ""))) {
                    return true;
                }
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

    @JsonIgnore
    public ProjectTagGroup getGroup() {
        return group;
    }

    @JsonIgnore
    public void setGroup(ProjectTagGroup group) {
        this.group = group;
    }
    @JsonIgnore
    public String getKey() {
        return (group != null ? group.getName() : "") + " / " + tag;
    }
}
