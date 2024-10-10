package nl.obren.sokrates.reports.landscape.data;

import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorRepositories;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorRepositoryInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ContributorExport {
    private Contributor contributor = new Contributor();

    private String mainLang = "";

    private List<ContributorRepositoryInfoExport> repositories = new ArrayList<>();

    public ContributorExport(ContributorRepositories contributor) {
        this.contributor = contributor.getContributor();
        this.repositories = contributor.getRepositories().stream().map(r -> new ContributorRepositoryInfoExport(r)).collect(Collectors.toList());
    }

    public Contributor getContributor() {
        return contributor;
    }

    public void setContributor(Contributor contributor) {
        this.contributor = contributor;
    }

    public List<ContributorRepositoryInfoExport> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<ContributorRepositoryInfoExport> repositories) {
        this.repositories = repositories;
    }

    public String getMainLang() {
        return mainLang;
    }

    public void setMainLang(String mainLang) {
        this.mainLang = mainLang;
    }
}