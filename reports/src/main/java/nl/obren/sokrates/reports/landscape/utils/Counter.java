package nl.obren.sokrates.reports.landscape.utils;

import nl.obren.sokrates.sourcecode.contributors.ContributionTimeSlot;

public interface Counter {
    int getCount(ContributionTimeSlot slot);
}
