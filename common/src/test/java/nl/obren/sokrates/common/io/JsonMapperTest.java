package nl.obren.sokrates.common.io;

import com.fasterxml.jackson.core.JsonLocation;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static junit.framework.TestCase.assertEquals;

public class JsonMapperTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void testGetMessage() throws Exception {
        JsonMapper mapper = new JsonMapper();
        String originalMessage = "An error message";
        JsonLocation jsonLocation = new JsonLocation(null, 0, 10, 20);

        String message = mapper.getMessage(originalMessage, jsonLocation);

        assertEquals(originalMessage + " (line 10, column 20)", message);
    }
}
