package nl.obren.sokrates.sourcecode.scoping;

import nl.obren.sokrates.sourcecode.SourceFileFilter;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The analysis output (_sokrates/, _sokrates_landscape/, the git exports, the conventions files) is
 * ignored unconditionally: before, those rules were written into a new configuration only when a
 * matching file existed at init, and on the very first analysis none does — so the second run of the
 * same configuration swallowed the first run's reports into its main scope.
 */
class ScopingConventionsSokratesOutputTest {

    private static List<String> patterns(List<SourceFileFilter> filters) {
        return filters.stream().map(SourceFileFilter::getPathPattern).collect(Collectors.toList());
    }

    @Test
    void sokratesOutputIsIgnoredEvenWhenNoSuchFileExistsYet() {
        CodeConfiguration configuration = new CodeConfiguration();
        new ScopingConventions().addConventions(configuration, new ArrayList<>());   // an empty tree: nothing matches any convention

        List<String> ignored = patterns(configuration.getIgnore());
        assertTrue(ignored.contains(".*/_sokrates/.*"), ignored.toString());
        assertTrue(ignored.contains(".*/_sokrates_landscape/.*"), ignored.toString());
        assertTrue(ignored.contains(".*/git[-][a-zA-Z0-9_]+[.]txt"), ignored.toString());
        assertTrue(ignored.contains(".*/sokrates_.*?[.]json"), ignored.toString());
    }

    @Test
    void ensureIsIdempotentAndKeepsExistingRules() {
        List<SourceFileFilter> ignore = new ArrayList<>();
        ignore.add(new SourceFileFilter(".*/vendor/.*", ""));
        ignore.add(new SourceFileFilter(".*/_sokrates/.*", ""));   // already there, e.g. hand-written

        ScopingConventions.ensureSokratesOutputIgnored(ignore);
        ScopingConventions.ensureSokratesOutputIgnored(ignore);

        assertEquals(List.of(".*/vendor/.*", ".*/_sokrates/.*", ".*/_sokrates_landscape/.*", ".*/git[-][a-zA-Z0-9_]+[.]txt", ".*/sokrates_.*?[.]json"), patterns(ignore));
    }
}
