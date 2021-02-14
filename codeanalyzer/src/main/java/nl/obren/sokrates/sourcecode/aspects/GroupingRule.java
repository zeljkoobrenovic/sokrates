package nl.obren.sokrates.sourcecode.aspects;

public class GroupingRule {
    private String name = "";
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
