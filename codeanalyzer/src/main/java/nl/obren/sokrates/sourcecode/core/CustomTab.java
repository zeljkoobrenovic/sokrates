/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;

/**
 * An optional custom tab in the per-repository report: a labeled tab whose whole content is an iframe.
 */
public class CustomTab {
    // The label shown on the tab button
    private String label = "";

    // The URL loaded in the iframe (absolute, or relative to the report's html/ folder)
    private String iframeLink = "";

    public CustomTab() {
    }

    public CustomTab(String label, String iframeLink) {
        this.label = label;
        this.iframeLink = iframeLink;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getIframeLink() {
        return iframeLink;
    }

    public void setIframeLink(String iframeLink) {
        this.iframeLink = iframeLink;
    }

    @JsonIgnore
    public boolean isValid() {
        return StringUtils.isNotBlank(label) && StringUtils.isNotBlank(iframeLink);
    }
}
