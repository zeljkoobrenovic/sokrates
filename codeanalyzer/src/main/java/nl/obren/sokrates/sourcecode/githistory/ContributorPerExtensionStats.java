package nl.obren.sokrates.sourcecode.githistory;

public class ContributorPerExtensionStats {
    private String contributor = "";
    private int fileUpdates30Days = 0;
    private int fileUpdates90Days = 0;
    private int fileUpdates = 0;

    public ContributorPerExtensionStats() {
    }

    public ContributorPerExtensionStats(String contributor) {
        this.contributor = contributor;
    }

    public String getContributor() {
        return contributor;
    }

    public void setContributor(String contributor) {
        this.contributor = contributor;
    }

    public int getFileUpdates30Days() {
        return fileUpdates30Days;
    }

    public void setFileUpdates30Days(int fileUpdates30Days) {
        this.fileUpdates30Days = fileUpdates30Days;
    }

    public int getFileUpdates90Days() {
        return fileUpdates90Days;
    }

    public void setFileUpdates90Days(int fileUpdates90Days) {
        this.fileUpdates90Days = fileUpdates90Days;
    }

    public int getFileUpdates() {
        return fileUpdates;
    }

    public void setFileUpdates(int fileUpdates) {
        this.fileUpdates = fileUpdates;
    }
}
