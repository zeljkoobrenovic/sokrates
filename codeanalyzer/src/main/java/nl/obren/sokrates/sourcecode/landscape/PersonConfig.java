package nl.obren.sokrates.sourcecode.landscape;

import nl.obren.sokrates.sourcecode.Link;

import java.util.ArrayList;
import java.util.List;

public class PersonConfig {
    // Identity of the person — replaces the matched contributor email/id (this is what the old
    // "name" field did). Used as the contributor key after email-pattern matching.
    private String email = "";
    // Optional display name. When set, it replaces the contributor's userName (from commits) in
    // the reports. Default "" (keep the commit-derived userName).
    private String userName = "";
    private String link = "";
    private List<Link> links = new ArrayList<>();
    private String image = "";
    private List<String> emailPatterns = new ArrayList<>();

    public PersonConfig() {
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

    public List<Link> getLinks() {
        return links;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setLink(List<Link> links) {
        this.links = links;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<String> getEmailPatterns() {
        return emailPatterns;
    }

    public void setEmailPatterns(List<String> emailPatterns) {
        this.emailPatterns = emailPatterns;
    }
}
