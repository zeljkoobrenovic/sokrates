package nl.obren.sokrates.cli.git;

import nl.obren.sokrates.sourcecode.Link;
import nl.obren.sokrates.sourcecode.Metadata;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GitRepoMetadataTest {

    @Test
    void parsesHttpsUrls() {
        GitRepoMetadata m = GitRepoMetadata.fromUrl("https://github.com/junit-team/junit4.git");
        assertEquals("junit4", m.getName());
        assertEquals("junit-team", m.getOwner());
        assertEquals("github.com", m.getHost());
        assertEquals("https://github.com/junit-team/junit4", m.getWebUrl());
        assertEquals("https://github.com/junit-team.png?size=200", m.getLogoLink());
        assertTrue(m.isGitHub());
        assertEquals("GitHub", m.linkLabel());
    }

    @Test
    void parsesScpStyleAndSshUrls() {
        GitRepoMetadata scp = GitRepoMetadata.fromUrl("git@github.com:zeljkoobrenovic/sokrates.git");
        assertEquals("sokrates", scp.getName());
        assertEquals("zeljkoobrenovic", scp.getOwner());
        assertEquals("https://github.com/zeljkoobrenovic/sokrates", scp.getWebUrl());

        GitRepoMetadata ssh = GitRepoMetadata.fromUrl("ssh://git@gitlab.example.com:2222/group/sub/project.git");
        assertEquals("project", ssh.getName());
        assertEquals("sub", ssh.getOwner());
        assertEquals("gitlab.example.com", ssh.getHost());
        assertEquals("https://gitlab.example.com/group/sub/project", ssh.getWebUrl());
        assertEquals("", ssh.getLogoLink(), "no avatar convention outside github.com");
        assertEquals("Repository", ssh.linkLabel());
    }

    @Test
    void localRemotesHaveNoWebLink() {
        GitRepoMetadata file = GitRepoMetadata.fromUrl("file:///tmp/repos/remote.git/");
        assertEquals("remote", file.getName());
        assertEquals("", file.getWebUrl());
        assertEquals("", file.getLogoLink());
        assertEquals("remote", GitRepoMetadata.fromUrl("/tmp/repos/remote.git").getName());
        assertNull(GitRepoMetadata.fromUrl("https://github.com/"));
    }

    @Test
    void fillsOnlyWhatTheUserDidNotSet() {
        GitRepoMetadata git = GitRepoMetadata.fromUrl("https://github.com/junit-team/junit4");
        Metadata metadata = new Metadata();
        metadata.setName("Code");
        assertTrue(git.applyTo(metadata, "Code"));
        assertEquals("junit4", metadata.getName());
        assertEquals("https://github.com/junit-team.png?size=200", metadata.getLogoLink());
        assertEquals(1, metadata.getLinks().size());
        assertEquals("https://github.com/junit-team/junit4", metadata.getLinks().get(0).getHref());
        assertEquals("GitHub", metadata.getLinks().get(0).getLabel());
        assertFalse(git.applyTo(metadata, "Code"), "a second application changes nothing");

        Metadata custom = new Metadata();
        custom.setName("My JUnit");
        custom.setLogoLink("https://example.com/logo.png");
        custom.getLinks().add(new Link("Docs", "https://junit.org"));
        assertFalse(git.applyTo(custom, "Code"), "user-set name, logo and links are kept");
        assertEquals("My JUnit", custom.getName());
        assertEquals("https://example.com/logo.png", custom.getLogoLink());
        assertEquals(1, custom.getLinks().size());
    }
}
