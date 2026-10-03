package nl.obren.sokrates.common.renderingutils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReportThemeTest {

    @Test
    void adaptiveIconTurnsBlackIntoCurrentColorAndKeepsOtherColors() {
        String svg = "<svg width='10px' viewBox='0 0 10 10'><path fill=\"#000000\" d='M0 0'/><path fill='black' d='M1 1'/>"
                + "<path fill=\"#e31a1c\" d='M2 2'/><path style=\"color:#000000;stroke:red\" fill=\"#000\" d='M3 3'/></svg>";
        String adapted = ReportTheme.adaptiveIcon(svg);

        assertTrue(adapted.startsWith("<svg class=\"sk-icon\" fill=\"currentColor\" width='10px'"), adapted);
        assertFalse(adapted.contains("#000000") || adapted.contains("'black'") || adapted.contains("\"#000\""), adapted);
        assertTrue(adapted.contains("fill=\"#e31a1c\""), "colored parts keep their color");
        assertTrue(adapted.contains("style=\"stroke:red\""), "an inline black color: is removed, the rest of the style kept: " + adapted);
    }

    @Test
    void adaptiveIconKeepsAnExplicitRootFill() {
        String adapted = ReportTheme.adaptiveIcon("<svg fill=\"#000000\" viewBox='0 0 1 1'></svg>");
        assertEquals("<svg class=\"sk-icon\" fill=\"currentColor\" viewBox='0 0 1 1'></svg>", adapted);
        assertNull(ReportTheme.adaptiveIcon(null));
    }

    @Test
    void applyReplacesThePlaceholderWithTheTokensAndScript() {
        String html = ReportTheme.apply("<head>${sokrates-theme}</head>");
        assertFalse(html.contains("${sokrates-theme}"));
        assertTrue(html.contains("--sk-text:"));
        assertTrue(html.contains(":root[data-theme=\"dark\"]"));
        assertTrue(html.contains("sokratesCycleTheme"));
    }

    @Test
    void darkOnlyCoversTheSystemSettingAndTheExplicitChoice() {
        String css = ReportTheme.darkOnly("td", "color: red;");
        assertTrue(css.contains("@media (prefers-color-scheme: dark) {:root:not([data-theme=\"light\"]) td {color: red;}}"));
        assertTrue(css.contains(":root[data-theme=\"dark\"] td {color: red;}"));
    }

    @Test
    void darkOnlyScopesEverySelectorOfAList() {
        String css = ReportTheme.darkOnly("pre.mermaid, .sk-canvas", "color: red;");
        assertTrue(css.contains(":root:not([data-theme=\"light\"]) pre.mermaid, :root:not([data-theme=\"light\"]) .sk-canvas {color: red;}"), css);
        assertTrue(css.contains(":root[data-theme=\"dark\"] pre.mermaid, :root[data-theme=\"dark\"] .sk-canvas {color: red;}"), css);
    }
}
