/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode;

import java.util.ArrayList;
import java.util.List;

public class IgnoredFilesGroup {
    private SourceFileFilter filter;
    private List<SourceFile> sourceFiles = new ArrayList<>();

    public IgnoredFilesGroup() {
    }

    public IgnoredFilesGroup(SourceFileFilter filter) {
        this.filter = filter;
    }

    public SourceFileFilter getFilter() {
        return filter;
    }

    public void setFilter(SourceFileFilter filter) {
        this.filter = filter;
    }

    public List<SourceFile> getSourceFiles() {
        return sourceFiles;
    }

    public void setSourceFiles(List<SourceFile> sourceFiles) {
        this.sourceFiles = sourceFiles;
    }
}
