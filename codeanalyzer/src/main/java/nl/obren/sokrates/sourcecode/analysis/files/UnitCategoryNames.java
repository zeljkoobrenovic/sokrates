package nl.obren.sokrates.sourcecode.analysis.files;

public class UnitCategoryNames {
    private String lowRisk = "";
    private String mediumRisk = "";
    private String highRisk = "";
    private String veryHighRisk = "";

    public UnitCategoryNames() {
    }

    public UnitCategoryNames(String lowRisk, String mediumRisk, String highRisk, String veryHighRisk) {
        this.lowRisk = lowRisk;
        this.mediumRisk = mediumRisk;
        this.highRisk = highRisk;
        this.veryHighRisk = veryHighRisk;
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
