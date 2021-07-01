/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape;

import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.metrics.MetricsWithGoal;
import nl.obren.sokrates.sourcecode.operations.OperationStatement;

import java.util.ArrayList;
import java.util.List;

public class LandscapeConfiguration {
    private Metadata metadata = new Metadata();
    private String analysisRoot = "";
    private String projectReportsUrlPrefix = "../";
    private String parentUrl = "";

    private int extensionThresholdLoc = 0;
    private int projectThresholdLocMain = 0;
    private int projectThresholdContributors = 2;
    private int contributorThresholdCommits = 2;
    private int commitsMaxYears = 10;
    private boolean anonymizeContributors = false;
    private boolean showProjectControls = true;
    private List<MetricsWithGoal> landscapeControls = new ArrayList<>();
    private String contributorLinkTemplate = "";
    private String contributorAvatarLinkTemplate = "";
    private List<String> ignoreContributors = new ArrayList<>();
    private List<OperationStatement> transformContributorEmails = new ArrayList<>();

    private List<CustomMetric> customMetrics = new ArrayList<>();
    private List<CustomMetric> customMetricsSmall = new ArrayList<>();
    private List<WebFrameLink> iFrames = new ArrayList<>();
    private List<WebFrameLink> iFramesAtStart = new ArrayList<>();
    private List<WebFrameLink> iFramesProjects = new ArrayList<>();
    private List<WebFrameLink> iFramesProjectsAtStart = new ArrayList<>();
    private List<WebFrameLink> iFramesContributors = new ArrayList<>();
    private List<WebFrameLink> iFramesContributorsAtStart = new ArrayList<>();
    private List<CustomTab> customTabs = new ArrayList<>();
    private String customHtmlReportHeaderFragment = "";
    private List<ProjectTag> projectTags = new ArrayList<>();

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

    public String getParentUrl() {
        return parentUrl;
    }

    public void setParentUrl(String parentUrl) {
        this.parentUrl = parentUrl;
    }

    public List<CustomMetric> getCustomMetrics() {
        return customMetrics;
    }

    public void setCustomMetrics(List<CustomMetric> customMetrics) {
        this.customMetrics = customMetrics;
    }

    public List<CustomMetric> getCustomMetricsSmall() {
        return customMetricsSmall;
    }

    public void setCustomMetricsSmall(List<CustomMetric> customMetricsSmall) {
        this.customMetricsSmall = customMetricsSmall;
    }

    public List<WebFrameLink> getiFrames() {
        return iFrames;
    }

    public void setiFrames(List<WebFrameLink> iFrames) {
        this.iFrames = iFrames;
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

    public List<MetricsWithGoal> getLandscapeControls() {
        return landscapeControls;
    }

    public void setLandscapeControls(List<MetricsWithGoal> landscapeControls) {
        this.landscapeControls = landscapeControls;
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
