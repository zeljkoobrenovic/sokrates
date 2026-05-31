/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape;

import nl.obren.sokrates.sourcecode.Metadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines a single virtual landscape: a landscape whose member repositories are selected
 * by regex patterns on repository names (rather than by physically moving report folders).
 * A repository belongs to this virtual landscape when its name matches any of the include
 * patterns and none of the exclude patterns.
 *
 * A virtual landscape may itself define nested virtual landscapes (unlimited depth). The nested
 * landscapes partition <em>this</em> virtual landscape's repositories, mirroring the top-level
 * behaviour, and are shown in this virtual landscape's own Sub-landscapes tab.
 */
public class VirtualLandscapeConfig {
    private Metadata metadata = new Metadata();
    private List<String> includeRepoNamePatterns = new ArrayList<>();
    private List<String> excludeRepoNamePatterns = new ArrayList<>();
    private VirtualLandscapesConfig virtualLandscapes = new VirtualLandscapesConfig();

    public VirtualLandscapeConfig() {
    }

    public VirtualLandscapesConfig getVirtualLandscapes() {
        return virtualLandscapes;
    }

    public void setVirtualLandscapes(VirtualLandscapesConfig virtualLandscapes) {
        this.virtualLandscapes = virtualLandscapes;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public List<String> getIncludeRepoNamePatterns() {
        return includeRepoNamePatterns;
    }

    public void setIncludeRepoNamePatterns(List<String> includeRepoNamePatterns) {
        this.includeRepoNamePatterns = includeRepoNamePatterns;
    }

    public List<String> getExcludeRepoNamePatterns() {
        return excludeRepoNamePatterns;
    }

    public void setExcludeRepoNamePatterns(List<String> excludeRepoNamePatterns) {
        this.excludeRepoNamePatterns = excludeRepoNamePatterns;
    }
}
