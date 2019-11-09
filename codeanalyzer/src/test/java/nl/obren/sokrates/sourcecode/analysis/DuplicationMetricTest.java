package nl.obren.sokrates.sourcecode.analysis;

import nl.obren.sokrates.sourcecode.metrics.DuplicationMetric;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class DuplicationMetricTest {
    @Test
    public void getDuplicationPercentage() throws Exception {
        assertEquals(new DuplicationMetric("", 100, 30).getDuplicationPercentage().doubleValue(), 30.0, 0.00001);
        assertEquals(new DuplicationMetric("", 2000, 200).getDuplicationPercentage().doubleValue(), 10.0, 0.00001);
    }

}