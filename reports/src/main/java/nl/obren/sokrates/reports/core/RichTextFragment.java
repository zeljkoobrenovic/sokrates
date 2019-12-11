/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.core;

public class RichTextFragment {
    private String id = "";
    private String fragment = "";
    private String description = "";
    private RichTextFragment.Type type = RichTextFragment.Type.HTML;

    public RichTextFragment() {
    }

    public RichTextFragment(String fragment, Type type) {
        this.fragment = fragment;
        this.type = type;
    }

    public RichTextFragment(String id, String fragment, Type type) {
        this.id = id;
        this.fragment = fragment;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
