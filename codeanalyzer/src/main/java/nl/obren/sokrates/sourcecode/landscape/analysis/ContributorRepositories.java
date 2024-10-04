package nl.obren.sokrates.sourcecode.landscape.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.contributors.Contributor;

import java.util.ArrayList;
import java.util.List;

public class ContributorRepositories {
    private Contributor contributor;
    private List<ContributorRepositoryInfo> repositories = new ArrayList<>();

    public ContributorRepositories(Contributor contributor) {
        this.contributor = contributor;
    }

    public void addRepository(RepositoryAnalysisResults repositoryAnalysisResults, String firstCommitDate, String latestCommitDate,
                              int commitsCount, int commits30Days, int commits90Days, int commits180Days, int commits365Days, List<String> commitDates) {
        String path = repositoryAnalysisResults.getAnalysisResults().getMetadata().getName();
        ContributorRepositoryInfo repositoryByPath = getRepositoryByPath(path);
        if (repositoryByPath != null) {
            if (firstCommitDate.compareTo(repositoryByPath.getFirstCommitDate()) < 0) {
                repositoryByPath.setFirstCommitDate(firstCommitDate);
            }
            if (latestCommitDate.compareTo(repositoryByPath.getLatestCommitDate()) > 0) {
                repositoryByPath.setLatestCommitDate(latestCommitDate);
            }
            repositoryByPath.setCommits30Days(repositoryByPath.getCommits30Days() + commits30Days);
            repositoryByPath.setCommits90Days(repositoryByPath.getCommits90Days() + commits90Days);
            repositoryByPath.setCommits180Days(repositoryByPath.getCommits180Days() + commits180Days);
            repositoryByPath.setCommits365Days(repositoryByPath.getCommits180Days() + commits365Days);
            repositoryByPath.setCommitsCount(repositoryByPath.getCommitsCount() + commitsCount);
            commitDates.forEach(date -> {
                if (!repositoryByPath.getCommitDates().contains(date)) {
                    repositoryByPath.getCommitDates().add(date);
                }
            });
        } else {
            repositories.add(new ContributorRepositoryInfo(repositoryAnalysisResults, firstCommitDate, latestCommitDate,
                    commitsCount, commits30Days, commits90Days, commits180Days, commits365Days, commitDates));
        }
    }

    public void addRepository(ContributorRepositoryInfo repository) {
        repositories.add(repository);
    }

    @JsonIgnore
    private ContributorRepositoryInfo getRepositoryByPath(String path) {
        for (ContributorRepositoryInfo repository : repositories) {
            if (repository.getRepositoryAnalysisResults().getAnalysisResults().getMetadata().getName().equalsIgnoreCase(path)) {
                return repository;
            }
        }
        return null;
    }

    public Contributor getContributor() {
        return contributor;
    }

    public void setContributor(Contributor contributor) {
        this.contributor = contributor;
    }

    public List<ContributorRepositoryInfo> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<ContributorRepositoryInfo> repositories) {
        this.repositories = repositories;
    }
}
