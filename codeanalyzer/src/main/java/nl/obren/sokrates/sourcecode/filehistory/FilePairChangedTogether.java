/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.filehistory;

import nl.obren.sokrates.sourcecode.SourceFile;

import java.util.ArrayList;
import java.util.List;

public class FilePairChangedTogether {
    private SourceFile sourceFile1;
    private SourceFile sourceFile2;

    private List<String> dates = new ArrayList<>();

    public FilePairChangedTogether() {
    }

    public FilePairChangedTogether(SourceFile sourceFile1, SourceFile sourceFile2) {
        this.sourceFile1 = sourceFile1;
        this.sourceFile2 = sourceFile2;
    }

    public SourceFile getSourceFile1() {
        return sourceFile1;
    }

    public void setSourceFile1(SourceFile sourceFile1) {
        this.sourceFile1 = sourceFile1;
    }

    public SourceFile getSourceFile2() {
        return sourceFile2;
    }

    public void setSourceFile2(SourceFile sourceFile2) {
        this.sourceFile2 = sourceFile2;
    }

    public List<String> getDates() {
        return dates;
    }

    public void setDates(List<String> dates) {
        this.dates = dates;
    }
}
