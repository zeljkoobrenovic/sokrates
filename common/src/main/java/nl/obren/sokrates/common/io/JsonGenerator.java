/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonGenerator {
    public String generate(Object data) throws JsonProcessingException {
        return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(data);
    }

    public String generateCompressed(Object data) throws JsonProcessingException {
        return new ObjectMapper().writer().writeValueAsString(data);
    }
}
