/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape;

import nl.obren.sokrates.sourcecode.Metadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Configures virtual landscapes for a (parent) landscape. When at least one virtual landscape
 * is defined, the parent generates a full landscape report per virtual landscape (selected by
 * repository-name patterns) plus a "Remainder" landscape for repositories not matched by any
 * virtual landscape. Empty by default, so existing configs are unaffected.
 */
public class VirtualLandscapesConfig {
    private Metadata remainderLandscapeMetadata = defaultRemainderMetadata();
    private List<VirtualLandscapeConfig> landscapes = new ArrayList<>();

    public VirtualLandscapesConfig() {
    }

    private static Metadata defaultRemainderMetadata() {
        Metadata metadata = new Metadata();
        metadata.setName("Remainder");
        return metadata;
    }

    public Metadata getRemainderLandscapeMetadata() {
        return remainderLandscapeMetadata;
    }

    public void setRemainderLandscapeMetadata(Metadata remainderLandscapeMetadata) {
        this.remainderLandscapeMetadata = remainderLandscapeMetadata;
    }

    public List<VirtualLandscapeConfig> getLandscapes() {
        return landscapes;
    }

    public void setLandscapes(List<VirtualLandscapeConfig> landscapes) {
        this.landscapes = landscapes;
    }
}
