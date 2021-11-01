/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode;

import nl.obren.sokrates.sourcecode.docs.Documentation;

public class Link {
    @Documentation(description = "A link display label")
    private String label = "";

    @Documentation(description = "A hyperlink href value")
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
