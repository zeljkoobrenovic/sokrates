/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.analysis.files;

public class UnitCategoryNames {
    private String negligibleRisk = "";
    private String lowRisk = "";
    private String mediumRisk = "";
    private String highRisk = "";
    private String veryHighRisk = "";

    public UnitCategoryNames() {
    }

    public UnitCategoryNames(String negligibleRisk, String lowRisk, String mediumRisk, String highRisk, String veryHighRisk) {
        this.negligibleRisk = negligibleRisk;
        this.lowRisk = lowRisk;
        this.mediumRisk = mediumRisk;
        this.highRisk = highRisk;
        this.veryHighRisk = veryHighRisk;
    }

    public String getNegligibleRisk() {
        return negligibleRisk;
    }

    public void setNegligibleRisk(String negligibleRisk) {
        this.negligibleRisk = negligibleRisk;
    }

    public String getLowRisk() {
        return lowRisk;
    }

    public void setLowRisk(String lowRisk) {
        this.lowRisk = lowRisk;
    }

    public String getMediumRisk() {
        return mediumRisk;
    }

    public void setMediumRisk(String mediumRisk) {
        this.mediumRisk = mediumRisk;
    }

    public String getHighRisk() {
        return highRisk;
    }

    public void setHighRisk(String highRisk) {
        this.highRisk = highRisk;
    }

    public String getVeryHighRisk() {
        return veryHighRisk;
    }

    public void setVeryHighRisk(String veryHighRisk) {
        this.veryHighRisk = veryHighRisk;
    }
}
