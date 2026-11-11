package nl.obren.sokrates.sourcecode.filehistory;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileHistoryComponentsHelperTest {

    private FileModificationHistory history(String path, String... dates) {
        FileModificationHistory history = new FileModificationHistory(path);
        history.setDates(new ArrayList<>(Arrays.asList(dates)));
        return history;
    }

    @Test
    void getUniqueDatesDeduplicatesAcrossFilesAndSorts() {
        List<FileModificationHistory> histories = Arrays.asList(
                history("a.java", "2020-03-01", "2020-01-01"),
                history("b.java", "2020-01-01", "2020-02-01")); // 2020-01-01 shared

        FileHistoryComponentsHelper helper = new FileHistoryComponentsHelper();

        assertEquals(Arrays.asList("2020-01-01", "2020-02-01", "2020-03-01"), helper.getUniqueDates(histories));
        assertEquals(3, helper.getNumberOfActiveDays(histories));
    }

    @Test
    void getUniqueDatesTruncatesToDayAndIgnoresShortStrings() {
        List<FileModificationHistory> histories = Arrays.asList(
                history("a.java", "2020-01-01T10:00:00", "2020-01-01T23:00:00", "bad"));

        FileHistoryComponentsHelper helper = new FileHistoryComponentsHelper();

        // both timestamps collapse to the same day; the too-short "bad" entry is ignored
        assertEquals(Arrays.asList("2020-01-01"), helper.getUniqueDates(histories));
    }
}
