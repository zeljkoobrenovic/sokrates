package nl.obren.sokrates.sourcecode.landscape;

import com.fasterxml.jackson.core.type.TypeReference;
import nl.obren.sokrates.common.io.JsonMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersonConfigTest {

    @Test
    void legacyNameFieldDeserializesIntoUserName() throws Exception {
        // Old config-people.json used a single "name" field as the display name (before it was
        // renamed to email + userName). It must still populate the display name (userName).
        String json = "{\"name\": \"ahmed hached\", \"emailPatterns\": [\"hachedahmeddev\"]}";
        PersonConfig person = JsonMapper.getObject(json, new TypeReference<PersonConfig>() {
        });

        assertEquals("ahmed hached", person.getUserName());
        assertEquals(java.util.List.of("hachedahmeddev"), person.getEmailPatterns());
    }

    @Test
    void explicitUserNameWinsOverLegacyName() throws Exception {
        String json = "{\"name\": \"old name\", \"userName\": \"New Name\", \"emailPatterns\": [\"x\"]}";
        PersonConfig person = JsonMapper.getObject(json, new TypeReference<PersonConfig>() {
        });

        assertEquals("New Name", person.getUserName());
    }

    @Test
    void deserializesEntryWithLinksWithoutConflictingSetterError() throws Exception {
        // The two setLink overloads previously both mapped to Jackson property "link", throwing
        // "Conflicting setter definitions for property link" and aborting deserialization of the
        // whole PeopleConfig (returning null -> config silently ignored). Guard against regression.
        String json = "{\"people\": [{\"name\": \"ahmed hached\", \"links\": [{\"label\": \"GitHub\", \"href\": \"https://github.com/x\"}], \"emailPatterns\": [\"hachedahmeddev\"]}]}";
        PeopleConfig config = JsonMapper.getObject(json, new TypeReference<PeopleConfig>() {
        });

        assertEquals(1, config.getPeople().size());
        PersonConfig person = config.getPeople().get(0);
        assertEquals("ahmed hached", person.getUserName());
        assertEquals(1, person.getLinks().size());
        assertEquals("GitHub", person.getLinks().get(0).getLabel());
    }

    @Test
    void newSchemaUserNameDeserializes() throws Exception {
        String json = "{\"userName\": \"Guido van Rossum\", \"email\": \"guido@python.org\", \"emailPatterns\": [\"guido[@]python.org\"]}";
        PersonConfig person = JsonMapper.getObject(json, new TypeReference<PersonConfig>() {
        });

        assertEquals("Guido van Rossum", person.getUserName());
        assertEquals("guido@python.org", person.getEmail());
    }
}
