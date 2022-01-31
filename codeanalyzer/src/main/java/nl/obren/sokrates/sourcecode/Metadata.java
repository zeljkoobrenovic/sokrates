/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode;

import java.util.ArrayList;
import java.util.List;

public class Metadata {
    // A project name
    private String name = "";

    // A project description (included in the index page of HTML reports)
    private String description = "";

    // Additional project description shown as a tooltip in the HTML index report
    private String tooltip = "";

    // A link to an image file to be used as a logo in generated HTML reports
    private String logoLink = "";

    // A list of web links to resources related to the project
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
