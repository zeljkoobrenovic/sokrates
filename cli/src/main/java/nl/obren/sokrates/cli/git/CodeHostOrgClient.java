package nl.obren.sokrates.cli.git;

import java.io.IOException;
import java.util.List;

/**
 * What the organization-level analysis commands (analyzeGitHubOrg, analyzeGitLabGroup) need from a
 * code host: the organization's profile and the list of its repositories. Implementations are
 * replaceable on the CLI so the commands are tested offline.
 */
public interface CodeHostOrgClient {
    /** The organization's (group's, user's) profile. */
    CodeHostOrg fetchOrg(String login) throws IOException;

    /** Every repository of the organization the credentials can see, before any filtering. */
    List<CodeHostRepo> listRepos(String login) throws IOException;

    /** "GitHub", "GitLab": for logs and the landscape link label. */
    String hostLabel();
}
