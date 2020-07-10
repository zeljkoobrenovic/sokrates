package nl.obren.sokrates.sourcecode.analysis.results;

import nl.obren.sokrates.sourcecode.contributors.Contributor;

import java.util.ArrayList;
import java.util.List;

public class ContributorsAnalysisResults {
    private List<Contributor> contributors = new ArrayList<>();

    public List<Contributor> getContributors() {
        return contributors;
    }

    public void setContributors(List<Contributor> contributors) {
        this.contributors = contributors;
    }
}
