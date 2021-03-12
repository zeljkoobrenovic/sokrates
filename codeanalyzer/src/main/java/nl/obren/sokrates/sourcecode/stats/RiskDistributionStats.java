/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.stats;

public class RiskDistributionStats {
    private String key = "";

    private int lowRiskThreshold;
    private int mediumRiskThreshold;
    private int highRiskThreshold;
    private int veryHighRiskThreshold;

    private int negligibleRiskValue;
    private int lowRiskValue;
    private int mediumRiskValue;
    private int highRiskValue;
    private int veryHighRiskValue;

    private int lowRiskCount;
    private int negligibleRiskCount;
    private int mediumRiskCount;
    private int highRiskCount;
    private int veryHighRiskCount;

    private String negligibleRiskLabel = "";
    private String lowRiskLabel = "";
    private String mediumRiskLabel = "";
    private String highRiskLabel = "";
    private String veryHighRiskLabel = "";

    public RiskDistributionStats() {
    }

    public RiskDistributionStats(String key) {
        this.key = key;
    }

    public RiskDistributionStats(int lowRiskThreshold, int mediumRiskThreshold, int highRiskThreshold, int veryHighRiskThreshold) {
        this.lowRiskThreshold = lowRiskThreshold;
        this.mediumRiskThreshold = mediumRiskThreshold;
        this.highRiskThreshold = highRiskThreshold;
        this.veryHighRiskThreshold = veryHighRiskThreshold;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void reset() {
        negligibleRiskValue = 0;
        negligibleRiskCount = 0;
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
        if (testValue <= lowRiskThreshold) {
            negligibleRiskValue += addValue;
            negligibleRiskCount++;
        } else if (testValue <= mediumRiskThreshold) {
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
        return negligibleRiskValue + lowRiskValue + mediumRiskValue + highRiskValue + veryHighRiskValue;
    }

    public int getTotalCount() {
        return negligibleRiskCount + lowRiskCount + mediumRiskCount + highRiskCount + veryHighRiskCount;
    }

    public int getNegligibleRiskValue() {
        return negligibleRiskValue;
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

    public int getNegligibleRiskCount() {
        return negligibleRiskCount;
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

    public String getNegligibleRiskLabel() {
        return negligibleRiskLabel;
    }

    public void setNegligibleRiskLabel(String negligibleRiskLabel) {
        this.negligibleRiskLabel = negligibleRiskLabel;
    }

    private int totalValue() {
        return veryHighRiskValue + highRiskValue + mediumRiskValue + lowRiskValue + negligibleRiskValue;
    }

    public double getVeryHighRiskPercentage() {
        return totalValue() > 0 ? 100.0 * veryHighRiskValue / totalValue() : 0;
    }

    public double getHighRiskPercentage() {
        return totalValue() > 0 ? 100.0 * highRiskValue / totalValue() : 0;
    }


    public double getMediumRiskPercentage() {
        return totalValue() > 0 ? 100.0 * mediumRiskValue / totalValue() : 0;
    }


    public double getLowRiskPercentage() {
        return totalValue() > 0 ? 100.0 * lowRiskValue / totalValue() : 0;
    }

    public double getNegligibleRiskPercentage() {
        return totalValue() > 0 ? 100.0 * negligibleRiskValue / totalValue() : 0;
    }

}

