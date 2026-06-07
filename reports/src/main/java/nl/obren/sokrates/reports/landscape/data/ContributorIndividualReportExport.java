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
                // The member link points at the shared people page (contributor-report.html?key=...);
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
        private String name;
        private String lang;
        private String repoUrl;
        private int commits30Days;
        private int commits90Days;
        private int commits180Days;
        private int commits365Days;
        private int commitsCount;
        private List<String> commitDates = new ArrayList<>();

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
            commits180Days = info.getCommits180Days();
            commits365Days = info.getCommits365Days();
            commitsCount = info.getCommitsCount();
            if (info.getCommitDates() != null) {
                commitDates = info.getCommitDates();
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

        public int getCommits180Days() {
            return commits180Days;
        }

        public int getCommits365Days() {
            return commits365Days;
        }

        public int getCommitsCount() {
            return commitsCount;
        }

        public List<String> getCommitDates() {
            return commitDates;
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
            reportUrl = LandscapeContributorsReport.getContributorUrl(email).replace("contributors/", "");
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
