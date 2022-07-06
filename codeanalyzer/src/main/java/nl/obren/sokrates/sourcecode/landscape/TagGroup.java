package nl.obren.sokrates.sourcecode.landscape;

import java.util.ArrayList;
import java.util.List;

public class TagGroup {
    private String name = "";
    private String description = "";
    private String color = "";

    private List<RepositoryTag> repositoryTags = new ArrayList<>();

    public TagGroup() {
    }

    public TagGroup(String name) {
        this.name = name;
    }

    public TagGroup(String name, String color) {
        this.name = name;
        this.color = color;
    }

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

    public List<RepositoryTag> getRepositoryTags() {
        return repositoryTags;
    }

    public void setRepositoryTags(List<RepositoryTag> repositoryTags) {
        this.repositoryTags = repositoryTags;
    }
    public void setProjectTags(List<RepositoryTag> repositoryTags) {
        this.repositoryTags = repositoryTags;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
