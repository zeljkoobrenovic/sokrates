package nl.obren.sokrates.sourcecode.landscape;

import java.util.ArrayList;
import java.util.List;

public class CustomTagsConfig {
    private String logosRoot = "";
    private List<CustomTagGroup> groups = new ArrayList<>();

    public String getLogosRoot() {
        return logosRoot;
    }

    public void setLogosRoot(String logosRoot) {
        this.logosRoot = logosRoot;
    }

    public List<CustomTagGroup> getGroups() {
        return groups;
    }

    public void setGroups(List<CustomTagGroup> groups) {
        this.groups = groups;
    }
}
