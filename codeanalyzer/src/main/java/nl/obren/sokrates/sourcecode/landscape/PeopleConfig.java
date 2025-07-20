package nl.obren.sokrates.sourcecode.landscape;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.common.utils.RegexUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PeopleConfig {
    private List<PersonConfig> people = new ArrayList<>();

    public PeopleConfig() {
    }

    @JsonIgnore
    private static final Map<String, PersonConfig> cache = new HashMap<>();
    @JsonIgnore
    private static final Map<String, PersonConfig> nameCache = new HashMap<>();

    public List<PersonConfig> getPeople() {
        return people;
    }

    public void setPeople(List<PersonConfig> people) {
        this.people = people;
    }

    @JsonIgnore
    public PersonConfig getPersonFromEmailPatterns(String contributorId) {
        if (cache.containsKey(contributorId)) {
            return cache.get(contributorId);
        }
        for (PersonConfig person : people) {
            if (RegexUtils.matchesAnyPattern(contributorId, person.getEmailPatterns())) {
                cache.put(contributorId, person);
                return person;
            }
        }

        PersonConfig newPersonConfig = new PersonConfig();
        newPersonConfig.setName(contributorId);
        cache.put(contributorId, newPersonConfig);

        return newPersonConfig;
    }
    @JsonIgnore
    public PersonConfig getPersonByName(String name) {
        if (nameCache.containsKey(name)) {
            return nameCache.get(name);
        }
        for (PersonConfig person : people) {
            if (person.getName().equals(name)) {
                cache.put(name, person);
                return person;
            }
        }

        PersonConfig newPersonConfig = new PersonConfig();
        newPersonConfig.setName(name);
        cache.put(name, newPersonConfig);

        return newPersonConfig;
    }
}
