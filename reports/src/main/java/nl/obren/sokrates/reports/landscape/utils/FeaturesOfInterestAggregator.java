package nl.obren.sokrates.reports.landscape.utils;

import nl.obren.sokrates.sourcecode.analysis.results.ConcernsAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.RepositoryAnalysisResults;

import java.util.*;

public class FeaturesOfInterestAggregator {
    private List<RepositoryAnalysisResults> repositoryAnalysisResults;
    private Map<String, List<RepositoryConcernData>> concernsMap = new HashMap<>();
    private Map<String, List<RepositoryConcernData>> repositoriesMap = new HashMap<>();
    private Map<String, RepositoryConcernData> repositoriesConcernMap = new HashMap<>();
    private List<List<RepositoryConcernData>> repositories = new ArrayList<>();
    private List<List<RepositoryConcernData>> concerns = new ArrayList<>();

    public FeaturesOfInterestAggregator(List<RepositoryAnalysisResults> repositoryAnalysisResults) {
        this.repositoryAnalysisResults = repositoryAnalysisResults;
    }

    public List<RepositoryAnalysisResults> getRepositoryAnalysisResults() {
        return repositoryAnalysisResults;
    }

    public void setRepositoryAnalysisResults(List<RepositoryAnalysisResults> repositoryAnalysisResults) {
        this.repositoryAnalysisResults = repositoryAnalysisResults;
    }

    public void aggregateFeaturesOfInterest(int limit) {
        repositoryAnalysisResults.stream()
                .filter(p -> p.getAnalysisResults().getConcernsAnalysisResults().size() > 0)
                .forEach(repository -> {
                    List<ConcernsAnalysisResults> concernsAnalysisResults = repository.getAnalysisResults().getConcernsAnalysisResults();
                    concernsAnalysisResults.forEach(concernResults -> {
                        String repositoryName = repository.getAnalysisResults().getMetadata().getName();
                        concernResults.getConcerns().stream()
                                .filter(c -> c.getFilesCount() > 0)
                                .filter(c -> !c.getName().equalsIgnoreCase("Unclassified"))
                                .forEach(concern -> {
                                    String concernName = concern.getName();
                                    if (!concernsMap.containsKey(concernName)) {
                                        concernsMap.put(concernName, new ArrayList<>());
                                    }
                                    if (!repositoriesMap.containsKey(repositoryName)) {
                                        repositoriesMap.put(repositoryName, new ArrayList<>());
                                    }

                                    RepositoryConcernData repositoryConcertData = new RepositoryConcernData(repositoryName, concern, repository);
                                    concernsMap.get(concernName).add(repositoryConcertData);
                                    repositoriesMap.get(repositoryName).add(repositoryConcertData);
                                    repositoriesConcernMap.put(repositoryName + "::" + concernName, repositoryConcertData);
                                });
                    });
                });

        concernsMap.values().forEach(repositoryList -> {
            Collections.sort(repositoryList, (a, b) -> b.getConcern().getFilesCount() - a.getConcern().getFilesCount());
        });

        repositoriesMap.values().forEach(repositoryList -> {
            Collections.sort(repositoryList, (a, b) -> b.getConcern().getFilesCount() - a.getConcern().getFilesCount());
        });

        repositories = new ArrayList(repositoriesMap.values());
        Collections.sort(repositories, (a, b) -> ((b.stream().mapToInt(c -> c.getConcern().getNumberOfRegexLineMatches()).sum()) -
                (a.stream().mapToInt(c -> c.getConcern().getNumberOfRegexLineMatches()).sum())));

        concerns = new ArrayList(concernsMap.values());
        Collections.sort(concerns, (a, b) -> ((b.stream().mapToInt(c -> c.getConcern().getFilesCount()).sum()) -
                (a.stream().mapToInt(c -> c.getConcern().getFilesCount()).sum())));

        if (repositories.size() > limit) {
            repositories = repositories.subList(0, limit);
        }

    }

    public Map<String, List<RepositoryConcernData>> getConcernsMap() {
        return concernsMap;
    }

    public void setConcernsMap(Map<String, List<RepositoryConcernData>> concernsMap) {
        this.concernsMap = concernsMap;
    }

    public Map<String, List<RepositoryConcernData>> getRepositoriesMap() {
        return repositoriesMap;
    }

    public void setRepositoriesMap(Map<String, List<RepositoryConcernData>> repositoriesMap) {
        this.repositoriesMap = repositoriesMap;
    }

    public Map<String, RepositoryConcernData> getRepositoriesConcernMap() {
        return repositoriesConcernMap;
    }

    public void setRepositoriesConcernMap(Map<String, RepositoryConcernData> repositoriesConcernMap) {
        this.repositoriesConcernMap = repositoriesConcernMap;
    }

    public List<List<RepositoryConcernData>> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<List<RepositoryConcernData>> repositories) {
        this.repositories = repositories;
    }

    public List<List<RepositoryConcernData>> getConcerns() {
        return concerns;
    }

    public void setConcerns(List<List<RepositoryConcernData>> concerns) {
        this.concerns = concerns;
    }
}
