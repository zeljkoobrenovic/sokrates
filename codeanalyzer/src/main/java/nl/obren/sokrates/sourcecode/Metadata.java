/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode;

import nl.obren.sokrates.sourcecode.docs.ComplexDocumentation;
import nl.obren.sokrates.sourcecode.docs.Documentation;

import java.util.ArrayList;
import java.util.List;

public class Metadata {
    @Documentation(description = "A project name")
    private String name = "";

    @Documentation(description = "a project description")
    private String description = "";

    @Documentation(description = "Additional project description shown as a tooltip over documentation")
    private String tooltip = "";

    @Documentation(description = "A link to an image file to be used as a logo in reports")
    private String logoLink = "";

    @ComplexDocumentation(description = "A list of web links to resources related to the project", clazz = Link.class)
    private List<Link> links = new ArrayList<>();

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

    public String getLogoLink() {
        return logoLink;
    }

    public void setLogoLink(String logoLink) {
        this.logoLink = logoLink;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public String getTooltip() {
        return tooltip;
    }

    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }
}
