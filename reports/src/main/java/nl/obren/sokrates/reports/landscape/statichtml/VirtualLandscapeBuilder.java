package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.landscape.VirtualLandscapeConfig;
import nl.obren.sokrates.sourcecode.landscape.VirtualLandscapesConfig;
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

    /**
     * One virtual landscape: a name/metadata plus the subset of repositories that belong to it.
     * Carries the originating {@link VirtualLandscapeConfig} (null for the Remainder) so the caller
     * can recurse into its own nested virtual landscapes.
     */
    public static class VirtualLandscape {
        private final String name;
        private final Metadata metadata;
        private final LandscapeAnalysisResults results;
        private final VirtualLandscapeConfig config;

        public VirtualLandscape(String name, Metadata metadata, LandscapeAnalysisResults results, VirtualLandscapeConfig config) {
            this.name = name;
            this.metadata = metadata;
            this.results = results;
            this.config = config;
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

        /** The config that defined this virtual landscape, or null for the Remainder landscape. */
        public VirtualLandscapeConfig getConfig() {
            return config;
        }
    }

    private final LandscapeAnalysisResults parentResults;

    public VirtualLandscapeBuilder(LandscapeAnalysisResults parentResults) {
        this.parentResults = parentResults;
    }

    public boolean hasVirtualLandscapes() {
        return hasVirtualLandscapes(parentResults.getConfiguration().getVirtualLandscapes());
    }

    /** True when the given config defines at least one virtual landscape. */
    public static boolean hasVirtualLandscapes(VirtualLandscapesConfig config) {
        return config != null && config.getLandscapes() != null && !config.getLandscapes().isEmpty();
    }

    /**
     * Builds the virtual landscapes (in config order) followed by the Remainder landscape
     * (repositories assigned to none). Returns an empty list when no virtual landscapes are configured.
     */
    public List<VirtualLandscape> build() {
        return build(parentResults.getConfiguration().getVirtualLandscapes(),
                parentResults.getRepositoryAnalysisResults());
    }

    /**
     * Partitions the given repositories according to {@code config}: one virtual landscape per
     * configured entry (in order), then a Remainder landscape of repositories matched by none.
     * Used both for the top-level partition (parent repositories) and for nested partitions (a
     * virtual landscape's own repositories), which is what makes nesting unlimited in depth.
     * Returns an empty list when {@code config} defines no virtual landscapes.
     */
    public List<VirtualLandscape> build(VirtualLandscapesConfig config, List<RepositoryAnalysisResults> repositories) {
        List<VirtualLandscape> result = new ArrayList<>();
        if (!hasVirtualLandscapes(config)) {
            return result;
        }

        List<VirtualLandscapeConfig> configs = config.getLandscapes();
        boolean[] assigned = new boolean[repositories.size()];

        for (VirtualLandscapeConfig vlConfig : configs) {
            List<RepositoryAnalysisResults> members = new ArrayList<>();
            for (int i = 0; i < repositories.size(); i++) {
                String name = repositoryName(repositories.get(i));
                if (matches(name, vlConfig.getIncludeRepoNamePatterns(), vlConfig.getExcludeRepoNamePatterns())) {
                    members.add(repositories.get(i));
                    assigned[i] = true; // a repository may belong to several virtual landscapes
                }
            }
            result.add(new VirtualLandscape(vlConfig.getMetadata().getName(), vlConfig.getMetadata(),
                    buildChildResults(members), vlConfig));
        }

        // Remainder: repositories matched by no virtual landscape.
        List<RepositoryAnalysisResults> remainder = new ArrayList<>();
        for (int i = 0; i < repositories.size(); i++) {
            if (!assigned[i]) {
                remainder.add(repositories.get(i));
            }
        }
        Metadata remainderMetadata = config.getRemainderLandscapeMetadata();
        result.add(new VirtualLandscape(remainderMetadata.getName(), remainderMetadata, buildChildResults(remainder), null));

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

    /** The number of folder levels a virtual landscape sits below its parent landscape folder. */
    public static final int FOLDER_LEVELS_PER_NESTING = 3;

    /**
     * A copy of the {@code root} configuration with the given metadata, the relative
     * {@code analysisRoot} / {@code repositoryReportsUrlPrefix} climbed enough levels to reach the
     * shared repository report folders, and {@code virtualLandscapes} set to {@code nested} so the
     * child renders (and persists) its own nested virtual landscapes.
     *
     * <p>{@code depth} is the nesting level (1 for a top-level virtual landscape, 2 for one nested
     * inside it, …). Each level lives at {@code landscapes/<name>/_sokrates_landscape/}, i.e.
     * {@link #FOLDER_LEVELS_PER_NESTING} folders deeper, so the relative paths must climb
     * {@code depth * FOLDER_LEVELS_PER_NESTING} extra levels relative to the top-level landscape.
     * The repository link list is left as-is so the child report links resolve to the same shared
     * repository report folders.
     *
     * @param root   the top-level landscape configuration (the absolute source of the relative paths)
     * @param metadata the virtual landscape's metadata
     * @param nested this virtual landscape's own nested virtual landscapes (empty for the Remainder)
     * @param depth  the nesting level (1-based)
     */
    public static LandscapeConfiguration childConfiguration(LandscapeConfiguration root, Metadata metadata,
                                                            VirtualLandscapesConfig nested, int depth) {
        LandscapeConfiguration copy;
        try {
            copy = (LandscapeConfiguration) new JsonMapper()
                    .getObject(new JsonGenerator().generate(root), LandscapeConfiguration.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to clone landscape configuration for virtual landscape", e);
        }
        copy.setMetadata(metadata);
        copy.setVirtualLandscapes(nested != null ? nested : new VirtualLandscapesConfig());
        copy.getSubLandscapes().clear(); // nested virtual landscapes are re-registered during generation
        int levels = depth * FOLDER_LEVELS_PER_NESTING;
        copy.setAnalysisRoot(prependParentDirs(root.getAnalysisRoot(), levels));
        copy.setRepositoryReportsUrlPrefix(prependParentDirs(root.getRepositoryReportsUrlPrefix(), levels));
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
