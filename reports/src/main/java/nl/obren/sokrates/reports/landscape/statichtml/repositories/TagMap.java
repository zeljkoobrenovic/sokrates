package nl.obren.sokrates.reports.landscape.statichtml.repositories;

import nl.obren.sokrates.common.utils.FormattingUtils;
import nl.obren.sokrates.reports.landscape.utils.LandscapeGeneratorUtils;
import nl.obren.sokrates.reports.landscape.utils.TagStats;
import nl.obren.sokrates.sourcecode.landscape.RepositoryTag;
import nl.obren.sokrates.sourcecode.landscape.TagGroup;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.RepositoryAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

public class TagMap {
    private LandscapeAnalysisResults landscapeAnalysisResults;
    private List<TagGroup> tagGroups;
    private Map<String, TagStats> tagStatsMap = new HashMap<>();
    private Map<String, List<RepositoryTag>> repositoryTags = new HashMap<>();
    private static final Log LOG = LogFactory.getLog(TagMap.class);

    public TagMap(LandscapeAnalysisResults landscapeAnalysisResults, List<TagGroup> tagGroups) {
        this.landscapeAnalysisResults = landscapeAnalysisResults;
        this.tagGroups = tagGroups;
    }

    public void updateTagMap(List<RepositoryAnalysisResults> repositories) {
        int filesCount = repositories.stream().mapToInt(r -> r.getFiles().size()).sum();

        LOG.info("Searching for tag patterns in " + FormattingUtils.formatCount(filesCount) + " files...");

        repositories.forEach(repository -> {
            List<NumericMetric> linesOfCodePerExtension = LandscapeGeneratorUtils.getLinesOfCodePerExtension(landscapeAnalysisResults, repository.getAnalysisResults().getMainAspectAnalysisResults().getLinesOfCodePerExtension());
            linesOfCodePerExtension.sort((a, b) -> b.getValue().intValue() - a.getValue().intValue());
            String mainTech = linesOfCodePerExtension.size() > 0 ? linesOfCodePerExtension.get(0).getName() : "";
            List<RepositoryTag> tags = new ArrayList<>();
            tagGroups.forEach(tagGroup -> tags.addAll(tagGroup.getRepositoryTags()));

            boolean tagged[] = {false};

            tags.forEach(tag -> {
                if (isTagged(repository, mainTech, tag, linesOfCodePerExtension)) {
                    addRepositoryTag(repository, tag);
                    updateTagStats(repository, tag);
                    tagged[0] = true;
                }
            });

            if (!tagged[0]) {
                if (!tagStatsMap.containsKey("")) {
                    tagStatsMap.put("", new TagStats(new RepositoryTag()));
                }
                tagStatsMap.get("").getRepositoryAnalysisResults().add(repository);
            }

        });
        System.out.println("");
    }

    private void updateTagStats(RepositoryAnalysisResults repository, RepositoryTag tag) {
        String key = tag.getKey();
        if (!tagStatsMap.containsKey(key)) {
            tagStatsMap.put(key, new TagStats(tag));
        }
        tagStatsMap.get(key).getRepositoryAnalysisResults().add(repository);
    }

    private void addRepositoryTag(RepositoryAnalysisResults repository, RepositoryTag tag) {
        String repositoryKey = getRepositoryKey(repository);
        if (!repositoryTags.containsKey(repositoryKey)) {
            repositoryTags.put(repositoryKey, new ArrayList<>());
        }
        repositoryTags.get(repositoryKey).add(tag);
    }

    private String getRepositoryKey(RepositoryAnalysisResults repository) {
        return repository.getSokratesRepositoryLink().getAnalysisResultsPath();
    }

    private boolean isTagged(RepositoryAnalysisResults repository, String mainTech, RepositoryTag tag, List<NumericMetric> linesOfCodePerExtension) {
        String name = repository.getAnalysisResults().getMetadata().getName();
        if (tag.excludesMainTechnology(mainTech) || tag.excludeName(name)) {
            return false;
        }

        boolean matchesName = tag.matchesName(name);
        boolean matchesMainTechnology = tag.matchesMainTechnology(mainTech);
        boolean matchesAnyTechnology = tag.matchesAnyTechnology(linesOfCodePerExtension);
        boolean matchesPath = tag.matchesPath(repository.getFiles());

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

    public List<RepositoryTag> getRepositoryTags(RepositoryAnalysisResults repository) {
        String repositoryKey = getRepositoryKey(repository);
        return repositoryTags.containsKey(repositoryKey) ? repositoryTags.get(repositoryKey) : new ArrayList<>();
    }

    public int tagsCount() {
        return tagStatsMap.containsKey("") ? tagStatsMap.size() - 1 : tagStatsMap.size();
    }
}
