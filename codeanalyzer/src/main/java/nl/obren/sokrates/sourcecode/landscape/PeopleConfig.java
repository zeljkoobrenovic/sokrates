package nl.obren.sokrates.sourcecode.landscape;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.common.utils.RegexUtils;

import java.util.ArrayList;
import java.util.List;

public class PeopleConfig {
    private List<PersonConfig> people = new ArrayList<>();

    public PeopleConfig() {
    }

    public List<PersonConfig> getPeople() {
        return people;
    }

    public void setPeople(List<PersonConfig> people) {
        this.people = people;
    }

    @JsonIgnore
    public String getPerson(String contributorId) {
        for (PersonConfig person : people) {
            if (RegexUtils.matchesAnyPattern(contributorId, person.getEmailPatterns())) {
                return person.getName();
            }
        }
        return contributorId;
    }
}
