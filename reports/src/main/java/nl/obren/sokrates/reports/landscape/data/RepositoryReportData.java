package nl.obren.sokrates.reports.landscape.data;

import nl.obren.sokrates.sourcecode.analysis.scores.MaintainabilityScore;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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
        // The Human / AI maintainability scores (null for analyses without them) and the AI lines read per change.
        private Score humanScore;
        private Score aiScore;
        private int contextLinesPerChange;

        public Score getHumanScore() {
            return humanScore;
        }

        public void setHumanScore(Score humanScore) {
            this.humanScore = humanScore;
        }

        public Score getAiScore() {
            return aiScore;
        }

        public void setAiScore(Score aiScore) {
            this.aiScore = aiScore;
        }

        public int getContextLinesPerChange() {
            return contextLinesPerChange;
        }

        public void setContextLinesPerChange(int contextLinesPerChange) {
            this.contextLinesPerChange = contextLinesPerChange;
        }

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
        // Total line churn (added + deleted) per slot, plus the added/deleted split, matching the
        // commits/contributors arrays. churn is kept for the combined per-week sparkline; the split
        // backs the per-year +added/-deleted stacked chart.
        private List<Integer> churn = new ArrayList<>();
        private List<Integer> churnAdded = new ArrayList<>();
        private List<Integer> churnDeleted = new ArrayList<>();

        public List<String> getSlots() {
            return slots;
        }

        public List<Integer> getCommits() {
            return commits;
        }

        public List<Integer> getContributors() {
            return contributors;
        }

        public List<Integer> getChurn() {
            return churn;
        }

        public List<Integer> getChurnAdded() {
            return churnAdded;
        }

        public List<Integer> getChurnDeleted() {
            return churnDeleted;
        }

        public void add(String slot, int commitsCount, int contributorsCount, int linesAdded, int linesDeleted) {
            slots.add(slot);
            commits.add(commitsCount);
            contributors.add(contributorsCount);
            churn.add(linesAdded + linesDeleted);
            churnAdded.add(linesAdded);
            churnDeleted.add(linesDeleted);
        }
    }

    /** A maintainability score for the repositories list: value, grade and the sub-scores that drag it down most. */
    public static class Score {
        private double value;
        private String grade = "";
        private String drags = "";
        // "custom" when the repository scores with its own framework (other grades and sub-scores), else ""
        private String framework = "";
        // "8/10" when some sub-scores could not be measured, else ""; coverageText always (when known)
        private String coverage = "";
        private String coverageText = "";

        public Score() {
        }

        public Score(MaintainabilityScore score, boolean customFramework) {
            this(score);
            this.framework = customFramework ? "custom" : "";
        }

        public Score(MaintainabilityScore score) {
            this.coverage = score.isFullyMeasured() ? "" : score.getCoverageShort();
            this.coverageText = score.getCoverageText();
            this.value = score.getValue();
            this.grade = score.getGrade();
            this.drags = score.getSubScores().stream()
                    .filter(s -> s.getDrag() > 0)
                    .sorted((a, b) -> Double.compare(b.getDrag(), a.getDrag()))
                    .limit(3)
                    .map(s -> s.getLabel() + " " + String.format(Locale.US, "-%.1f", s.getDrag()))
                    .collect(Collectors.joining(", "));
            if (!score.getCappedBy().isEmpty()) {
                this.drags += (this.drags.isEmpty() ? "" : "; ") + "capped by " + score.getCappedBy();
            }
        }

        public double getValue() {
            return value;
        }

        public String getCoverage() {
            return coverage;
        }

        public void setCoverage(String coverage) {
            this.coverage = coverage != null ? coverage : "";
        }

        public String getCoverageText() {
            return coverageText;
        }

        public void setCoverageText(String coverageText) {
            this.coverageText = coverageText != null ? coverageText : "";
        }

        public String getFramework() {
            return framework;
        }

        public void setFramework(String framework) {
            this.framework = framework != null ? framework : "";
        }

        public void setValue(double value) {
            this.value = value;
        }

        public String getGrade() {
            return grade;
        }

        public void setGrade(String grade) {
            this.grade = grade;
        }

        public String getDrags() {
            return drags;
        }

        public void setDrags(String drags) {
            this.drags = drags;
        }
    }
}
