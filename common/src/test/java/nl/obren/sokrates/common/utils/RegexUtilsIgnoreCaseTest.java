package nl.obren.sokrates.common.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegexUtilsIgnoreCaseTest {

    @Test
    void matchesEntirelyIgnoreCaseIsCaseInsensitive() {
        assertTrue(RegexUtils.matchesEntirelyIgnoreCase("alice@corp[.]com", "Alice@Corp.com"));
        assertTrue(RegexUtils.matchesEntirelyIgnoreCase("ALICE@CORP[.]COM", "alice@corp.com"));
        assertTrue(RegexUtils.matchesEntirelyIgnoreCase("a.*z", "AbcZ"));
    }

    @Test
    void caseSensitiveMatcherStillCaseSensitive() {
        // The plain matcher must remain case-sensitive (no cross-contamination via the shared cache).
        assertFalse(RegexUtils.matchesEntirely("alice@corp[.]com", "Alice@Corp.com"));
        assertTrue(RegexUtils.matchesEntirely("alice@corp[.]com", "alice@corp.com"));
    }

    @Test
    void matchesAnyPatternIgnoreCase() {
        assertTrue(RegexUtils.matchesAnyPatternIgnoreCase("Bob@X.COM", List.of("alice@.*", "bob@x[.]com")));
        assertFalse(RegexUtils.matchesAnyPatternIgnoreCase("carol@x.com", List.of("alice@.*", "bob@x[.]com")));
    }

    @Test
    void matchesAnyPatternIgnoreCaseNullPatternsIsFalse() {
        assertFalse(RegexUtils.matchesAnyPatternIgnoreCase("anything", null));
    }

    @Test
    void invalidPatternIsTreatedAsNonMatching() {
        assertFalse(RegexUtils.matchesEntirelyIgnoreCase("([unclosed", "whatever"));
    }
}
