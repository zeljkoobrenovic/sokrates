/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape.analysis;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;

import java.util.*;

public class LandscapeAnalysisResults {
    private LandscapeConfiguration configuration = new LandscapeConfiguration();
    private List<ProjectAnalysisResults> projectAnalysisResults = new ArrayList<>();

    public LandscapeConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(LandscapeConfiguration configuration) {
        this.configuration = configuration;
    }

    public List<ProjectAnalysisResults> getProjectAnalysisResults() {
        return projectAnalysisResults;
    }

    public void setProjectAnalysisResults(List<ProjectAnalysisResults> projectAnalysisResults) {
        this.projectAnalysisResults = projectAnalysisResults;
    }

    @JsonIgnore
    public List<ProjectAnalysisResults> getAllProjects() {
        return this.projectAnalysisResults;
    }

    @JsonIgnore
    public int getProjectsCount() {
        return projectAnalysisResults.size();
    }

    @JsonIgnore
    public int getMainLoc() {
        int count[] = {0};
        this.projectAnalysisResults.forEach(projectAnalysisResults -> {
            count[0] += projectAnalysisResults.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode();
        });
        return count[0];
    }

    @JsonIgnore
    public int getTestLoc() {
        int count[] = {0};
        this.projectAnalysisResults.forEach(projectAnalysisResults -> {
            count[0] += projectAnalysisResults.getAnalysisResults().getTestAspectAnalysisResults().getLinesOfCode();
        });
        return count[0];
    }

    @JsonIgnore
    public int getGeneratedLoc() {
        int count[] = {0};
        this.projectAnalysisResults.forEach(projectAnalysisResults -> {
            count[0] += projectAnalysisResults.getAnalysisResults().getGeneratedAspectAnalysisResults().getLinesOfCode();
        });
        return count[0];
    }

    @JsonIgnore
    public int getBuildAndDeploymentLoc() {
        int count[] = {0};
        this.projectAnalysisResults.forEach(projectAnalysisResults -> {
            count[0] += projectAnalysisResults.getAnalysisResults().getBuildAndDeployAspectAnalysisResults().getLinesOfCode();
        });
        return count[0];
    }

    @JsonIgnore
    public int getOtherLoc() {
        int count[] = {0};
        this.projectAnalysisResults.forEach(projectAnalysisResults -> {
            count[0] += projectAnalysisResults.getAnalysisResults().getOtherAspectAnalysisResults().getLinesOfCode();
        });
        return count[0];
    }

    @JsonIgnore
    public int getAllLoc() {
        int count[] = {0};
        this.projectAnalysisResults.forEach(projectAnalysisResults -> {
            count[0] += projectAnalysisResults.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCode();
            count[0] += projectAnalysisResults.getAnalysisResults().getTestAspectAnalysisResults().getLinesOfCode();
            count[0] += projectAnalysisResults.getAnalysisResults().getGeneratedAspectAnalysisResults().getLinesOfCode();
            count[0] += projectAnalysisResults.getAnalysisResults().getBuildAndDeployAspectAnalysisResults().getLinesOfCode();
            count[0] += projectAnalysisResults.getAnalysisResults().getOtherAspectAnalysisResults().getLinesOfCode();
        });
        return count[0];
    }

    @JsonIgnore
    public List<NumericMetric> getLinesOfCodePerExtension() {
        List<NumericMetric> linesOfCodePerExtension = new ArrayList<>();
        getProjectAnalysisResults().forEach(projectAnalysisResults -> {
            AspectAnalysisResults main = projectAnalysisResults.getAnalysisResults().getMainAspectAnalysisResults();
            List<NumericMetric> projectLinesOfCodePerExtension = main.getLinesOfCodePerExtension();
            projectLinesOfCodePerExtension.forEach(metric -> {
                String id = metric.getName();
                Optional<NumericMetric> existingMetric = linesOfCodePerExtension.stream().filter(c -> c.getName().equalsIgnoreCase(id)).findAny();
                if (existingMetric.isPresent()) {
                    existingMetric.get().setValue(existingMetric.get().getValue().intValue() + metric.getValue().intValue());
                } else {
                    linesOfCodePerExtension.add(new NumericMetric(metric.getName(), metric.getValue()));
                }
            });
        });

        Collections.sort(linesOfCodePerExtension, (a, b) -> b.getValue().intValue() - a.getValue().intValue());
        return linesOfCodePerExtension;
    }

    @JsonIgnore

    public List<ContributorProject> getContributors() {
        List<ContributorProject> list = new ArrayList<>();
        Map<String, ContributorProject> map = new HashMap<>();

        getProjectAnalysisResults().forEach(projectAnalysisResults -> {
            ContributorsAnalysisResults contributorsAnalysisResults = projectAnalysisResults.getAnalysisResults().getContributorsAnalysisResults();
            contributorsAnalysisResults.getContributors().forEach(contributor -> {
                String name = contributor.getName();
                int projectCommits = contributor.getCommitsCount();
                int commits = projectCommits;

                if (map.containsKey(name)) {
                    ContributorProject existingContributor = map.get(name);
                    existingContributor.getContributor().setCommitsCount(existingContributor.getContributor().getCommitsCount() + projectCommits);
                    existingContributor.addProject(projectAnalysisResults, projectCommits);
                } else {
                    Contributor newContributor = new Contributor(contributor.getName(), projectCommits);
                    ContributorProject newContributorWithProjects = new ContributorProject(contributor);
                    newContributorWithProjects.addProject(projectAnalysisResults, projectCommits);
                    map.put(name, newContributorWithProjects);
                    list.add(newContributorWithProjects);
                }
            });
        });

        Collections.sort(list, (a, b) -> b.getContributor().getCommitsCount() - a.getContributor().getCommitsCount());

        return list;
    }

}
