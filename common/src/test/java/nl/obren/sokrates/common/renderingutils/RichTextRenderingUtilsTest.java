package nl.obren.sokrates.common.renderingutils;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class RichTextRenderingUtilsTest {
    @Test
    public void getLinkWikiStyle() throws Exception {
        assertEquals(RichTextRenderingUtils.getLinkWikiStyle("http://link.witout.title.com"), "<a href='http://link.witout.title.com'>http://link.witout.title.com</a>");
        assertEquals(RichTextRenderingUtils.getLinkWikiStyle("http://link.with.title.com My Title"), "<a href='http://link.with.title.com'>My Title</a>");
    }

    @Test
    public void testGetPercentage() throws Exception {
        assertEquals(RichTextRenderingUtils.getPercentage(100, 20), 20.0);
        assertEquals(RichTextRenderingUtils.getPercentage(100, 0), 0.0);
        assertEquals(RichTextRenderingUtils.getPercentage(100, 100), 100.0);
        assertEquals(RichTextRenderingUtils.getPercentage(100, 50), 50.0);
    }

    @Test
    public void testRenderDouble() throws Exception {
        assertEquals(RichTextRenderingUtils.renderNumberStrong(1.0), "<b>1.0</b>");
        assertEquals(RichTextRenderingUtils.renderNumberStrong(10.0), "<b>10.0</b>");
        assertEquals(RichTextRenderingUtils.renderNumberStrong(25.6), "<b>25.6</b>");
        assertEquals(RichTextRenderingUtils.renderNumberStrong(-1.0), "<b>-1.0</b>");
    }

    @Test
    public void testRenderInteger() throws Exception {
        assertEquals(RichTextRenderingUtils.renderNumberStrong(1), "<b>1</b>");
        assertEquals(RichTextRenderingUtils.renderNumberStrong(10), "<b>10</b>");
        assertEquals(RichTextRenderingUtils.renderNumberStrong(25), "<b>25</b>");
        assertEquals(RichTextRenderingUtils.renderNumberStrong(-1), "<b>-1</b>");
    }
}