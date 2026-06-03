package nl.obren.sokrates.sourcecode.landscape.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.contributors.Contributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ContributorRepositories {
    private Contributor contributor;
    private List<ContributorRepositoryInfo> repositories = new ArrayList<>();
    // Index of repositories by lower-cased repository name, so addRepository() does not scan the
    // whole list on every merge (building a contributor active in N repos was O(N^2)).
    @JsonIgnore
    private final Map<String, ContributorRepositoryInfo> repositoriesByName = new HashMap<>();
    @JsonIgnore
    private List<ContributorRepositories> members = new ArrayList<>();

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
            repositoryByPath.setCommits365Days(repositoryByPath.getCommits365Days() + commits365Days);
            repositoryByPath.setCommitsCount(repositoryByPath.getCommitsCount() + commitsCount);
            List<String> existingDates = repositoryByPath.getCommitDates();
            Set<String> existingDatesSet = new HashSet<>(existingDates);
            commitDates.forEach(date -> {
                if (existingDatesSet.add(date)) {
                    existingDates.add(date);
                }
            });
        } else {
            addRepository(new ContributorRepositoryInfo(repositoryAnalysisResults, firstCommitDate, latestCommitDate,
                    commitsCount, commits30Days, commits90Days, commits180Days, commits365Days, commitDates));
        }
    }

    public void addRepository(ContributorRepositoryInfo repository) {
        repositories.add(repository);
        String name = repository.getRepositoryAnalysisResults().getAnalysisResults().getMetadata().getName();
        if (name != null) {
            repositoriesByName.putIfAbsent(name.toLowerCase(), repository);
        }
    }

    @JsonIgnore
    private ContributorRepositoryInfo getRepositoryByPath(String path) {
        return path != null ? repositoriesByName.get(path.toLowerCase()) : null;
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
        repositoriesByName.clear();
        if (repositories != null) {
            repositories.forEach(repository -> {
                String name = repository.getRepositoryAnalysisResults().getAnalysisResults().getMetadata().getName();
                if (name != null) {
                    repositoriesByName.putIfAbsent(name.toLowerCase(), repository);
                }
            });
        }
    }

    @JsonIgnore
    public List<ContributorRepositories> getMembers() {
        return members;
    }

    @JsonIgnore
    public void setMembers(List<ContributorRepositories> members) {
        this.members = members;
    }
}
