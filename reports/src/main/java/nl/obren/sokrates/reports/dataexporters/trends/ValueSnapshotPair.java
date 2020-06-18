/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.dataexporters.trends;

public class ValueSnapshotPair {
    private String snapshot = "";
    private Number value;

    public ValueSnapshotPair() {
    }

    public ValueSnapshotPair(String snapshot, Number value) {
        this.snapshot = snapshot;
        this.value = value;
    }

    public String getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(String snapshot) {
        this.snapshot = snapshot;
    }

    public Number getValue() {
        return value;
    }

    public void setValue(Number value) {
        this.value = value;
    }
}
