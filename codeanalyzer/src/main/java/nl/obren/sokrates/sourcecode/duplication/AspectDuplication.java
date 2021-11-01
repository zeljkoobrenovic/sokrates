/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.duplication;

import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;

public class AspectDuplication {
    private NamedSourceCodeAspect aspect;
    private int cleanedLinesOfCode;
    private int duplicatedLinesOfCode;

    public NamedSourceCodeAspect getAspect() {
        return aspect;
    }

    public void setAspect(NamedSourceCodeAspect aspect) {
        this.aspect = aspect;
    }

    public int getCleanedLinesOfCode() {
        return cleanedLinesOfCode;
    }

    public void setCleanedLinesOfCode(int cleanedLinesOfCode) {
        this.cleanedLinesOfCode = cleanedLinesOfCode;
    }

    public int getDuplicatedLinesOfCode() {
        return duplicatedLinesOfCode;
    }

    public void setDuplicatedLinesOfCode(int duplicatedLinesOfCode) {
        this.duplicatedLinesOfCode = duplicatedLinesOfCode;
    }
}
