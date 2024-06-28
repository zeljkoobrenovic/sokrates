package nl.obren.sokrates.reports.landscape.data;

import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.SokratesRepositoryLink;
import nl.obren.sokrates.sourcecode.landscape.analysis.RepositoryAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.MetricsList;

public class RepositoryExport {
    private final Metadata metadata;
    private final String latestCommitDate;
    private final int commitsCount30Days;
    private final int commitsCount;
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

    public RepositoryExport(RepositoryAnalysisResults repository) {
        CodeAnalysisResults analysis = repository.getAnalysisResults();

        metadata = analysis.getMetadata();

        latestCommitDate = repository.getAnalysisResults().getContributorsAnalysisResults().getLatestCommitDate();
        commitsCount = repository.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount();
        commitsCount30Days = repository.getAnalysisResults().getContributorsAnalysisResults().getCommitsCount30Days();

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

    public int getCommitsCount() {
        return commitsCount;
    }

}
