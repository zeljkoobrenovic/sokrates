package nl.obren.sokrates.reports.landscape.utils;

import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.RepositoryAnalysisResults;

public class RepositoryConcernData {
    private String repositoryName;
    private AspectAnalysisResults concern;
    private RepositoryAnalysisResults repository;

    public RepositoryConcernData(String repositoryName, AspectAnalysisResults concern, RepositoryAnalysisResults repository) {
        this.repositoryName = repositoryName;
        this.concern = concern;
        this.repository = repository;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public AspectAnalysisResults getConcern() {
        return concern;
    }

    public void setConcern(AspectAnalysisResults concern) {
        this.concern = concern;
    }

    public RepositoryAnalysisResults getRepository() {
        return repository;
    }

    public void setRepository(RepositoryAnalysisResults repository) {
        this.repository = repository;
    }
}
