/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.dependencies;

import nl.obren.sokrates.sourcecode.SourceFile;

public class SourceFileDependency {
    private SourceFile sourceFile;
    private String codeFragment = "";

    public SourceFileDependency() {
    }

    public SourceFileDependency(SourceFile sourceFile) {
        this.sourceFile = sourceFile;
    }

    public SourceFile getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(SourceFile sourceFile) {
        this.sourceFile = sourceFile;
    }

    public String getCodeFragment() {
        return codeFragment;
    }

    public void setCodeFragment(String codeFragment) {
        this.codeFragment = codeFragment;
    }
}
