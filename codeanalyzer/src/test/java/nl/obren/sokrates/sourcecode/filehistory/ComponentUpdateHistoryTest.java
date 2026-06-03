package nl.obren.sokrates.sourcecode.filehistory;

import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComponentUpdateHistoryTest {

    @Test
    void addDateKeepsDatesDistinctAndSorted() {
        ComponentUpdateHistory history = new ComponentUpdateHistory(new NamedSourceCodeAspect("comp"));

        history.addDate("2020-03-01");
        history.addDate("2020-01-01");
        history.addDate("2020-03-01"); // duplicate
        history.addDate("2020-02-01");

        assertEquals(Arrays.asList("2020-01-01", "2020-02-01", "2020-03-01"), history.getDates());
    }

    @Test
    void setDatesKeepsAddDateConsistent() {
        ComponentUpdateHistory history = new ComponentUpdateHistory(new NamedSourceCodeAspect("comp"));
        history.setDates(new java.util.ArrayList<>(Arrays.asList("2020-01-01", "2020-02-01")));

        history.addDate("2020-02-01"); // already present
        history.addDate("2020-01-15");

        assertEquals(Arrays.asList("2020-01-01", "2020-01-15", "2020-02-01"), history.getDates());
    }
}
