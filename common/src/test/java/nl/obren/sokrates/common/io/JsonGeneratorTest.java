/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.io;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.condition.OS.LINUX;
import static org.junit.jupiter.api.condition.OS.MAC;
import static org.junit.jupiter.api.condition.OS.WINDOWS;

public class JsonGeneratorTest {

    private String generateJson() throws JsonProcessingException {
        class A {
        private String a = "a";
        public String getA() {
            return a;
        }
        public void setA(String a) {
            this.a = a;
        }
    }
        return new JsonGenerator().generate(new A());
}

    @Test
    @EnabledOnOs({LINUX, MAC})
    public void testGenerateJsonOnLinuxOrMac() throws Exception {
        String json = generateJson();
        assertEquals(json, "{\n  \"a\" : \"a\"\n}");
    }

    @Test
    @EnabledOnOs(WINDOWS)
    public void testGenerateJsonOnWindows() throws Exception {
        String json = generateJson();
        assertEquals(json, "{\r\n  \"a\" : \"a\"\r\n}");
    }

}
