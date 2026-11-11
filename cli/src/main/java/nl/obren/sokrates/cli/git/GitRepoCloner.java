package nl.obren.sokrates.cli.git;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;

/**
 * Clones a git repository with JGit (no git binary needed), or brings an existing clone up to date,
 * so that the {@code analyzeGitRepo} command can analyze a repository straight from its URL.
 * <p>
 * Credentials for private HTTPS repositories come from the environment: {@link #ENV_TOKEN} holds a
 * personal access token (or password) and the optional {@link #ENV_USER} the user name it belongs
 * to (GitHub accepts any name with a token; GitLab wants {@code oauth2}). SSH URLs use the JGit
 * default SSH setup (~/.ssh keys).
 */
public class GitRepoCloner {
    public static final String ENV_TOKEN = "SOKRATES_GIT_TOKEN";
    public static final String ENV_USER = "SOKRATES_GIT_USER";
    public static final String DEFAULT_USER = "x-access-token";

    private static final Log LOG = LogFactory.getLog(GitRepoCloner.class);

    /**
     * Folder name a clone of {@code url} gets when the caller gives none: the last path segment
     * without a trailing {@code .git} (works for https://host/org/repo.git, ssh scp-style
     * git@host:org/repo.git and file:///path/repo.git).
     */
    public static String folderNameFromUrl(String url) {
        String cleaned = StringUtils.stripEnd(url.trim(), "/\\");
        if (cleaned.endsWith(".git")) {
            cleaned = cleaned.substring(0, cleaned.length() - ".git".length());
        }
        int cut = Math.max(cleaned.lastIndexOf('/'), cleaned.lastIndexOf(':'));
        String name = cut >= 0 ? cleaned.substring(cut + 1) : cleaned;
        name = name.replaceAll("[^A-Za-z0-9._-]", "_");
        return name.isEmpty() ? "repo" : name;
    }

    /**
     * Clones {@code url} into {@code destination}, or, when {@code destination} already holds a git
     * repository, fetches and hard-resets it to the remote branch (the clone is Sokrates' own
     * working copy, so local edits are not expected there). Returns the destination.
     *
     * @param branch the branch to analyze, or blank for the remote's default branch
     * @param depth  a clone depth (shallow clone), or 0 for the full history — the history feeds the
     *               contributor and trend reports, so shallow clones make those incomplete
     */
    public File cloneOrUpdate(String url, File destination, String branch, int depth) throws GitAPIException, IOException {
        CredentialsProvider credentials = credentialsFromEnvironment();
        if (new File(destination, ".git").exists()) {
            update(destination, branch, credentials);
        } else {
            clone(url, destination, branch, depth, credentials);
        }
        return destination;
    }

    private void clone(String url, File destination, String branch, int depth, CredentialsProvider credentials) throws GitAPIException, IOException {
        LOG.info("Cloning " + url + " into " + destination.getPath() + (depth > 0 ? " (depth " + depth + ")" : "") + "...");
        CloneCommand command = Git.cloneRepository().setURI(url).setDirectory(destination);
        if (StringUtils.isNotBlank(branch)) {
            command.setBranch(branch);
        }
        if (depth > 0) {
            command.setDepth(depth);
        }
        if (credentials != null) {
            command.setCredentialsProvider(credentials);
        }
        try (Git git = command.call()) {
            LOG.info("Cloned " + url + " (" + git.getRepository().getBranch() + ")");
        }
    }

    private void update(File destination, String branch, CredentialsProvider credentials) throws GitAPIException, IOException {
        LOG.info(destination.getPath() + " is already a git repository: fetching the latest changes...");
        try (Git git = Git.open(destination)) {
            git.fetch().setRemoveDeletedRefs(true).setCredentialsProvider(credentials).call();
            Repository repository = git.getRepository();
            String target = StringUtils.isNotBlank(branch) ? branch : repository.getBranch();
            String remoteRef = "refs/remotes/origin/" + target;
            if (repository.resolve(remoteRef) == null) {
                throw new IOException("Branch '" + target + "' does not exist on the remote (no " + remoteRef + ")");
            }
            if (!target.equals(repository.getBranch())) {
                boolean exists = repository.resolve("refs/heads/" + target) != null;
                git.checkout().setName(target).setCreateBranch(!exists)
                        .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                        .setStartPoint("origin/" + target).call();
            }
            ObjectId head = git.reset().setMode(ResetCommand.ResetType.HARD).setRef(remoteRef).call().getObjectId();
            LOG.info("Updated " + destination.getPath() + " to " + target + " @ " + head.abbreviate(10).name());
        }
    }

    static CredentialsProvider credentialsFromEnvironment() {
        String token = System.getenv(ENV_TOKEN);
        if (StringUtils.isBlank(token)) {
            return null;
        }
        String user = StringUtils.defaultIfBlank(System.getenv(ENV_USER), DEFAULT_USER);
        return new UsernamePasswordCredentialsProvider(user, token);
    }
}
