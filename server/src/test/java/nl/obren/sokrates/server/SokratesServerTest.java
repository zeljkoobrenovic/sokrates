package nl.obren.sokrates.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.server.api.Api;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A synthetic landscape on disk (two repositories in the analyzeLandscape layout, one with AI findings, one loose
 * older-style analysis outside the landscape, plus a source file that must never be served) and every front-end:
 * the query service, the MCP handler and the HTTP server.
 */
class SokratesServerTest {
    @TempDir
    File root;
    // the columns the hotspots read (the real export has more; columns are found by header name)
    static final String HISTORY_HEADER = "path\t# lines of code\t# active days\t# commits\t# commits (30d)\t# commits (90d)\t# contributors\tlast updated";
    private SokratesQueryService service;
    private SokratesHttpServer http;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        File landscapeRoot = new File(root, "portfolio");
        File payments = repository(landscapeRoot, "acme/payments", "acme/payments", "Card payments", Map.of("java", 9000, "kt", 1000));
        File infra = repository(landscapeRoot, "acme/infra", "acme/infra", null, Map.of("tf", 2000, "py", 500));
        FileUtils.writeStringToFile(new File(landscapeRoot, "_sokrates_landscape/config-people.json"),
                "{\"people\": [{\"email\": \"ada@acme.com\", \"emailPatterns\": [\"ada@old-acme\\\\.com\"]}]}", StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(new File(landscapeRoot, "_sokrates_landscape/config.json"),
                "{\"metadata\": {\"name\": \"ACME\", \"description\": \"All of ACME\"}}", StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(new File(landscapeRoot, "_sokrates_landscape/index.html"), "<html></html>", StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(new File(root, "secret/Main.java"), "class Main {}", StandardCharsets.UTF_8);

        // an older analysis with loose data files (no data.zip), outside the landscape
        File loose = new File(root, "legacy/reports/data");
        FileUtils.writeStringToFile(new File(loose, "analysisResults.json"), analysis("legacy", null, Map.of("cs", 300)), StandardCharsets.UTF_8);

        // payments: two main files with history, and AI findings
        File paymentsData = new File(payments, "reports/data/data.zip");
        writeZip(paymentsData, Map.of(
                "analysisResults.json", analysis("acme/payments", "Card payments", Map.of("java", 9000, "kt", 1000), SokratesServerTest::peopleAndDependencies),
                "text/mainFileContributors.txt", "path\tcontributor\t# commits\t# commits (365d)\tfirst commit\tlast commit\n"
                        + "src/Ledger.java\tada@acme.com\t10\t6\t2024-02-01\t2026-11-01\n"
                        + "src/Ledger.java\tbob@acme.com\t30\t0\t2020-01-01\t2025-01-01\n"
                        + "src/Util.java\tada@acme.com\t2\t2\t2026-09-01\t2026-10-20\n"
                        + "src/Util.java\tbot@acme.com\t3\t3\t2026-01-01\t2026-10-01\n"
                        + "src/Util.java\tada@old-acme.com\t1\t1\t2025-12-01\t2025-12-01\n"
                        + "src/Old.java\tbob@acme.com\t12\t0\t2020-01-01\t2024-01-01\n",
                "files.json", "[{\"relativePath\":\"src/Ledger.java\",\"components\":[\"primary::core\"]},"
                        + "{\"relativePath\":\"src/Util.java\",\"components\":[\"primary::util\"]},"
                        + "{\"relativePath\":\"src/Old.java\",\"components\":[\"primary::core\"]}]",
                "mainFiles.json", "[{\"relativePath\":\"src/Ledger.java\",\"linesOfCode\":800,\"unitsMcCabeIndexSum\":90},"
                        + "{\"relativePath\":\"src/Util.java\",\"linesOfCode\":100,\"unitsMcCabeIndexSum\":10},"
                        + "{\"relativePath\":\"src/Old.java\",\"linesOfCode\":400,\"unitsMcCabeIndexSum\":200}]",
                "text/mainFilesWithHistory.txt", HISTORY_HEADER + "\t# active days (365d)\n"
                        + "src/Ledger.java\t800\t30\t40\t3\t5\t4\t2026-11-01\t6\n"
                        + "src/Util.java\t100\t8\t10\t1\t2\t1\t2026-10-20\t2\n"
                        + "src/Old.java\t400\t10\t12\t0\t0\t1\t2024-01-01\t0\n"));
        FileUtils.writeStringToFile(new File(payments, "reports/ai-insights/security-scan.json"),
                "{\"scanner\":\"security-scan\",\"analyzed_at\":\"2026-11-01T00:00:00Z\",\"findings\":["
                        + "{\"id\":\"security-scan/secrets/key\",\"group\":\"secrets\",\"title\":\"API key in source\",\"severity\":\"High\",\"description\":\"A key is committed\",\"tags\":[\"secrets\"]},"
                        + "{\"id\":\"security-scan/crypto/rng\",\"group\":\"crypto\",\"title\":\"Weak random\",\"severity\":\"low\",\"description\":\"java.util.Random for tokens, written by ada@acme.com\"}]}",
                StandardCharsets.UTF_8);
        FileUtils.writeStringToFile(new File(payments, "reports/index.html"), "<html>payments</html>", StandardCharsets.UTF_8);
        writeZip(new File(infra, "reports/data/data.zip"), Map.of(
                "analysisResults.json", analysis("acme/infra", null, Map.of("tf", 2000, "py", 500)),
                "text/mainFileContributors.txt", "path\tcontributor\t# commits\t# commits (365d)\tfirst commit\tlast commit\n"
                        + "main.tf\tada@old-acme.com\t20\t9\t2023-01-01\t2026-11-10\n"
                        + "main.tf\tcarol@acme.com\t4\t4\t2026-05-01\t2026-08-01\n"
                        + "scripts/deploy.py\tcarol@acme.com\t3\t1\t2025-01-01\t2026-02-01\n"));

        service = new SokratesQueryService(new AnalysisCatalog(root, 0));
    }

    @AfterEach
    void tearDown() {
        if (http != null) {
            http.stop();
        }
    }

    private File repository(File landscapeRoot, String folder, String name, String description, Map<String, Integer> languages) throws Exception {
        File analysisFolder = new File(landscapeRoot, folder);
        writeZip(new File(analysisFolder, "reports/data/data.zip"), Map.of("analysisResults.json", analysis(name, description, languages)));
        return analysisFolder;
    }

    private static String analysis(String name, String description, Map<String, Integer> languages) throws Exception {
        return analysis(name, description, languages, results -> {
        });
    }

    private static String analysis(String name, String description, Map<String, Integer> languages,
                                   java.util.function.Consumer<CodeAnalysisResults> customize) throws Exception {
        CodeAnalysisResults results = new CodeAnalysisResults();
        customize.accept(results);
        results.getMetadata().setName(name);
        results.getMetadata().setDescription(description);
        List<NumericMetric> perExtension = languages.entrySet().stream()
                .map(e -> new NumericMetric("  *." + e.getKey(), e.getValue())).collect(Collectors.toList());
        results.getMainAspectAnalysisResults().setLinesOfCodePerExtension(perExtension);
        results.getMainAspectAnalysisResults().setLinesOfCode(languages.values().stream().mapToInt(Integer::intValue).sum());
        return new JsonGenerator().generate(results);
    }

    private static void peopleAndDependencies(CodeAnalysisResults results) {
        results.setAnalysisDate("2026-01-01");
        results.getContributorsAnalysisResults().getContributors().add(contributor("ada@acme.com", 50, 30, false));
        results.getContributorsAnalysisResults().getContributors().add(contributor("bob@acme.com", 80, 5, false));
        results.getContributorsAnalysisResults().getContributors().add(contributor("bot@acme.com", 9, 9, true));
        // the same person as ada@acme.com, by the landscape's config-people.json only
        results.getContributorsAnalysisResults().getContributors().add(contributor("ada@old-acme.com", 3, 1, false));
        nl.obren.sokrates.sourcecode.analysis.results.LogicalDecompositionAnalysisResults primary =
                new nl.obren.sokrates.sourcecode.analysis.results.LogicalDecompositionAnalysisResults("primary");
        primary.setComponentDependencies(new java.util.ArrayList<>(List.of(
                dependency("db", "core", 2), dependency("core", "db", 7), dependency("api", "core", 4))));
        results.getLogicalDecompositionsAnalysisResults().add(primary);
    }

    private static nl.obren.sokrates.sourcecode.contributors.Contributor contributor(String email, int commits, int commits365, boolean bot) {
        nl.obren.sokrates.sourcecode.contributors.Contributor contributor = new nl.obren.sokrates.sourcecode.contributors.Contributor(email);
        contributor.setCommitsCount(commits);
        contributor.setCommitsCount365Days(commits365);
        contributor.setBot(bot);
        return contributor;
    }

    private static nl.obren.sokrates.sourcecode.dependencies.ComponentDependency dependency(String from, String to, int count) {
        nl.obren.sokrates.sourcecode.dependencies.ComponentDependency dependency = new nl.obren.sokrates.sourcecode.dependencies.ComponentDependency(from, to);
        dependency.setCount(count);
        return dependency;
    }

    private static void writeZip(File zip, Map<String, String> entries) throws Exception {
        zip.getParentFile().mkdirs();
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                out.putNextEntry(new ZipEntry(entry.getKey()));
                out.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
    }

    @Test
    void findsRepositoriesAndLandscapes() {
        Api.Page<Api.RepositoryItem> all = service.repositories(null, null, null, 0, 0);
        assertEquals(3, all.total());
        // largest first by default
        assertEquals(List.of("acme/payments", "acme/infra", "legacy"), all.items().stream().map(Api.RepositoryItem::name).collect(Collectors.toList()));
        assertEquals("portfolio/acme/payments", all.items().get(0).id());
        assertEquals("java", all.items().get(0).mainLanguage());
        assertEquals(2, all.items().get(0).aiFindings());

        List<Api.Landscape> landscapes = service.landscapes();
        assertEquals(1, landscapes.size());
        assertEquals("ACME", landscapes.get(0).name());
        assertEquals("portfolio", landscapes.get(0).id());
        assertEquals(2, landscapes.get(0).repositories());
        assertEquals(2, service.repositories(null, "portfolio", null, 0, 0).total());
        assertThrows(SokratesQueryService.NotFoundException.class, () -> service.repositories(null, "nope", null, 0, 0));
    }

    @Test
    void countsEachRepositoryNameOnceKeepingTheNewestAnalysis() throws Exception {
        // an older leftover copy of acme/infra in another folder of the landscape
        File copy = repository(new File(root, "portfolio"), "old/acme-infra", "acme/infra", "stale copy", Map.of("tf", 1));
        assertTrue(new File(copy, "reports/data/data.zip").setLastModified(1_000_000L));
        service = new SokratesQueryService(new AnalysisCatalog(root, 0));

        assertEquals(3, service.repositories(null, null, null, 0, 0).total());
        assertEquals(2, service.landscapes().get(0).repositories());
        Api.RepositoryDetails infra = service.repository("acme/infra").orElseThrow();
        assertEquals("portfolio/acme/infra", infra.id(), "by name: the newest analysis");
        assertEquals("stale copy", service.repository("portfolio/old/acme-infra").orElseThrow().description(), "still reachable by id");
        assertEquals(2, service.experts(null, "portfolio", null, null, null, null, 10).repositories());
        // the stale copy has no per-file contributors: counted, it would show up as not covered
        Api.Experts terraform = service.experts(null, "portfolio", null, null, null, "tf", 10);
        assertEquals(0, terraform.repositoriesNotCovered());
        assertEquals(1, terraform.files());
    }

    @Test
    void saysWhenEachAnalysisRan() {
        int age = (int) java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.parse("2026-01-01"), java.time.LocalDate.now());
        Api.RepositoryItem payments = service.repositories("payments", null, null, 0, 0).items().get(0);
        assertEquals("2026-01-01", payments.analyzedOn());
        assertEquals(age, payments.analysisAgeDays());
        assertEquals(age, service.repository("acme/payments").orElseThrow().analysisAgeDays());
        assertEquals("2026-01-01", service.hotspots("acme/payments", null, 10).analyzedOn());
        assertEquals("2026-01-01", service.experts("acme/payments", null, null, null, null, null, 10).analyzedOn());
        assertNull(service.experts(null, "portfolio", null, null, null, null, 10).analyzedOn(), "across repositories: see the overview");
        // an analysis without a stored date (older Sokrates, start time 0 in the fixture) has none
        assertNull(service.repository("acme/infra").orElseThrow().analyzedOn());

        Api.AnalysisDates dates = service.overview("portfolio").analyses();
        assertEquals("2026-01-01", dates.oldest());
        assertEquals(age > LandscapeOverviewBuilder.STALE_DAYS ? 1 : 0, dates.olderThanStaleDays());
    }

    @Test
    void anOlderAnalysisFallsBackToTheDayItRan() {
        CodeAnalysisResults results = new CodeAnalysisResults();
        results.setAnalysisStartTimeMs(java.time.LocalDate.parse("2025-03-04").atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() + 3_600_000);
        assertEquals("2025-03-04", SokratesQueryService.analyzedOn(results));
        results.setAnalysisDate("2025-02-01"); // -date
        assertEquals("2025-02-01", SokratesQueryService.analyzedOn(results));
        assertNull(SokratesQueryService.ageDays(null));
    }

    @Test
    void findsARepositoryByItsShortNameAndSuggestsNames() {
        assertEquals("acme/payments", service.repository("payments").orElseThrow().name());
        assertEquals("acme/infra", service.hotspots("INFRA", null, 10).repository());
        SokratesQueryService.NotFoundException miss = assertThrows(SokratesQueryService.NotFoundException.class,
                () -> service.hotspots("pay", null, 10));
        assertTrue(miss.getMessage().contains("did you mean: acme/payments"), miss.getMessage());
        assertTrue(service.notFound("zzz").contains("list_repositories"));
    }

    @Test
    void countsPeopleNotAddresses() {
        // payments: ada@acme.com + ada@old-acme.com (one person by the landscape), bob, and a bot
        Api.Activity activity = service.repository("acme/payments").orElseThrow().activity();
        assertEquals(2, activity.contributors());
    }

    @Test
    void filtersWithTheLandscapeSearchSyntax() {
        assertEquals(List.of("acme/payments"), names(service.repositories("mainLang:java,cs;payments", null, null, 0, 0)));
        assertEquals(List.of("acme/infra"), names(service.repositories("includesLang:py", null, null, 0, 0)));
        assertEquals(List.of("acme/payments", "legacy"), names(service.repositories("card;legacy", null, null, 0, 0)));
        assertEquals(List.of("acme/infra", "acme/payments", "legacy"), names(service.repositories(null, null, "name", 0, 0)));
        Api.Page<Api.RepositoryItem> page = service.repositories(null, null, "name", 1, 1);
        assertEquals(3, page.total());
        assertEquals(List.of("acme/payments"), names(page));
    }

    @Test
    void findsARepositoryByIdOrName() {
        assertEquals("Card payments", service.repository("ACME/Payments").orElseThrow().description());
        assertEquals("acme/payments", service.repository("portfolio/acme/payments").orElseThrow().name());
        assertTrue(service.repository("missing").isEmpty());
        Api.RepositoryDetails details = service.repository("acme/infra").orElseThrow();
        assertEquals("tf", details.mainLanguages().get(0).language());
        assertEquals("/reports/portfolio/acme/infra/reports/index.html", details.reportUrl());
    }

    @Test
    void ranksHotspotsLikeTheHighlightsTab() {
        Api.Hotspots hotspots = service.hotspots("acme/payments", null, 10);
        assertEquals("365 days", hotspots.window());
        assertTrue(hotspots.basis().contains("days changed in the past 365 days"), hotspots.basis());
        // Old.java is the most complex but did not change in the past year
        assertEquals(List.of("src/Ledger.java", "src/Util.java"), paths(hotspots));
        assertEquals(540, hotspots.items().get(0).score()); // 90 × 6 days
        assertEquals(6, hotspots.items().get(0).daysChanged365Days());

        assertEquals(450, service.hotspots("acme/payments", "90", 10).items().get(0).score()); // 90 × 5 commits
        Api.Hotspots allTime = service.hotspots("acme/payments", "all", 10);
        assertEquals(List.of("src/Ledger.java", "src/Old.java", "src/Util.java"), paths(allTime));
        assertEquals(2000, allTime.items().get(1).score()); // 200 × 10 days
        assertThrows(IllegalArgumentException.class, () -> service.hotspots("acme/payments", "week", 10));
    }

    @Test
    void olderAnalysesWithoutTheYearColumnFallBackToRecentCommits() {
        Map<String, Integer> complexity = Map.of("a.java", 10, "b.java", 50);
        Map<String, Integer> lines = Map.of("a.java", 100, "b.java", 300);
        List<HotspotData.FileHistory> history = HotspotData.readHistory(HISTORY_HEADER + "\n"
                + "a.java\t100\t4\t9\t1\t3\t1\t2026-11-01\n"
                + "b.java\t300\t20\t25\t0\t0\t2\t2025-01-01\n");
        Api.Hotspots hotspots = HotspotData.rank("old", complexity, lines, true, history, null, 10);
        assertEquals("90 days", hotspots.window());
        assertEquals(List.of("a.java"), paths(hotspots));
        assertNull(hotspots.items().get(0).daysChanged365Days());

        // nothing changed in the window: all time
        Api.Hotspots stale = HotspotData.rank("old", complexity, lines, true, HotspotData.readHistory(HISTORY_HEADER + "\n"
                + "b.java\t300\t20\t25\t0\t0\t2\t2025-01-01\n"), null, 10);
        assertEquals("all time", stale.window());
        assertEquals(1000, stale.items().get(0).score()); // 50 × 20 days
    }

    private static List<String> paths(Api.Hotspots hotspots) {
        return hotspots.items().stream().map(Api.Hotspot::path).collect(Collectors.toList());
    }

    @Test
    void listsContributorsMostActiveInThePastYearFirst() {
        Api.Page<Api.Contributor> people = service.contributors("acme/payments", null, false, 0, 0);
        assertEquals(List.of("ada@acme.com", "bob@acme.com"), people.items().stream().map(Api.Contributor::email).collect(Collectors.toList()));
        assertEquals(3, service.contributors("acme/payments", null, true, 0, 0).total());
        assertEquals(1, service.contributors("acme/payments", "bob", false, 0, 0).total());
    }

    @Test
    void findsWhoKnowsAFileFolderOrComponent() {
        Api.Experts core = service.experts("acme/payments", null, null, "core", null, null, 10);
        assertEquals(2, core.files());
        assertEquals(1, core.filesChanged365Days());
        assertEquals(List.of("ada@acme.com", "bob@acme.com"), experts(core));
        assertEquals(42, core.items().get(1).fileChanges()); // bob: 30 + 12
        assertEquals(100.0, core.topContributorShare365Days());
        assertEquals(2, service.experts("acme/payments", null, null, "primary::core", null, null, 10).files());
        assertThrows(SokratesQueryService.NotFoundException.class, () -> service.experts("acme/payments", null, null, "core", "other", null, 10));

        Api.Experts folder = service.experts("acme/payments", null, "src/", null, null, null, 10);
        assertEquals(3, folder.files());
        assertEquals(List.of("ada@acme.com", "bot@acme.com", "bob@acme.com"), experts(folder));
        assertTrue(folder.items().get(1).bot());
        assertEquals(100.0, folder.topContributorShare365Days(), "bots do not count in the share");

        assertEquals(List.of("ada@acme.com", "bob@acme.com"), experts(service.experts("acme/payments", null, "src/Ledger.java", null, null, null, 10)));
        assertThrows(SokratesQueryService.NotFoundException.class, () -> service.experts("acme/payments", null, "lib", null, null, null, 10));
        // an analysis without the per-file contributors export
        assertThrows(IllegalArgumentException.class, () -> service.experts("legacy", null, null, null, null, null, 10));
    }

    @Test
    void mergesARepositorysPeopleByItsLandscapesRules() throws Exception {
        // payments is in the portfolio landscape, whose config-people.json makes ada@old-acme.com ada@acme.com
        Api.Page<Api.Contributor> people = service.contributors("acme/payments", null, false, 0, 0);
        assertEquals(List.of("ada@acme.com", "bob@acme.com"), people.items().stream().map(Api.Contributor::email).collect(Collectors.toList()));
        assertEquals(53, people.items().get(0).commits()); // 50 + 3
        assertEquals(31, people.items().get(0).commits365Days());
        Api.Experts util = service.experts("acme/payments", null, "src/Util.java", null, null, null, 10);
        assertEquals(List.of("ada@acme.com", "bot@acme.com"), experts(util));
        assertEquals(3, util.items().get(0).fileChanges()); // 2 + 1 under the old address

        // a landscape at the root does not take over: the deepest landscape holding the repository decides
        FileUtils.writeStringToFile(new File(root, "_sokrates_landscape/config.json"), "{\"metadata\": {\"name\": \"Root\"}}", StandardCharsets.UTF_8);
        service = new SokratesQueryService(new AnalysisCatalog(root, 0));
        assertEquals(2, service.contributors("acme/payments", null, false, 0, 0).total());
        // and the root landscape's (empty) rules apply to the analysis outside portfolio
        assertEquals(0, service.contributors("legacy", null, false, 0, 0).total());
    }

    @Test
    void findsWhoKnowsAcrossALandscapeWithItsMergedIdentities() {
        Api.Experts all = service.experts(null, "portfolio", null, null, null, null, 10);
        assertEquals("portfolio", all.repository());
        assertEquals(2, all.repositories());
        assertEquals(0, all.repositoriesNotCovered());
        Api.Expert ada = all.items().get(0);
        // ada@old-acme.com (infra) is ada@acme.com (payments) by the landscape's config-people.json
        assertEquals("ada@acme.com", ada.contributor());
        assertEquals(2, ada.repositories());
        assertEquals(List.of("acme/infra", "acme/payments"), ada.topRepositories()); // 9 vs 8 changes in the past year
        assertEquals(18, ada.fileChanges365Days());

        Api.Experts terraform = service.experts(null, "portfolio", null, null, null, "tf", 10);
        assertEquals(1, terraform.files());
        assertEquals(List.of("ada@acme.com", "carol@acme.com"), experts(terraform));
        assertEquals(69.2, terraform.topContributorShare365Days()); // 9 of 13

        // without a landscape: every analysis, each repository's people by the landscape it belongs to (infra's
        // ada@old-acme.com is ada@acme.com by portfolio's rules); the legacy analysis is not covered
        Api.Experts everything = service.experts(null, null, null, null, null, ".TF, py", 10);
        assertEquals("all", everything.repository());
        assertEquals(1, everything.repositoriesNotCovered());
        assertEquals(List.of("ada@acme.com", "carol@acme.com"), experts(everything));
        assertThrows(SokratesQueryService.NotFoundException.class, () -> service.experts(null, "portfolio", null, null, null, "rs", 10));

        // the same person, the same pseudonym, in a repository and across the landscape
        SokratesQueryService hidden = new SokratesQueryService(new AnalysisCatalog(root, 0), PeoplePrivacy.hidden("k"));
        assertEquals(hidden.contributors("acme/payments", null, false, 0, 0).items().get(0).email(),
                hidden.experts(null, "portfolio", null, null, null, "tf", 10).items().get(0).contributor());
    }

    @Test
    void summarizesALandscape() {
        Api.LandscapeOverview overview = service.overview("portfolio");
        assertEquals("ACME", overview.name());
        assertEquals(2, overview.repositories());
        assertEquals(2, overview.repositoriesWithCode());
        assertEquals(12500, overview.scopes().get(0).linesOfCode()); // main: 10,000 + 2,500
        assertEquals(List.of("java", "tf", "kt", "py"), overview.mainLanguages().stream().map(Api.LanguageShare::language).collect(Collectors.toList()));
        assertEquals(72.0, overview.mainLanguages().get(0).percentage());
        assertEquals(2, overview.activity().contributors365Days()); // ada and bob; the bot apart
        assertEquals(1, overview.activity().bots365Days());
        assertEquals(1, overview.aiFindingsBySeverity().get("high"));
        assertEquals(1, overview.aiFindingsBySeverity().get("low"));
        assertEquals(List.of("acme/payments", "acme/infra"), overview.largest().stream().map(Api.RepositoryItem::name).collect(Collectors.toList()));
        assertNull(overview.averageHumanScore(), "no scores in the fixture");
        assertEquals("/reports/portfolio/_sokrates_landscape/index.html", overview.reportUrl());

        Api.LandscapeOverview all = service.overview(null);
        assertEquals("all", all.landscape());
        assertEquals(3, all.repositories());
        assertThrows(SokratesQueryService.NotFoundException.class, () -> service.overview("nope"));
    }

    @Test
    void listsComponentDependenciesWithCycles() {
        Api.Dependencies dependencies = service.dependencies("acme/payments", null, null);
        assertEquals("primary", dependencies.decomposition());
        assertEquals(List.of("core->db", "api->core", "db->core"), dependencies.items().stream()
                .map(d -> d.from() + "->" + d.to()).collect(Collectors.toList()));
        assertTrue(dependencies.items().get(0).cyclic());
        assertFalse(dependencies.items().get(1).cyclic());
        assertEquals(1, dependencies.cyclicPairs());
        assertEquals(1, service.dependencies("acme/payments", "PRIMARY", "api").items().size());
        assertThrows(SokratesQueryService.NotFoundException.class, () -> service.dependencies("acme/payments", "layers", null));
        assertThrows(SokratesQueryService.NotFoundException.class, () -> service.dependencies("acme/infra", null, null));
    }

    @Test
    void hidePeoplePseudonymizesEveryIdentityAndDropsReportLinks() throws Exception {
        SokratesQueryService hidden = new SokratesQueryService(new AnalysisCatalog(root, 0), PeoplePrivacy.hidden("test-key"));
        Api.Page<Api.Contributor> people = hidden.contributors("acme/payments", null, true, 0, 0);
        String ada = people.items().get(0).email();
        assertTrue(ada.matches("person-[0-9a-f]{10}"), ada);
        assertTrue(people.items().stream().allMatch(c -> c.userName() == null && !c.email().contains("@")));
        // the same person under the same pseudonym in every answer; stable for a given key
        assertEquals(ada, hidden.experts("acme/payments", null, null, "core", null, null, 10).items().get(0).contributor());
        assertEquals(ada, PeoplePrivacy.hidden("test-key").person("ADA@acme.com"));
        assertNotEquals(ada, PeoplePrivacy.hidden("other-key").person("ada@acme.com"));
        // no probing by email; the pseudonym itself can be searched
        assertEquals(0, hidden.contributors("acme/payments", "ada@acme.com", true, 0, 0).total());
        assertEquals(1, hidden.contributors("acme/payments", ada, true, 0, 0).total());

        Api.Finding weakRandom = hidden.findings("acme/payments", null, "random", null, null, 0, 0).items().get(0);
        assertEquals("java.util.Random for tokens, written by " + ada, weakRandom.description());
        assertNull(weakRandom.url());
        assertNull(hidden.repository("acme/payments").orElseThrow().reportUrl());
        assertNull(hidden.landscapes().get(0).reportUrl());

        http = new SokratesHttpServer(hidden, new McpHandler(hidden, "test"), null);
        http.start("127.0.0.1", 0);
        String base = "http://127.0.0.1:" + http.getPort();
        assertEquals(403, get(base + "/reports/portfolio/acme/payments/reports/index.html", null).statusCode());
        assertTrue(mapper.readTree(get(base + "/api/v1", null).body()).path("peopleHidden").asBoolean());
        assertFalse(get(base + "/api/v1/repository/contributors?name=acme/payments&includeBots=true", null).body().contains("@"));
    }

    @Test
    void peopleAreVisibleByDefault() {
        assertEquals("ada@acme.com", service.contributors("acme/payments", null, false, 0, 0).items().get(0).email());
        assertEquals("java.util.Random for tokens, written by ada@acme.com",
                service.findings("acme/payments", null, "random", null, null, 0, 0).items().get(0).description());
    }

    private static List<String> experts(Api.Experts experts) {
        return experts.items().stream().map(Api.Expert::contributor).collect(Collectors.toList());
    }

    @Test
    void searchesFindings() {
        Api.Page<Api.Finding> all = service.findings(null, null, null, null, null, 0, 0);
        assertEquals(2, all.total());
        assertEquals("high", all.items().get(0).severity()); // normalized from "High", most severe first
        assertTrue(all.items().get(0).url().startsWith("/reports/portfolio/acme/payments/reports/ai-insights/index.html#"));
        assertEquals(1, service.findings(null, null, null, "medium", null, 0, 0).total());
        assertEquals(List.of("Weak random"), service.findings("acme/payments", null, "random", null, null, 0, 0)
                .items().stream().map(Api.Finding::title).collect(Collectors.toList()));
        assertEquals(0, service.findings(null, null, null, null, "architecture-scan", 0, 0).total());
        assertThrows(IllegalArgumentException.class, () -> service.findings(null, null, null, "urgent", null, 0, 0));
    }

    @Test
    void mcpListsAndCallsTools() throws Exception {
        McpHandler mcp = new McpHandler(service, "test");
        JsonNode init = mapper.readTree(mcp.handle("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{\"protocolVersion\":\"2025-03-26\"}}"));
        assertEquals("2025-03-26", init.path("result").path("protocolVersion").asText());
        assertNull(mcp.handle("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}"));

        JsonNode tools = mapper.readTree(mcp.handle("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}"));
        assertEquals(9, tools.path("result").path("tools").size());

        JsonNode call = mapper.readTree(mcp.handle("{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":"
                + "{\"name\":\"get_hotspots\",\"arguments\":{\"repository\":\"acme/payments\",\"limit\":1}}}"));
        assertFalse(call.path("result").path("isError").asBoolean());
        JsonNode hotspots = mapper.readTree(call.path("result").path("content").get(0).path("text").asText());
        assertEquals("src/Ledger.java", hotspots.path("items").get(0).path("path").asText());

        JsonNode missing = mapper.readTree(mcp.handle("{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/call\",\"params\":"
                + "{\"name\":\"get_repository\",\"arguments\":{\"repository\":\"nope\"}}}"));
        assertTrue(missing.path("result").path("isError").asBoolean());

        JsonNode unknown = mapper.readTree(mcp.handle("{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"resources/list\"}"));
        assertEquals(-32601, unknown.path("error").path("code").asInt());
        assertEquals(-32700, mapper.readTree(mcp.handle("{not json")).path("error").path("code").asInt());
    }

    @Test
    void servesTheApiMcpAndOnlyReportFilesWithAToken() throws Exception {
        http = new SokratesHttpServer(service, new McpHandler(service, "test"), "s3cret");
        http.start("127.0.0.1", 0);
        String base = "http://127.0.0.1:" + http.getPort();

        assertEquals(401, get(base + "/api/v1/repositories", null).statusCode());
        HttpResponse<String> list = get(base + "/api/v1/repositories?query=includesLang%3Atf", "s3cret");
        assertEquals(200, list.statusCode());
        assertEquals("acme/infra", mapper.readTree(list.body()).path("items").get(0).path("name").asText());
        assertEquals(404, get(base + "/api/v1/repository?name=nope", "s3cret").statusCode());
        assertEquals(400, get(base + "/api/v1/repository", "s3cret").statusCode());

        assertEquals(200, get(base + "/reports/portfolio/acme/payments/reports/index.html", "s3cret").statusCode());
        assertEquals(200, get(base + "/reports/portfolio/_sokrates_landscape/", "s3cret").statusCode());
        assertEquals(404, get(base + "/reports/secret/Main.java", "s3cret").statusCode());
        assertEquals(404, get(base + "/reports/portfolio/acme/payments/reports/../../../../secret/Main.java", "s3cret").statusCode());

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> mcp = client.send(HttpRequest.newBuilder(URI.create(base + "/mcp"))
                .header("Authorization", "Bearer s3cret").header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}")).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, mcp.statusCode());
        assertEquals(1, mapper.readTree(mcp.body()).path("id").asInt());
        HttpResponse<String> notification = client.send(HttpRequest.newBuilder(URI.create(base + "/mcp"))
                .header("Authorization", "Bearer s3cret")
                .POST(HttpRequest.BodyPublishers.ofString("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}")).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(202, notification.statusCode());
    }

    private static HttpResponse<String> get(String url, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url)).GET();
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static List<String> names(Api.Page<Api.RepositoryItem> page) {
        return page.items().stream().map(Api.RepositoryItem::name).collect(Collectors.toList());
    }
}
