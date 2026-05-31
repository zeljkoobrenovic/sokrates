package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.renderingutils.ExplorerTemplate;
import nl.obren.sokrates.common.utils.SystemUtils;
import nl.obren.sokrates.reports.core.RichTextReport;
import nl.obren.sokrates.reports.landscape.data.ContributorIndividualReportExport;
import nl.obren.sokrates.reports.utils.DataImageUtils;
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
import java.util.HashMap;
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

    public static String getContributorIndividualReportFileName(String email) {
        return SystemUtils.getSafeFileName(email).toLowerCase() + ".html";
    }

    public List<RichTextReport> getIndividualReports(List<ContributorRepositories> contributors) {
        File individualReportsFolder = new File(reportsFolder, "contributors");
        individualReportsFolder.mkdirs();
        contributors.forEach(contributor -> writeIndividualReport(contributor, individualReportsFolder));
        // The pages are written directly above; nothing is returned for the export pipeline.
        return new ArrayList<>();
    }

    private void writeIndividualReport(ContributorRepositories contributorRepositories, File folder) {
        try {
            LandscapeConfiguration configuration = landscapeAnalysisResults.getConfiguration();
            PeopleConfig peopleConfig = landscapeAnalysisResults.getPeopleConfig();

            ContributorIndividualReportExport data =
                    new ContributorIndividualReportExport(contributorRepositories, configuration, peopleConfig);

            // Language icons for the contributor's repositories, members and extensions.
            List<String> langs = new ArrayList<>();
            data.getRepositories().forEach(r -> langs.add(r.getLang()));
            data.getMembers().forEach(m -> langs.add(m.getLang()));
            data.getExtensions().forEach(e -> langs.add(e.getLang()));
            String langIcons = DataImageUtils.getLangDataImageMapJson(langs);

            Map<String, Object> options = new LinkedHashMap<>();
            options.put("latestCommitDate", landscapeAnalysisResults.getLatestCommitDate());
            options.put("commitsMaxYears", configuration.getCommitsMaxYears());
            options.put("breadcrumbsLabel", configuration.getMetadata().getName() + " / Contributors");
            options.put("indexUrl", "../index.html");
            options.put("avatarTeam", DataImageUtils.TEAM);
            options.put("avatarDeveloper", DataImageUtils.DEVELOPER);
            options.put("isTeam", !data.getMembers().isEmpty());

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("langIcons", langIcons);
            placeholders.put("options", new JsonGenerator().generateCompressed(options));

            String html = new ExplorerTemplate().render("contributor-report.html", data, placeholders);
            String fileName = getContributorIndividualReportFileName(contributorRepositories.getContributor().getEmail());
            FileUtils.write(new File(folder, fileName), html, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOG.error(e);
        }
    }
}
