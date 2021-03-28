package nl.obren.sokrates.sourcecode.threshold;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ThresholdsTest {

    @Test
    void getLabels() {
        Thresholds thresholds = new Thresholds(10, 20, 30, 40);

        List<String> labels = thresholds.getLabels();

        assertEquals(labels.get(4), "1-10");
        assertEquals(labels.get(3), "11-20");
        assertEquals(labels.get(2), "21-30");
        assertEquals(labels.get(1), "31-40");
        assertEquals(labels.get(0), "41+");
    }
}