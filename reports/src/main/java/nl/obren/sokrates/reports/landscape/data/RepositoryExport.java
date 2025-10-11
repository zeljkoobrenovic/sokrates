package nl.obren.sokrates.reports.landscape.data;

import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.SokratesRepositoryLink;
import nl.obren.sokrates.sourcecode.landscape.analysis.RepositoryAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.MetricsList;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;

import java.util.ArrayList;
import java.util.List;

public class RepositoryExport {
    private final Metadata metadata;
    private final String latestCommitDate;
    private final int commitsCount30Days;
    private final int commitsCount90Days;
    private final int commitsCount;

    private List<String> contributors30Days;
    private List<String> contributors90Days;
    private List<String> contributors;
    private SokratesRepositoryLink sokratesRepositoryLink;
    private MetricsList metrics;
    private int mainFilesCount;
    private int mainLinesOfCode;
    private int testFilesCount;
    private int testLinesOfCode;
    private int generatedFilesCount;
    private int generatedLinesOfCode;
    private int buildAndDeployFilesCount;
    private int buildAndDeployLinesOfCode;
    private int otherFilesCount;
    private int otherLinesOfCode;
    private List<LanguageBreakdown> languages;

    public RepositoryExport(RepositoryAnalysisResults repository) {
        this.contributors30Days = contributors30Days;
        CodeAnalysisResults analysis = repository.getAnalysisResults();

        metadata = analysis.getMetadata();

        latestCommitDate = repository.getAnalysisResults().getContributorsAnalysisResults().getLatestCommitDate();
        commitsCount = repository.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount();
        commitsCount30Days = repository.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days();
        commitsCount90Days = repository.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount90Days();

        contributors30Days = new ArrayList<>();
        contributors90Days = new ArrayList<>();
        contributors = new ArrayList<>();

        repository.getAnalysisResults().getContributorsAnalysisResults().getContributors().forEach(contributor -> {
            if (contributor.getCommitsCount30Days() > 0) {
                contributors30Days.add(contributor.getEmail());
            }
            if (contributor.getCommitsCount90Days() > 0) {
                contributors90Days.add(contributor.getEmail());
            }
            contributors.add(contributor.getEmail());
        });

        sokratesRepositoryLink = repository.getSokratesRepositoryLink();

        AspectAnalysisResults main = analysis.getMainAspectAnalysisResults();
        AspectAnalysisResults test = analysis.getTestAspectAnalysisResults();
        AspectAnalysisResults build = analysis.getBuildAndDeployAspectAnalysisResults();
        AspectAnalysisResults generated = analysis.getGeneratedAspectAnalysisResults();
        AspectAnalysisResults other = analysis.getOtherAspectAnalysisResults();

        mainFilesCount = main.getFilesCount();
        mainLinesOfCode = main.getLinesOfCode();

        testFilesCount = test.getFilesCount();
        testLinesOfCode = test.getLinesOfCode();

        generatedFilesCount = generated.getFilesCount();
        generatedLinesOfCode = generated.getLinesOfCode();

        buildAndDeployFilesCount = build.getFilesCount();
        buildAndDeployLinesOfCode = build.getLinesOfCode();

        otherFilesCount = other.getFilesCount();
        generatedLinesOfCode = other.getLinesOfCode();

        this.languages = buildLanguageBreakdown(main);
    }

    /**
     * Builds language breakdown with LOC counts and percentages
     * @param main The main aspect analysis results containing language data
     * @return List of LanguageBreakdown objects sorted by LOC descending
     */
    private List<LanguageBreakdown> buildLanguageBreakdown(AspectAnalysisResults main) {
        List<LanguageBreakdown> result = new ArrayList<>();
        List<NumericMetric> linesOfCodePerExtension = main.getLinesOfCodePerExtension();
        int totalLoc = main.getLinesOfCode();

        if (totalLoc > 0) {
            for (NumericMetric metric : linesOfCodePerExtension) {
                // Clean up the extension name (remove "*." prefix and convert to proper language name)
                String extension = metric.getName().replace("*.", "").trim();
                String languageName = formatLanguageName(extension);

                int loc = metric.getValue().intValue();
                double percentage = (loc * 100.0) / totalLoc;

                // Round percentage to 2 decimal places
                percentage = Math.round(percentage * 100.0) / 100.0;

                result.add(new LanguageBreakdown(languageName, loc, percentage));
            }
        }

        return result;
    }

    /**
     * Formats extension names into proper language names
     * @param extension The file extension
     * @return Formatted language name
     */
    private String formatLanguageName(String extension) {
        // Handle common extensions and convert to standard language names
        switch (extension.toLowerCase()) {
            case "java":
                return "Java";
            case "js":
                return "JavaScript";
            case "ts":
                return "TypeScript";
            case "py":
                return "Python";
            case "rb":
                return "Ruby";
            case "go":
                return "Go";
            case "cs":
                return "C#";
            case "cpp":
            case "cc":
            case "cxx":
                return "C++";
            case "c":
                return "C";
            case "php":
                return "PHP";
            case "swift":
                return "Swift";
            case "kt":
            case "kts":
                return "Kotlin";
            case "rs":
                return "Rust";
            case "scala":
                return "Scala";
            case "cfm":
            case "cfc":
                return "ColdFusion";
            case "sql":
                return "SQL";
            case "html":
                return "HTML";
            case "css":
                return "CSS";
            case "scss":
                return "SCSS";
            case "less":
                return "Less";
            case "xml":
                return "XML";
            case "json":
                return "JSON";
            case "yaml":
            case "yml":
                return "YAML";
            case "sh":
            case "bash":
                return "Shell";
            case "ps1":
                return "PowerShell";
            case "rdl":
                return "SQL Reporting";
            case "rsd":
                return "SQL Reporting";
            case "tmdl":
                return "DAX/Power BI";
            case "dtsx":
                return "SSIS/ETL";
            default:
                // Capitalize first letter for unknown extensions
                return extension.isEmpty() ? "Unknown" :
                        Character.toUpperCase(extension.charAt(0)) + extension.substring(1).toLowerCase();
        }
    }

    public List<LanguageBreakdown> getLanguages() {
        return languages;
    }

    public SokratesRepositoryLink getSokratesRepositoryLink() {
        return sokratesRepositoryLink;
    }

    public MetricsList getMetrics() {
        return metrics;
    }

    public int getMainFilesCount() {
        return mainFilesCount;
    }

    public int getMainLinesOfCode() {
        return mainLinesOfCode;
    }

    public int getTestFilesCount() {
        return testFilesCount;
    }

    public int getTestLinesOfCode() {
        return testLinesOfCode;
    }

    public int getGeneratedFilesCount() {
        return generatedFilesCount;
    }

    public int getGeneratedLinesOfCode() {
        return generatedLinesOfCode;
    }

    public int getBuildAndDeployFilesCount() {
        return buildAndDeployFilesCount;
    }

    public int getBuildAndDeployLinesOfCode() {
        return buildAndDeployLinesOfCode;
    }

    public int getOtherFilesCount() {
        return otherFilesCount;
    }

    public int getOtherLinesOfCode() {
        return otherLinesOfCode;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public String getLatestCommitDate() {
        return latestCommitDate;
    }

    public int getCommitsCount30Days() {
        return commitsCount30Days;
    }

    public int getCommitsCount90Days() {
        return commitsCount90Days;
    }

    public List<String> getContributors30Days() {
        return contributors30Days;
    }

    public List<String> getContributors90Days() {
        return contributors90Days;
    }

    public List<String> getContributors() {
        return contributors;
    }

    public int getCommitsCount() {
        return commitsCount;
    }

}
