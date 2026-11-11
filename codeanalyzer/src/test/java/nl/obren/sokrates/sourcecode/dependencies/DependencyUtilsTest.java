/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.dependencies;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class DependencyUtilsTest {

    private static ComponentDependency dep(String from, String to, int count) {
        ComponentDependency dependency = new ComponentDependency(from, to);
        dependency.setCount(count);
        return dependency;
    }

    @Test
    public void noCycles() throws Exception {
        List<ComponentDependency> dependencies = Arrays.asList(
                dep("a", "b", 3),
                dep("b", "c", 5));

        assertEquals(0, DependencyUtils.getCyclicDependencyPlacesCount(dependencies));
        assertEquals(0, DependencyUtils.getCyclicDependencyCount(dependencies));
    }

    @Test
    public void oneCycle() throws Exception {
        List<ComponentDependency> dependencies = Arrays.asList(
                dep("a", "b", 3),
                dep("b", "a", 5),
                dep("b", "c", 7)); // not part of a cycle

        // One bidirectional pair (a<->b) => one "place".
        assertEquals(1, DependencyUtils.getCyclicDependencyPlacesCount(dependencies));
        // Both directions' counts are summed: 3 + 5.
        assertEquals(8, DependencyUtils.getCyclicDependencyCount(dependencies));
    }

    @Test
    public void twoCycles() throws Exception {
        List<ComponentDependency> dependencies = Arrays.asList(
                dep("a", "b", 1),
                dep("b", "a", 2),
                dep("c", "d", 4),
                dep("d", "c", 8));

        assertEquals(2, DependencyUtils.getCyclicDependencyPlacesCount(dependencies));
        assertEquals(1 + 2 + 4 + 8, DependencyUtils.getCyclicDependencyCount(dependencies));
    }

    // A self-edge (a -> a) is not a cycle and must not be counted.
    @Test
    public void selfEdgeIsNotCyclic() throws Exception {
        List<ComponentDependency> dependencies = new ArrayList<>(Arrays.asList(
                dep("a", "a", 9),
                dep("b", "c", 1)));

        assertEquals(0, DependencyUtils.getCyclicDependencyPlacesCount(dependencies));
        assertEquals(0, DependencyUtils.getCyclicDependencyCount(dependencies));
    }

    @Test
    public void getDependenciesCount() throws Exception {
        List<ComponentDependency> dependencies = Arrays.asList(
                dep("a", "b", 3),
                dep("b", "c", 5));

        assertEquals(8, DependencyUtils.getDependenciesCount(dependencies));
    }
}
