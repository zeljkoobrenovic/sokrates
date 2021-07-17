package nl.obren.sokrates.sourcecode.githistory;

public class MergeUpdate {
    private String date = "";
    private String authorEmail = "";

    public MergeUpdate() {
    }

    public MergeUpdate(String date, String authorEmail) {
        this.date = date;
        this.authorEmail = authorEmail;
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

}
