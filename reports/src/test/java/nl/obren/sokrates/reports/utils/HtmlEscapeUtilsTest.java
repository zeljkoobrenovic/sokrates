package nl.obren.sokrates.reports.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HtmlEscapeUtilsTest {

    @Test
    void escapeRewritesOnlyTheFiveHtmlMetacharacters() {
        assertEquals("&lt;img src=x onerror=XSS&gt;.ts", HtmlEscapeUtils.escape("<img src=x onerror=XSS>.ts"));
        assertEquals("pwn&quot; onmouseover=&quot;XSS", HtmlEscapeUtils.escape("pwn\" onmouseover=\"XSS"));
        assertEquals("pwn&#39; onmouseover=&#39;XSS", HtmlEscapeUtils.escape("pwn' onmouseover='XSS"));
        assertEquals("a &amp; b", HtmlEscapeUtils.escape("a & b"));
        // Already-escaped input is escaped again: the caller passes raw text, never HTML.
        assertEquals("&amp;lt;", HtmlEscapeUtils.escape("&lt;"));
        assertEquals("", HtmlEscapeUtils.escape(null));
    }

    @Test
    void escapeLeavesOrdinaryNamesUnchanged() {
        assertEquals("src/main/java/Foo.java", HtmlEscapeUtils.escape("src/main/java/Foo.java"));
        assertEquals("Željko Obrenović <zeljko@example.com>".replace("<", "&lt;").replace(">", "&gt;"),
                HtmlEscapeUtils.escape("Željko Obrenović <zeljko@example.com>"));
        assertEquals("café.ts", HtmlEscapeUtils.escape("café.ts"));
        assertEquals("a-b_c.d~e (1) [2] {3} $4 = 5 + 6", HtmlEscapeUtils.escape("a-b_c.d~e (1) [2] {3} $4 = 5 + 6"));
    }

    @Test
    void viewerHrefKeepsOrdinaryPathsAsBefore() {
        assertEquals("../src/viewer.html#aspect=main&file=com/x/Foo.java", HtmlEscapeUtils.viewerFileHref("main", "com/x/Foo.java"));
        assertEquals("../src/viewer.html#aspect=test&file=src/a-b_c.d~e/Foo_1.java", HtmlEscapeUtils.viewerFileHref("test", "src/a-b_c.d~e/Foo_1.java"));
    }

    @Test
    void viewerHrefPercentEncodesFragmentDelimitersAndMarkup() {
        // # & % would end the file= parameter early in viewer.html's param(); + would become a space.
        assertEquals("src/odd%20%23%26%25%20dir/plus%2Bfile.ts", HtmlEscapeUtils.encodeFragmentComponent("src/odd #&% dir/plus+file.ts"));
        assertEquals("src/%3Cimg%20src%3Dx%20onerror%3DXSS%3E.ts", HtmlEscapeUtils.encodeFragmentComponent("src/<img src=x onerror=XSS>.ts"));
        assertEquals("src/pwn%22%20onmouseover%3D%22XSS/inside.ts", HtmlEscapeUtils.encodeFragmentComponent("src/pwn\" onmouseover=\"XSS/inside.ts"));
        assertEquals("a%27b.ts", HtmlEscapeUtils.encodeFragmentComponent("a'b.ts"));
        // Non-ASCII goes out as UTF-8 bytes, which decodeURIComponent reverses.
        assertEquals("caf%C3%A9.ts", HtmlEscapeUtils.encodeFragmentComponent("café.ts"));
        assertEquals("", HtmlEscapeUtils.encodeFragmentComponent(null));
    }
}
