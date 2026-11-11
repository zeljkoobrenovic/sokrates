package nl.obren.sokrates.common.renderingutils.charts;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaletteTest {

    // The CSS variant must fall back to exactly the literal risk colors, in the same order, so a page
    // without the theme tokens looks as before.
    @Test
    void cssRiskPaletteFallsBackToTheRiskPalette() {
        List<String> literal = Palette.getRiskPalette().getColors();
        List<String> css = Palette.getRiskPaletteCss().getColors();

        assertEquals(literal.size(), css.size());
        for (int i = 0; i < literal.size(); i++) {
            assertTrue(css.get(i).matches("var\\(--sk-risk-[a-z-]+, " + literal.get(i) + "\\)"), css.get(i));
        }
    }
}
