package nl.obren.sokrates.sourcecode.aspects;

public class GroupingRule {
    // A name of the group
    private String name = "";

    // A regex pattern, applied on a component name, used to include components in this group
    private String pattern = "";

    public GroupingRule() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
}
