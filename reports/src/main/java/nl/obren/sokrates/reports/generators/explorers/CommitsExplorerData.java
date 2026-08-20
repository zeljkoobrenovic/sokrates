package nl.obren.sokrates.reports.generators.explorers;

import java.util.ArrayList;
import java.util.List;

/**
 * The whole payload embedded into commits-explorer.html: the file list (current files of all
 * scopes plus the history-only "deleted" paths referenced by exported commits) and the commit
 * list (newest first, capped at {@link CommitsExplorerGenerators#MAX_COMMITS}).
 * {@code totalCommitsCount} is the uncapped number of commits in the git history so the page can
 * say "newest N of M commits" when truncated.
 */
public class CommitsExplorerData {
    private List<CommitFileExport> files = new ArrayList<>();
    private List<CommitExport> commits = new ArrayList<>();
    private int totalCommitsCount = 0;

    public List<CommitFileExport> getFiles() {
        return files;
    }

    public void setFiles(List<CommitFileExport> files) {
        this.files = files;
    }

    public List<CommitExport> getCommits() {
        return commits;
    }

    public void setCommits(List<CommitExport> commits) {
        this.commits = commits;
    }

    public int getTotalCommitsCount() {
        return totalCommitsCount;
    }

    public void setTotalCommitsCount(int totalCommitsCount) {
        this.totalCommitsCount = totalCommitsCount;
    }
}
