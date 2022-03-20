/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.scoping.custom;

import nl.obren.sokrates.sourcecode.analysis.FileHistoryAnalysisConfig;
import nl.obren.sokrates.sourcecode.aspects.Concern;
import nl.obren.sokrates.sourcecode.core.AnalysisConfig;
import nl.obren.sokrates.sourcecode.metrics.MetricsWithGoal;
import nl.obren.sokrates.sourcecode.scoping.Convention;

import java.util.ArrayList;
import java.util.List;

public class CustomScopingConventions {
    // Lists of file extensions that Sokrates will include or exclude rom scoping
    private CustomExtensionConventions extensions = new CustomExtensionConventions();

    // If true, Sokrates will use only custom conventions. If false, Sokrates will combine custom conventions with standard ones.
    private boolean ignoreStandardScopingConventions = false;

    // If true, Sokrates will remove any added features of interest (e.g TODOs).
    private boolean removeStandardConcerns = false;

    // A link to the logo image that Sokrates will use in headers of all HTML reports.
    private String logoLink = "";

    // A list of scoping conventions that Sokrates will use to exclude files from analyses.
    private List<Convention> ignoredFilesConventions = new ArrayList<>();

    // A list of scoping conventions that Sokrates will use to identify test files.
    private List<Convention> testFilesConventions = new ArrayList<>();

    // A list of scoping conventions that Sokrates will use to identify generated files.
    private List<Convention> generatedFilesConventions = new ArrayList<>();

    // A list of scoping conventions that Sokrates will use to identify build and deployment files.
    private List<Convention> buildAndDeploymentFilesConventions = new ArrayList<>();

    // A list of scoping conventions that Sokrates will use to identify other files.
    private List<Convention> otherFilesConventions = new ArrayList<>();

    // Features of interest that Sokrates will include in the configuration file.
    private List<Concern> concerns = new ArrayList<>();

    // A list of regex expressions that Sokrates uses to configure ignoring of contributors (e.g. bots)
    private List<String> ignoreContributors = new ArrayList<>();

    // Sokrates uses this value to define components by folder depth
    private int componentsFolderDepth = 1;

    // A minimal number of components. If bigger than 0, Sokrates will ignore componentsFolderDepth and, if feasible, look for a folder depth that generates at least the desired number of components.
    private int minComponentsCount = 0;

    // If true, Sokrates will remove any standard goals and controls, and add only the custom defined ones.
    private boolean ignoreStandardControls = false;

    // Custom goals and controls
    List<MetricsWithGoal> goalsAndControls = new ArrayList<>();

    // Default settings of file history analysis parameters
    private FileHistoryAnalysisConfig fileHistoryAnalysis = new FileHistoryAnalysisConfig();

    // Default settings of analysis parameters
    private AnalysisConfig analysis = new AnalysisConfig();

    public CustomExtensionConventions getExtensions() {
        return extensions;
    }

    public void setExtensions(CustomExtensionConventions extensions) {
        this.extensions = extensions;
    }

    public boolean isIgnoreStandardScopingConventions() {
        return ignoreStandardScopingConventions;
    }

    public void setIgnoreStandardScopingConventions(boolean ignoreStandardScopingConventions) {
        this.ignoreStandardScopingConventions = ignoreStandardScopingConventions;
    }

    public boolean isRemoveStandardConcerns() {
        return removeStandardConcerns;
    }

    public void setRemoveStandardConcerns(boolean removeStandardConcerns) {
        this.removeStandardConcerns = removeStandardConcerns;
    }

    public List<Convention> getIgnoredFilesConventions() {
        return ignoredFilesConventions;
    }

    public void setIgnoredFilesConventions(List<Convention> ignoredFilesConventions) {
        this.ignoredFilesConventions = ignoredFilesConventions;
    }

    public List<Convention> getTestFilesConventions() {
        return testFilesConventions;
    }

    public void setTestFilesConventions(List<Convention> testFilesConventions) {
        this.testFilesConventions = testFilesConventions;
    }

    public List<Convention> getGeneratedFilesConventions() {
        return generatedFilesConventions;
    }

    public void setGeneratedFilesConventions(List<Convention> generatedFilesConventions) {
        this.generatedFilesConventions = generatedFilesConventions;
    }

    public List<Convention> getBuildAndDeploymentFilesConventions() {
        return buildAndDeploymentFilesConventions;
    }

    public void setBuildAndDeploymentFilesConventions(List<Convention> buildAndDeploymentFilesConventions) {
        this.buildAndDeploymentFilesConventions = buildAndDeploymentFilesConventions;
    }

    public List<Convention> getOtherFilesConventions() {
        return otherFilesConventions;
    }

    public void setOtherFilesConventions(List<Convention> otherFilesConventions) {
        this.otherFilesConventions = otherFilesConventions;
    }

    public List<Concern> getConcerns() {
        return concerns;
    }

    public void setConcerns(List<Concern> concerns) {
        this.concerns = concerns;
    }

    public List<String> getIgnoreContributors() {
        return ignoreContributors;
    }

    public void setIgnoreContributors(List<String> ignoreContributors) {
        this.ignoreContributors = ignoreContributors;
    }

    public AnalysisConfig getAnalysis() {
        return analysis;
    }

    public void setAnalysis(AnalysisConfig analysis) {
        this.analysis = analysis;
    }

    public String getLogoLink() {
        return logoLink;
    }

    public void setLogoLink(String logoLink) {
        this.logoLink = logoLink;
    }

    public int getMinComponentsCount() {
        return minComponentsCount;
    }

    public void setMinComponentsCount(int minComponentsCount) {
        this.minComponentsCount = minComponentsCount;
    }

    public int getComponentsFolderDepth() {
        return componentsFolderDepth;
    }

    public void setComponentsFolderDepth(int componentsFolderDepth) {
        this.componentsFolderDepth = componentsFolderDepth;
    }

    public boolean isIgnoreStandardControls() {
        return ignoreStandardControls;
    }

    public void setIgnoreStandardControls(boolean ignoreStandardControls) {
        this.ignoreStandardControls = ignoreStandardControls;
    }

    public List<MetricsWithGoal> getGoalsAndControls() {
        return goalsAndControls;
    }

    public void setGoalsAndControls(List<MetricsWithGoal> goalsAndControls) {
        this.goalsAndControls = goalsAndControls;
    }

    public FileHistoryAnalysisConfig getFileHistoryAnalysis() {
        return fileHistoryAnalysis;
    }

    public void setFileHistoryAnalysis(FileHistoryAnalysisConfig fileHistoryAnalysis) {
        this.fileHistoryAnalysis = fileHistoryAnalysis;
    }
}
