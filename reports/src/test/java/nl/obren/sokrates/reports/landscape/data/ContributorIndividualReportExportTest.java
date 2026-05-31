package nl.obren.sokrates.reports.landscape.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorRepositories;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContributorIndividualReportExportTest {

    private ContributorRepositories sampleContributor() {
        Contributor c = new Contributor("alice@example.com");
        c.setCommitsCount(500);
        c.setCommitsCount30Days(5);
        c.setCommitsCount90Days(12);
        c.setCommitsCount180Days(20);
        c.setCommitsCount365Days(40);
        c.setFirstCommitDate("2022-01-03");
        c.setLatestCommitDate("2025-05-05");
        return new ContributorRepositories(c);
    }

    @Test
    void exportsHeaderFields() {
        ContributorIndividualReportExport export = new ContributorIndividualReportExport(
                sampleContributor(), new LandscapeConfiguration(), null);

        assertEquals("alice@example.com", export.getEmail());
        assertEquals(500, export.getCommitsCount());
        assertEquals(5, export.getCommitsCount30Days());
        assertEquals(40, export.getCommitsCount365Days());
        assertEquals("2022-01-03", export.getFirstCommitDate());
        assertEquals("2025-05-05", export.getLatestCommitDate());
        assertEquals(0, export.getRepositoriesCount());
        assertNotNull(export.getRepositories());
        assertNotNull(export.getMembers());
        assertTrue(export.getMembers().isEmpty());
    }

    @Test
    void serializesToValidJsonWithExpectedShape() throws Exception {
        ContributorIndividualReportExport export = new ContributorIndividualReportExport(
                sampleContributor(), new LandscapeConfiguration(), null);
        String json = new JsonGenerator().generateCompressed(export);

        JsonNode node = new ObjectMapper().readTree(json);
        assertEquals("alice@example.com", node.get("email").asText());
        assertTrue(node.has("commitsCount30Days"));
        assertTrue(node.has("repositoriesCount365Days"));
        assertTrue(node.get("repositories").isArray());
        assertTrue(node.get("members").isArray());
        assertTrue(node.get("extensions").isArray());
    }
}
