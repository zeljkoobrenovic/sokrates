package nl.obren.sokrates.sourcecode.core;

public class FoundTag {
    private TagRule tagRule;
    private String evidence = "";

    public FoundTag() {
    }

    public FoundTag(TagRule tagRule, String evidence) {
        this.tagRule = tagRule;
        this.evidence = evidence;
    }

    public TagRule getTagRule() {
        return tagRule;
    }

    public void setTagRule(TagRule tagRule) {
        this.tagRule = tagRule;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }
}
