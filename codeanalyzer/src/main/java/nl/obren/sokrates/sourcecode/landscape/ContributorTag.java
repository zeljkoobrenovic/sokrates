package nl.obren.sokrates.sourcecode.landscape;

import java.util.ArrayList;
import java.util.List;

public class ContributorTag {
    private String name = "";
    private List<String> patterns = new ArrayList<>();

    public ContributorTag() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<String> patterns) {
        this.patterns = patterns;
    }
}
