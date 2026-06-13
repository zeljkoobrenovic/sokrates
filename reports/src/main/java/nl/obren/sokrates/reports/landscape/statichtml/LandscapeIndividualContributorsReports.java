package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.renderingutils.VisualizationTemplate;
import nl.obren.sokrates.common.utils.SystemUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.landscape.data.ContributorIndividualReportExport;
import nl.obren.sokrates.reports.utils.DataImageUtils;
import nl.obren.sokrates.reports.utils.ZipUtils;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.landscape.PeopleConfig;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorRepositories;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates the per-contributor (and per-team) individual reports as client-rendered HTML
 * pages from the {@code contributor-report.html} template: a header, an extensions chart,
 * searchable Per Year / Per Month / Per Week repository-activity grids, and (for teams) a
 * searchable Members table — all rendered in the browser from embedded JSON.
 *
 * <p>The pages are written straight into the {@code contributors/} folder; unlike the old
 * implementation they are not {@link RichTextReport}s, so {@link #getIndividualReports} returns
 * an empty list (nothing for the caller's export loop to write).
 */
public class LandscapeIndividualContributorsReports {
    private static final Log LOG = LogFactory.getLog(LandscapeIndividualContributorsReports.class);

    private final LandscapeAnalysisResults landscapeAnalysisResults;
    private final File reportsFolder;

    public LandscapeIndividualContributorsReports(LandscapeAnalysisResults landscapeAnalysisResults, File reportsFolder) {
        this.landscapeAnalysisResults = landscapeAnalysisResults;
        this.reportsFolder = reportsFolder;
    }

    // The safe-email key used both as the zip entry name (<key>.json) and the URL fragment (#key).
    public static String getContributorReportKey(String email) {
        return SystemUtils.getSafeFileName(email).toLowerCase();
    }

    public static final String PEOPLE_ZIP_FILE_NAME = "people.zip";
    public static final String PEOPLE_ALL_ZIP_FILE_NAME = "people-all.zip";
    public static final String TEAMS_ZIP_FILE_NAME = "teams.zip";
    public static final String CONTRIBUTOR_REPORT_FILE_NAME = "contributor-report.html";
    public static final String CONTRIBUTOR_REPORT_ALL_FILE_NAME = "contributor-report-all.html";
    public static final String TEAM_REPORT_FILE_NAME = "team-report.html";

    // Routing sets: the safe-email keys that belong to teams, and the keys of RECENT (last-30-days)
    // contributors. Each kind of page embeds its own archive inline so no single file bundles
    // everyone — on big landscapes one shared file grew huge (e.g. ~60 MB for 5000 developers).
    // getContributorReportUrl sends a team's link to team-report.html; a recent contributor's link
    // to contributor-report.html (small, the common case); everyone else (non-recent contributors +
    // bots) to contributor-report-all.html (the big, rarely-opened file). Seeded eagerly
    // (registerTeams / registerRecentContributors) before any link is rendered, so routing never
    // depends on report-generation order.
    private static final java.util.Set<String> teamReportKeys = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final java.util.Set<String> recentContributorKeys = java.util.concurrent.ConcurrentHashMap.newKeySet();

    // Records which safe-email keys are teams (call early, e.g. when the report generator is built).
    // Replaces the prior set so a team in one landscape doesn't leak into another (virtual/sub-
    // landscapes share the JVM); a stale team key would otherwise route to a team-report.html whose
    // teams.zip lacks that entry.
    public static void registerTeams(List<ContributorRepositories> teams) {
        teamReportKeys.clear();
        if (teams == null) {
            return;
        }
        teams.forEach(t -> teamReportKeys.add(getContributorReportKey(t.getContributor().getEmail())));
    }

    // Records which safe-email keys are RECENT contributors (a commit in the last 30 days — the same
    // rule the landscape page uses, getCommitsCount30Days() > 0). Replaces the prior set per
    // landscape (see registerTeams) so a stale recent key never routes to a contributor-report.html
    // whose people.zip lacks that entry.
    public static void registerRecentContributors(List<ContributorRepositories> contributors) {
        recentContributorKeys.clear();
        if (contributors == null) {
            return;
        }
        contributors.stream()
                .filter(LandscapeIndividualContributorsReports::isRecent)
                .forEach(c -> recentContributorKeys.add(getContributorReportKey(c.getContributor().getEmail())));
    }

    // Recent = a commit in the last 30 days (matches the landscape's getRecentContributors).
    static boolean isRecent(ContributorRepositories contributor) {
        return contributor.getContributor().getCommitsCount30Days() > 0;
    }

    public static boolean isTeamKey(String key) {
        return teamReportKeys.contains(key);
    }

    public static boolean isRecentKey(String key) {
        return recentContributorKeys.contains(key);
    }

    // The relative URL to a person's page when the caller does NOT know whether the email is a team
    // (e.g. a member link). Falls back to the team-email set to decide. Prefer
    // getContributorReportUrl(email, isTeam) at sites that know the type — team and contributor keys
    // share one namespace, so the set-based guess can mis-route a contributor whose safe-email key
    // collides with a team name.
    public static String getContributorReportUrl(String email) {
        return getContributorReportUrl(email, isTeamKey(getContributorReportKey(email)));
    }

    // The relative URL to a person's page: team-report.html for teams; contributor-report.html for
    // recent contributors (small inline archive, the common case); contributor-report-all.html for
    // everyone else (non-recent contributors + bots). Each page selects the person by safe-email key
    // from its own embedded archive.
    public static String getContributorReportUrl(String email, boolean isTeam) {
        String key = getContributorReportKey(email);
        String file;
        if (isTeam) {
            file = TEAM_REPORT_FILE_NAME;
        } else if (isRecentKey(key)) {
            file = CONTRIBUTOR_REPORT_FILE_NAME;
        } else {
            file = CONTRIBUTOR_REPORT_ALL_FILE_NAME;
        }
        // The key goes in the URL fragment (#), not the query string (?): the fragment is not part
        // of the browser's HTTP cache key, so the shared report HTML is fetched ONCE and reused for
        // every person — with "?key=" each person was a distinct URL and re-fetched the whole page.
        return "contributors/" + file + "#" + key;
    }

    /**
     * Individual people pages used to be one self-contained HTML file each (thousands per large
     * landscape). They are now shared client-rendered pages whose data archive is embedded inline:
     * <b>contributors and bots</b> live in {@code contributors/contributor-report.html} (archive of
     * one JSON entry per person, keyed by safe email), and <b>teams</b> live in a separate
     * {@code contributors/team-report.html} — splitting them keeps neither file huge on big
     * landscapes. Each entry bundles everything the page needs ({@code data}, {@code langIcons},
     * {@code options}); teams are still flagged by {@code isTeam} in the data. A page opens as
     * {@code <contributor|team>-report.html#<safe-email>} (see {@link #getContributorReportUrl}).
     *
     * <p>This method is called once per group (contributors, then bots — both {@code isTeam=false};
     * teams from the teams tab — {@code isTeam=true}). Same-file groups (contributors + bots) merge
     * into a shared on-disk zip accumulator so the file holds all of them.
     */
    public List<RichTextReport> getIndividualReports(List<ContributorRepositories> contributors, boolean isTeam) {
        File folder = new File(reportsFolder, "contributors");
        folder.mkdirs();

        if (isTeam) {
            // Teams: one team-report.html (own archive); no recent/all split.
            writeReport(folder, contributors, TEAMS_ZIP_FILE_NAME, TEAM_REPORT_FILE_NAME);
            return new ArrayList<>();
        }

        // Contributors (and bots) split into two self-contained files so the default page stays
        // small on big landscapes: recent (last-30-days) contributors → contributor-report.html;
        // EVERYONE (recent + non-recent + bots) → contributor-report-all.html (the big, rarely
        // opened file). Bots are never recent, so they only ever land in the all-time file. Both the
        // contributors call and the bots call run with isTeam=false and merge into these same files.
        List<ContributorRepositories> recent = contributors.stream()
                .filter(LandscapeIndividualContributorsReports::isRecent)
                .collect(Collectors.toList());
        writeReport(folder, recent, PEOPLE_ZIP_FILE_NAME, CONTRIBUTOR_REPORT_FILE_NAME);
        writeReport(folder, contributors, PEOPLE_ALL_ZIP_FILE_NAME, CONTRIBUTOR_REPORT_ALL_FILE_NAME);

        // The pages are embedded in the templates above; nothing is returned for the export pipeline.
        return new ArrayList<>();
    }

    // Builds one self-contained report file: merges {@code people}'s entries into {@code zipName}
    // (the on-disk merge accumulator, so same-file groups like contributors + bots accumulate across
    // calls), then writes {@code reportFileName} from the shared template with that merged archive
    // embedded inline as base64 (the page extracts its #key person in-browser — no fetch, file://).
    // Reserved zip entry (not a person) that persists the union of language keys used by everyone in
    // this file across the merge-accumulator calls, so the shared langIcons map can be rebuilt to
    // cover all merged people. Stripped from the page's embedded archive.
    private static final String LANGS_ENTRY_NAME = "__langs.json";

    private void writeReport(File folder, List<ContributorRepositories> people, String zipName, String reportFileName) {
        File accumulatorZip = new File(folder, zipName);

        Map<String, String> entriesByName = new LinkedHashMap<>();
        if (accumulatorZip.exists()) {
            ZipUtils.unzipAllEntriesAsStrings(accumulatorZip).forEach((name, entry) -> entriesByName.put(name, entry.getContent()));
        }

        // Accumulated language keys (seeded from a prior call's reserved entry, if any).
        Set<String> langKeys = new LinkedHashSet<>(readLangKeys(entriesByName.remove(LANGS_ENTRY_NAME)));

        people.forEach(contributor -> {
            String[] entry = buildEntry(contributor, folder, langKeys);
            if (entry != null) {
                entriesByName.put(entry[0], entry[1]);
            }
        });

        // Persist the merged person entries + the reserved lang-keys entry to the accumulator zip
        // (so a later same-file call sees them); but the page's embedded archive holds only the
        // person entries — the icons are injected separately as one shared map.
        Map<String, String> personEntries = new LinkedHashMap<>(entriesByName);
        Map<String, String> zipEntries = new LinkedHashMap<>(entriesByName);
        zipEntries.put(LANGS_ENTRY_NAME, writeLangKeys(langKeys));
        ZipUtils.stringToZipFile(accumulatorZip, toArray(zipEntries));

        try {
            String archiveB64 = VisualizationTemplate.base64(ZipUtils.stringEntriesToZipBytes(toArray(personEntries)));
            String langIconsJson = DataImageUtils.getLangDataImageMapJson(langKeys);
            if (langIconsJson == null || langIconsJson.isEmpty()) {
                langIconsJson = "{}";
            }
            java.io.InputStream in = this.getClass().getClassLoader().getResourceAsStream("templates/contributor-report.html");
            String template = org.apache.commons.io.IOUtils.toString(in, StandardCharsets.UTF_8)
                    .replace("${sokrates-unzip-lib}", VisualizationTemplate.embedZipLib())
                    .replace("${embedded-archive}", "var SOKRATES_ARCHIVE = \"" + archiveB64 + "\";")
                    .replace("${lang-icons}", "var SOKRATES_LANG_ICONS = " + langIconsJson + ";");
            FileUtils.write(new File(folder, reportFileName), template, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOG.error(e);
        }
    }

    private static String[][] toArray(Map<String, String> entries) {
        return entries.entrySet().stream()
                .map(e -> new String[]{e.getKey(), e.getValue()})
                .toArray(String[][]::new);
    }

    private static List<String> readLangKeys(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static String writeLangKeys(Set<String> keys) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(keys);
        } catch (Exception e) {
            return "[]";
        }
    }

    // Builds one person's zip entry. The per-person JSON bundles only {data, options} — the language
    // icons (large, identical base64 data URIs shared by everyone) are NOT repeated per person; the
    // lang keys this person uses are added to {@code langSink} so the page can carry ONE shared
    // langIcons map (see writeReport). Returns {entryName, json} or null on failure.
    private String[] buildEntry(ContributorRepositories contributorRepositories, File folder, Set<String> langSink) {
        try {
            LandscapeConfiguration configuration = landscapeAnalysisResults.getConfiguration();
            PeopleConfig peopleConfig = landscapeAnalysisResults.getPeopleConfig();

            ContributorIndividualReportExport data =
                    new ContributorIndividualReportExport(contributorRepositories, configuration, peopleConfig, folder);

            // Collect the languages this person references (repositories, members, extensions) into
            // the shared sink; the icons themselves are resolved once, file-wide, in writeReport.
            data.getRepositories().forEach(r -> addLang(langSink, r.getLang()));
            data.getMembers().forEach(m -> addLang(langSink, m.getLang()));
            data.getExtensions().forEach(e -> addLang(langSink, e.getLang()));

            Map<String, Object> options = new LinkedHashMap<>();
            options.put("latestCommitDate", landscapeAnalysisResults.getLatestCommitDate());
            options.put("commitsMaxYears", configuration.getCommitsMaxYears());
            options.put("breadcrumbsLabel", configuration.getMetadata().getName() + " / Contributors");
            options.put("indexUrl", "../index.html");
            options.put("avatarTeam", DataImageUtils.TEAM);
            options.put("avatarDeveloper", DataImageUtils.DEVELOPER);
            options.put("isTeam", !data.getMembers().isEmpty());

            // Per-person payload: data + options only (langIcons is shared at the page level).
            String payload = "{\"data\":" + new JsonGenerator().generateCompressed(data)
                    + ",\"options\":" + new JsonGenerator().generateCompressed(options) + "}";

            String key = getContributorReportKey(contributorRepositories.getContributor().getEmail());
            return new String[]{key + ".json", payload};
        } catch (Exception e) {
            LOG.error(e);
            return null;
        }
    }

    private static void addLang(Set<String> sink, String lang) {
        if (lang != null) {
            String key = lang.toLowerCase().trim();
            if (!key.isEmpty()) {
                sink.add(key);
            }
        }
    }
}
