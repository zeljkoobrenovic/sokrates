/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.io;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

public class JsonMapper {
    private static final Log LOG = LogFactory.getLog(JsonMapper.class);

    public Object getObject(String json, Class clazz) throws IOException {
        try {
            return getObjectMapper().readValue(json, clazz);
        } catch (JsonParseException e) {
            String message = getMessage(e.getMessage(), e.getLocation());
            throw new IllegalArgumentException(message);
        } catch (JsonMappingException e) {
            String message = getMessage(e.getMessage(), e.getLocation());
            LOG.error("\nJSON:\n" + json + "\n==================\n");
            LOG.error(e);
            throw new IllegalArgumentException(message);
        }
    }

    private ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false)
                .configure(DeserializationFeature.FAIL_ON_UNRESOLVED_OBJECT_IDS, false)
                .configure(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY, false);

        return mapper;
    }

    protected String getMessage(String message, JsonLocation jsonLocation) {
        String[] lines = message.split("\n");
        if (jsonLocation != null && lines.length > 0) {
            String line = lines[0];
            line += " (line " + jsonLocation.getLineNr();
            line += (", column " + jsonLocation.getColumnNr() + ")");
            return line;
        }
        return message;
    }
}
