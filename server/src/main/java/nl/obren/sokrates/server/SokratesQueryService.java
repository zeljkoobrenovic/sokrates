package nl.obren.sokrates.server;

import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.reports.dataexporters.DataExporter;
import nl.obren.sokrates.reports.landscape.ai.AiFindingExport;
import nl.obren.sokrates.reports.landscape.ai.AiInsightsAggregator;
import nl.obren.sokrates.reports.landscape.ai.AiInsightsLandscapeExport;
import nl.obren.sokrates.server.AnalysisCatalog.LandscapeLocation;
import nl.obren.sokrates.server.AnalysisCatalog.RepositoryLocation;
import nl.obren.sokrates.server.api.Api;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.ContributorsAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.LogicalDecompositionAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.scores.MaintainabilityScore;
import nl.obren.sokrates.sourcecode.analysis.scores.MaintainabilityScores;
import nl.obren.sokrates.sourcecode.dependencies.ComponentDependency;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalyzer;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import nl.obren.sokrates.sourcecode.stats.RiskDistributionStats;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The queries the REST API and the MCP tools share. Reads only what the analyses already wrote (each repository's
 * data.zip and reports/ai-insights/*.json, each landscape's config.json); nothing is stored beside them. A
 * repository's summary is built once per data.zip version and kept (small), the full analysis model is not.
 */
public class SokratesQueryService {
    private static final Log LOG = LogFactory.getLog(SokratesQueryService.class);
    public static final String REPORTS_PATH = "/reports/";
    static final String MAIN_FILE_CONTRIBUTORS = DataExporter.MAIN_FILE_CONTRIBUTORS_FILE_NAME;
    public static final int MAX_LIMIT = 500;
    // A decomposition can have hundreds of components; the summary keeps the largest.
    static final int MAX_COMPONENTS = 30;
    static final List<String> SEVERITIES = Arrays.asList("critical", "high", "medium", "low", "info");

    private final AnalysisCatalog catalog;
    private final PeoplePrivacy privacy;
    private final Map<String, Loaded> loaded = new ConcurrentHashMap<>();
    private final Map<String, CachedFindings> findings = new ConcurrentHashMap<>();
    private final Map<String, LandscapeIdentities> landscapeIdentities = new ConcurrentHashMap<>();

    private record Loaded(long stamp, RepositoryLocation location, Api.RepositoryItem item, Api.RepositoryDetails details, List<String> languages,
                          List<Api.Contributor> contributors, Map<String, List<Api.Dependency>> dependencies) {
    }

    private record CachedFindings(long stamp, List<Api.Finding> findings) {
    }

    public SokratesQueryService(AnalysisCatalog catalog) {
        this(catalog, PeoplePrivacy.visible());
    }

    public SokratesQueryService(AnalysisCatalog catalog, PeoplePrivacy privacy) {
        this.catalog = catalog;
        this.privacy = privacy;
    }

    public PeoplePrivacy getPrivacy() {
        return privacy;
    }

    public AnalysisCatalog getCatalog() {
        return catalog;
    }

    // ---- landscapes ----

    public List<Api.Landscape> landscapes() {
        AnalysisCatalog.Snapshot snapshot = catalog.snapshot();
        List<Api.Landscape> result = new ArrayList<>();
        for (LandscapeLocation landscape : snapshot.landscapes()) {
            Metadata metadata = landscapeMetadata(landscape);
            int count = distinctByName(snapshot.repositories().stream().filter(inLandscape(landscape)).collect(Collectors.toList())).size();
            String indexPath = catalog.servedPath(new File(landscape.landscapeFolder(), "index.html"));
            result.add(new Api.Landscape(landscape.id(),
                    metadata != null && StringUtils.isNotBlank(metadata.getName()) ? metadata.getName() : landscape.id(),
                    metadata != null ? metadata.getDescription() : null, count, privacy.reportLink(REPORTS_PATH + indexPath)));
        }
        return result;
    }

    private Metadata landscapeMetadata(LandscapeLocation landscape) {
        try {
            String json = FileUtils.readFileToString(new File(landscape.landscapeFolder(), "config.json"), StandardCharsets.UTF_8);
            LandscapeConfiguration configuration = (LandscapeConfiguration) new JsonMapper().getObject(json, LandscapeConfiguration.class);
            return configuration == null ? null : configuration.getMetadata();
        } catch (Exception e) {
            LOG.warn("Could not read the landscape configuration in " + landscape.landscapeFolder().getPath() + ": " + e.getMessage());
            return null;
        }
    }

    private static Predicate<RepositoryLocation> inLandscape(LandscapeLocation landscape) {
        String prefix = landscape.id().equals(".") ? "" : landscape.id() + "/";
        return repository -> prefix.isEmpty() || repository.id().startsWith(prefix);
    }

    /**
     * A landscape at a glance ({@code landscapeId} blank: every analysis): repository counts, size per scope and
     * language, grade distributions, size-weighted averages, activity with distinct people (merged as the landscape
     * merges them; counts only), AI findings per severity, and short lists of the largest, most active and
     * lowest-scoring repositories.
     */
    public Api.LandscapeOverview overview(String landscapeId) {
        List<RepositoryLocation> locations = catalog.snapshot().repositories();
        LandscapeIdentities identities = LandscapeIdentities.AS_ANALYZED;
        String id = "all";
        String name = "All analyses";
        String description = null;
        String reportUrl = null;
        if (StringUtils.isNotBlank(landscapeId)) {
            LandscapeLocation landscape = landscape(landscapeId);
            locations = locations.stream().filter(inLandscape(landscape)).collect(Collectors.toList());
            identities = identities(landscape);
            id = landscape.id();
            Metadata metadata = landscapeMetadata(landscape);
            name = metadata != null && StringUtils.isNotBlank(metadata.getName()) ? metadata.getName() : landscape.id();
            description = metadata == null ? null : StringUtils.trimToNull(metadata.getDescription());
            reportUrl = privacy.reportLink(REPORTS_PATH + catalog.servedPath(new File(landscape.landscapeFolder(), "index.html")));
        }
        LandscapeIdentities people = identities;
        List<LandscapeOverviewBuilder.Input> inputs = distinctByName(locations).stream().map(location -> {
            Loaded row = load(location);
            Map<String, Integer> bySeverity = new HashMap<>();
            findingsOf(location, row.item().name()).forEach(f -> bySeverity.merge(f.severity(), 1, Integer::sum));
            return new LandscapeOverviewBuilder.Input(withFindingsCount(row), row.details(), row.contributors(), bySeverity);
        }).collect(Collectors.toList());
        return LandscapeOverviewBuilder.build(id, name, description, reportUrl, inputs,
                contributor -> people.canonical(contributor.email(), contributor.userName()));
    }

    // ---- repositories ----

    /**
     * The repositories matching the query, in the landscape repository list's syntax: terms split by {@code ;},
     * {@code mainLang:java,kt} (main language one of these), {@code includesLang:tf} (any code in one of these, any
     * scope), other terms match the name, id or description (any of them).
     */
    public Api.Page<Api.RepositoryItem> repositories(String query, String landscapeId, String sort, int offset, int limit) {
        List<Loaded> rows = loadAll(landscapeId);
        RepositoryQuery repositoryQuery = RepositoryQuery.parse(query);
        List<Api.RepositoryItem> items = rows.stream()
                .filter(row -> repositoryQuery.matches(row.item(), row.languages()))
                .map(this::withFindingsCount)
                .sorted(comparator(sort))
                .collect(Collectors.toList());
        return page(items, offset, limit);
    }

    public Optional<Api.RepositoryDetails> repository(String idOrName) {
        return find(idOrName).map(this::load).map(Loaded::details);
    }

    private List<Loaded> loadAll(String landscapeId) {
        AnalysisCatalog.Snapshot snapshot = catalog.snapshot();
        Predicate<RepositoryLocation> filter = repository -> true;
        if (StringUtils.isNotBlank(landscapeId)) {
            LandscapeLocation landscape = snapshot.landscapes().stream()
                    .filter(l -> l.id().equals(landscapeId)).findFirst()
                    .orElseThrow(() -> new NotFoundException("No landscape with id \"" + landscapeId + "\""));
            filter = inLandscape(landscape);
        }
        List<RepositoryLocation> selected = snapshot.repositories().stream().filter(filter).collect(Collectors.toList());
        return distinctByName(selected).parallelStream().map(this::load).filter(row -> row != null).collect(Collectors.toList());
    }

    /**
     * One analysis per repository name, as the landscape counts them (includeOnlyOneRepositoryWithSameName): a
     * leftover or moved copy would otherwise count a repository twice. The most recently generated one is kept; the
     * others stay reachable by id. Unreadable analyses are dropped.
     */
    List<RepositoryLocation> distinctByName(List<RepositoryLocation> locations) {
        Map<String, RepositoryLocation> byName = new LinkedHashMap<>();
        List<RepositoryLocation> loadedLocations = locations.parallelStream().filter(location -> load(location) != null).collect(Collectors.toList());
        for (RepositoryLocation location : loadedLocations) {
            String name = load(location).item().name().toLowerCase(Locale.ROOT);
            RepositoryLocation kept = byName.get(name);
            if (kept == null || location.stamp() > kept.stamp()) {
                if (kept != null) {
                    LOG.info("Two analyses of " + name + ": using " + location.id() + ", not " + kept.id());
                }
                byName.put(name, location);
            } else {
                LOG.info("Two analyses of " + name + ": using " + kept.id() + ", not " + location.id());
            }
        }
        return new ArrayList<>(byName.values());
    }

    // The findings are written after the analysis (by the agent skills), so they are counted per request, not per data.zip.
    private Api.RepositoryItem withFindingsCount(Loaded row) {
        Api.RepositoryItem item = row.item();
        int count = findingsOf(row.location(), item.name()).size();
        return new Api.RepositoryItem(item.id(), item.name(), item.description(), item.mainLanguage(), item.mainLinesOfCode(),
                item.testLinesOfCode(), item.humanScore(), item.aiScore(), item.commits30Days(), item.contributors30Days(),
                item.latestCommitDate(), count, item.reportUrl());
    }

    /** By id (the analysis folder relative to the root), else by name (case-insensitive; the first when two share it). */
    Optional<RepositoryLocation> find(String idOrName) {
        if (StringUtils.isBlank(idOrName)) {
            return Optional.empty();
        }
        String wanted = idOrName.trim();
        List<RepositoryLocation> repositories = catalog.snapshot().repositories();
        Optional<RepositoryLocation> byId = repositories.stream().filter(r -> r.id().equals(wanted)).findFirst();
        if (byId.isPresent()) {
            return byId;
        }
        // by name: the most recent analysis of that name, as in the lists
        return distinctByName(repositories).stream().filter(r -> wanted.equalsIgnoreCase(load(r).item().name())).findFirst();
    }

    RepositoryLocation require(String idOrName) {
        return find(idOrName).orElseThrow(() -> new NotFoundException("No repository with id or name \"" + idOrName + "\""));
    }

    private Loaded load(RepositoryLocation location) {
        long stamp = location.stamp();
        Loaded cached = loaded.get(location.id());
        if (cached != null && cached.stamp() == stamp) {
            return cached;
        }
        String json = LandscapeAnalyzer.readDataEntry(location.dataFolder(), "analysisResults.json");
        if (json == null) {
            LOG.warn("Skipping " + location.id() + ": no analysisResults.json");
            return null;
        }
        try {
            CodeAnalysisResults results = (CodeAnalysisResults) new JsonMapper().getObject(json, CodeAnalysisResults.class);
            Loaded row = summarize(location, results);
            loaded.put(location.id(), row);
            return row;
        } catch (RuntimeException | java.io.IOException e) {
            LOG.warn("Skipping " + location.id() + ": " + e.getMessage());
            return null;
        }
    }

    private Loaded summarize(RepositoryLocation location, CodeAnalysisResults results) {
        Metadata metadata = results.getMetadata();
        String name = metadata != null && StringUtils.isNotBlank(metadata.getName()) ? metadata.getName() : location.id();
        String description = metadata != null ? StringUtils.trimToNull(metadata.getDescription()) : null;
        String reportUrl = privacy.reportLink(REPORTS_PATH + catalog.servedPath(new File(location.reportsFolder(), "index.html")));

        AspectAnalysisResults main = results.getMainAspectAnalysisResults();
        List<Api.LanguageSize> mainLanguages = languages(main);
        String mainLanguage = mainLanguages.isEmpty() ? null : mainLanguages.get(0).language();

        ContributorsAnalysisResults contributors = results.getContributorsAnalysisResults();
        int contributors30Days = (int) contributors.getContributors().stream().filter(c -> c.getCommitsCount30Days() > 0).count();
        String firstCommitDate = contributors.getContributors().stream().map(c -> c.getFirstCommitDate())
                .filter(StringUtils::isNotBlank).min(String::compareTo).orElse(null);
        Api.Activity activity = new Api.Activity(contributors.getCommitsCount(), contributors.getCommitsCount30Days(),
                contributors.getCommitsCount90Days(), contributors.getCommitsCount365Days(),
                contributors.getContributors().size(), contributors30Days, contributors.getChurn30Days(), contributors.getChurn90Days(),
                firstCommitDate, StringUtils.trimToNull(contributors.getLatestCommitDate()));

        MaintainabilityScores scores = results.getMaintainabilityScores();
        Api.Score human = scores == null ? null : score(scores.getHuman());
        Api.Score ai = scores == null ? null : score(scores.getAi());

        Double duplication = null;
        if (results.getDuplicationAnalysisResults() != null && results.getDuplicationAnalysisResults().getOverallDuplication() != null
                && results.getDuplicationAnalysisResults().getOverallDuplication().getDuplicationPercentage() != null) {
            duplication = round(results.getDuplicationAnalysisResults().getOverallDuplication().getDuplicationPercentage().doubleValue());
        }

        List<Api.ScopeSize> scopes = List.of(
                scope("main", main), scope("test", results.getTestAspectAnalysisResults()),
                scope("generated", results.getGeneratedAspectAnalysisResults()),
                scope("build", results.getBuildAndDeployAspectAnalysisResults()),
                scope("other", results.getOtherAspectAnalysisResults()));

        List<String> allLanguages = new ArrayList<>();
        for (AspectAnalysisResults aspect : Arrays.asList(main, results.getTestAspectAnalysisResults(), results.getGeneratedAspectAnalysisResults(),
                results.getBuildAndDeployAspectAnalysisResults(), results.getOtherAspectAnalysisResults())) {
            languages(aspect).forEach(language -> {
                if (!allLanguages.contains(language.language())) {
                    allLanguages.add(language.language());
                }
            });
        }

        Api.RepositoryDetails details = new Api.RepositoryDetails(location.id(), name, description, links(metadata),
                scopes, mainLanguages, duplication,
                riskProfile("lines of code per file", results.getFilesAnalysisResults() == null ? null : results.getFilesAnalysisResults().getOverallFileSizeDistribution()),
                riskProfile("lines of code per unit", results.getUnitsAnalysisResults() == null ? null : results.getUnitsAnalysisResults().getUnitSizeRiskDistribution()),
                riskProfile("McCabe index per unit", results.getUnitsAnalysisResults() == null ? null : results.getUnitsAnalysisResults().getConditionalComplexityRiskDistribution()),
                human, ai, activity, decompositions(results), reportUrl);
        Api.RepositoryItem item = new Api.RepositoryItem(location.id(), name, description, mainLanguage,
                main.getLinesOfCode(), results.getTestAspectAnalysisResults().getLinesOfCode(),
                human == null ? null : new Api.ScoreBrief(human.value(), human.grade()),
                ai == null ? null : new Api.ScoreBrief(ai.value(), ai.grade()),
                contributors.getCommitsCount30Days(), contributors30Days, activity.latestCommitDate(),
                0, reportUrl);
        List<Api.Contributor> people = contributors.getContributors().stream()
                .map(c -> new Api.Contributor(c.getEmail(), StringUtils.trimToNull(c.getUserName()), c.isBot(), c.getCommitsCount(),
                        c.getCommitsCount30Days(), c.getCommitsCount90Days(), c.getCommitsCount365Days(),
                        StringUtils.trimToNull(c.getFirstCommitDate()), StringUtils.trimToNull(c.getLatestCommitDate())))
                .collect(Collectors.toList());
        return new Loaded(location.stamp(), location, item, details, allLanguages, people, dependencies(results));
    }

    private static Api.ScopeSize scope(String scope, AspectAnalysisResults aspect) {
        return new Api.ScopeSize(scope, aspect == null ? 0 : aspect.getFilesCount(), aspect == null ? 0 : aspect.getLinesOfCode());
    }

    // Lines of code per extension ("  *.java" -> "java"), largest first.
    private static List<Api.LanguageSize> languages(AspectAnalysisResults aspect) {
        List<Api.LanguageSize> languages = new ArrayList<>();
        if (aspect == null || aspect.getLinesOfCodePerExtension() == null) {
            return languages;
        }
        for (NumericMetric metric : aspect.getLinesOfCodePerExtension()) {
            String language = metric.getName().replace("*.", "").trim().toLowerCase(Locale.ROOT);
            int lines = metric.getValue() == null ? 0 : metric.getValue().intValue();
            if (!language.isEmpty() && lines > 0) {
                languages.add(new Api.LanguageSize(language, lines));
            }
        }
        languages.sort(Comparator.comparingInt(Api.LanguageSize::linesOfCode).reversed());
        return languages;
    }

    private static List<Api.Link> links(Metadata metadata) {
        if (metadata == null || metadata.getLinks() == null) {
            return List.of();
        }
        return metadata.getLinks().stream().map(link -> new Api.Link(link.getLabel(), link.getHref())).collect(Collectors.toList());
    }

    private static Api.Score score(MaintainabilityScore score) {
        if (score == null) {
            return null;
        }
        List<Api.SubScore> subScores = score.getSubScores().stream()
                .map(s -> new Api.SubScore(s.getKey(), s.getLabel(), s.getScore(), s.getWeight(), s.getDrag(), s.getMeasureText()))
                .collect(Collectors.toList());
        return new Api.Score(score.getValue(), score.getGrade(), StringUtils.trimToNull(score.getCappedBy()),
                StringUtils.trimToNull(score.getCoverageShort()), subScores);
    }

    static Api.RiskProfile riskProfile(String measure, RiskDistributionStats stats) {
        if (stats == null || stats.getTotalValue() <= 0) {
            return null;
        }
        double total = stats.getTotalValue();
        List<Api.RiskBand> bands = List.of(
                band("very high", "> " + stats.getVeryHighRiskThreshold(), stats.getVeryHighRiskValue(), total),
                band("high", (stats.getHighRiskThreshold() + 1) + "-" + stats.getVeryHighRiskThreshold(), stats.getHighRiskValue(), total),
                band("medium", (stats.getMediumRiskThreshold() + 1) + "-" + stats.getHighRiskThreshold(), stats.getMediumRiskValue(), total),
                band("low", (stats.getLowRiskThreshold() + 1) + "-" + stats.getMediumRiskThreshold(), stats.getLowRiskValue(), total),
                band("negligible", "1-" + stats.getLowRiskThreshold(), stats.getNegligibleRiskValue(), total));
        return new Api.RiskProfile(measure, bands);
    }

    private static Api.RiskBand band(String risk, String range, int value, double total) {
        return new Api.RiskBand(risk, range, value, round(100.0 * value / total));
    }

    private static List<Api.Decomposition> decompositions(CodeAnalysisResults results) {
        List<Api.Decomposition> decompositions = new ArrayList<>();
        if (results.getLogicalDecompositionsAnalysisResults() == null) {
            return decompositions;
        }
        for (LogicalDecompositionAnalysisResults decomposition : results.getLogicalDecompositionsAnalysisResults()) {
            List<Api.ComponentSize> components = decomposition.getComponents().stream()
                    .map(c -> new Api.ComponentSize(c.getName(), c.getFilesCount(), c.getLinesOfCode()))
                    .sorted(Comparator.comparingInt(Api.ComponentSize::linesOfCode).reversed())
                    .collect(Collectors.toList());
            int count = components.size();
            decompositions.add(new Api.Decomposition(decomposition.getKey(), count,
                    count > MAX_COMPONENTS ? new ArrayList<>(components.subList(0, MAX_COMPONENTS)) : components));
        }
        return decompositions;
    }

    // decomposition → its component dependencies, most evidence first, each marked cyclic when the reverse exists too
    private static Map<String, List<Api.Dependency>> dependencies(CodeAnalysisResults results) {
        Map<String, List<Api.Dependency>> byDecomposition = new LinkedHashMap<>();
        if (results.getLogicalDecompositionsAnalysisResults() == null) {
            return byDecomposition;
        }
        for (LogicalDecompositionAnalysisResults decomposition : results.getLogicalDecompositionsAnalysisResults()) {
            List<ComponentDependency> dependencies = decomposition.getComponentDependencies() == null ? List.of() : decomposition.getComponentDependencies();
            java.util.Set<String> pairs = new java.util.HashSet<>();
            dependencies.forEach(d -> pairs.add(d.getFromComponent() + "\u0000" + d.getToComponent()));
            List<Api.Dependency> items = dependencies.stream()
                    .filter(d -> !StringUtils.equals(d.getFromComponent(), d.getToComponent()))
                    .map(d -> new Api.Dependency(d.getFromComponent(), d.getToComponent(), d.getCount(),
                            pairs.contains(d.getToComponent() + "\u0000" + d.getFromComponent())))
                    .sorted(Comparator.comparingInt(Api.Dependency::count).reversed()
                            .thenComparing(Api.Dependency::from).thenComparing(Api.Dependency::to))
                    .collect(Collectors.toList());
            byDecomposition.put(decomposition.getKey(), items);
        }
        return byDecomposition;
    }

    private static Comparator<Api.RepositoryItem> comparator(String sort) {
        Comparator<Api.RepositoryItem> byName = Comparator.comparing(item -> item.name().toLowerCase(Locale.ROOT));
        switch (StringUtils.defaultString(sort).toLowerCase(Locale.ROOT)) {
            case "name":
                return byName;
            case "commits":
            case "commits30days":
                return Comparator.comparingInt(Api.RepositoryItem::commits30Days).reversed().thenComparing(byName);
            case "humanscore":
                return Comparator.comparingDouble((Api.RepositoryItem item) -> item.humanScore() == null ? 99 : item.humanScore().value()).thenComparing(byName);
            case "aiscore":
                return Comparator.comparingDouble((Api.RepositoryItem item) -> item.aiScore() == null ? 99 : item.aiScore().value()).thenComparing(byName);
            case "findings":
                return Comparator.comparingInt(Api.RepositoryItem::aiFindings).reversed().thenComparing(byName);
            default:
                return Comparator.comparingInt(Api.RepositoryItem::mainLinesOfCode).reversed().thenComparing(byName);
        }
    }

    // ---- hotspots ----

    /** The main files to look at first; see {@link HotspotData#rank} for the windows. */
    public Api.Hotspots hotspots(String idOrName, String window, int limit) {
        RepositoryLocation location = require(idOrName);
        Loaded row = load(location);
        String name = row == null ? location.id() : row.item().name();
        Map<String, Integer> complexity = new LinkedHashMap<>();
        Map<String, Integer> linesOfCode = new HashMap<>();
        boolean anyUnits = HotspotData.readComplexity(LandscapeAnalyzer.readDataEntry(location.dataFolder(), "mainFiles.json"), complexity, linesOfCode);
        List<HotspotData.FileHistory> history = HotspotData.readHistory(LandscapeAnalyzer.readDataEntry(location.dataFolder(), "text/mainFilesWithHistory.txt"));
        return HotspotData.rank(name, complexity, linesOfCode, anyUnits, history, window, clamp(limit, 10));
    }

    // ---- people ----

    /** The repository's contributors, most active in the past 365 days first; {@code query} terms match email or name. */
    public Api.Page<Api.Contributor> contributors(String idOrName, String query, boolean includeBots, int offset, int limit) {
        Loaded row = loaded(idOrName);
        List<String> terms = terms(query);
        // Filtered after pseudonymizing, so a hidden server cannot be probed for an email address.
        List<Api.Contributor> people = row.contributors().stream()
                .filter(c -> includeBots || !c.bot())
                .map(c -> new Api.Contributor(privacy.person(c.email()), privacy.userName(c.userName()), c.bot(), c.commits(),
                        c.commits30Days(), c.commits90Days(), c.commits365Days(), c.firstCommitDate(), c.latestCommitDate()))
                .filter(c -> terms.isEmpty() || terms.stream().anyMatch(term ->
                        (c.email() + " " + StringUtils.defaultString(c.userName())).toLowerCase(Locale.ROOT).contains(term)))
                .sorted(Comparator.comparingInt(Api.Contributor::commits365Days).reversed()
                        .thenComparing(Comparator.comparingInt(Api.Contributor::commits).reversed())
                        .thenComparing(Api.Contributor::email))
                .collect(Collectors.toList());
        return page(people, offset, limit);
    }

    /**
     * Who knows a part of the main code: a file or folder ({@code path}, a prefix relative to the repository root), a
     * logical component ({@code component}: a name or {@code decomposition::component}, optionally limited to
     * {@code decomposition}) and/or languages ({@code languages}: file extensions, comma-separated); all of it when
     * none is given. In one repository, or, without {@code repository}, across a landscape (its people merged as the
     * landscape report merges them) or every analysis.
     */
    public Api.Experts experts(String repository, String landscapeId, String path, String component, String decomposition,
                               String languages, int limit) {
        ExpertsData.Scope scope = ExpertsData.Scope.of(path, component, decomposition, languages);
        boolean single = StringUtils.isNotBlank(repository);
        List<RepositoryLocation> locations;
        LandscapeIdentities identities = LandscapeIdentities.AS_ANALYZED;
        String label;
        if (single) {
            locations = List.of(require(repository));
            label = null;
        } else if (StringUtils.isNotBlank(landscapeId)) {
            LandscapeLocation landscape = landscape(landscapeId);
            locations = distinctByName(catalog.snapshot().repositories().stream().filter(inLandscape(landscape)).collect(Collectors.toList()));
            identities = identities(landscape);
            label = landscape.id();
        } else {
            locations = distinctByName(catalog.snapshot().repositories());
            label = "all";
        }
        LandscapeIdentities people = identities;
        List<RepositoryExperts> perRepository = locations.parallelStream().map(location -> repositoryChanges(location, scope, people))
                .filter(r -> r != null).collect(Collectors.toList());
        int notCovered = (int) perRepository.stream().filter(r -> !r.covered()).count();
        if (single && notCovered > 0) {
            throw new IllegalArgumentException("This analysis has no per-file contributors (text/" + MAIN_FILE_CONTRIBUTORS
                    + "); regenerate it with a current Sokrates.");
        }
        int files = perRepository.stream().mapToInt(RepositoryExperts::files).sum();
        if (files == 0 && (single || notCovered < perRepository.size())) {
            throw new NotFoundException("No main files match " + scope.describe());
        }
        List<ExpertsData.Change> changes = perRepository.stream().flatMap(r -> r.changes().stream()).collect(Collectors.toList());
        String name = single ? perRepository.get(0).name() : label;
        return ExpertsData.aggregate(name, scope.describe(), files, changes, !single, notCovered, clamp(limit, 10));
    }

    private record RepositoryExperts(String name, boolean covered, int files, List<ExpertsData.Change> changes) {
    }

    // One repository's changes in scope, people in their final identity (landscape-merged, then pseudonymized when hidden).
    private RepositoryExperts repositoryChanges(RepositoryLocation location, ExpertsData.Scope scope, LandscapeIdentities identities) {
        Loaded row = load(location);
        if (row == null) {
            return null;
        }
        String name = row.item().name();
        String text = LandscapeAnalyzer.readDataEntry(location.dataFolder(), "text/" + MAIN_FILE_CONTRIBUTORS);
        if (text == null) {
            return new RepositoryExperts(name, false, 0, List.of());
        }
        java.util.Set<String> componentFiles = scope.hasComponent()
                ? ExpertsData.filesOfComponent(LandscapeAnalyzer.readDataEntry(location.dataFolder(), "files.json"), scope.component(), scope.decomposition())
                : null;
        Map<String, Api.Contributor> byEmail = new HashMap<>();
        row.contributors().forEach(c -> byEmail.put(c.email(), c));
        java.util.Set<String> files = new java.util.HashSet<>();
        if (componentFiles != null) {
            componentFiles.stream().filter(file -> scope.matches(file, null)).forEach(files::add);
        }
        List<ExpertsData.Change> changes = new ArrayList<>();
        for (ExpertsData.FileContributor fileContributor : ExpertsData.readFileContributors(text)) {
            if (!scope.matches(fileContributor.path(), componentFiles)) {
                continue;
            }
            files.add(fileContributor.path());
            Api.Contributor contributor = byEmail.get(fileContributor.contributor());
            String id = identities.canonical(fileContributor.contributor(), contributor == null ? null : contributor.userName());
            if (id == null) {
                continue;
            }
            changes.add(new ExpertsData.Change(name, fileContributor.path(), privacy.person(id), contributor != null && contributor.bot(),
                    fileContributor.commits(), fileContributor.commits365(), fileContributor.lastCommit()));
        }
        return new RepositoryExperts(name, true, files.size(), changes);
    }

    private LandscapeLocation landscape(String landscapeId) {
        return catalog.snapshot().landscapes().stream().filter(l -> l.id().equals(landscapeId)).findFirst()
                .orElseThrow(() -> new NotFoundException("No landscape with id \"" + landscapeId + "\""));
    }

    private LandscapeIdentities identities(LandscapeLocation landscape) {
        LandscapeIdentities cached = landscapeIdentities.get(landscape.id());
        if (cached != null && cached.getStamp() == LandscapeIdentities.stamp(landscape.landscapeFolder())) {
            return cached;
        }
        LandscapeIdentities loaded = LandscapeIdentities.load(landscape.landscapeFolder());
        landscapeIdentities.put(landscape.id(), loaded);
        return loaded;
    }

    // ---- dependencies ----

    /** The component dependencies of a decomposition (the first when none is given), optionally only those of one component. */
    public Api.Dependencies dependencies(String idOrName, String decomposition, String component) {
        Loaded row = loaded(idOrName);
        List<String> names = new ArrayList<>(row.dependencies().keySet());
        if (names.isEmpty()) {
            throw new NotFoundException("This analysis has no logical decompositions");
        }
        String name = StringUtils.isBlank(decomposition) ? names.get(0)
                : names.stream().filter(n -> n.equalsIgnoreCase(decomposition.trim())).findFirst()
                .orElseThrow(() -> new NotFoundException("No decomposition \"" + decomposition + "\"; one of " + names));
        List<Api.Dependency> items = row.dependencies().get(name).stream()
                .filter(d -> StringUtils.isBlank(component) || d.from().equalsIgnoreCase(component.trim()) || d.to().equalsIgnoreCase(component.trim()))
                .collect(Collectors.toList());
        int componentsCount = row.details().decompositions().stream().filter(d -> d.name().equals(name))
                .mapToInt(Api.Decomposition::componentsCount).findFirst().orElse(0);
        int cyclicPairs = (int) items.stream().filter(Api.Dependency::cyclic).count() / 2;
        return new Api.Dependencies(row.item().name(), name, names, componentsCount, cyclicPairs, items);
    }

    private Loaded loaded(String idOrName) {
        Loaded row = load(require(idOrName));
        if (row == null) {
            throw new NotFoundException("The analysis of \"" + idOrName + "\" could not be read");
        }
        return row;
    }

    // ---- AI findings ----

    /**
     * AI findings of one repository, a landscape, or every repository: {@code query} terms split by {@code ;} match
     * title, description, recommendation, group or tags (any of them); {@code minSeverity} keeps that severity and
     * above; {@code scanner} matches the scanner name (e.g. security-scan).
     */
    public Api.Page<Api.Finding> findings(String repository, String landscapeId, String query, String minSeverity, String scanner, int offset, int limit) {
        List<RepositoryLocation> locations;
        if (StringUtils.isNotBlank(repository)) {
            locations = List.of(require(repository));
        } else {
            locations = catalog.snapshot().repositories();
            if (StringUtils.isNotBlank(landscapeId)) {
                locations = locations.stream().filter(inLandscape(landscape(landscapeId))).collect(Collectors.toList());
            }
            locations = distinctByName(locations);
        }
        int maxRank = StringUtils.isBlank(minSeverity) ? SEVERITIES.size() - 1 : SEVERITIES.indexOf(minSeverity.trim().toLowerCase(Locale.ROOT));
        if (maxRank < 0) {
            throw new IllegalArgumentException("Unknown severity \"" + minSeverity + "\"; one of " + SEVERITIES);
        }
        List<String> terms = terms(query);
        List<Api.Finding> result = new ArrayList<>();
        for (RepositoryLocation location : locations) {
            Loaded row = load(location);
            for (Api.Finding finding : findingsOf(location, row == null ? location.id() : row.item().name())) {
                if (SEVERITIES.indexOf(finding.severity()) > maxRank) {
                    continue;
                }
                if (StringUtils.isNotBlank(scanner) && !finding.scanner().equalsIgnoreCase(scanner.trim())) {
                    continue;
                }
                if (!terms.isEmpty() && terms.stream().noneMatch(term -> findingText(finding).contains(term))) {
                    continue;
                }
                result.add(finding);
            }
        }
        result.sort(Comparator.comparingInt((Api.Finding f) -> SEVERITIES.indexOf(f.severity()))
                .thenComparing(Api.Finding::repository).thenComparing(Api.Finding::scanner).thenComparing(Api.Finding::title));
        return page(result, offset, limit);
    }

    private static String findingText(Api.Finding finding) {
        return (finding.title() + " " + finding.description() + " " + finding.recommendation() + " " + finding.group() + " "
                + String.join(" ", finding.tags())).toLowerCase(Locale.ROOT);
    }

    private List<Api.Finding> findingsOf(RepositoryLocation location, String name) {
        File folder = new File(location.reportsFolder(), AiInsightsAggregator.AI_INSIGHTS_FOLDER);
        long stamp = fingerprint(folder);
        CachedFindings cached = findings.get(location.id());
        if (cached != null && cached.stamp() == stamp) {
            return cached.findings();
        }
        AiInsightsLandscapeExport export = new AiInsightsLandscapeExport();
        if (stamp != 0) {
            String reportsUrl = REPORTS_PATH + catalog.servedPath(location.reportsFolder());
            AiInsightsAggregator.addRepository(export, name, location.reportsFolder(), reportsUrl);
        }
        List<Api.Finding> list = export.getFindings().stream().map(this::finding).collect(Collectors.toList());
        findings.put(location.id(), new CachedFindings(stamp, list));
        return list;
    }

    // Scanners sometimes quote an author's email in the text: replaced by the pseudonym while people are hidden.
    private Api.Finding finding(AiFindingExport f) {
        return new Api.Finding(f.getRepo(), f.getScanner(), f.getId(), f.getGroup(), privacy.text(f.getTitle()), f.getSeverity(),
                f.getConfidence(), privacy.text(f.getDescription()), privacy.text(f.getRecommendation()), new ArrayList<>(f.getTags()),
                privacy.reportLink(f.getUrl()));
    }

    // Changes when a findings file is added, removed or rewritten; 0 without findings.
    private static long fingerprint(File folder) {
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            return 0;
        }
        long stamp = files.length;
        for (File file : files) {
            stamp = 31 * stamp + file.lastModified() + file.length();
        }
        return stamp == 0 ? 1 : stamp;
    }

    // ---- helpers ----

    static List<String> terms(String query) {
        if (StringUtils.isBlank(query)) {
            return List.of();
        }
        return Arrays.stream(query.split(";")).map(String::trim).filter(StringUtils::isNotBlank)
                .map(term -> term.toLowerCase(Locale.ROOT)).collect(Collectors.toList());
    }

    static <T> Api.Page<T> page(List<T> items, int offset, int limit) {
        int from = Math.max(0, Math.min(offset, items.size()));
        int to = Math.min(items.size(), from + clamp(limit, 50));
        return new Api.Page<>(items.size(), from, new ArrayList<>(items.subList(from, to)));
    }

    static int clamp(int limit, int defaultLimit) {
        return limit <= 0 ? defaultLimit : Math.min(limit, MAX_LIMIT);
    }

    static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    /** An unknown repository or landscape: 404 in the REST API, a tool error in MCP. */
    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
    }
}
