/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape;

import nl.obren.sokrates.sourcecode.Metadata;

import java.util.ArrayList;
import java.util.List;

public class LandscapeGroup {
    private Metadata metadata = new Metadata();

    private List<SokratesProjectLink> projects = new ArrayList<>();

    private List<LandscapeGroup> subGroups = new ArrayList<>();

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public List<SokratesProjectLink> getProjects() {
        return projects;
    }

    public void setProjects(List<SokratesProjectLink> projects) {
        this.projects = projects;
    }

    public List<LandscapeGroup> getSubGroups() {
        return subGroups;
    }

    public void setSubGroups(List<LandscapeGroup> subGroups) {
        this.subGroups = subGroups;
    }
}
