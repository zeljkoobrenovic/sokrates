/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.Link;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.operations.OperationStatement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LandscapeConfiguration {
    // Basic info about the landscape (name, description, logo, links)
    private Metadata metadata = new Metadata();

    // The relative path of the analysis (contains sub-folders with repository analysis results)
    private String analysisRoot = ".";

    // A prefix attached to repository reports
    private String repositoryReportsUrlPrefix = "../";

    // An optional parent URL, if defined a click on the title of the landscape report will go to this link
    private String parentUrl = "";

    // An optional list of links representing breadcrumps, to reflect the hierarchy of the landscape pages
    private List<Link> breadcrumbs = new ArrayList<>();

    // Only files with extensions that have more or equal lines of main code will be included in the landscape report
    private int extensionThresholdLoc = 0;

    // Only repositories having more or equal to the given number of lines of main code will be included in the landscape report
    private int repositoryThresholdLocMain = 0;

    // Only repositories having more or equal to the given number of unique contributors will be included in the landscape report
    private int repositoryThresholdContributors = 2;

    // Only repositories having more or equal to the given number of commits will be included in the landscape report
    private int contributorThresholdCommits = 2;

    // If not empty, only repositories before the given date (in the "YYYY-MM-dd" format) will be included in the landscape report
    private String ignoreRepositoriesLastUpdatedBefore = "";

    // A maximal number of years of commit history dispalyed in the report
    private int commitsMaxYears = 10;

    // A minimal number of commits days per year for a contributor to be classified as a "significant" contributor
    private int significantContributorMinCommitDaysPerYear = 10;

    // If true, contributors IDs (e.g. emails) will be replaces with anonymous IDs (e.g. Contributor 1, Contributor 2)
    private boolean anonymizeContributors = false;

    // If true, the repositories report will show the status of controls of each repository
    private boolean showRepositoryControls = true;

    // A maximal number of repositories shown in the short repository pages (embedded in the index page)
    private int repositoriesShortListLimit = 100;

    // A maximal number of repositories shown in repository pages (linked from the short page)
    private int repositoriesListLimit = 1000;

    // A maximal number of years to be displayed for repositories' history
    private int repositoriesHistoryLimit = 30;

    // A maximal number of contributors shown in contributor pages (linked from the short page)
    private int contributorsListLimit = 1000;

    // An optional template of the link to a web page with more info about a contributor. The string fragment "${contributorid}" will be replace with the actual contributor ID.
    private String contributorLinkTemplate = "";

    // An optional template of the link to a avatar image of a contributor. The string fragment "${contributorid}" will be replaced with the actual contributor ID (e.g. transformed email).
    private String contributorAvatarLinkTemplate = "";

    // The list of regex expressions used to exclude contributors from analysis. If empty, all contributors are included.
    private List<String> ignoreContributors = new ArrayList<>(Arrays.asList(".*\\[bot\\].*", ".*[-]bot[@].*"));

    // The list of contributor tagging rules (regex expressions for email).
    private List<ContributorTag> tagContributors = new ArrayList<>();

    // The list of extensions to ignore
    private List<String> ignoreExtensions = new ArrayList<>();

    // If true, only one repository with the same repository name will be included in the landscape analyses (the first one found in file scan). Otherwise, all repositories will be included.
    private boolean includeOnlyOneRepositoryWithSameName = true;

    // The list of extensions to merge (e.g. yml => yaml)
    private List<MergeExtension> mergeExtensions = new ArrayList<>();

    // An optional list of string transformation used to transform contributor IDs (e.g. to remove domain from email)
    private List<OperationStatement> transformContributorEmails = new ArrayList<>();

    // If true, the list with extensions will be displayed in the first "Overview" tab. If false, the list with extensions will be displayed in the first "Repositories" tab.
    private boolean showExtensionsOnFirstTab = true;

    // If true, the contributors and commits trend will be displayed in the first "Overview" tab in addition to it being shown in the "Contributors" tab.
    private boolean showContributorsTrendsOnFirstTab = true;

    // Maximal depth of indexed sub-landsacpes
    private int maxSublandscapeDepth = 0;

    // A list of iFrames displayed at the start of the "Overview" tab
    private List<WebFrameLink> iFramesAtStart = new ArrayList<>();

    // A list of iFrames displayed at the end of the "Overview" tab
    private List<WebFrameLink> iFrames = new ArrayList<>();

    // A list of iFrames displayed at the start of the "Proejcts" tab
    private List<WebFrameLink> iFramesRepositoriesAtStart = new ArrayList<>();

    // A list of iFrames displayed at the end of the "Proejcts" tab
    private List<WebFrameLink> iFramesRepositories = new ArrayList<>();

    // A list of iFrames displayed at the start of the "Contributors" tab
    private List<WebFrameLink> iFramesContributorsAtStart = new ArrayList<>();

    // A list of iFrames displayed at the end of the "Contributors" tab
    private List<WebFrameLink> iFramesContributors = new ArrayList<>();

    // Optional additional tabs (with iFrames only)
    private List<CustomTab> customTabs = new ArrayList<>();

    // An optional HTML fragment to be included in the report HTML header (e.g. Google Analytics smippet)
    private String customHtmlReportHeaderFragment = "";

    // values automatically populated by Sokrates, do not change manually
    @JsonIgnore
    private List<SubLandscapeLink> subLandscapes = new ArrayList<>();
    @JsonIgnore
    private List<SokratesRepositoryLink> repositories = new ArrayList<>();

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public String getAnalysisRoot() {
        return analysisRoot;
    }

    public void setAnalysisRoot(String analysisRoot) {
        this.analysisRoot = analysisRoot;
    }

    public String getRepositoryReportsUrlPrefix() {
        return repositoryReportsUrlPrefix;
    }

    public void setRepositoryReportsUrlPrefix(String repositoryReportsUrlPrefix) {
        this.repositoryReportsUrlPrefix = repositoryReportsUrlPrefix;
    }

    @JsonIgnore
    public List<SubLandscapeLink> getSubLandscapes() {
        return subLandscapes;
    }

    @JsonIgnore
    public void setSubLandscapes(List<SubLandscapeLink> subLandscapes) {
        this.subLandscapes = subLandscapes;
    }

    @JsonIgnore
    public List<SokratesRepositoryLink> getRepositories() {
        return repositories;
    }

    @JsonIgnore
    public void setRepositories(List<SokratesRepositoryLink> repositories) {
        this.repositories = repositories;
    }

    public int getExtensionThresholdLoc() {
        return extensionThresholdLoc;
    }

    public void setExtensionThresholdLoc(int extensionThresholdLoc) {
        this.extensionThresholdLoc = extensionThresholdLoc;
    }

    public int getRepositoryThresholdLocMain() {
        return repositoryThresholdLocMain;
    }

    public void setRepositoryThresholdLocMain(int repositoryThresholdLocMain) {
        this.repositoryThresholdLocMain = repositoryThresholdLocMain;
    }

    public int getContributorThresholdCommits() {
        return contributorThresholdCommits;
    }

    public void setContributorThresholdCommits(int contributorThresholdCommits) {
        this.contributorThresholdCommits = contributorThresholdCommits;
    }

    public String getIgnoreRepositoriesLastUpdatedBefore() {
        return ignoreRepositoriesLastUpdatedBefore;
    }

    public void setIgnoreRepositoriesLastUpdatedBefore(String ignoreRepositoriesLastUpdatedBefore) {
        this.ignoreRepositoriesLastUpdatedBefore = ignoreRepositoriesLastUpdatedBefore;
    }

    public int getRepositoryThresholdContributors() {
        return repositoryThresholdContributors;
    }

    public void setRepositoryThresholdContributors(int repositoryThresholdContributors) {
        this.repositoryThresholdContributors = repositoryThresholdContributors;
    }

    public boolean isShowExtensionsOnFirstTab() {
        return showExtensionsOnFirstTab;
    }

    public void setShowExtensionsOnFirstTab(boolean showExtensionsOnFirstTab) {
        this.showExtensionsOnFirstTab = showExtensionsOnFirstTab;
    }

    public boolean isShowContributorsTrendsOnFirstTab() {
        return showContributorsTrendsOnFirstTab;
    }

    public void setShowContributorsTrendsOnFirstTab(boolean showContributorsTrendsOnFirstTab) {
        this.showContributorsTrendsOnFirstTab = showContributorsTrendsOnFirstTab;
    }

    public String getParentUrl() {
        return parentUrl;
    }

    public void setParentUrl(String parentUrl) {
        this.parentUrl = parentUrl;
    }

    public List<Link> getBreadcrumbs() {
        return breadcrumbs;
    }

    public void setBreadcrumbs(List<Link> breadcrumbs) {
        this.breadcrumbs = breadcrumbs;
    }

    public List<WebFrameLink> getiFrames() {
        return iFrames;
    }

    public void setiFrames(List<WebFrameLink> iFrames) {
        this.iFrames = iFrames;
    }

    public int getMaxSublandscapeDepth() {
        return maxSublandscapeDepth;
    }

    public void setMaxSublandscapeDepth(int maxSublandscapeDepth) {
        this.maxSublandscapeDepth = maxSublandscapeDepth;
    }

    public List<WebFrameLink> getiFramesAtStart() {
        return iFramesAtStart;
    }

    public void setiFramesAtStart(List<WebFrameLink> iFramesAtStart) {
        this.iFramesAtStart = iFramesAtStart;
    }

    public List<WebFrameLink> getiFramesRepositories() {
        return iFramesRepositories;
    }

    public void setiFramesRepositories(List<WebFrameLink> iFramesRepositories) {
        this.iFramesRepositories = iFramesRepositories;
    }

    public List<WebFrameLink> getiFramesContributors() {
        return iFramesContributors;
    }

    public void setiFramesContributors(List<WebFrameLink> iFramesContributors) {
        this.iFramesContributors = iFramesContributors;
    }

    public List<WebFrameLink> getiFramesRepositoriesAtStart() {
        return iFramesRepositoriesAtStart;
    }

    public void setiFramesRepositoriesAtStart(List<WebFrameLink> iFramesRepositoriesAtStart) {
        this.iFramesRepositoriesAtStart = iFramesRepositoriesAtStart;
    }

    public List<WebFrameLink> getiFramesContributorsAtStart() {
        return iFramesContributorsAtStart;
    }

    public void setiFramesContributorsAtStart(List<WebFrameLink> iFramesContributorsAtStart) {
        this.iFramesContributorsAtStart = iFramesContributorsAtStart;
    }

    public boolean isAnonymizeContributors() {
        return anonymizeContributors;
    }

    public void setAnonymizeContributors(boolean anonymizeContributors) {
        this.anonymizeContributors = anonymizeContributors;
    }

    public boolean isShowRepositoryControls() {
        return showRepositoryControls;
    }

    public void setShowRepositoryControls(boolean showRepositoryControls) {
        this.showRepositoryControls = showRepositoryControls;
    }

    public int getRepositoriesListLimit() {
        return repositoriesListLimit;
    }

    public void setRepositoriesListLimit(int repositoriesListLimit) {
        this.repositoriesListLimit = repositoriesListLimit;
    }

    public int getRepositoriesHistoryLimit() {
        return repositoriesHistoryLimit;
    }

    public void setRepositoriesHistoryLimit(int repositoriesHistoryLimit) {
        this.repositoriesHistoryLimit = repositoriesHistoryLimit;
    }

    public int getRepositoriesShortListLimit() {
        return repositoriesShortListLimit;
    }

    public void setRepositoriesShortListLimit(int repositoriesShortListLimit) {
        this.repositoriesShortListLimit = repositoriesShortListLimit;
    }

    public int getContributorsListLimit() {
        return contributorsListLimit;
    }

    public void setContributorsListLimit(int contributorsListLimit) {
        this.contributorsListLimit = contributorsListLimit;
    }

    public int getCommitsMaxYears() {
        return commitsMaxYears;
    }

    public int getSignificantContributorMinCommitDaysPerYear() {
        return significantContributorMinCommitDaysPerYear;
    }

    public void setSignificantContributorMinCommitDaysPerYear(int significantContributorMinCommitDaysPerYear) {
        this.significantContributorMinCommitDaysPerYear = significantContributorMinCommitDaysPerYear;
    }

    public void setCommitsMaxYears(int commitsMaxYears) {
        this.commitsMaxYears = commitsMaxYears;
    }

    public List<OperationStatement> getTransformContributorEmails() {
        return transformContributorEmails;
    }

    public void setTransformContributorEmails(List<OperationStatement> transformContributorEmails) {
        this.transformContributorEmails = transformContributorEmails;
    }

    public String getContributorLinkTemplate() {
        return contributorLinkTemplate;
    }

    public void setContributorLinkTemplate(String contributorLinkTemplate) {
        this.contributorLinkTemplate = contributorLinkTemplate;
    }

    public String getContributorAvatarLinkTemplate() {
        return contributorAvatarLinkTemplate;
    }

    public void setContributorAvatarLinkTemplate(String contributorAvatarLinkTemplate) {
        this.contributorAvatarLinkTemplate = contributorAvatarLinkTemplate;
    }

    public List<String> getIgnoreContributors() {
        return ignoreContributors;
    }

    public void setIgnoreContributors(List<String> ignoreContributors) {
        this.ignoreContributors = ignoreContributors;
    }

    public List<String> getIgnoreExtensions() {
        return ignoreExtensions;
    }

    public void setIgnoreExtensions(List<String> ignoreExtensions) {
        this.ignoreExtensions = ignoreExtensions;
    }

    public List<MergeExtension> getMergeExtensions() {
        return mergeExtensions;
    }

    public void setMergeExtensions(List<MergeExtension> mergeExtensions) {
        this.mergeExtensions = mergeExtensions;
    }

    public String getCustomHtmlReportHeaderFragment() {
        return customHtmlReportHeaderFragment;
    }

    public void setCustomHtmlReportHeaderFragment(String customHtmlReportHeaderFragment) {
        this.customHtmlReportHeaderFragment = customHtmlReportHeaderFragment;
    }

    public List<CustomTab> getCustomTabs() {
        return customTabs;
    }

    public void setCustomTabs(List<CustomTab> customTabs) {
        this.customTabs = customTabs;
    }

    public boolean isIncludeOnlyOneRepositoryWithSameName() {
        return includeOnlyOneRepositoryWithSameName;
    }

    public void setIncludeOnlyOneRepositoryWithSameName(boolean includeOnlyOneRepositoryWithSameName) {
        this.includeOnlyOneRepositoryWithSameName = includeOnlyOneRepositoryWithSameName;
    }


    // legacy tolerant reader

    public void setProjectReportsUrlPrefix(String repositoryReportsUrlPrefix) {
        this.repositoryReportsUrlPrefix = repositoryReportsUrlPrefix;
    }

    public void setProjectThresholdLocMain(int repositoryThresholdLocMain) {
        this.repositoryThresholdLocMain = repositoryThresholdLocMain;
    }

    public void setProjectThresholdContributors(int repositoryThresholdContributors) {
        this.repositoryThresholdContributors = repositoryThresholdContributors;
    }

    public void setIgnoreProjectsLastUpdatedBefore(String ignoreRepositoriesLastUpdatedBefore) {
        this.ignoreRepositoriesLastUpdatedBefore = ignoreRepositoriesLastUpdatedBefore;
    }

    public void setProjectsShortListLimit(int repositoriesShortListLimit) {
        this.repositoriesShortListLimit = repositoriesShortListLimit;
    }

    public void setProjectsListLimit(int repositoriesListLimit) {
        this.repositoriesListLimit = repositoriesListLimit;
    }

    public void setProjectsHistoryLimit(int repositoriesHistoryLimit) {
        this.repositoriesHistoryLimit = repositoriesHistoryLimit;
    }

    public void setiFramesProjectsAtStart(List<WebFrameLink> iFramesRepositoriesAtStart) {
        this.iFramesRepositoriesAtStart = iFramesRepositoriesAtStart;
    }

    public List<ContributorTag> getTagContributors() {
        return tagContributors;
    }

    public void setTagContributors(List<ContributorTag> tagContributors) {
        this.tagContributors = tagContributors;
    }
}
