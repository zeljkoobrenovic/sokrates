package nl.obren.sokrates.reports.core;

import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The {@code ...Text} primitives escape their text argument and otherwise render exactly as their
 * HTML-by-contract sibling, so migrating a call site changes nothing for ordinary names.
 */
class RichTextReportTest {

    private static final String PAYLOAD = "<img src=x onerror=XSS>.ts & 'q' \"d\"";
    private static final String ESCAPED = "&lt;img src=x onerror=XSS&gt;.ts &amp; &#39;q&#39; &quot;d&quot;";

    private static String html(RichTextReport report) {
        return report.getRichTextFragments().stream().map(RichTextFragment::getFragment).collect(Collectors.joining());
    }

    @Test
    void textPrimitivesEscapeThePayload() {
        RichTextReport report = new RichTextReport();
        report.addText(PAYLOAD);
        report.addParagraphText(PAYLOAD);
        report.addListItemText(PAYLOAD);
        report.addTableCellText(PAYLOAD);
        report.addTableCellText(PAYLOAD, "color: grey");
        report.addContentInDivText(PAYLOAD);
        report.addContentInDivText(PAYLOAD, "width: 10px");
        report.addTabText("tab-1", PAYLOAD, true);
        report.startSubSectionText(PAYLOAD, PAYLOAD);

        String html = html(report);
        assertFalse(html.contains("<img"), html);
        assertFalse(html.contains("onerror=XSS>"), html);
        assertEquals(10, countOf(html, ESCAPED), html); // 9 calls, the sub-section escapes title and subtitle
    }

    @Test
    void eachTextPrimitiveRendersLikeItsHtmlSiblingGivenEscapedInput() {
        RichTextReport text = new RichTextReport();
        RichTextReport html = new RichTextReport();

        text.addText(PAYLOAD);                          html.addHtmlContent(ESCAPED);
        text.addParagraphText(PAYLOAD);                 html.addParagraph(ESCAPED);
        text.addListItemText(PAYLOAD);                  html.addListItem(ESCAPED);
        text.addTableCellText(PAYLOAD);                 html.addTableCell(ESCAPED);
        text.addTableCellText(PAYLOAD, "color: grey");  html.addTableCell(ESCAPED, "color: grey");
        text.addContentInDivText(PAYLOAD);              html.addContentInDiv(ESCAPED);
        text.addContentInDivText(PAYLOAD, "w: 1px");    html.addContentInDiv(ESCAPED, "w: 1px");
        text.addTabText("id", PAYLOAD, false);          html.addTab("id", ESCAPED, false);
        text.startSubSectionText(PAYLOAD, PAYLOAD);     html.startSubSection(ESCAPED, ESCAPED);

        assertEquals(html(html), html(text));
    }

    @Test
    void ordinaryTextIsUnchanged() {
        RichTextReport report = new RichTextReport();
        report.addTableCellText("src/main/java/Foo.java");
        report.addListItemText("primary (3 components)");
        report.addText("Željko Obrenović zeljko@example.com");
        report.addTabText("t", "java", true);
        assertEquals("<td>src/main/java/Foo.java</td>"
                + "<li>primary (3 components)</li>"
                + "Željko Obrenović zeljko@example.com"
                + "    <button class='tablinks active' data-tab='t' onclick='openTab(event, \"t\")'>java</button>", html(report));
    }

    @Test
    void nullTextRendersAsEmpty() {
        RichTextReport report = new RichTextReport();
        report.addTableCellText(null);
        report.startSubSectionText(null, null);
        String html = html(report);
        assertTrue(html.startsWith("<td></td>"), html);
        assertFalse(html.contains("null"), html);
    }

    private static int countOf(String haystack, String needle) {
        int count = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + needle.length())) count++;
        return count;
    }
}
