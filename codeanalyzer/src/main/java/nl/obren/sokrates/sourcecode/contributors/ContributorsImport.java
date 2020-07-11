/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.contributors;

import java.util.ArrayList;
import java.util.List;

public class ContributorsImport {
    private List<Contributor> contributors = new ArrayList<>();
    private List<ContributionYear> contributorsPerYear = new ArrayList<>();
    private List<ContributionYear> rookiesPerYear = new ArrayList<>();
    private List<ContributionYear> leaversPerYear = new ArrayList<>();

    public List<Contributor> getContributors() {
        return contributors;
    }

    public void setContributors(List<Contributor> contributors) {
        this.contributors = contributors;
    }

    public List<ContributionYear> getContributorsPerYear() {
        return contributorsPerYear;
    }

    public void setContributorsPerYear(List<ContributionYear> contributorsPerYear) {
        this.contributorsPerYear = contributorsPerYear;
    }

    public List<ContributionYear> getRookiesPerYear() {
        return rookiesPerYear;
    }

    public void setRookiesPerYear(List<ContributionYear> rookiesPerYear) {
        this.rookiesPerYear = rookiesPerYear;
    }

    public List<ContributionYear> getLeaversPerYear() {
        return leaversPerYear;
    }

    public void setLeaversPerYear(List<ContributionYear> leaversPerYear) {
        this.leaversPerYear = leaversPerYear;
    }
}
