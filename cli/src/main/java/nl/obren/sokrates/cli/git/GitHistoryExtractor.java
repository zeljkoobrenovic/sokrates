package nl.obren.sokrates.cli.git;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class GitHistoryExtractor {
    private static final Log LOG = LogFactory.getLog(GitHistoryExtractor.class);

    public void extractGitHistory(File root) {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        File gitHistoryFile = new File(root, "git-history.txt");
        try {
            LOG.info("Extracted git history...");
            FileUtils.writeStringToFile(gitHistoryFile, "", StandardCharsets.UTF_8);
            Repository repo = builder.setGitDir(new File(root, ".git")).setMustExist(true).build();
            Git git = new Git(repo);
            Iterable<RevCommit> log = git.log().call();
            AtomicInteger count = new AtomicInteger();
            for (Iterator<RevCommit> iterator = log.iterator(); iterator.hasNext(); ) {
                RevCommit rev = iterator.next();
                if (rev.getParentCount() == 0) {
                    continue;
                }
                RevCommit prev = rev.getParent(0);
                List<FileChange> changes = new ArrayList<>();
                try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
                    diffFormatter.setRepository(repo);
                    diffFormatter.setDetectRenames(true);
                    for (DiffEntry entry : diffFormatter.scan(prev, rev)) {
                        String newPath = entry.getNewPath();
                        if (!newPath.equals("/dev/null")) {
                            int added = 0;
                            int deleted = 0;
                            for (Edit edit : diffFormatter.toFileHeader(entry).toEditList()) {
                                added += edit.getEndB() - edit.getBeginB();
                                deleted += edit.getEndA() - edit.getBeginA();
                            }
                            changes.add(new FileChange(newPath, added, deleted));
                        }
                    }
                }

                changes.forEach(change -> {
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                    PersonIdent authorIdent = rev.getAuthorIdent();
                    String safePath = change.path.replace(" ", "&nbsp;");
                    String safeName = authorIdent.getName().replace(" ", "&nbsp;");
                    String email = authorIdent.getEmailAddress();
                    String line = format.format(authorIdent.getWhen()) + " "
                            + email + " "
                            + rev.getId().getName() + " " + safePath + " " + safeName
                            + " " + change.added + " " + change.deleted;
                    try {
                        FileUtils.writeStringToFile(gitHistoryFile, line + "\n", StandardCharsets.UTF_8, true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    count.incrementAndGet();
                });
            }
            LOG.info("Extracted " + count.get() + " commits");
        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
        }
    }

    private static class FileChange {
        final String path;
        final int added;
        final int deleted;

        FileChange(String path, int added, int deleted) {
            this.path = path;
            this.added = added;
            this.deleted = deleted;
        }
    }

    private AbstractTreeIterator getCanonicalTreeParser(Repository repo, ObjectId commitId) throws IOException {
        try (RevWalk walk = new RevWalk(repo)) {
            RevCommit commit = walk.parseCommit(commitId);
            ObjectId treeId = commit.getTree().getId();
            try (ObjectReader reader = repo.newObjectReader()) {
                return new CanonicalTreeParser(null, reader, treeId);
            }
        }
    }
}
