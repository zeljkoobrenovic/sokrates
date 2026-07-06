package nl.obren.sokrates.sourcecode.landscape;

import nl.obren.sokrates.common.utils.RegexUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class TeamConfig {
    private String name = "";
    private String description = "";
    private List<String> emailPatterns = new ArrayList<>();
    // Regex patterns matched against the contributor's userName. A contributor joins this team when
    // their email matches any emailPattern OR their userName matches any of these. Matching happens
    // AFTER config-people.json transformations, so both the email and userName here are the
    // people-config-canonical values (falling back to the original commit values when no person
    // config applied).
    private List<String> userNamePatterns = new ArrayList<>();

    public TeamConfig() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    // A contributor matches this team when their email matches any emailPattern OR their userName
    // matches any userNamePattern. Shared by every team-assignment site so the rule stays identical.
    // Matching is case-insensitive (identity matching on emails/userNames).
    public boolean matches(String email, String userName) {
        return RegexUtils.matchesAnyPatternIgnoreCase(email, emailPatterns)
                || (StringUtils.isNotBlank(userName) && RegexUtils.matchesAnyPatternIgnoreCase(userName, userNamePatterns));
    }
}
