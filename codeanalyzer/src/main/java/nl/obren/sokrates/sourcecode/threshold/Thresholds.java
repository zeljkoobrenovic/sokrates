package nl.obren.sokrates.sourcecode.threshold;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;

import java.util.ArrayList;
import java.util.List;

public class Thresholds {
    private int low;
    private int medium;
    private int high;
    private int veryHigh;

    public Thresholds() {
    }

    public Thresholds(int low, int medium, int high, int veryHigh) {
        this.low = low;
        this.medium = medium;
        this.high = high;
        this.veryHigh = veryHigh;
    }

    @JsonIgnore
    public static Thresholds defaultFileSizeThresholds() {
        return new Thresholds(100, 200, 500, 1000);
    }

    @JsonIgnore
    public static Thresholds defaultFileAgeThresholds() {
        return new Thresholds(30, 90, 180, 365);
    }

    @JsonIgnore
    public static Thresholds defaultFileUpdateFrequencyThresholds() {
        return new Thresholds(5, 20, 50, 100);
    }

    @JsonIgnore
    public static Thresholds defaultFileContributorsCountThresholds() {
        return new Thresholds(1, 5, 10, 25);
    }

    @JsonIgnore
    public static Thresholds defaultUnitSizeThresholds() {
        return new Thresholds(10, 20, 50, 100);
    }

    @JsonIgnore
    public static Thresholds defaultConditionalComplexityThresholds() {
        return new Thresholds(5, 10, 25, 50);
    }
    @JsonIgnore
    public static Thresholds defaultFileConditionalComplexityThresholds() {
        return new Thresholds(50, 100, 250, 500);
    }

    public int getLow() {
        return Math.max(1, low);
    }

    public void setLow(int low) {
        this.low = low;
    }

    public int getMedium() {
        return Math.max(getLow() + 1, medium);
    }

    public void setMedium(int medium) {
        this.medium = medium;
    }

    public int getHigh() {
        return Math.max(getMedium() + 1, high);
    }

    public void setHigh(int high) {
        this.high = high;
    }

    public int getVeryHigh() {
        return Math.max(getHigh() + 1, veryHigh);
    }

    public void setVeryHigh(int veryHigh) {
        this.veryHigh = veryHigh;
    }

    @JsonIgnore
    public List<String> getLabels() {
        List<String> labels = new ArrayList<>();

        labels.add(getVeryHighRiskLabel());
        labels.add(getHighRiskLabel());
        labels.add(getMediumRiskLabel());
        labels.add(getLowRiskLabel());
        labels.add(getNegligibleRiskLabel());

        return labels;
    }

    @JsonIgnore
    public String getVeryHighRiskLabel() {
        return (getVeryHigh() + 1) + "+";
    }

    @JsonIgnore
    public String getHighRiskLabel() {
        return (getHigh() + 1) + "-" + getVeryHigh();
    }

    @JsonIgnore
    public String getMediumRiskLabel() {
        return (getMedium() + 1) + "-" + getHigh();
    }

    @JsonIgnore
    public String getLowRiskLabel() {
        return (getLow() + 1) + "-" + getMedium();
    }

    @JsonIgnore
    public String getNegligibleRiskLabel() {
        return getLow() > 1 ? "1-" + getLow() : "1";
    }

    @JsonIgnore
    public RiskDistributionStats toRiskDistribution() {
        RiskDistributionStats distributionStats = new RiskDistributionStats(getLow(), getMedium(), getHigh(), getVeryHigh());
        distributionStats.setNegligibleRiskLabel(getNegligibleRiskLabel());
        distributionStats.setLowRiskLabel(getLowRiskLabel());
        distributionStats.setMediumRiskLabel(getMediumRiskLabel());
        distributionStats.setHighRiskLabel(getHighRiskLabel());
        distributionStats.setVeryHighRiskLabel(getVeryHighRiskLabel());

        return distributionStats;
    }
}
