/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.dependencies;

import org.junit.Test;

import static junit.framework.TestCase.*;

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

    @Test
    public void hasPathFrom() throws Exception {
        ComponentDependency componentDependency = new ComponentDependency("a", "b");

        componentDependency.getEvidence().add(new DependencyEvidence("path1/a", "evidence 1"));
        componentDependency.getEvidence().add(new DependencyEvidence("path2/b", "evidence 2"));

        assertTrue(componentDependency.hasPathFrom("path1/a"));
        assertTrue(componentDependency.hasPathFrom("path2/b"));
        assertFalse(componentDependency.hasPathFrom("path3/c"));
    }

}
