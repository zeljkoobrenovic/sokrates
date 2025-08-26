package nl.obren.sokrates.sourcecode.githistory;

import nl.obren.sokrates.sourcecode.ExtensionGroupExtractor;

public class FileUpdate {
    private String date = "";
    private String authorEmail = "";
    private String userName = "";
    private String commitId = "";
    private String path = "";

    private boolean bot = false;

    public FileUpdate(String date, String authorEmail, String userName, String commitId, String path, boolean bot) {
        this.date = date;
        this.authorEmail = authorEmail;
        this.userName = userName;
        this.commitId = commitId;
        this.path = path;
        this.bot = bot;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCommitId() {
        return commitId;
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    public String getExtension() {
        return ExtensionGroupExtractor.getExtension(getPath());
    }

    public boolean isBot() {
        return bot;
    }

    public void setBot(boolean bot) {
        this.bot = bot;
    }
}
