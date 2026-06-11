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
import java.util.List;
import java.util.Map;

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

    // The safe-email key used both as the zip entry name (<key>.json) and the ?key= URL param.
    public static String getContributorReportKey(String email) {
        return SystemUtils.getSafeFileName(email).toLowerCase();
    }

    public static final String PEOPLE_ZIP_FILE_NAME = "people.zip";
    public static final String TEAMS_ZIP_FILE_NAME = "teams.zip";
    public static final String CONTRIBUTOR_REPORT_FILE_NAME = "contributor-report.html";
    public static final String TEAM_REPORT_FILE_NAME = "team-report.html";

    // Routing set: the safe-email keys that belong to teams. Teams are rendered in a separate
    // team-report.html (with its own embedded archive) so neither file bundles everyone — a single
    // shared file grew very large on big landscapes. getContributorReportUrl consults this to send a
    // team's link to team-report.html and everyone else's to contributor-report.html. Seeded eagerly
    // from analysisResults.getTeams() (registerTeams) before any link is rendered, so routing never
    // depends on report-generation order.
    private static final java.util.Set<String> teamReportKeys = java.util.concurrent.ConcurrentHashMap.newKeySet();

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

    public static boolean isTeamKey(String key) {
        return teamReportKeys.contains(key);
    }

    // The relative URL to a person's page: team-report.html for teams, contributor-report.html for
    // contributors/bots. Both select the person by safe-email key from their own embedded archive.
    public static String getContributorReportUrl(String email) {
        String key = getContributorReportKey(email);
        String file = isTeamKey(key) ? TEAM_REPORT_FILE_NAME : CONTRIBUTOR_REPORT_FILE_NAME;
        return "contributors/" + file + "?key=" + key;
    }

    /**
     * Individual people pages used to be one self-contained HTML file each (thousands per large
     * landscape). They are now shared client-rendered pages whose data archive is embedded inline:
     * <b>contributors and bots</b> live in {@code contributors/contributor-report.html} (archive of
     * one JSON entry per person, keyed by safe email), and <b>teams</b> live in a separate
     * {@code contributors/team-report.html} — splitting them keeps neither file huge on big
     * landscapes. Each entry bundles everything the page needs ({@code data}, {@code langIcons},
     * {@code options}); teams are still flagged by {@code isTeam} in the data. A page opens as
     * {@code <contributor|team>-report.html?key=<safe-email>} (see {@link #getContributorReportUrl}).
     *
     * <p>This method is called once per group (contributors, then bots — both {@code isTeam=false};
     * teams from the teams tab — {@code isTeam=true}). Same-file groups (contributors + bots) merge
     * into a shared on-disk zip accumulator so the file holds all of them.
     */
    public List<RichTextReport> getIndividualReports(List<ContributorRepositories> contributors, boolean isTeam) {
        File individualReportsFolder = new File(reportsFolder, "contributors");
        individualReportsFolder.mkdirs();
        String zipName = isTeam ? TEAMS_ZIP_FILE_NAME : PEOPLE_ZIP_FILE_NAME;
        String reportFileName = isTeam ? TEAM_REPORT_FILE_NAME : CONTRIBUTOR_REPORT_FILE_NAME;
        File accumulatorZip = new File(individualReportsFolder, zipName);

        // Merge into the existing zip (contributors and bots run as two calls into the same
        // people.zip; keep entries from the prior call).
        Map<String, String> entriesByName = new LinkedHashMap<>();
        if (accumulatorZip.exists()) {
            ZipUtils.unzipAllEntriesAsStrings(accumulatorZip).forEach((name, entry) -> entriesByName.put(name, entry.getContent()));
        }
        contributors.forEach(contributor -> {
            String[] entry = buildEntry(contributor, individualReportsFolder);
            if (entry != null) {
                entriesByName.put(entry[0], entry[1]);
            }
        });
        String[][] entries = entriesByName.entrySet().stream()
                .map(e -> new String[]{e.getKey(), e.getValue()})
                .toArray(String[][]::new);
        // The zip persists across same-file per-group calls as the merge accumulator read back above
        // — kept on disk for that purpose only; the page no longer fetches it.
        ZipUtils.stringToZipFile(accumulatorZip, entries);

        // Write the shared template (same source for both files) with this group's archive embedded
        // inline as base64 (last same-file call wins and holds all its entries), so the page extracts
        // its ?key= person in-browser (no fetch) and opens from file://.
        try {
            String archiveB64 = VisualizationTemplate.base64(ZipUtils.stringEntriesToZipBytes(entries));
            java.io.InputStream in = this.getClass().getClassLoader().getResourceAsStream("templates/contributor-report.html");
            String template = org.apache.commons.io.IOUtils.toString(in, StandardCharsets.UTF_8)
                    .replace("${sokrates-unzip-lib}", VisualizationTemplate.embedZipLib())
                    .replace("${embedded-archive}", "var SOKRATES_ARCHIVE = \"" + archiveB64 + "\";");
            FileUtils.write(new File(individualReportsFolder, reportFileName), template, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOG.error(e);
        }

        // The pages are embedded in the template above; nothing is returned for the export pipeline.
        return new ArrayList<>();
    }

    // Builds the {key, json} zip entry for one person; the JSON bundles data + langIcons + options.
    private String[] buildEntry(ContributorRepositories contributorRepositories, File folder) {
        try {
            LandscapeConfiguration configuration = landscapeAnalysisResults.getConfiguration();
            PeopleConfig peopleConfig = landscapeAnalysisResults.getPeopleConfig();

            ContributorIndividualReportExport data =
                    new ContributorIndividualReportExport(contributorRepositories, configuration, peopleConfig, folder);

            // Language icons for the contributor's repositories, members and extensions.
            List<String> langs = new ArrayList<>();
            data.getRepositories().forEach(r -> langs.add(r.getLang()));
            data.getMembers().forEach(m -> langs.add(m.getLang()));
            data.getExtensions().forEach(e -> langs.add(e.getLang()));
            String langIconsJson = DataImageUtils.getLangDataImageMapJson(langs);

            Map<String, Object> options = new LinkedHashMap<>();
            options.put("latestCommitDate", landscapeAnalysisResults.getLatestCommitDate());
            options.put("commitsMaxYears", configuration.getCommitsMaxYears());
            options.put("breadcrumbsLabel", configuration.getMetadata().getName() + " / Contributors");
            options.put("indexUrl", "../index.html");
            options.put("avatarTeam", DataImageUtils.TEAM);
            options.put("avatarDeveloper", DataImageUtils.DEVELOPER);
            options.put("isTeam", !data.getMembers().isEmpty());

            // One self-sufficient payload per person: data + langIcons + options. langIcons is a
            // pre-built JSON object literal; embed it as raw JSON (not re-stringified).
            String payload = "{\"data\":" + new JsonGenerator().generateCompressed(data)
                    + ",\"langIcons\":" + (langIconsJson == null || langIconsJson.isEmpty() ? "{}" : langIconsJson)
                    + ",\"options\":" + new JsonGenerator().generateCompressed(options) + "}";

            String key = getContributorReportKey(contributorRepositories.getContributor().getEmail());
            return new String[]{key + ".json", payload};
        } catch (Exception e) {
            LOG.error(e);
            return null;
        }
    }
}
