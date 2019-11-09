package nl.obren.sokrates.sourcecode.dependencies;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotSame;

public class ComponentDependencyTest {
    @Test
    public void equals() throws Exception {
        assertEquals(new ComponentDependency("a", "b"), new ComponentDependency("a", "b"));
        assertNotSame(new ComponentDependency("a", "b"), new ComponentDependency("b", "a"));
    }

    @Test
    public void testHashCode() throws Exception {
        assertEquals(new ComponentDependency("a", "b").hashCode(), -1486985710);
        assertEquals(new ComponentDependency("c", "d").hashCode(), -1429727406);
    }

    @Test
    public void getDependencyString() throws Exception {
        assertEquals(new ComponentDependency("a", "b").getDependencyString(), "a -> b");
    }

}