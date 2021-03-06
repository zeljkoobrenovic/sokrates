/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.scoping.custom;

import nl.obren.sokrates.sourcecode.aspects.Concern;
import nl.obren.sokrates.sourcecode.core.AnalysisConfig;
import nl.obren.sokrates.sourcecode.metrics.MetricsWithGoal;
import nl.obren.sokrates.sourcecode.scoping.Convention;

import java.util.ArrayList;
import java.util.List;

public class CustomScopingConventions {
    private CustomExtensionConventions extensions = new CustomExtensionConventions();
    private int maxLineLength = 0;
    private boolean ignoreStandardScopingConventions = false;
    private boolean removeStandardConcerns = false;
    private String logoLink = "";

    private List<Convention> ignoredFilesConventions = new ArrayList<>();
    private List<Convention> testFilesConventions = new ArrayList<>();
    private List<Convention> generatedFilesConventions = new ArrayList<>();
    private List<Convention> buildAndDeploymentFilesConventions = new ArrayList<>();
    private List<Convention> otherFilesConventions = new ArrayList<>();

    private List<Concern> concerns = new ArrayList<>();
    private List<String> ignoreContributors = new ArrayList<>();

    private int componentsFolderDepth = 1;
    private int minComponentsCount = 0;

    private boolean ignoreStandardControls = false;
    List<MetricsWithGoal> goalsAndControls = new ArrayList<>();

    private AnalysisConfig analysis = new AnalysisConfig();

    public CustomExtensionConventions getExtensions() {
        return extensions;
    }

    public void setExtensions(CustomExtensionConventions extensions) {
        this.extensions = extensions;
    }

    public int getMaxLineLength() {
        return maxLineLength;
    }

    public void setMaxLineLength(int maxLineLength) {
        this.maxLineLength = maxLineLength;
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
}
