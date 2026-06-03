/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.metrics;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class MetricsListTest {

    @Test
    public void getMetricByIdFindsMetricCaseInsensitively() {
        MetricsList list = new MetricsList();
        Metric a = list.addMetric().id("MAIN_LOC").value(100);
        Metric b = list.addMetric().id("TEST_LOC").value(50);

        assertSame(a, list.getMetricById("MAIN_LOC"));
        assertSame(a, list.getMetricById("main_loc"));
        assertSame(b, list.getMetricById("Test_Loc"));
        assertNull(list.getMetricById("MISSING"));
    }

    @Test
    public void indexIsBuiltAfterIdsAreSet() {
        // addMetric() returns a Metric whose id is assigned afterwards via the builder; the lazy
        // index must reflect those final ids, not an empty/early state.
        MetricsList list = new MetricsList();
        Metric metric = list.addMetric();
        metric.id("LATE_ID").value(1);

        assertSame(metric, list.getMetricById("LATE_ID"));
    }

    @Test
    public void firstMatchWinsForDuplicateIds() {
        MetricsList list = new MetricsList();
        Metric first = list.addMetric().id("DUP").value(1);
        list.addMetric().id("DUP").value(2);

        assertSame(first, list.getMetricById("DUP"));
    }

    @Test
    public void indexInvalidatedWhenMetricsAdded() {
        MetricsList list = new MetricsList();
        list.addMetric().id("A").value(1);
        assertNull(list.getMetricById("B")); // builds the index

        Metric b = list.addMetric().id("B").value(2); // must invalidate the cached index
        assertSame(b, list.getMetricById("B"));
    }

    @Test
    public void indexInvalidatedOnRemoveAndSetMetrics() {
        MetricsList list = new MetricsList();
        list.addMetric().id("A").value(1);
        assertEquals(1, list.getMetricById("A").getValue().intValue());

        list.remove("A");
        assertNull(list.getMetricById("A"));

        Metric c = new Metric().id("C").value(3);
        list.setMetrics(new ArrayList<>(Arrays.asList(c)));
        assertSame(c, list.getMetricById("C"));
    }
}
