package nl.obren.sokrates.reports.generators.statichtml;

import java.util.List;

public class Histogram {

    public String generateHistogram(List<Integer> values, int factor) {
        if (values.size() == 0 || factor <= 0) {
            return "";
        }
        StringBuilder html = new StringBuilder();

        int max = values.stream().mapToInt(i -> i).max().getAsInt();

        return html.toString();
    }
}
