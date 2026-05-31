package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.landscape.VirtualLandscapeConfig;
import nl.obren.sokrates.sourcecode.landscape.analysis.DependenciesCreator;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import nl.obren.sokrates.sourcecode.landscape.analysis.RepositoryAnalysisResults;

import java.util.ArrayList;
import java.util.List;

/**
 * Partitions a parent landscape's already-analysed repositories into virtual landscapes
 * (selected by repository-name include/exclude patterns) plus a "Remainder" landscape for
 * repositories matched by no virtual landscape. Each partition is turned into a full child
 * {@link LandscapeAnalysisResults} reusing the parent's analysis (no disk re-read), so it can
 * be rendered by the normal landscape report pipeline.
 */
public class VirtualLandscapeBuilder {

    /** One virtual landscape: a name/metadata plus the subset of repositories that belong to it. */
    public static class VirtualLandscape {
        private final String name;
        private final Metadata metadata;
        private final LandscapeAnalysisResults results;

        public VirtualLandscape(String name, Metadata metadata, LandscapeAnalysisResults results) {
            this.name = name;
            this.metadata = metadata;
            this.results = results;
        }

        public String getName() {
            return name;
        }

        public Metadata getMetadata() {
            return metadata;
        }

        public LandscapeAnalysisResults getResults() {
            return results;
        }
    }

    private final LandscapeAnalysisResults parentResults;

    public VirtualLandscapeBuilder(LandscapeAnalysisResults parentResults) {
        this.parentResults = parentResults;
    }

    public boolean hasVirtualLandscapes() {
        LandscapeConfiguration config = parentResults.getConfiguration();
        return config.getVirtualLandscapes() != null
                && config.getVirtualLandscapes().getLandscapes() != null
                && !config.getVirtualLandscapes().getLandscapes().isEmpty();
    }

    /**
     * Builds the virtual landscapes (in config order) followed by the Remainder landscape
     * (repositories assigned to none). Returns an empty list when no virtual landscapes are configured.
     */
    public List<VirtualLandscape> build() {
        List<VirtualLandscape> result = new ArrayList<>();
        if (!hasVirtualLandscapes()) {
            return result;
        }

        LandscapeConfiguration config = parentResults.getConfiguration();
        List<VirtualLandscapeConfig> configs = config.getVirtualLandscapes().getLandscapes();
        List<RepositoryAnalysisResults> allRepositories = parentResults.getRepositoryAnalysisResults();

        boolean[] assigned = new boolean[allRepositories.size()];

        for (VirtualLandscapeConfig vlConfig : configs) {
            List<RepositoryAnalysisResults> members = new ArrayList<>();
            for (int i = 0; i < allRepositories.size(); i++) {
                String name = repositoryName(allRepositories.get(i));
                if (matches(name, vlConfig.getIncludeRepoNamePatterns(), vlConfig.getExcludeRepoNamePatterns())) {
                    members.add(allRepositories.get(i));
                    assigned[i] = true; // a repository may belong to several virtual landscapes
                }
            }
            result.add(new VirtualLandscape(vlConfig.getMetadata().getName(), vlConfig.getMetadata(),
                    buildChildResults(members)));
        }

        // Remainder: repositories matched by no virtual landscape.
        List<RepositoryAnalysisResults> remainder = new ArrayList<>();
        for (int i = 0; i < allRepositories.size(); i++) {
            if (!assigned[i]) {
                remainder.add(allRepositories.get(i));
            }
        }
        Metadata remainderMetadata = config.getVirtualLandscapes().getRemainderLandscapeMetadata();
        result.add(new VirtualLandscape(remainderMetadata.getName(), remainderMetadata, buildChildResults(remainder)));

        return result;
    }

    private String repositoryName(RepositoryAnalysisResults repository) {
        return repository.getAnalysisResults().getMetadata().getName();
    }

    private boolean matches(String name, List<String> includePatterns, List<String> excludePatterns) {
        boolean included = includePatterns != null && !includePatterns.isEmpty()
                && RegexUtils.matchesAnyPattern(name, includePatterns);
        if (!included) {
            return false;
        }
        return excludePatterns == null || excludePatterns.isEmpty()
                || !RegexUtils.matchesAnyPattern(name, excludePatterns);
    }

    /**
     * Builds a child {@link LandscapeAnalysisResults} for a repository subset, reusing the parent's
     * team/people config and re-deriving the first/latest commit dates and the level-1 sub-landscape
     * dependency aggregates (mirrors {@code LandscapeAnalyzer.analyze}).
     */
    private LandscapeAnalysisResults buildChildResults(List<RepositoryAnalysisResults> repositories) {
        LandscapeAnalysisResults child = new LandscapeAnalysisResults(parentResults.getTeamsConfig(), parentResults.getPeopleConfig());
        child.getRepositoryAnalysisResults().addAll(repositories);

        DependenciesCreator viaContributors = new DependenciesCreator();
        DependenciesCreator viaSameName = new DependenciesCreator();

        repositories.forEach(repository -> {
            String repositoryName = repositoryName(repository);
            repository.getAnalysisResults().getContributorsAnalysisResults().getContributors().forEach(contributor ->
                    contributor.getCommitDates().forEach(commitDate -> {
                        if (child.getFirstCommitDate().isEmpty() || commitDate.compareTo(child.getFirstCommitDate()) < 0) {
                            child.setFirstCommitDate(commitDate);
                        }
                        if (child.getLatestCommitDate().isEmpty() || commitDate.compareTo(child.getLatestCommitDate()) > 0) {
                            child.setLatestCommitDate(commitDate);
                        }
                    }));
            String level1SubLandscape = repository.getSokratesRepositoryLink().getAnalysisResultsPath().replaceAll("/.*", "");
            child.getLevel1SubLandscapes().add(level1SubLandscape);
            repository.getAnalysisResults().getContributorsAnalysisResults().getContributors().stream()
                    .filter(c -> c.isActive(Contributor.RECENTLY_ACTIVITY_THRESHOLD_DAYS))
                    .forEach(contributor -> viaContributors.add("[" + level1SubLandscape + "]", contributor.getEmail()));
            viaSameName.add("[" + level1SubLandscape + "]", repositoryName);
        });

        child.setSubLandscapeDependenciesViaRepositoriesWithSameContributors(viaContributors.getDependencies());
        child.setSubLandscapeIndirectDependenciesViaRepositoriesWithSameContributors(viaContributors.getIndirectDependencies());
        child.setSubLandscapeDependenciesViaRepositoriesWithSameName(viaSameName.getDependencies());
        child.setSubLandscapeIndirectDependenciesViaRepositoriesWithSameName(viaSameName.getIndirectDependencies());

        return child;
    }

    /**
     * A copy of the parent configuration with the given metadata, an extra path level added to the
     * relative {@code analysisRoot} / {@code repositoryReportsUrlPrefix} (the virtual landscape sits
     * two folders deeper: landscapes/&lt;name&gt;/_sokrates_landscape/), and virtual landscapes cleared
     * (no nesting yet). The repository link list is left as-is so the child report links resolve to the
     * same shared repository report folders.
     */
    public static LandscapeConfiguration childConfiguration(LandscapeConfiguration parent, Metadata metadata) {
        LandscapeConfiguration copy;
        try {
            copy = (LandscapeConfiguration) new JsonMapper()
                    .getObject(new JsonGenerator().generate(parent), LandscapeConfiguration.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to clone landscape configuration for virtual landscape", e);
        }
        copy.setMetadata(metadata);
        copy.setVirtualLandscapes(new nl.obren.sokrates.sourcecode.landscape.VirtualLandscapesConfig());
        // The child report sits at landscapes/<name>/_sokrates_landscape/, i.e. three folder levels
        // deeper than the parent's _sokrates_landscape folder, so its relative analysisRoot /
        // report URL prefix must climb three extra levels to reach the shared repository folders.
        copy.setAnalysisRoot(prependParentDirs(parent.getAnalysisRoot(), 3));
        copy.setRepositoryReportsUrlPrefix(prependParentDirs(parent.getRepositoryReportsUrlPrefix(), 3));
        return copy;
    }

    private static String prependParentDirs(String relativePath, int levels) {
        if (relativePath == null) {
            relativePath = "";
        }
        StringBuilder up = new StringBuilder();
        for (int i = 0; i < levels; i++) {
            up.append("../");
        }
        // "." stays meaningful; otherwise just climb extra levels before the original relative path.
        if (relativePath.equals(".") || relativePath.equals("./")) {
            return up + ".";
        }
        return up + relativePath;
    }
}
