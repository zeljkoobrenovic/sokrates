/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.dataexporters.dependencies;

import nl.obren.sokrates.sourcecode.dependencies.Dependency;

import java.util.ArrayList;
import java.util.List;

public class DependencyExportInfo {
    private List<SourceFileDependencyExportInfo> fromFiles = new ArrayList<>();
    private DependencyAnchorExportInfo from;
    private DependencyAnchorExportInfo to;

    public DependencyExportInfo getInstance(Dependency dependency) {
        DependencyExportInfo exportInfo = new DependencyExportInfo();

        exportInfo.setFrom(DependencyAnchorExportInfo.getInstance(dependency.getFrom()));
        exportInfo.setTo(DependencyAnchorExportInfo.getInstance(dependency.getTo()));

        dependency.getFromFiles().forEach(fromFile -> {
            exportInfo.getFromFiles().add(SourceFileDependencyExportInfo.getInstance(fromFile));
        });

        return exportInfo;
    }

    public List<SourceFileDependencyExportInfo> getFromFiles() {
        return fromFiles;
    }

    public void setFromFiles(List<SourceFileDependencyExportInfo> fromFiles) {
        this.fromFiles = fromFiles;
    }

    public DependencyAnchorExportInfo getFrom() {
        return from;
    }

    public void setFrom(DependencyAnchorExportInfo from) {
        this.from = from;
    }

    public DependencyAnchorExportInfo getTo() {
        return to;
    }

    public void setTo(DependencyAnchorExportInfo to) {
        this.to = to;
    }
}
