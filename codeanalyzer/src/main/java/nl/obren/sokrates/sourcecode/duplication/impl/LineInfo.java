/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication.impl;

public class LineInfo {
    private final int id;

    public LineInfo(int id) {
        this.id = id;
    }

    private int count = 1;

    public int getId() {
        return id;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void incrementCount() {
        this.count++;
    }
}
