package nl.obren.sokrates.reports.core;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class SummaryUtilsFeatureCardTest {
    @Test
    public void cardHasMaxWidthTwoLineClampAndEscapedName() {
        String card = SummaryUtils.getFeatureOfInterestCard("os: #[cfg(target_os = \"windows\")] <x>", 1);

        assertTrue(card.contains("width: 120px"));
        assertTrue(card.contains("-webkit-line-clamp: 2"));
        assertTrue(card.contains("text-overflow: ellipsis"));
        // full name is available as a tooltip, HTML-escaped
        assertTrue(card.contains("title='os: #[cfg(target_os = &quot;windows&quot;)] &lt;x&gt;'"));
        assertTrue(!card.contains("<x>"));
        assertTrue(card.contains(">1 file<"));
        assertTrue(SummaryUtils.getFeatureOfInterestCard("a", 2).contains(">2 files<"));
    }
}
