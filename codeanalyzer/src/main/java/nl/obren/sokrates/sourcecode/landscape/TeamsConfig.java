package nl.obren.sokrates.sourcecode.landscape;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

public class TeamsConfig {
    private List<TeamConfig> teams = new ArrayList<>();

    public TeamsConfig() {
    }

    public List<TeamConfig> getTeams() {
        return teams;
    }

    public void setTeams(List<TeamConfig> teams) {
        this.teams = teams;
    }

    @JsonIgnore
    public String getTeam(String email) {
        return getTeam(email, null);
    }

    // Match a contributor to a team by email patterns OR userName patterns. Pass the userName that is
    // available (post config-people.json transformation where applicable); null when only the email
    // is known.
    @JsonIgnore
    public String getTeam(String email, String userName) {
        for (TeamConfig team : teams) {
            if (team.matches(email, userName)) {
                return team.getName();
            }
        }
        return null;
    }
}
