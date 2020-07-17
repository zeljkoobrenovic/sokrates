package nl.obren.sokrates.sourcecode.githistory;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class AuthorCommit {
    private String date = "";
    private String authorEmail = "";

    public AuthorCommit() {
    }

    public AuthorCommit(String date, String authorEmail) {
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

    @JsonIgnore
    public String getYear() {
        return getDate().length() >= 4 ? getDate().substring(0, 4) : "";
    }

}
