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

    /**
     * Individual people pages used to be one self-contained HTML file each (thousands per large
     * landscape). They are now a single shared {@code contributors/contributor-report.html}
     * template plus one {@code contributors/people.zip} holding one JSON entry per person, keyed by
     * the safe email (contributors, teams and bots all live in this one zip — teams are identified
     * by {@code isTeam} in the data). Each entry bundles everything the page needs ({@code data},
     * {@code langIcons}, {@code options}) so the template is fully static; a person's page opens as
     * {@code contributor-report.html?key=<safe-email>}.
     *
     * <p>This method is called once per group (contributors, then bots; teams from the teams tab);
     * each call merges its people into the shared {@code people.zip}.
     */
    public List<RichTextReport> getIndividualReports(List<ContributorRepositories> contributors) {
        File individualReportsFolder = new File(reportsFolder, "contributors");
        individualReportsFolder.mkdirs();
        File peopleZip = new File(individualReportsFolder, PEOPLE_ZIP_FILE_NAME);

        // Merge into the existing zip (this method runs once per group; keep entries from prior groups).
        Map<String, String> entriesByName = new LinkedHashMap<>();
        if (peopleZip.exists()) {
            ZipUtils.unzipAllEntriesAsStrings(peopleZip).forEach((name, entry) -> entriesByName.put(name, entry.getContent()));
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
        ZipUtils.stringToZipFile(peopleZip, entries);

        // Write the shared static template once (idempotent across the contributor/team/bot groups).
        try {
            java.io.InputStream in = this.getClass().getClassLoader().getResourceAsStream("templates/contributor-report.html");
            String template = org.apache.commons.io.IOUtils.toString(in, StandardCharsets.UTF_8)
                    .replace("${sokrates-inflate-lib}", VisualizationTemplate.inflateLib());
            FileUtils.write(new File(individualReportsFolder, "contributor-report.html"), template, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOG.error(e);
        }

        // The pages are zipped above; nothing is returned for the export pipeline.
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
