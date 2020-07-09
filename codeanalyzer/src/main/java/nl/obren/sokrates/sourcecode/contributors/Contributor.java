/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.contributors;

public class Contributor {
    private String name = "";
    private int commitsCount = 0;

    public Contributor() {
    }

    public Contributor(String name, int commitsCount) {
        this.name = name;
        this.commitsCount = commitsCount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCommitsCount() {
        return commitsCount;
    }

    public void setCommitsCount(int commitsCount) {
        this.commitsCount = commitsCount;
    }
}
