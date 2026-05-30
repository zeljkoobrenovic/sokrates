package nl.obren.sokrates.reports.landscape.data;

import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight, JSON-serializable value objects carrying the per-repository data that the
 * client-rendered landscape repositories report ({@code repositories-report.html}) needs to
 * draw its tables and mini-charts. Kept compact on purpose: the whole landscape is embedded
 * as JSON in a single HTML page, so these mirror only the numbers the page renders.
 */
public class RepositoryReportData {

    /** A 5-band risk distribution (negligible..veryHigh) reduced to plain counts. */
    public static class RiskBands {
        private int negligible;
        private int low;
        private int medium;
        private int high;
        private int veryHigh;

        public RiskBands() {
        }

        public RiskBands(RiskDistributionStats stats) {
            if (stats != null) {
                this.negligible = stats.getNegligibleRiskValue();
                this.low = stats.getLowRiskValue();
                this.medium = stats.getMediumRiskValue();
                this.high = stats.getHighRiskValue();
                this.veryHigh = stats.getVeryHighRiskValue();
            }
        }

        public int getNegligible() {
            return negligible;
        }

        public int getLow() {
            return low;
        }

        public int getMedium() {
            return medium;
        }

        public int getHigh() {
            return high;
        }

        public int getVeryHigh() {
            return veryHigh;
        }
    }

    /** Quality metrics shown in the "Metrics" tab. */
    public static class Metrics {
        private boolean skipDuplication;
        private double duplicationPercentage;
        private RiskBands fileSize;
        private RiskBands unitSize;
        private RiskBands conditionalComplexity;
        private RiskBands newness;
        private RiskBands freshness;
        private RiskBands updateFrequency;
        private List<Control> controls = new ArrayList<>();

        public boolean isSkipDuplication() {
            return skipDuplication;
        }

        public void setSkipDuplication(boolean skipDuplication) {
            this.skipDuplication = skipDuplication;
        }

        public double getDuplicationPercentage() {
            return duplicationPercentage;
        }

        public void setDuplicationPercentage(double duplicationPercentage) {
            this.duplicationPercentage = duplicationPercentage;
        }

        public RiskBands getFileSize() {
            return fileSize;
        }

        public void setFileSize(RiskBands fileSize) {
            this.fileSize = fileSize;
        }

        public RiskBands getUnitSize() {
            return unitSize;
        }

        public void setUnitSize(RiskBands unitSize) {
            this.unitSize = unitSize;
        }

        public RiskBands getConditionalComplexity() {
            return conditionalComplexity;
        }

        public void setConditionalComplexity(RiskBands conditionalComplexity) {
            this.conditionalComplexity = conditionalComplexity;
        }

        public RiskBands getNewness() {
            return newness;
        }

        public void setNewness(RiskBands newness) {
            this.newness = newness;
        }

        public RiskBands getFreshness() {
            return freshness;
        }

        public void setFreshness(RiskBands freshness) {
            this.freshness = freshness;
        }

        public RiskBands getUpdateFrequency() {
            return updateFrequency;
        }

        public void setUpdateFrequency(RiskBands updateFrequency) {
            this.updateFrequency = updateFrequency;
        }

        public List<Control> getControls() {
            return controls;
        }

        public void setControls(List<Control> controls) {
            this.controls = controls;
        }
    }

    /** A single goal/control status (rendered as a colored dot). */
    public static class Control {
        private String status;
        private String description;
        private String value;

        public Control() {
        }

        public Control(String status, String description, String value) {
            this.status = status;
            this.description = description;
            this.value = value;
        }

        public String getStatus() {
            return status;
        }

        public String getDescription() {
            return description;
        }

        public String getValue() {
            return value;
        }
    }

    /** A repository tag (rendered as a colored badge). */
    public static class Tag {
        private String tag;
        private String color;

        public Tag() {
        }

        public Tag(String tag, String color) {
            this.tag = tag;
            this.color = color;
        }

        public String getTag() {
            return tag;
        }

        public String getColor() {
            return color;
        }
    }

    /**
     * Windowed contribution history (per-week or per-year). Slots are ordered most-recent-first.
     * {@code slots} holds the time-slot labels (for tooltips); {@code commits} and
     * {@code contributors} hold the matching counts.
     */
    public static class History {
        private List<String> slots = new ArrayList<>();
        private List<Integer> commits = new ArrayList<>();
        private List<Integer> contributors = new ArrayList<>();

        public List<String> getSlots() {
            return slots;
        }

        public List<Integer> getCommits() {
            return commits;
        }

        public List<Integer> getContributors() {
            return contributors;
        }

        public void add(String slot, int commitsCount, int contributorsCount) {
            slots.add(slot);
            commits.add(commitsCount);
            contributors.add(contributorsCount);
        }
    }
}
