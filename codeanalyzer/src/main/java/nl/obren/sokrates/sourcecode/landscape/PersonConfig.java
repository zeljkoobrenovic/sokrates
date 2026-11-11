package nl.obren.sokrates.sourcecode.landscape;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import nl.obren.sokrates.sourcecode.Link;
import org.apache.commons.lang3.StringUtils;

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
    // Regex patterns matched against the contributor's commit userName. A contributor is assigned to
    // this person when their email matches any emailPattern OR their userName matches any of these.
    private List<String> userNamePatterns = new ArrayList<>();

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

    // Backward compatibility: legacy config-people.json used a single "name" field as the display
    // name (what "userName" now holds). Deserialize it into userName so old configs keep showing
    // their display names. Only fills userName when not already set by an explicit "userName".
    @JsonSetter("name")
    public void setName(String name) {
        if (StringUtils.isBlank(this.userName)) {
            this.userName = name;
        }
    }

    public List<Link> getLinks() {
        return links;
    }

    // The two setLink overloads both map to Jackson property "link", which makes Jackson throw
    // "Conflicting setter definitions for property link" and abort deserialization of the whole
    // PeopleConfig. Bind them to distinct properties explicitly: the String overload is the legacy
    // single "link", the List overload is "links" (paired with getLinks()).
    @JsonProperty("link")
    public void setLink(String link) {
        this.link = link;
    }

    @JsonProperty("links")
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

    public List<String> getUserNamePatterns() {
        return userNamePatterns;
    }

    public void setUserNamePatterns(List<String> userNamePatterns) {
        this.userNamePatterns = userNamePatterns;
    }
}
