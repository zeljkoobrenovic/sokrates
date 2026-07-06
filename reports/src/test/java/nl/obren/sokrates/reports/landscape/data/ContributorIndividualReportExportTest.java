package nl.obren.sokrates.reports.landscape.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.sourcecode.contributors.Contributor;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.landscape.PeopleConfig;
import nl.obren.sokrates.sourcecode.landscape.PersonConfig;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorRepositories;
import org.junit.jupiter.api.Test;

import java.util.List;

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
    void configPeopleUserNameOverridesCommitUserName() {
        Contributor c = new Contributor("alice@example.com");
        c.setUserName("alice (commit name)");

        PersonConfig person = new PersonConfig();
        person.setEmail("alice@example.com");
        person.setEmailPatterns(List.of("alice@example[.]com"));
        person.setUserName("Alice Configured");
        PeopleConfig peopleConfig = new PeopleConfig();
        peopleConfig.setPeople(List.of(person));

        ContributorIndividualReportExport export = new ContributorIndividualReportExport(
                new ContributorRepositories(c), new LandscapeConfiguration(), peopleConfig);

        assertEquals("Alice Configured", export.getUserName());
    }

    @Test
    void keepsCommitUserNameWhenConfigUserNameIsBlank() {
        Contributor c = new Contributor("alice@example.com");
        c.setUserName("alice (commit name)");

        PersonConfig person = new PersonConfig();
        person.setEmail("alice@example.com");
        person.setEmailPatterns(List.of("alice@example[.]com"));
        PeopleConfig peopleConfig = new PeopleConfig();
        peopleConfig.setPeople(List.of(person));

        ContributorIndividualReportExport export = new ContributorIndividualReportExport(
                new ContributorRepositories(c), new LandscapeConfiguration(), peopleConfig);

        assertEquals("alice (commit name)", export.getUserName());
    }

    @Test
    void exportsMemberUserNameAndEmail() {
        Contributor teamContributor = new Contributor("Team Alpha");
        teamContributor.setCommitsCount(100);
        ContributorRepositories team = new ContributorRepositories(teamContributor);

        Contributor member = new Contributor("alice@example.com");
        member.setUserName("Alice Example");
        member.setCommitsCount(50);
        team.getMembers().add(new ContributorRepositories(member));

        ContributorIndividualReportExport export = new ContributorIndividualReportExport(
                team, new LandscapeConfiguration(), null);

        assertEquals(1, export.getMembers().size());
        ContributorIndividualReportExport.Member m = export.getMembers().get(0);
        assertEquals("alice@example.com", m.getEmail());
        assertEquals("Alice Example", m.getUserName());
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
