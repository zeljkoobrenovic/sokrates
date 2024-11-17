package nl.obren.sokrates.sourcecode.landscape;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.common.utils.RegexUtils;

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
        for (TeamConfig team : teams) {
            if (RegexUtils.matchesAnyPattern(email, team.getEmailPatterns())) {
                return team.getName();
            }
        }
        return null;
    }
}
