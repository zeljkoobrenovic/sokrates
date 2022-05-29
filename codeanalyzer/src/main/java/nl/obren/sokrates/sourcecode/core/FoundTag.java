package nl.obren.sokrates.sourcecode.core;

public class FoundTag {
    private TagRule tagRule;
    private String path;

    public FoundTag() {
    }

    public FoundTag(TagRule tagRule, String path) {
        this.tagRule = tagRule;
        this.path = path;
    }

    public TagRule getTagRule() {
        return tagRule;
    }

    public void setTagRule(TagRule tagRule) {
        this.tagRule = tagRule;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
