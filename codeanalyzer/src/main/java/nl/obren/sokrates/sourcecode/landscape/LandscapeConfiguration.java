/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape;

import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.operations.OperationStatement;

import java.util.ArrayList;
import java.util.List;

public class LandscapeConfiguration {
    // Basic info about the landscape (name, description, logo, links)
    private Metadata metadata = new Metadata();

    // The relative path of the analysis (folders with project analysis results)
    private String analysisRoot = "";

    // A prefix attached to project reports
    private String projectReportsUrlPrefix = "../";

    // An optional parent URL, if defined a click on the title of the landscape report will go to this link
    private String parentUrl = "";

    // Only files with extensions that have more or equal lines of main code will be included in the landscape report
    private int extensionThresholdLoc = 0;

    // Only project having more or equal to the given number of lines of main code will be included in the landscape report
    private int projectThresholdLocMain = 0;

    // Only project having more or equal to the given number of unique contributors will be included in the landscape report
    private int projectThresholdContributors = 2;

    // Only project having more or equal to the given number of commits will be included in the landscape report
    private int contributorThresholdCommits = 2;

    // A maximal number of years of commit history dispalyed in the report
    private int commitsMaxYears = 10;

    // If true, contributors IDs (e.g. emails) will be replaces with anonymous IDs (e.g. Contributor 1, Contributor 2)
    private boolean anonymizeContributors = false;

    // If true, the projects report will shows the status of controls of each project
    private boolean showProjectControls = true;

    // A maximal number of projects shown in project pages
    private int projectsListLimit = 1000;

    // A maximal number of contributors shown in contributor pages
    private int contributorsListLimit = 1000;

    // An optional template of the link to a web page with more info about a contributor. The string fragment "${contributorid}" will be replace with the actual contributor ID.
    private String contributorLinkTemplate = "";

    // An optional template of the link to a avatar image of a contributor. The string fragment "${contributorid}" will be replaced with the actual contributor ID (e.g. transformed email).
    private String contributorAvatarLinkTemplate = "";

    // The list of regex expressions used to exclude contributors from analysis. If empty, all contributors are included.
    private List<String> ignoreContributors = new ArrayList<>();

    // The list of extensions to ignore
    private List<String> ignoreExtensions = new ArrayList<>();

    // The list of extensions to merge (e.g. yml => yaml)
    private List<MergeExtension> mergeExtensions = new ArrayList<>();

    // An optional list of string transformation used to transform contributor IDs (e.g. to remove domain from email)
    private List<OperationStatement> transformContributorEmails = new ArrayList<>();

    // If true, the list with extensions will be displayed in the first "Overview" tab. If false, the list with extensions will be displayed in the first "Projects" tab.
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
    private List<WebFrameLink> iFramesProjectsAtStart = new ArrayList<>();

    // A list of iFrames displayed at the end of the "Proejcts" tab
    private List<WebFrameLink> iFramesProjects = new ArrayList<>();

    // A list of iFrames displayed at the start of the "Contributors" tab
    private List<WebFrameLink> iFramesContributorsAtStart = new ArrayList<>();

    // A list of iFrames displayed at the end of the "Contributors" tab
    private List<WebFrameLink> iFramesContributors = new ArrayList<>();

    // Optional additional tabs (with iFrames only)
    private List<CustomTab> customTabs = new ArrayList<>();

    // An optional HTML fragment to be included in the report HTML header (e.g. Google Analytics smippet)
    private String customHtmlReportHeaderFragment = "";

    // An optional list of tags to be used to mark projects
    private List<ProjectTag> projectTags = new ArrayList<>();

    // values automatically populated by Sokrates
    private List<SubLandscapeLink> subLandscapes = new ArrayList<>();
    private List<SokratesProjectLink> projects = new ArrayList<>();

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

    public String getProjectReportsUrlPrefix() {
        return projectReportsUrlPrefix;
    }

    public void setProjectReportsUrlPrefix(String projectReportsUrlPrefix) {
        this.projectReportsUrlPrefix = projectReportsUrlPrefix;
    }

    public List<SubLandscapeLink> getSubLandscapes() {
        return subLandscapes;
    }

    public void setSubLandscapes(List<SubLandscapeLink> subLandscapes) {
        this.subLandscapes = subLandscapes;
    }

    public List<SokratesProjectLink> getProjects() {
        return projects;
    }

    public void setProjects(List<SokratesProjectLink> projects) {
        this.projects = projects;
    }

    public int getExtensionThresholdLoc() {
        return extensionThresholdLoc;
    }

    public void setExtensionThresholdLoc(int extensionThresholdLoc) {
        this.extensionThresholdLoc = extensionThresholdLoc;
    }

    public int getProjectThresholdLocMain() {
        return projectThresholdLocMain;
    }

    public void setProjectThresholdLocMain(int projectThresholdLocMain) {
        this.projectThresholdLocMain = projectThresholdLocMain;
    }

    public int getContributorThresholdCommits() {
        return contributorThresholdCommits;
    }

    public void setContributorThresholdCommits(int contributorThresholdCommits) {
        this.contributorThresholdCommits = contributorThresholdCommits;
    }

    public int getProjectThresholdContributors() {
        return projectThresholdContributors;
    }

    public void setProjectThresholdContributors(int projectThresholdContributors) {
        this.projectThresholdContributors = projectThresholdContributors;
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

    public List<WebFrameLink> getiFramesProjects() {
        return iFramesProjects;
    }

    public void setiFramesProjects(List<WebFrameLink> iFramesProjects) {
        this.iFramesProjects = iFramesProjects;
    }

    public List<WebFrameLink> getiFramesContributors() {
        return iFramesContributors;
    }

    public void setiFramesContributors(List<WebFrameLink> iFramesContributors) {
        this.iFramesContributors = iFramesContributors;
    }

    public List<WebFrameLink> getiFramesProjectsAtStart() {
        return iFramesProjectsAtStart;
    }

    public void setiFramesProjectsAtStart(List<WebFrameLink> iFramesProjectsAtStart) {
        this.iFramesProjectsAtStart = iFramesProjectsAtStart;
    }

    public List<WebFrameLink> getiFramesContributorsAtStart() {
        return iFramesContributorsAtStart;
    }

    public void setiFramesContributorsAtStart(List<WebFrameLink> iFramesContributorsAtStart) {
        this.iFramesContributorsAtStart = iFramesContributorsAtStart;
    }

    public List<ProjectTag> getProjectTags() {
        return projectTags;
    }

    public void setProjectTags(List<ProjectTag> projectTags) {
        this.projectTags = projectTags;
    }

    public boolean isAnonymizeContributors() {
        return anonymizeContributors;
    }

    public void setAnonymizeContributors(boolean anonymizeContributors) {
        this.anonymizeContributors = anonymizeContributors;
    }

    public boolean isShowProjectControls() {
        return showProjectControls;
    }

    public void setShowProjectControls(boolean showProjectControls) {
        this.showProjectControls = showProjectControls;
    }

    public int getProjectsListLimit() {
        return projectsListLimit;
    }

    public void setProjectsListLimit(int projectsListLimit) {
        this.projectsListLimit = projectsListLimit;
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
}
