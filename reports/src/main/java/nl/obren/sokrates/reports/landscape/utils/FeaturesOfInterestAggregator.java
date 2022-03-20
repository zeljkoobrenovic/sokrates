package nl.obren.sokrates.reports.landscape.utils;

import nl.obren.sokrates.sourcecode.analysis.results.ConcernsAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.ProjectAnalysisResults;

import java.util.*;

public class FeaturesOfInterestAggregator {
    private List<ProjectAnalysisResults> projectAnalysisResults;
    private Map<String, List<ProjectConcernData>> concernsMap = new HashMap<>();
    private Map<String, List<ProjectConcernData>> projectsMap = new HashMap<>();
    private Map<String, ProjectConcernData> projectsConcernMap = new HashMap<>();
    private List<List<ProjectConcernData>> projects = new ArrayList<>();
    private List<List<ProjectConcernData>> concerns = new ArrayList<>();

    public FeaturesOfInterestAggregator(List<ProjectAnalysisResults> projectAnalysisResults) {
        this.projectAnalysisResults = projectAnalysisResults;
    }

    public List<ProjectAnalysisResults> getProjectAnalysisResults() {
        return projectAnalysisResults;
    }

    public void setProjectAnalysisResults(List<ProjectAnalysisResults> projectAnalysisResults) {
        this.projectAnalysisResults = projectAnalysisResults;
    }

    public void aggregateFeaturesOfInterest(int limit) {
        projectAnalysisResults.stream()
                .filter(p -> p.getAnalysisResults().getConcernsAnalysisResults().size() > 0)
                .forEach(project -> {
                    List<ConcernsAnalysisResults> concernsAnalysisResults = project.getAnalysisResults().getConcernsAnalysisResults();
                    concernsAnalysisResults.forEach(concernResults -> {
                        String projectName = project.getAnalysisResults().getMetadata().getName();
                        concernResults.getConcerns().stream()
                                .filter(c -> c.getFilesCount() > 0)
                                .filter(c -> !c.getName().equalsIgnoreCase("Unclassified"))
                                .forEach(concern -> {
                                    String concernName = concern.getName();
                                    if (!concernsMap.containsKey(concernName)) {
                                        concernsMap.put(concernName, new ArrayList<>());
                                    }
                                    if (!projectsMap.containsKey(projectName)) {
                                        projectsMap.put(projectName, new ArrayList<>());
                                    }

                                    ProjectConcernData projectConcertData = new ProjectConcernData(projectName, concern, project);
                                    concernsMap.get(concernName).add(projectConcertData);
                                    projectsMap.get(projectName).add(projectConcertData);
                                    projectsConcernMap.put(projectName + "::" + concernName, projectConcertData);
                                });
                    });
                });

        concernsMap.values().forEach(projectList -> {
            Collections.sort(projectList, (a, b) -> b.getConcern().getFilesCount() - a.getConcern().getFilesCount());
        });

        projectsMap.values().forEach(projectList -> {
            Collections.sort(projectList, (a, b) -> b.getConcern().getFilesCount() - a.getConcern().getFilesCount());
        });

        projects = new ArrayList(projectsMap.values());
        Collections.sort(projects, (a, b) -> ((b.stream().mapToInt(c -> c.getConcern().getNumberOfRegexLineMatches()).sum()) -
                (a.stream().mapToInt(c -> c.getConcern().getNumberOfRegexLineMatches()).sum())));

        concerns = new ArrayList(concernsMap.values());
        Collections.sort(concerns, (a, b) -> ((b.stream().mapToInt(c -> c.getConcern().getFilesCount()).sum()) -
                (a.stream().mapToInt(c -> c.getConcern().getFilesCount()).sum())));

        if (projects.size() > limit) {
            projects = projects.subList(0, limit);
        }

    }

    public Map<String, List<ProjectConcernData>> getConcernsMap() {
        return concernsMap;
    }

    public void setConcernsMap(Map<String, List<ProjectConcernData>> concernsMap) {
        this.concernsMap = concernsMap;
    }

    public Map<String, List<ProjectConcernData>> getProjectsMap() {
        return projectsMap;
    }

    public void setProjectsMap(Map<String, List<ProjectConcernData>> projectsMap) {
        this.projectsMap = projectsMap;
    }

    public Map<String, ProjectConcernData> getProjectsConcernMap() {
        return projectsConcernMap;
    }

    public void setProjectsConcernMap(Map<String, ProjectConcernData> projectsConcernMap) {
        this.projectsConcernMap = projectsConcernMap;
    }

    public List<List<ProjectConcernData>> getProjects() {
        return projects;
    }

    public void setProjects(List<List<ProjectConcernData>> projects) {
        this.projects = projects;
    }

    public List<List<ProjectConcernData>> getConcerns() {
        return concerns;
    }

    public void setConcerns(List<List<ProjectConcernData>> concerns) {
        this.concerns = concerns;
    }
}
