package nl.obren.sokrates.cli.git;

import nl.obren.sokrates.sourcecode.githistory.GitHistoryUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;

public class GitHistoryExtractor {
    private static final Log LOG = LogFactory.getLog(GitHistoryExtractor.class);

    public void extractGitHistory(File root) {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        File gitHistoryFile = new File(root, GitHistoryUtils.GIT_HISTORY_FILE_NAME);
        File gitCommitsFile = new File(root, GitHistoryUtils.GIT_COMMITS_FILE_NAME);
        // SimpleDateFormat is not thread-safe, but this loop is single-threaded; hoisting it out
        // of the per-line loop avoids allocating one instance per file change (millions on big repos).
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        long count = 0;
        try {
            LOG.info("Extracting git history...");
            Repository repo = builder.setGitDir(new File(root, ".git")).setMustExist(true).build();
            Git git = new Git(repo);
            Iterable<RevCommit> log = git.log().call();

            // Open the output file once and stream through a buffered writer. The previous code
            // re-opened/closed the file via FileUtils.writeStringToFile(append=true) for every
            // single file-change line, which is O(commits * files) syscalls and dominated runtime.
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                    Files.newOutputStream(gitHistoryFile.toPath()), StandardCharsets.UTF_8), 1 << 16);
                 Writer commitsWriter = new BufferedWriter(new OutputStreamWriter(
                         Files.newOutputStream(gitCommitsFile.toPath()), StandardCharsets.UTF_8), 1 << 16);
                 DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {

                // One DiffFormatter reused across all commits (it is repository-scoped). Use the
                // histogram diff with whitespace-insensitive comparison; detecting renames is the
                // single most expensive option, so leave it off for speed (renamed files still show
                // up under their new path, just without rename linkage).
                diffFormatter.setRepository(repo);
                diffFormatter.setDiffAlgorithm(DiffAlgorithm.getAlgorithm(
                        DiffAlgorithm.SupportedAlgorithm.HISTOGRAM));
                diffFormatter.setDiffComparator(RawTextComparator.WS_IGNORE_ALL);
                diffFormatter.setDetectRenames(false);

                for (RevCommit rev : log) {
                    if (rev.getParentCount() == 0) {
                        continue;
                    }
                    RevCommit prev = rev.getParent(0);
                    PersonIdent authorIdent = rev.getAuthorIdent();
                    String date = format.format(authorIdent.getWhen());
                    String email = authorIdent.getEmailAddress();
                    String safeName = authorIdent.getName().replace(" ", "&nbsp;");
                    String commitId = rev.getId().getName();
                    long countBeforeCommit = count;

                    for (DiffEntry entry : diffFormatter.scan(prev, rev)) {
                        // For a deletion the new path is /dev/null; attribute the removed lines to
                        // the old path so deleting a file still counts as churn under that file.
                        String path = entry.getNewPath();
                        if (path.equals("/dev/null")) {
                            path = entry.getOldPath();
                        }
                        if (path.equals("/dev/null")) {
                            continue;
                        }
                        int added = 0;
                        int deleted = 0;
                        for (Edit edit : diffFormatter.toFileHeader(entry).toEditList()) {
                            added += edit.getEndB() - edit.getBeginB();
                            deleted += edit.getEndA() - edit.getBeginA();
                        }
                        String safePath = path.replace(" ", "&nbsp;");
                        writer.write(date + " " + email + " " + commitId + " "
                                + safePath + " " + safeName + " " + added + " " + deleted + "\n");
                        count++;
                    }
                    // Sidecar with the commit message's first line, only for commits that
                    // produced at least one file-change line (the ones consumers can join on).
                    // See GitHistoryUtils.GIT_COMMITS_FILE_NAME for the format contract.
                    if (count > countBeforeCommit) {
                        String message = rev.getShortMessage().replaceAll("[\\r\\n]+", " ").trim();
                        commitsWriter.write(commitId + " " + message + "\n");
                    }
                }
            }
            LOG.info("Extracted " + count + " file changes");
        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
        }
    }
}
