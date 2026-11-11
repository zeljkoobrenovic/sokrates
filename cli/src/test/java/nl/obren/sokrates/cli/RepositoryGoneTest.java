package nl.obren.sokrates.cli;

import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.NoRemoteRepositoryException;
import org.eclipse.jgit.transport.URIish;
import org.junit.jupiter.api.Test;

import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;

/** Which clone failures mean "the repository is gone" (prunable) and which are transient. Offline: no github.com URLs. */
class RepositoryGoneTest {

    @Test
    void aMissingRemoteIsGone() throws Exception {
        Throwable failure = new InvalidRemoteException("Invalid remote: origin",
                new NoRemoteRepositoryException(new URIish("file:///tmp/nope.git"), "not found."));
        assertTrue(GitRepoCommands.repositoryGone("file:///tmp/nope.git", failure));
    }

    @Test
    void networkAndAuthenticationFailuresAreNotGone() throws Exception {
        Throwable unknownHost = new TransportException("https://gitlab.example.com/a/b.git: cannot open git-upload-pack",
                new UnknownHostException("gitlab.example.com"));
        assertFalse(GitRepoCommands.repositoryGone("https://gitlab.example.com/a/b.git", unknownHost));

        Throwable auth = new TransportException("https://gitlab.example.com/a/b.git: not authorized");
        assertFalse(GitRepoCommands.repositoryGone("https://gitlab.example.com/a/b.git", auth));
    }

    @Test
    void aNotFoundMessageFromANonGitHubHostIsGone() {
        Throwable notFound = new TransportException("https://gitlab.example.com/a/b.git: Git repository not found");
        assertTrue(GitRepoCommands.repositoryGone("https://gitlab.example.com/a/b.git", notFound));
    }
}
