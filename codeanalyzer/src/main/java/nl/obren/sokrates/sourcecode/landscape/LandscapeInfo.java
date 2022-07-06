package nl.obren.sokrates.sourcecode.landscape;

import java.util.ArrayList;
import java.util.List;

public class LandscapeInfo {
    private List<SubLandscapeLink> subLandscapes = new ArrayList<>();
    private List<SokratesRepositoryLink> repositories = new ArrayList<>();

    public LandscapeInfo() {
    }

    public List<SubLandscapeLink> getSubLandscapes() {
        return subLandscapes;
    }

    public void setSubLandscapes(List<SubLandscapeLink> subLandscapes) {
        this.subLandscapes = subLandscapes;
    }

    public List<SokratesRepositoryLink> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<SokratesRepositoryLink> repositories) {
        this.repositories = repositories;
    }

    // legacy tolerant reader
    public void setProjects(List<SokratesRepositoryLink> repositories) {
        this.repositories = repositories;
    }
}
