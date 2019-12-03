package nl.obren.sokrates.reports.core;

public class RichTextFragment {
    private String fragment = "";
    private String description = "";
    private RichTextFragment.Type type = RichTextFragment.Type.HTML;

    public RichTextFragment() {
    }

    public RichTextFragment(String fragment, Type type) {
        this.fragment = fragment;
        this.type = type;
    }

    public String getFragment() {
        return fragment;
    }

    public void setFragment(String fragment) {
        this.fragment = fragment;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public enum Type {HTML, GRAPHVIZ, SVG}
}
