package nl.obren.sokrates.reports.landscape.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.landscape.ContributorTag;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorRepositories;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContributorReportExportTest {

    private ContributorRepositories sampleContributor() {
        Contributor c = new Contributor("alice@example.com");
        c.setCommitsCount(500);
        c.setCommitsCount30Days(5);
        c.setCommitsCount90Days(12);
        c.setCommitsCount365Days(40);
        c.setFirstCommitDate("2020-01-01");
        c.setLatestCommitDate("2025-05-01");
        return new ContributorRepositories(c);
    }

    @Test
    void exportsCoreContributorFields() {
        ContributorReportExport export = new ContributorReportExport(
                sampleContributor(), new LandscapeConfiguration(), null, null, Collections.emptyList());

        assertEquals("alice@example.com", export.getEmail());
        assertEquals(500, export.getCommitsCount());
        assertEquals(5, export.getCommitsCount30Days());
        assertEquals(12, export.getCommitsCount90Days());
        assertEquals(40, export.getCommitsCount365Days());
        assertEquals("2020-01-01", export.getFirstCommitDate());
        assertEquals("2025-05-01", export.getLatestCommitDate());
        assertEquals(0, export.getRepositoriesCount());
        assertEquals(0, export.getRepositoriesCount30Days());
        assertTrue(export.getReportUrl().contains("contributors/"));
    }

    @Test
    void appliesTagRulesMatchingTheEmail() {
        ContributorTag rule = new ContributorTag();
        rule.setName("example");
        rule.setPatterns(List.of(".*@example[.]com"));

        ContributorReportExport export = new ContributorReportExport(
                sampleContributor(), new LandscapeConfiguration(), null, null, List.of(rule));

        assertEquals(List.of("example"), export.getTags());
    }

    @Test
    void serializesToValidJsonWithExpectedFields() throws Exception {
        ContributorReportExport export = new ContributorReportExport(
                sampleContributor(), new LandscapeConfiguration(), null, null, Collections.emptyList());
        String json = new JsonGenerator().generateCompressed(export);

        JsonNode node = new ObjectMapper().readTree(json);
        assertEquals("alice@example.com", node.get("email").asText());
        assertEquals(500, node.get("commitsCount").asInt());
        assertEquals(5, node.get("commitsCount30Days").asInt());
        assertTrue(node.has("mainLang"));
        assertTrue(node.has("repositoriesCount30Days"));
        assertTrue(node.has("reportUrl"));
    }
}
