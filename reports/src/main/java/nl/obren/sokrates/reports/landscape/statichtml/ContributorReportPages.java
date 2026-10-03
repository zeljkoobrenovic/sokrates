package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.renderingutils.ExplorerTemplate;
import nl.obren.sokrates.reports.landscape.data.ContributorReportExport;
import nl.obren.sokrates.reports.landscape.utils.*;
import nl.obren.sokrates.reports.utils.DataImageUtils;
import nl.obren.sokrates.sourcecode.landscape.*;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorRepositories;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisResults;
import org.apache.commons.io.FileUtils;
import java.io.File;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import static nl.obren.sokrates.reports.landscape.statichtml.LandscapeReportGenerator.*;

/**
 * The client-rendered contributors / teams report pages of the landscape: the exports per contributor (with the
 * 30-day languages that back the includesLang filter) written as the Recent / All / Bots pages, and the set of
 * contributors that get an individual report. Moved out of {@link LandscapeReportContributorsTab}.
 */
class ContributorReportPages {
    private static final Log LOG = LogFactory.getLog(ContributorReportPages.class);
    private final LandscapeAnalysisResults landscapeAnalysisResults;
    private final List<ContributorRepositories> contributors;
    private final File reportsFolder;
    private final LandscapeReportContributorsTab.Type type;
    private final TeamsConfig teamsConfig;

    ContributorReportPages(LandscapeAnalysisResults landscapeAnalysisResults, List<ContributorRepositories> contributors, File reportsFolder,
                           LandscapeReportContributorsTab.Type type, TeamsConfig teamsConfig) {
        this.landscapeAnalysisResults = landscapeAnalysisResults;
        this.contributors = contributors;
        this.reportsFolder = reportsFolder;
        this.type = type;
        this.teamsConfig = teamsConfig;
    }

    /**
     * Renders the client-rendered, searchable/sortable contributors report ({@code &lt;type&gt;-report.html})
     * with Recent / All time / Bots tabs, replacing the separate static contributor tables.
     */
    void saveContributorsReportPage(List<ContributorRepositories> recentContributors,
                                            List<ContributorRepositories> contributors,
                                            List<ContributorRepositories> bots) {
        try {
            LandscapeConfiguration configuration = landscapeAnalysisResults.getConfiguration();
            PeopleConfig peopleConfig = landscapeAnalysisResults.getPeopleConfig();
            List<ContributorTag> tagRules = configuration.getTagContributors();

            // Map contributorId -> languages they committed to in the last 30 days, built from the
            // SAME per-extension commit history the Overview "Contributors Per File Extension"
            // badges count, so includesLang:<lang> in the report matches those badge counts exactly.
            Map<String, List<String>> recentLangsByContributor = buildRecentLangsByContributor();

            Map<String, List<ContributorReportExport>> groups = new LinkedHashMap<>();
            groups.put("recent", toExports(recentContributors, configuration, peopleConfig, tagRules, recentLangsByContributor));
            groups.put("all", toExports(contributors, configuration, peopleConfig, tagRules, recentLangsByContributor));
            groups.put("bots", toExports(bots, configuration, peopleConfig, tagRules, recentLangsByContributor));

            // Language icons for every distinct main language across the three lists.
            List<String> langs = new ArrayList<>();
            groups.values().forEach(list -> list.forEach(e -> langs.add(e.getMainLang())));
            String langIcons = DataImageUtils.getLangDataImageMapJson(langs);

            JsonGenerator jsonGenerator = new JsonGenerator();
            Map<String, Object> optionsData = new LinkedHashMap<>();
            optionsData.put("showBots", type.showBots() && !bots.isEmpty());
            optionsData.put("avatarTeam", DataImageUtils.TEAM);
            optionsData.put("avatarDeveloper", DataImageUtils.DEVELOPER);

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("langIcons", langIcons);
            placeholders.put("options", jsonGenerator.generateCompressed(optionsData));

            String html = new ExplorerTemplate().render("contributors-report.html", groups, placeholders);
            FileUtils.write(new File(reportsFolder, type.plural() + "-report.html"), html, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    private List<ContributorReportExport> toExports(List<ContributorRepositories> list, LandscapeConfiguration configuration,
                                                    PeopleConfig peopleConfig, List<ContributorTag> tagRules,
                                                    Map<String, List<String>> recentLangsByContributor) {
        // Export every contributor (no list-limit cap): the client-rendered report pages the
        // display itself (show-more), and search needs the full set. Sorted by commit recency.
        return list.stream()
                .sorted((a, b) -> b.getContributor().getCommitsCount() - a.getContributor().getCommitsCount())
                .sorted((a, b) -> b.getContributor().getCommitsCount365Days() - a.getContributor().getCommitsCount365Days())
                .sorted((a, b) -> b.getContributor().getCommitsCount90Days() - a.getContributor().getCommitsCount90Days())
                .sorted((a, b) -> b.getContributor().getCommitsCount30Days() - a.getContributor().getCommitsCount30Days())
                .map(cr -> new ContributorReportExport(cr, configuration, peopleConfig, teamsConfig, tagRules,
                        recentLangsByContributor.get(cr.getContributor().getEmail().toLowerCase())))
                .collect(Collectors.toList());
    }

    // Records the top-N contributors (capped at getContributorsListLimit, sorted by recency like
    // the former table) into the linked set, so the matching individual per-person reports are
    // generated. This preserves the selection that the removed server-rendered contributor tables
    // used to make, without rendering any HTML.
    void collectLinkedContributors(List<ContributorRepositories> contributors, Set<String> linked) {
        int limit = landscapeAnalysisResults.getConfiguration().getContributorsListLimit();
        contributors.stream()
                .sorted((a, b) -> b.getContributor().getCommitsCount() - a.getContributor().getCommitsCount())
                .sorted((a, b) -> b.getContributor().getCommitsCount365Days() - a.getContributor().getCommitsCount365Days())
                .sorted((a, b) -> b.getContributor().getCommitsCount180Days() - a.getContributor().getCommitsCount180Days())
                .sorted((a, b) -> b.getContributor().getCommitsCount90Days() - a.getContributor().getCommitsCount90Days())
                .sorted((a, b) -> b.getContributor().getCommitsCount30Days() - a.getContributor().getCommitsCount30Days())
                .limit(limit)
                .forEach(contributor -> linked.add(contributor.getContributor().getEmail()));
    }

    // For each contributor id (lowercased, matching the report rows' email key), the set of
    // languages (lowercased extensions) they committed to in the last 30 days — inverted from the
    // landscape's per-extension committers30Days, the exact data the Overview badges count.
    private Map<String, List<String>> buildRecentLangsByContributor() {
        Map<String, List<String>> map = new HashMap<>();
        landscapeAnalysisResults.getContributorsPerExtension().forEach(commitsPerExtension -> {
            String lang = commitsPerExtension.getExtension().replace("*.", "").trim().toLowerCase();
            if (lang.isEmpty()) {
                return;
            }
            commitsPerExtension.getCommitters30Days().forEach(committerId -> {
                String key = committerId.toLowerCase();
                List<String> langs = map.computeIfAbsent(key, k -> new ArrayList<>());
                if (!langs.contains(lang)) {
                    langs.add(lang);
                }
            });
        });
        return map;
    }
}
