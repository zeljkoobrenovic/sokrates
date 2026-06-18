package nl.obren.sokrates.reports.landscape.data;

import nl.obren.sokrates.reports.landscape.statichtml.LandscapeContributorsReport;
import nl.obren.sokrates.reports.landscape.utils.ContributorPerExtensionHelper;
import nl.obren.sokrates.sourcecode.Link;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.githistory.ContributorPerExtensionStats;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.landscape.PeopleConfig;
import nl.obren.sokrates.sourcecode.landscape.PersonConfig;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorRepositories;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorRepositoryInfo;
import nl.obren.sokrates.sourcecode.metrics.NumericMetric;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON-serializable view of one contributor (or team) for the client-rendered individual
 * contributor report ({@code contributor-report.html}). Carries the header data, the
 * per-extension activity, every repository's commit dates (so the page can re-grid the
 * per-week/month/year activity views as the user searches), and team members.
 */
public class ContributorIndividualReportExport {
    private String email;
    private String avatarUrl;
    private String detailsLink;
    private List<NamedLink> links = new ArrayList<>();
    private String firstCommitDate = "";
    private String latestCommitDate = "";
    private int commitsCount;
    private int commitsCount30Days;
    private int commitsCount90Days;
    private int commitsCount180Days;
    private int commitsCount365Days;
    // Lines added/deleted across all of this contributor's commits (and the 30-day window). 0 when the
    // underlying git-history.txt had no churn columns (older exports).
    private int linesAdded;
    private int linesDeleted;
    private int linesAdded30Days;
    private int linesDeleted30Days;
    private int repositoriesCount;
    private int repositoriesCount30Days;
    private int repositoriesCount90Days;
    private int repositoriesCount180Days;
    private int repositoriesCount365Days;
    private List<ExtensionActivity> extensions = new ArrayList<>();
    private List<Repository> repositories = new ArrayList<>();
    private List<Member> members = new ArrayList<>();

    public ContributorIndividualReportExport() {
    }

    public ContributorIndividualReportExport(ContributorRepositories cr, LandscapeConfiguration configuration,
                                             PeopleConfig peopleConfig) {
        this(cr, configuration, peopleConfig, null);
    }

    /**
     * @param contributorsFolder the {@code contributors/} folder; when non-null, a member is only
     *                           linked to its individual report if that report file actually exists.
     */
    public ContributorIndividualReportExport(ContributorRepositories cr, LandscapeConfiguration configuration,
                                             PeopleConfig peopleConfig, java.io.File contributorsFolder) {
        Contributor c = cr.getContributor();
        email = c.getEmail();

        PersonConfig personConfig = peopleConfig != null ? peopleConfig.getPersonByName(email) : null;
        if (personConfig != null && StringUtils.isNotBlank(personConfig.getImage())) {
            avatarUrl = personConfig.getImage();
        } else {
            avatarUrl = LandscapeContributorsReport.getAvatarUrl(email, configuration.getContributorAvatarLinkTemplate());
        }

        String template = configuration.getContributorLinkTemplate();
        if (StringUtils.isNotBlank(template)) {
            detailsLink = LandscapeContributorsReport.getContributorUrlFromTemplate(email, template);
        }
        if (personConfig != null && personConfig.getLinks() != null) {
            for (Link link : personConfig.getLinks()) {
                if (StringUtils.isNotBlank(link.getHref())) {
                    links.add(new NamedLink(link.getLabel(), link.getHref()));
                }
            }
        }

        firstCommitDate = c.getFirstCommitDate() != null ? c.getFirstCommitDate() : "";
        latestCommitDate = c.getLatestCommitDate() != null ? c.getLatestCommitDate() : "";
        commitsCount = c.getCommitsCount();
        commitsCount30Days = c.getCommitsCount30Days();
        commitsCount90Days = c.getCommitsCount90Days();
        commitsCount180Days = c.getCommitsCount180Days();
        commitsCount365Days = c.getCommitsCount365Days();
        linesAdded = c.getLinesAdded();
        linesDeleted = c.getLinesDeleted();
        linesAdded30Days = c.getLinesAdded30Days();
        linesDeleted30Days = c.getLinesDeleted30Days();

        List<ContributorRepositoryInfo> repos = cr.getRepositories();
        repositoriesCount = repos.size();
        repositoriesCount30Days = (int) repos.stream().filter(p -> p.getCommits30Days() > 0).count();
        repositoriesCount90Days = (int) repos.stream().filter(p -> p.getCommits90Days() > 0).count();
        repositoriesCount180Days = (int) repos.stream().filter(p -> p.getCommits180Days() > 0).count();
        repositoriesCount365Days = (int) repos.stream().filter(p -> p.getCommits365Days() > 0).count();

        ContributorPerExtensionHelper helper = new ContributorPerExtensionHelper();
        List<Pair<String, ContributorPerExtensionStats>> extensionUpdates =
                helper.getContributorStatsPerExtension(configuration, cr, peopleConfig);
        helper.getContributorsPerExtensionStream(extensionUpdates).forEach(e ->
                extensions.add(new ExtensionActivity(
                        e.getLeft() != null ? e.getLeft().replace("*.", "").trim().toLowerCase() : "",
                        e.getRight().getFileUpdates90Days())));

        repos.forEach(r -> repositories.add(new Repository(r)));

        if (cr.getMembers() != null) {
            cr.getMembers().forEach(m -> {
                Member member = new Member(m);
                String biggest = helper.getBiggestExtension(configuration, m, peopleConfig);
                member.setLang(biggest != null ? biggest.replace("*.", "").trim().toLowerCase() : "");
                // The member link points at the shared people page (contributor-report.html#<key>);
                // if that member isn't in people.zip the page shows a graceful "not found" message,
                // so no file-existence gate is needed (and there are no per-person files to check).
                members.add(member);
            });
        }
    }

    public static class NamedLink {
        private String label;
        private String href;

        public NamedLink() {
        }

        public NamedLink(String label, String href) {
            this.label = label;
            this.href = href;
        }

        public String getLabel() {
            return label;
        }

        public String getHref() {
            return href;
        }
    }

    public static class ExtensionActivity {
        private String lang;
        private int fileUpdates90Days;

        public ExtensionActivity() {
        }

        public ExtensionActivity(String lang, int fileUpdates90Days) {
            this.lang = lang;
            this.fileUpdates90Days = fileUpdates90Days;
        }

        public String getLang() {
            return lang;
        }

        public int getFileUpdates90Days() {
            return fileUpdates90Days;
        }
    }

    public static class Repository {
        // The fields the contributor-report.html template reads: name, lang, repoUrl, the per-repo
        // commit counts shown as the leading columns (30d / 3m / 1y / all time; commits90Days also
        // drives the bold/dim row weight) and commitDates (the activity grid).
        private String name;
        private String lang;
        private String repoUrl;
        private int commits30Days;
        private int commits90Days;
        private int commits365Days;
        private int commitsCount;
        // Per-window line churn by this contributor in this repository, split into lines added and
        // deleted (mirroring the commit columns). 0 when the history carried no churn data.
        private int churnAdded30Days;
        private int churnDeleted30Days;
        private int churnAdded90Days;
        private int churnDeleted90Days;
        private int churnAdded365Days;
        private int churnDeleted365Days;
        private int churnAdded;
        private int churnDeleted;
        private List<String> commitDates = new ArrayList<>();
        // Per-day commit counts (date -> commits). The activity grid sizes its circles/bars by
        // commits per slot using this; commitDates (distinct days) is the fallback when this is
        // empty (older analyses).
        private java.util.Map<String, Integer> commitsPerDate = new java.util.LinkedHashMap<>();
        // Per-day line churn (date -> lines), split added/deleted, used to draw the stacked per-slot
        // churn bar in the total row. Empty for older analyses.
        private java.util.Map<String, Integer> churnAddedPerDate = new java.util.LinkedHashMap<>();
        private java.util.Map<String, Integer> churnDeletedPerDate = new java.util.LinkedHashMap<>();

        public Repository() {
        }

        public Repository(ContributorRepositoryInfo info) {
            name = info.getRepositoryAnalysisResults().getAnalysisResults().getMetadata().getName();
            List<NumericMetric> locPerExtension = info.getRepositoryAnalysisResults().getAnalysisResults()
                    .getMainAspectAnalysisResults().getLinesOfCodePerExtension();
            lang = (locPerExtension != null && !locPerExtension.isEmpty())
                    ? locPerExtension.get(0).getName().replace("*.", "").trim().toLowerCase() : "";
            repoUrl = "../../" + info.getRepositoryAnalysisResults().getSokratesRepositoryLink().getHtmlReportsRoot() + "/index.html";
            commits30Days = info.getCommits30Days();
            commits90Days = info.getCommits90Days();
            commits365Days = info.getCommits365Days();
            commitsCount = info.getCommitsCount();
            churnAdded30Days = info.getChurnAdded30Days();
            churnDeleted30Days = info.getChurnDeleted30Days();
            churnAdded90Days = info.getChurnAdded90Days();
            churnDeleted90Days = info.getChurnDeleted90Days();
            churnAdded365Days = info.getChurnAdded365Days();
            churnDeleted365Days = info.getChurnDeleted365Days();
            churnAdded = info.getChurnAdded();
            churnDeleted = info.getChurnDeleted();
            if (info.getCommitDates() != null) {
                commitDates = info.getCommitDates();
            }
            if (info.getCommitsPerDate() != null) {
                commitsPerDate = info.getCommitsPerDate();
            }
            if (info.getChurnAddedPerDate() != null) {
                churnAddedPerDate = info.getChurnAddedPerDate();
            }
            if (info.getChurnDeletedPerDate() != null) {
                churnDeletedPerDate = info.getChurnDeletedPerDate();
            }
        }

        public String getName() {
            return name;
        }

        public String getLang() {
            return lang;
        }

        public String getRepoUrl() {
            return repoUrl;
        }

        public int getCommits30Days() {
            return commits30Days;
        }

        public int getCommits90Days() {
            return commits90Days;
        }

        public int getCommits365Days() {
            return commits365Days;
        }

        public int getCommitsCount() {
            return commitsCount;
        }

        public int getChurnAdded30Days() {
            return churnAdded30Days;
        }

        public int getChurnDeleted30Days() {
            return churnDeleted30Days;
        }

        public int getChurnAdded90Days() {
            return churnAdded90Days;
        }

        public int getChurnDeleted90Days() {
            return churnDeleted90Days;
        }

        public int getChurnAdded365Days() {
            return churnAdded365Days;
        }

        public int getChurnDeleted365Days() {
            return churnDeleted365Days;
        }

        public int getChurnAdded() {
            return churnAdded;
        }

        public int getChurnDeleted() {
            return churnDeleted;
        }

        public List<String> getCommitDates() {
            return commitDates;
        }

        public java.util.Map<String, Integer> getCommitsPerDate() {
            return commitsPerDate;
        }

        public java.util.Map<String, Integer> getChurnAddedPerDate() {
            return churnAddedPerDate;
        }

        public java.util.Map<String, Integer> getChurnDeletedPerDate() {
            return churnDeletedPerDate;
        }
    }

    public static class Member {
        private String email;
        private String lang;
        private String reportUrl;
        private int commitsCount;
        private int commitsCount30Days;
        private int commitsCount90Days;
        private int commitsCount180Days;
        private int commitsCount365Days;
        private String firstCommitDate = "";
        private String latestCommitDate = "";

        public Member() {
        }

        public Member(ContributorRepositories cr) {
            Contributor c = cr.getContributor();
            email = c.getEmail();
            // A member is normally a contributor; route on whether it itself has members (a sub-team)
            // rather than the shared team-email set (which can mis-route — see getContributorUrl).
            boolean isTeam = cr.getMembers() != null && cr.getMembers().size() > 0;
            reportUrl = LandscapeContributorsReport.getContributorUrl(email, isTeam).replace("contributors/", "");
            commitsCount = c.getCommitsCount();
            commitsCount30Days = c.getCommitsCount30Days();
            commitsCount90Days = c.getCommitsCount90Days();
            commitsCount180Days = c.getCommitsCount180Days();
            commitsCount365Days = c.getCommitsCount365Days();
            firstCommitDate = c.getFirstCommitDate() != null ? c.getFirstCommitDate() : "";
            latestCommitDate = c.getLatestCommitDate() != null ? c.getLatestCommitDate() : "";
        }

        public String getEmail() {
            return email;
        }

        public String getLang() {
            return lang;
        }

        public void setLang(String lang) {
            this.lang = lang;
        }

        public String getReportUrl() {
            return reportUrl;
        }

        public void setReportUrl(String reportUrl) {
            this.reportUrl = reportUrl;
        }

        public int getCommitsCount() {
            return commitsCount;
        }

        public int getCommitsCount30Days() {
            return commitsCount30Days;
        }

        public int getCommitsCount90Days() {
            return commitsCount90Days;
        }

        public int getCommitsCount180Days() {
            return commitsCount180Days;
        }

        public int getCommitsCount365Days() {
            return commitsCount365Days;
        }

        public String getFirstCommitDate() {
            return firstCommitDate;
        }

        public String getLatestCommitDate() {
            return latestCommitDate;
        }
    }

    public String getEmail() {
        return email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getDetailsLink() {
        return detailsLink;
    }

    public List<NamedLink> getLinks() {
        return links;
    }

    public String getFirstCommitDate() {
        return firstCommitDate;
    }

    public String getLatestCommitDate() {
        return latestCommitDate;
    }

    public int getCommitsCount() {
        return commitsCount;
    }

    public int getCommitsCount30Days() {
        return commitsCount30Days;
    }

    public int getCommitsCount90Days() {
        return commitsCount90Days;
    }

    public int getCommitsCount180Days() {
        return commitsCount180Days;
    }

    public int getCommitsCount365Days() {
        return commitsCount365Days;
    }

    public int getLinesAdded() {
        return linesAdded;
    }

    public int getLinesDeleted() {
        return linesDeleted;
    }

    public int getLinesAdded30Days() {
        return linesAdded30Days;
    }

    public int getLinesDeleted30Days() {
        return linesDeleted30Days;
    }

    public int getRepositoriesCount() {
        return repositoriesCount;
    }

    public int getRepositoriesCount30Days() {
        return repositoriesCount30Days;
    }

    public int getRepositoriesCount90Days() {
        return repositoriesCount90Days;
    }

    public int getRepositoriesCount180Days() {
        return repositoriesCount180Days;
    }

    public int getRepositoriesCount365Days() {
        return repositoriesCount365Days;
    }

    public List<ExtensionActivity> getExtensions() {
        return extensions;
    }

    public List<Repository> getRepositories() {
        return repositories;
    }

    public List<Member> getMembers() {
        return members;
    }
}
