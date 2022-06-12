package nl.obren.sokrates.reports.landscape.statichtml.projects;

import nl.obren.sokrates.reports.landscape.utils.LandscapeGeneratorUtils;
import nl.obren.sokrates.reports.landscape.utils.TagStats;
import nl.obren.sokrates.sourcecode.landscape.ProjectTag;
import nl.obren.sokrates.sourcecode.landscape.ProjectTagGroup;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.ProjectAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

public class TagMap {
    private LandscapeAnalysisResults landscapeAnalysisResults;
    private List<ProjectTagGroup> projectTagGroups;
    private Map<String, TagStats> tagStatsMap = new HashMap<>();
    private Map<String, List<ProjectTag>> projectTags = new HashMap<>();

    public TagMap(LandscapeAnalysisResults landscapeAnalysisResults, List<ProjectTagGroup> projectTagGroups) {
        this.landscapeAnalysisResults = landscapeAnalysisResults;
        this.projectTagGroups = projectTagGroups;
    }

    public void updateTagMap(List<ProjectAnalysisResults> projects) {
        projects.forEach(project -> {
            List<NumericMetric> linesOfCodePerExtension = LandscapeGeneratorUtils.getLinesOfCodePerExtension(landscapeAnalysisResults, project.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCodePerExtension());
            linesOfCodePerExtension.sort((a, b) -> b.getValue().intValue() - a.getValue().intValue());
            String mainTech = linesOfCodePerExtension.size() > 0 ? linesOfCodePerExtension.get(0).getName() : "";
            List<ProjectTag> tags = new ArrayList<>();
            projectTagGroups.forEach(tagGroup -> tags.addAll(tagGroup.getProjectTags()));

            boolean tagged[] = {false};

            tags.forEach(tag -> {
                if (isTagged(project, mainTech, tag, linesOfCodePerExtension)) {
                    addProjectTag(project, tag);
                    updateTagStats(project, tag);
                    tagged[0] = true;
                }
            });

            if (!tagged[0]) {
                if (!tagStatsMap.containsKey("")) {
                    tagStatsMap.put("", new TagStats(new ProjectTag()));
                }
                tagStatsMap.get("").getProjectsAnalysisResults().add(project);
            }

        });
        System.out.println("");
    }

    private void updateTagStats(ProjectAnalysisResults project, ProjectTag tag) {
        String key = tag.getKey();
        if (!tagStatsMap.containsKey(key)) {
            tagStatsMap.put(key, new TagStats(tag));
        }
        tagStatsMap.get(key).getProjectsAnalysisResults().add(project);
    }

    private void addProjectTag(ProjectAnalysisResults project, ProjectTag tag) {
        String projectKey = getProjectKey(project);
        if (!projectTags.containsKey(projectKey)) {
            projectTags.put(projectKey, new ArrayList<>());
        }
        projectTags.get(projectKey).add(tag);
    }

    private String getProjectKey(ProjectAnalysisResults project) {
        return project.getSokratesProjectLink().getAnalysisResultsPath();
    }

    private boolean isTagged(ProjectAnalysisResults project, String mainTech, ProjectTag tag, List<NumericMetric> linesOfCodePerExtension) {
        String name = project.getAnalysisResults().getMetadata().getName();
        if (tag.excludesMainTechnology(mainTech) || tag.excludeName(name)) {
            return false;
        }

        boolean matchesName = tag.matchesName(name);
        boolean matchesMainTechnology = tag.matchesMainTechnology(mainTech);
        boolean matchesAnyTechnology = tag.matchesAnyTechnology(linesOfCodePerExtension);
        boolean matchesPath = tag.matchesPath(project.getFiles());

        return matchesName || matchesMainTechnology || matchesAnyTechnology || matchesPath;
    }

    public TagStats getTagStats(String key) {
        return tagStatsMap.get(key);
    }

    public boolean containsKey(String key) {
        return tagStatsMap.containsKey(key);
    }

    public Set<String> keySet() {
        return tagStatsMap.keySet();
    }

    public List<ProjectTag> getProjectTags(ProjectAnalysisResults project) {
        String projectKey = getProjectKey(project);
        return projectTags.containsKey(projectKey) ? projectTags.get(projectKey) : new ArrayList<>();
    }

    public int tagsCount() {
        return tagStatsMap.containsKey("") ? tagStatsMap.size() - 1 : tagStatsMap.size();
    }
}
