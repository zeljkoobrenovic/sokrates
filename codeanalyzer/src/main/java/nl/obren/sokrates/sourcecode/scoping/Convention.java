package nl.obren.sokrates.sourcecode.scoping;

import nl.obren.sokrates.sourcecode.SourceFileFilter;

public class Convention {
    private SourceFileFilter filter;

    public Convention(String pathPattern, String contentPattern, String note) {
        this.filter = new SourceFileFilter(pathPattern, contentPattern);
        this.filter.setNote(note);
    }

    public SourceFileFilter getFilter() {
        return filter;
    }

    public void setFilter(SourceFileFilter filter) {
        this.filter = filter;
    }
}
