package nl.obren.sokrates.sourcecode.landscape;

import nl.obren.sokrates.sourcecode.contributors.Contributor;

public interface ContributionCounter {
    int count(Contributor contributor);
}