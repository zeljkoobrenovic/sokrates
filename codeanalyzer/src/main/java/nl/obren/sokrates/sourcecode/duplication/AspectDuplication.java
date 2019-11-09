package nl.obren.sokrates.sourcecode.duplication;

import nl.obren.sokrates.sourcecode.aspects.SourceCodeAspect;

public class AspectDuplication {
    private SourceCodeAspect aspect;
    private int cleanedLinesOfCode;
    private int duplicatedLinesOfCode;

    public SourceCodeAspect getAspect() {
        return aspect;
    }

    public void setAspect(SourceCodeAspect aspect) {
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
