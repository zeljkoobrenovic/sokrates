/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.common.utils;

import nl.obren.sokrates.common.io.JsonGenerator;
import org.apache.log4j.BasicConfigurator;
import org.junit.Test;

import static junit.framework.TestCase.*;

public class ReflectionUtilsTest {
    @Test
    public void compareWithDefaultJson() throws Exception {
        BasicConfigurator.configure();
        assertTrue(ReflectionUtils.compareWithDefaultJson(TestClassA.class, new JsonGenerator().generate(new TestClassA())));
        assertFalse(ReflectionUtils.compareWithDefaultJson(TestClassA.class, new JsonGenerator().generate(new TestClassA("b"))));
    }

    @Test
    public void getAllFields() throws Exception {
        class A {
        }
        class B extends A {
            private String b;
        }
        class C extends B {
            private String c;
        }

        assertEquals(ReflectionUtils.getAllFields(A.class).size(), 0);
        assertEquals(ReflectionUtils.getAllFields(B.class).size(), 1);
        assertEquals(ReflectionUtils.getAllFields(C.class).size(), 2);
    }

    @Test
    public void getFieldValue() throws Exception {
        class A {
        }
        class C {
        }
        class B {
            private A a = new A();
        }

        B b = new B();

        assertNotNull(ReflectionUtils.getFieldValue(b, A.class));
        assertNull(ReflectionUtils.getFieldValue(b, C.class));
    }


}

class TestClassA {
    private String a = "a";

    public TestClassA() {
    }

    public TestClassA(String a) {
        this.a = a;
    }

    public String getA() {
        return a;
    }

    public void setA(String a) {
        this.a = a;
    }
}
