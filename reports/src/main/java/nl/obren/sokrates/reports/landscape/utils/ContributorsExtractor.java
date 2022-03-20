package nl.obren.sokrates.reports.landscape.utils;

import java.util.List;

public interface ContributorsExtractor {
    List<String> getContributors(String timeSlot, boolean rookiesOnly);
}
