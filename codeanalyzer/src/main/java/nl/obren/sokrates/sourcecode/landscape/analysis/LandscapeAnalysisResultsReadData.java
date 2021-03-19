/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.landscape.analysis;

public class LandscapeAnalysisResultsReadData {
    private String latestCommitDate = "";
    private int commitsCount = 0;
    private int commitsCount30Days = 0;
    private int contributorsCount = 0;
    private int projectsCount = 0;
    private int mainLoc = 0;
    private int mainLocActive = 0;
    private int mainLocNew = 0;
    private int testLoc = 0;
    private int generatedLoc = 0;
    private int buildAndDeploymentLoc = 0;
    private int otherLoc = 0;
    private int allLoc = 0;
    private int recentContributorsCount = 0;
    private int recentContributorsCount6Months = 0;
    private int recentContributorsCount3Months = 0;
    private int rookiesContributorsCount = 0;

    public String getLatestCommitDate() {
        return latestCommitDate;
    }

    public void setLatestCommitDate(String latestCommitDate) {
        this.latestCommitDate = latestCommitDate;
    }

    public int getCommitsCount() {
        return commitsCount;
    }

    public void setCommitsCount(int commitsCount) {
        this.commitsCount = commitsCount;
    }

    public int getCommitsCount30Days() {
        return commitsCount30Days;
    }

    public void setCommitsCount30Days(int commitsCount30Days) {
        this.commitsCount30Days = commitsCount30Days;
    }

    public int getContributorsCount() {
        return contributorsCount;
    }

    public void setContributorsCount(int contributorsCount) {
        this.contributorsCount = contributorsCount;
    }

    public int getProjectsCount() {
        return projectsCount;
    }

    public void setProjectsCount(int projectsCount) {
        this.projectsCount = projectsCount;
    }

    public int getMainLoc() {
        return mainLoc;
    }

    public void setMainLoc(int mainLoc) {
        this.mainLoc = mainLoc;
    }

    public int getMainLocActive() {
        return mainLocActive;
    }

    public void setMainLocActive(int mainLocActive) {
        this.mainLocActive = mainLocActive;
    }

    public int getMainLocNew() {
        return mainLocNew;
    }

    public void setMainLocNew(int mainLocNew) {
        this.mainLocNew = mainLocNew;
    }

    public int getTestLoc() {
        return testLoc;
    }

    public void setTestLoc(int testLoc) {
        this.testLoc = testLoc;
    }

    public int getGeneratedLoc() {
        return generatedLoc;
    }

    public void setGeneratedLoc(int generatedLoc) {
        this.generatedLoc = generatedLoc;
    }

    public int getBuildAndDeploymentLoc() {
        return buildAndDeploymentLoc;
    }

    public void setBuildAndDeploymentLoc(int buildAndDeploymentLoc) {
        this.buildAndDeploymentLoc = buildAndDeploymentLoc;
    }

    public int getOtherLoc() {
        return otherLoc;
    }

    public void setOtherLoc(int otherLoc) {
        this.otherLoc = otherLoc;
    }

    public int getAllLoc() {
        return allLoc;
    }

    public void setAllLoc(int allLoc) {
        this.allLoc = allLoc;
    }

    public int getRecentContributorsCount() {
        return recentContributorsCount;
    }

    public void setRecentContributorsCount(int recentContributorsCount) {
        this.recentContributorsCount = recentContributorsCount;
    }

    public int getRecentContributorsCount6Months() {
        return recentContributorsCount6Months;
    }

    public void setRecentContributorsCount6Months(int recentContributorsCount6Months) {
        this.recentContributorsCount6Months = recentContributorsCount6Months;
    }

    public int getRecentContributorsCount3Months() {
        return recentContributorsCount3Months;
    }

    public void setRecentContributorsCount3Months(int recentContributorsCount3Months) {
        this.recentContributorsCount3Months = recentContributorsCount3Months;
    }

    public int getRookiesContributorsCount() {
        return rookiesContributorsCount;
    }

    public void setRookiesContributorsCount(int rookiesContributorsCount) {
        this.rookiesContributorsCount = rookiesContributorsCount;
    }
}
