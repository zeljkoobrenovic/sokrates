/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.reports.dataexporters.units;

/**
 * One entry in a {@code src/fragments/<type>.json} bundle, consumed by {@code src/viewer.html}.
 * The JSON field names ({@code name}, {@code file}, {@code from}, {@code to}, {@code loc},
 * {@code mccabe}, {@code ext}, {@code code}) are a contract with the viewer's render logic.
 */
public class FragmentExport {
    private String name = "";
    private String file = "";
    private int from = 0;
    private int to = 0;
    private int loc = 0;
    private int mccabe = 1;
    private String ext = "";
    private String code = "";

    public FragmentExport() {
    }

    public FragmentExport(String name, String file, int from, int to, int loc, int mccabe, String ext, String code) {
        this.name = name;
        this.file = file;
        this.from = from;
        this.to = to;
        this.loc = loc;
        this.mccabe = mccabe;
        this.ext = ext;
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public int getFrom() {
        return from;
    }

    public void setFrom(int from) {
        this.from = from;
    }

    public int getTo() {
        return to;
    }

    public void setTo(int to) {
        this.to = to;
    }

    public int getLoc() {
        return loc;
    }

    public void setLoc(int loc) {
        this.loc = loc;
    }

    public int getMccabe() {
        return mccabe;
    }

    public void setMccabe(int mccabe) {
        this.mccabe = mccabe;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
