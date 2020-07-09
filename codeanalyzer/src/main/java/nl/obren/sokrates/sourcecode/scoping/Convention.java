/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.scoping;

import nl.obren.sokrates.sourcecode.SourceFileFilter;

public class Convention extends SourceFileFilter {
    public Convention() {
    }

    public Convention(String pathPattern, String contentPattern, String note) {
        super(pathPattern, contentPattern);
        setNote(note);
    }

    public Convention(String pathPattern, String contentPattern, int maxLinesForContentSearch, String note) {
        this(pathPattern, contentPattern, note);
        setMaxLinesForContentSearch(maxLinesForContentSearch);
    }
}
