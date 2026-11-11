/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.stats;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.sourcecode.threshold.Thresholds;

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

    private String valueUnit = "LOC";
    private String countUnit = "files";

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

    public RiskDistributionStats(Thresholds thresholds) {
        this(thresholds.getLow(), thresholds.getMedium(), thresholds.getHigh(), thresholds.getVeryHigh());
        setNegligibleRiskLabel(thresholds.getNegligibleRiskLabel());
        setLowRiskLabel(thresholds.getLowRiskLabel());
        setMediumRiskLabel(thresholds.getMediumRiskLabel());
        setHighRiskLabel(thresholds.getHighRiskLabel());
        setVeryHighRiskLabel(thresholds.getVeryHighRiskLabel());
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

    public void setNegligibleRiskValue(int negligibleRiskValue) {
        this.negligibleRiskValue = negligibleRiskValue;
    }

    public int getLowRiskValue() {
        return lowRiskValue;
    }

    public void setLowRiskValue(int lowRiskValue) {
        this.lowRiskValue = lowRiskValue;
    }

    public int getMediumRiskValue() {
        return mediumRiskValue;
    }

    public void setMediumRiskValue(int mediumRiskValue) {
        this.mediumRiskValue = mediumRiskValue;
    }

    public int getHighRiskValue() {
        return highRiskValue;
    }

    public void setHighRiskValue(int highRiskValue) {
        this.highRiskValue = highRiskValue;
    }

    public int getVeryHighRiskValue() {
        return veryHighRiskValue;
    }

    public void setVeryHighRiskValue(int veryHighRiskValue) {
        this.veryHighRiskValue = veryHighRiskValue;
    }

    public int getNegligibleRiskCount() {
        return negligibleRiskCount;
    }

    public void setNegligibleRiskCount(int negligibleRiskCount) {
        this.negligibleRiskCount = negligibleRiskCount;
    }

    public int getLowRiskCount() {
        return lowRiskCount;
    }

    public void setLowRiskCount(int lowRiskCount) {
        this.lowRiskCount = lowRiskCount;
    }

    public int getLowRiskThreshold() {
        return lowRiskThreshold;
    }

    public void setLowRiskThreshold(int lowRiskThreshold) {
        this.lowRiskThreshold = lowRiskThreshold;
    }

    public int getMediumRiskCount() {
        return mediumRiskCount;
    }

    public void setMediumRiskCount(int mediumRiskCount) {
        this.mediumRiskCount = mediumRiskCount;
    }

    public int getHighRiskCount() {
        return highRiskCount;
    }

    public void setHighRiskCount(int highRiskCount) {
        this.highRiskCount = highRiskCount;
    }

    public int getVeryHighRiskCount() {
        return veryHighRiskCount;
    }

    public void setVeryHighRiskCount(int veryHighRiskCount) {
        this.veryHighRiskCount = veryHighRiskCount;
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

    @JsonIgnore
    private String formatLine(double perc, String label, int value, int count) {
        return FormattingUtils.getFormattedPercentage(perc) + "% " + label + ": " + FormattingUtils.formatCount(value) + " " + valueUnit + " in " + FormattingUtils.formatCount(count) + " " + countUnit + ";\n";
    }

    public String getValueUnit() {
        return valueUnit;
    }

    public void setValueUnit(String valueUnit) {
        this.valueUnit = valueUnit;
    }

    public String getCountUnit() {
        return countUnit;
    }

    public void setCountUnit(String countUnit) {
        this.countUnit = countUnit;
    }

    @JsonIgnore
    public String getDescription() {
        StringBuilder text = new StringBuilder();

        text.append(formatLine(getVeryHighRiskPercentage(), veryHighRiskLabel, veryHighRiskValue, veryHighRiskCount));
        text.append(formatLine(getHighRiskPercentage(), highRiskLabel, highRiskValue, highRiskCount));
        text.append(formatLine(getMediumRiskPercentage(), mediumRiskLabel, mediumRiskValue, mediumRiskCount));
        text.append(formatLine(getLowRiskPercentage(), lowRiskLabel, lowRiskValue, lowRiskCount));
        text.append(formatLine(getNegligibleRiskPercentage(), negligibleRiskLabel, negligibleRiskValue, negligibleRiskCount));

        return text.toString();
    }

}

