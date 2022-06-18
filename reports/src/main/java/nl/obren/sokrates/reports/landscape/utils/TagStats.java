package nl.obren.sokrates.reports.landscape.utils;

import nl.obren.sokrates.sourcecode.landscape.RepositoryTag;
import nl.obren.sokrates.sourcecode.landscape.analysis.RepositoryAnalysisResults;

import java.util.ArrayList;
import java.util.List;

public class TagStats {
    private RepositoryTag tag;
    private List<RepositoryAnalysisResults> repositoryAnalysisResults = new ArrayList<>();

    public TagStats(RepositoryTag tag) {
        this.tag = tag;
    }

    public RepositoryTag getTag() {
        return tag;
    }

    public void setTag(RepositoryTag tag) {
        this.tag = tag;
    }

    public List<RepositoryAnalysisResults> getRepositoryAnalysisResults() {
        return repositoryAnalysisResults;
    }

    public void setRepositoryAnalysisResults(List<RepositoryAnalysisResults> repositoryAnalysisResults) {
        this.repositoryAnalysisResults = repositoryAnalysisResults;
    }
}
