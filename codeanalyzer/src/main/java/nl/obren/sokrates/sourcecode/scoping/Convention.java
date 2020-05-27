/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.scoping;

import nl.obren.sokrates.sourcecode.SourceFileFilter;

public class Convention {
    private SourceFileFilter filter;

    public Convention(String pathPattern, String note) {
        this(pathPattern, "", note);
    }

    public Convention(String pathPattern, String contentPattern, String note) {
        this.filter = new SourceFileFilter(pathPattern, contentPattern);
        this.filter.setNote(note);
    }

    public Convention(String pathPattern, String contentPattern, int maxLinesForContentSearch, String note) {
        this(pathPattern, contentPattern, note);
        this.filter.setMaxLinesForContentSearch(maxLinesForContentSearch);
    }

    public SourceFileFilter getFilter() {
        return filter;
    }

    public void setFilter(SourceFileFilter filter) {
        this.filter = filter;
    }
}
