package nl.obren.sokrates.reports.generators.explorers;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * One commit row for the commits explorer (commits-explorer.html), reconstructed from
 * git-history.txt (one line per changed file per commit, grouped by commit sha). The sha is
 * shortened to 10 characters to keep the embedded payload small (full 40-char hex compresses
 * poorly); {@code fileIds} are indices into the explorer's file list (see
 * {@link CommitFileExport}); {@code message} is the first line of the commit message when the
 * git-commits.txt sidecar exists (newer extractions), otherwise empty. git-history.txt carries
 * no time of day, and merge commits are not extracted, so neither appears here. {@code coAuthors}
 * (omitted when empty) come from the git-commit-trailers.txt sidecar (see CoAuthorExport).
 */
public class CommitExport {
    private String sha = "";
    private String date = "";
    private String email = "";
    private String userName = "";
    // First line of the commit message, from the optional git-commits.txt sidecar; empty when
    // the history was extracted by an older version (the explorer then hides the column).
    private String message = "";
    private boolean bot = false;
    private int linesAdded = 0;
    private int linesDeleted = 0;
    private List<Integer> fileIds = new ArrayList<>();
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<CoAuthorExport> coAuthors = new ArrayList<>();

    public List<CoAuthorExport> getCoAuthors() {
        return coAuthors;
    }

    public void setCoAuthors(List<CoAuthorExport> coAuthors) {
        this.coAuthors = coAuthors == null ? new ArrayList<>() : coAuthors;
    }

    public String getSha() {
        return sha;
    }

    public void setSha(String sha) {
        this.sha = sha;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isBot() {
        return bot;
    }

    public void setBot(boolean bot) {
        this.bot = bot;
    }

    public int getLinesAdded() {
        return linesAdded;
    }

    public void setLinesAdded(int linesAdded) {
        this.linesAdded = linesAdded;
    }

    public int getLinesDeleted() {
        return linesDeleted;
    }

    public void setLinesDeleted(int linesDeleted) {
        this.linesDeleted = linesDeleted;
    }

    public List<Integer> getFileIds() {
        return fileIds;
    }

    public void setFileIds(List<Integer> fileIds) {
        this.fileIds = fileIds;
    }
}
