/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication.impl;

public class LineInfo {
    private static int idCounter = 0;
    private int id;

    public LineInfo() {
        id = idCounter++;
    }

    public static void resetCounter() {
        idCounter = 0;
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
