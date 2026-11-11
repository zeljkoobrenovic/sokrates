/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class JsonGenerator {
    public String generate(Object data) throws JsonProcessingException {
        return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(data);
    }

    public String generateCompressed(Object data) throws JsonProcessingException {
        return new ObjectMapper().writer().writeValueAsString(data);
    }

    /**
     * Serializes {@code data} as pretty-printed JSON straight to {@code file}, streaming through
     * Jackson without ever materializing the whole document as a String. Use this instead of
     * {@code FileUtils.write(file, generate(data))} for large objects (e.g. analysisResults.json
     * on huge repositories), where the String form could exceed Java's ~2 GB array limit.
     */
    public void generateToFile(Object data, File file) throws IOException {
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(file, data);
    }
}
