package nl.obren.sokrates.sourcecode.landscape;

import java.util.ArrayList;
import java.util.List;

public class CustomTagGroup {
    private String name = "";
    private String description = "";
    private List<CustomTag> tags = new ArrayList<>();
    private List<CustomTagGroup> subGroups = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<CustomTag> getTags() {
        return tags;
    }

    public void setTags(List<CustomTag> tags) {
        this.tags = tags;
    }

    public List<CustomTagGroup> getSubGroups() {
        return subGroups;
    }

    public void setSubGroups(List<CustomTagGroup> subGroups) {
        this.subGroups = subGroups;
    }
}
