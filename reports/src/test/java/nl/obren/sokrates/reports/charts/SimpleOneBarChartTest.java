package nl.obren.sokrates.reports.charts;

import nl.obren.sokrates.common.renderingutils.charts.Palette;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleOneBarChartTest {

    @Test
    void leftTextIsEscapedAndRightTextIsHtml() {
        SimpleOneBarChart chart = new SimpleOneBarChart();
        String svg = chart.getPercentageSvg(50, "<img src=x onerror=XSS>", "12 files <a href='#'>list</a>");
        assertFalse(svg.contains("<img"), svg);
        assertTrue(svg.contains(">&lt;img src=x onerror=XSS&gt;</text>"), svg);
        assertTrue(svg.contains("12 files <a href='#'>list</a></text>"), svg);
    }

    @Test
    void stackedBarLeftTextIsEscaped() {
        SimpleOneBarChart chart = new SimpleOneBarChart();
        String svg = chart.getStackedBarSvg(Arrays.asList(3, 5), Palette.getDefaultPalette(), "a & b", "");
        assertTrue(svg.contains(">a &amp; b</text>"), svg);
    }

    @Test
    void ordinaryLeftTextUnchanged() {
        SimpleOneBarChart chart = new SimpleOneBarChart();
        String svg = chart.getPercentageSvg(10, "src/main", "");
        assertTrue(svg.contains(">src/main</text>"), svg);
    }
}
