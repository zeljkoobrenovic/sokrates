/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode;

public class Link {
    // Display label
    private String label = "";

    // A hyperlink href value (a URL)
    private String href = "";

    public Link() {
    }

    public Link(String label, String href) {
        this.label = label;
        this.href = href;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }
}
