package nl.obren.sokrates.common.io;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JsonGeneratorTest {

    @Test
    public void testGenerateJson() throws Exception {
        class A {
            private String a = "a";

            public String getA() {
                return a;
            }

            public void setA(String a) {
                this.a = a;
            }
        }

        String json = new JsonGenerator().generate(new A());
        assertEquals(json, "{\n  \"a\" : \"a\"\n}");
    }
}
