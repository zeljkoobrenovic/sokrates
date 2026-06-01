/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.units;

import nl.obren.sokrates.sourcecode.ExtensionGroupExtractor;

/**
 * A flat, serializable view of a {@link UnitInfo} for the client-rendered units explorer
 * (units-explorer.html). Mirrors {@code FileExport}: carries only the fields the explorer renders,
 * embedded into the template as JSON.
 */
public class UnitExport {
    private String name = "";
    private String longName = "";
    private String file = "";
    private int linesOfCode;
    private int mcCabeIndex;
    private int numberOfParameters;
    private int startLine;

    public UnitExport() {
    }

    public UnitExport(UnitInfo unit) {
        this.name = unit.getShortName();
        this.longName = unit.getLongName();
        this.file = unit.getSourceFile() != null ? unit.getSourceFile().getRelativePath() : "";
        this.linesOfCode = unit.getLinesOfCode();
        this.mcCabeIndex = unit.getMcCabeIndex();
        this.numberOfParameters = unit.getNumberOfParameters();
        this.startLine = unit.getStartLine();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLongName() {
        return longName;
    }

    public void setLongName(String longName) {
        this.longName = longName;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public int getLinesOfCode() {
        return linesOfCode;
    }

    public void setLinesOfCode(int linesOfCode) {
        this.linesOfCode = linesOfCode;
    }

    public int getMcCabeIndex() {
        return mcCabeIndex;
    }

    public void setMcCabeIndex(int mcCabeIndex) {
        this.mcCabeIndex = mcCabeIndex;
    }

    public int getNumberOfParameters() {
        return numberOfParameters;
    }

    public void setNumberOfParameters(int numberOfParameters) {
        this.numberOfParameters = numberOfParameters;
    }

    public int getStartLine() {
        return startLine;
    }

    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    /**
     * The unit's language, derived from its file's path extension (same convention as FileExport),
     * so the explorer can render a matching language icon.
     */
    public String getMainLang() {
        return file != null && !file.isEmpty()
                ? ExtensionGroupExtractor.getExtension(file).toLowerCase().trim() : "";
    }
}
