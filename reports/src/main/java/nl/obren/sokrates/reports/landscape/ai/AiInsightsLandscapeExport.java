package nl.obren.sokrates.reports.landscape.ai;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

/** The landscape-level view of the AI scanner results: every repository that has some, and every finding (the embedded data of ai-insights.html, also data/ai-insights.json). */
public class AiInsightsLandscapeExport {
    private List<AiRepositoryExport> repositories = new ArrayList<>();
    private List<AiFindingExport> findings = new ArrayList<>();

    public List<AiRepositoryExport> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<AiRepositoryExport> repositories) {
        this.repositories = repositories;
    }

    public List<AiFindingExport> getFindings() {
        return findings;
    }

    public void setFindings(List<AiFindingExport> findings) {
        this.findings = findings;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return findings.isEmpty();
    }
}
