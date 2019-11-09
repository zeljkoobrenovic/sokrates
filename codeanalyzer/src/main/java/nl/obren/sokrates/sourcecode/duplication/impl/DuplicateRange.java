package nl.obren.sokrates.sourcecode.duplication.impl;

public class DuplicateRange {
    private int start;
    private int end;

    public DuplicateRange(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public boolean includes(DuplicateRange otherRange) {
        return this.start <= otherRange.start && this.end >= otherRange.end;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }
}