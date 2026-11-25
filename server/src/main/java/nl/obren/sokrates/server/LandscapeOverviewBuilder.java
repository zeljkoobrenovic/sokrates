package nl.obren.sokrates.server;

import nl.obren.sokrates.server.api.Api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Sums the summaries of a set of repositories into an {@link Api.LandscapeOverview}. */
class LandscapeOverviewBuilder {
    static final int TOP = 5;
    static final int MIN_LINES_FOR_SCORE_RANKING = 1000;
    // An analysis older than this is flagged as stale: its "recent" numbers end that long before the question.
    static final int STALE_DAYS = 30;
    static final List<String> GRADES = List.of("A", "B", "C", "D", "E");
    static final List<String> SCOPES = List.of("main", "test", "generated", "build", "other");

    /** One repository: its list row, its details, its contributors, its AI findings per severity, and how its people are identified. */
    record Input(Api.RepositoryItem item, Api.RepositoryDetails details, List<Api.Contributor> contributors, Map<String, Integer> findings,
                 Function<Api.Contributor, String> identity) {
    }

    static Api.LandscapeOverview build(String landscape, String name, String description, String reportUrl,
                                       List<Input> inputs) {
        int withCode = 0;
        int active30 = 0;
        int active365 = 0;
        Map<String, int[]> scopes = new LinkedHashMap<>();
        SCOPES.forEach(scope -> scopes.put(scope, new int[2]));
        Map<String, int[]> languages = new HashMap<>(); // lines, repositories
        Map<String, Integer> humanGrades = grades();
        Map<String, Integer> aiGrades = grades();
        double humanWeighted = 0;
        double aiWeighted = 0;
        double duplicationWeighted = 0;
        long humanLines = 0;
        long aiLines = 0;
        long duplicationLines = 0;
        int commits30 = 0;
        int commits90 = 0;
        int commits365 = 0;
        Set<String> people30 = new HashSet<>();
        Set<String> people365 = new HashSet<>();
        Set<String> bots365 = new HashSet<>();
        Map<String, Integer> findings = new LinkedHashMap<>();
        SokratesQueryService.SEVERITIES.forEach(severity -> findings.put(severity, 0));

        for (Input input : inputs) {
            Api.RepositoryDetails details = input.details();
            int mainLines = input.item().mainLinesOfCode();
            if (mainLines > 0) {
                withCode++;
            }
            if (details.activity().commits30Days() > 0) {
                active30++;
            }
            if (details.activity().commits365Days() > 0) {
                active365++;
            }
            details.scopes().forEach(scope -> {
                int[] total = scopes.computeIfAbsent(scope.scope(), s -> new int[2]);
                total[0] += scope.files();
                total[1] += scope.linesOfCode();
            });
            details.mainLanguages().forEach(language -> {
                int[] total = languages.computeIfAbsent(language.language(), l -> new int[2]);
                total[0] += language.linesOfCode();
                total[1]++;
            });
            boolean rankable = mainLines >= MIN_LINES_FOR_SCORE_RANKING;
            if (details.humanScore() != null && mainLines > 0) {
                if (rankable) {
                    humanGrades.merge(details.humanScore().grade(), 1, Integer::sum);
                }
                humanWeighted += details.humanScore().value() * mainLines;
                humanLines += mainLines;
            }
            if (details.aiScore() != null && mainLines > 0) {
                if (rankable) {
                    aiGrades.merge(details.aiScore().grade(), 1, Integer::sum);
                }
                aiWeighted += details.aiScore().value() * mainLines;
                aiLines += mainLines;
            }
            if (details.duplicationPercentage() != null && mainLines > 0) {
                duplicationWeighted += details.duplicationPercentage() * mainLines;
                duplicationLines += mainLines;
            }
            commits30 += details.activity().commits30Days();
            commits90 += details.activity().commits90Days();
            commits365 += details.activity().commits365Days();
            for (Api.Contributor contributor : input.contributors()) {
                String id = input.identity().apply(contributor);
                if (id == null) {
                    continue;
                }
                if (contributor.bot()) {
                    if (contributor.commits365Days() > 0) {
                        bots365.add(id);
                    }
                    continue;
                }
                if (contributor.commits30Days() > 0) {
                    people30.add(id);
                }
                if (contributor.commits365Days() > 0) {
                    people365.add(id);
                }
            }
            input.findings().forEach((severity, count) -> findings.merge(severity, count, Integer::sum));
        }

        long totalMainLines = scopes.get("main")[1];
        List<Api.LanguageShare> languageShares = languages.entrySet().stream()
                .map(e -> new Api.LanguageShare(e.getKey(), e.getValue()[0],
                        totalMainLines == 0 ? 0 : SokratesQueryService.round(100.0 * e.getValue()[0] / totalMainLines), e.getValue()[1]))
                .sorted(Comparator.comparingInt(Api.LanguageShare::linesOfCode).reversed().thenComparing(Api.LanguageShare::language))
                .collect(Collectors.toList());
        List<Api.ScopeSize> scopeSizes = scopes.entrySet().stream()
                .map(e -> new Api.ScopeSize(e.getKey(), e.getValue()[0], e.getValue()[1])).collect(Collectors.toList());

        List<Api.RepositoryItem> items = inputs.stream().map(Input::item).collect(Collectors.toList());
        Comparator<Api.RepositoryItem> byName = Comparator.comparing(Api.RepositoryItem::name);
        List<Api.RepositoryItem> largest = top(items, Comparator.comparingInt(Api.RepositoryItem::mainLinesOfCode).reversed().thenComparing(byName),
                item -> item.mainLinesOfCode() > 0);
        List<Api.RepositoryItem> mostActive = top(items, Comparator.comparingInt(Api.RepositoryItem::commits30Days).reversed().thenComparing(byName),
                item -> item.commits30Days() > 0);
        List<Api.RepositoryItem> lowest = top(items, Comparator.comparingDouble((Api.RepositoryItem item) -> item.humanScore().value()).thenComparing(byName),
                item -> item.humanScore() != null && item.mainLinesOfCode() >= MIN_LINES_FOR_SCORE_RANKING);

        return new Api.LandscapeOverview(landscape, name, description, inputs.size(), withCode, active30, active365,
                scopeSizes, languageShares, humanGrades, aiGrades,
                humanLines == 0 ? null : SokratesQueryService.round(humanWeighted / humanLines),
                aiLines == 0 ? null : SokratesQueryService.round(aiWeighted / aiLines),
                duplicationLines == 0 ? null : SokratesQueryService.round(duplicationWeighted / duplicationLines),
                new Api.PortfolioActivity(commits30, commits90, commits365, people30.size(), people365.size(), bots365.size()),
                findings, largest, mostActive, MIN_LINES_FOR_SCORE_RANKING, lowest, reportUrl, dates(items));
    }

    private static Api.AnalysisDates dates(List<Api.RepositoryItem> items) {
        List<String> dates = items.stream().map(Api.RepositoryItem::analyzedOn).filter(d -> d != null && !d.isBlank()).sorted().collect(Collectors.toList());
        int stale = (int) items.stream().filter(item -> item.analysisAgeDays() != null && item.analysisAgeDays() > STALE_DAYS).count();
        return new Api.AnalysisDates(dates.isEmpty() ? null : dates.get(0), dates.isEmpty() ? null : dates.get(dates.size() - 1), stale, STALE_DAYS);
    }

    private static Map<String, Integer> grades() {
        Map<String, Integer> grades = new LinkedHashMap<>();
        GRADES.forEach(grade -> grades.put(grade, 0));
        return grades;
    }

    private static List<Api.RepositoryItem> top(List<Api.RepositoryItem> items, Comparator<Api.RepositoryItem> order,
                                                java.util.function.Predicate<Api.RepositoryItem> keep) {
        return new ArrayList<>(items.stream().filter(keep).sorted(order).limit(TOP).collect(Collectors.toList()));
    }
}
