package nl.obren.sokrates.sourcecode.stats;

public class RiskDistributionStats {
    private String key = "";

    private int mediumRiskThreshold;
    private int highRiskThreshold;
    private int veryHighRiskThreshold;

    private int lowRiskValue;
    private int mediumRiskValue;
    private int highRiskValue;
    private int veryHighRiskValue;

    private int lowRiskCount;
    private int mediumRiskCount;
    private int highRiskCount;
    private int veryHighRiskCount;

    private String lowRiskLabel = "";
    private String mediumRiskLabel = "";
    private String highRiskLabel = "";
    private String veryHighRiskLabel = "";

    public RiskDistributionStats() {
    }

    public RiskDistributionStats(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public RiskDistributionStats(int mediumRiskThreshold, int highRiskThreshold, int veryHighRiskThreshold) {
        this.mediumRiskThreshold = mediumRiskThreshold;
        this.highRiskThreshold = highRiskThreshold;
        this.veryHighRiskThreshold = veryHighRiskThreshold;
    }

    public void reset() {
        lowRiskValue = 0;
        lowRiskCount = 0;
        mediumRiskValue = 0;
        mediumRiskCount = 0;
        highRiskValue = 0;
        highRiskCount = 0;
        veryHighRiskValue = 0;
        veryHighRiskCount = 0;
    }

    public void update(int testValue, int addValue) {
        if (testValue <= mediumRiskThreshold) {
            lowRiskValue += addValue;
            lowRiskCount++;
        } else if (testValue <= highRiskThreshold) {
            mediumRiskValue += addValue;
            mediumRiskCount++;
        } else if (testValue <= veryHighRiskThreshold) {
            highRiskValue += addValue;
            highRiskCount++;
        } else {
            veryHighRiskValue += addValue;
            veryHighRiskCount++;
        }
    }

    public int getTotalValue() {
        return lowRiskValue + mediumRiskValue + highRiskValue + veryHighRiskValue;
    }

    public int getTotalCount() {
        return lowRiskCount + mediumRiskCount + highRiskCount + veryHighRiskCount;
    }

    public int getLowRiskValue() {
        return lowRiskValue;
    }

    public int getMediumRiskValue() {
        return mediumRiskValue;
    }

    public int getHighRiskValue() {
        return highRiskValue;
    }

    public int getVeryHighRiskValue() {
        return veryHighRiskValue;
    }

    public int getLowRiskCount() {
        return lowRiskCount;
    }

    public int getMediumRiskCount() {
        return mediumRiskCount;
    }

    public int getHighRiskCount() {
        return highRiskCount;
    }

    public int getVeryHighRiskCount() {
        return veryHighRiskCount;
    }

    public int getMediumRiskThreshold() {
        return mediumRiskThreshold;
    }

    public void setMediumRiskThreshold(int mediumRiskThreshold) {
        this.mediumRiskThreshold = mediumRiskThreshold;
    }

    public int getHighRiskThreshold() {
        return highRiskThreshold;
    }

    public void setHighRiskThreshold(int highRiskThreshold) {
        this.highRiskThreshold = highRiskThreshold;
    }

    public int getVeryHighRiskThreshold() {
        return veryHighRiskThreshold;
    }

    public void setVeryHighRiskThreshold(int veryHighRiskThreshold) {
        this.veryHighRiskThreshold = veryHighRiskThreshold;
    }

    public String getVeryHighRiskLabel() {
        return veryHighRiskLabel;
    }

    public void setVeryHighRiskLabel(String veryHighRiskLabel) {
        this.veryHighRiskLabel = veryHighRiskLabel;
    }

    public String getHighRiskLabel() {
        return highRiskLabel;
    }

    public void setHighRiskLabel(String highRiskLabel) {
        this.highRiskLabel = highRiskLabel;
    }

    public String getMediumRiskLabel() {
        return mediumRiskLabel;
    }

    public void setMediumRiskLabel(String mediumRiskLabel) {
        this.mediumRiskLabel = mediumRiskLabel;
    }

    public String getLowRiskLabel() {
        return lowRiskLabel;
    }

    public void setLowRiskLabel(String lowRiskLabel) {
        this.lowRiskLabel = lowRiskLabel;
    }


}

