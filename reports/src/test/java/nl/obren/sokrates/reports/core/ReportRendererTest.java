package nl.obren.sokrates.reports.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReportRendererTest {

    @Test
    void mermaidBlockEscapesMarkupInBothCopies() {
        String definition = "flowchart TB\n    n0[\"<img src=x onerror=XSS>.ts\"] --- n1[\"a & b\"]";

        String block = ReportRenderer.mermaidBlock("g1", definition);

        assertFalse(block.contains("<img"), block);
        String escaped = "n0[&quot;&lt;img src=x onerror=XSS&gt;.ts&quot;] --- n1[&quot;a &amp; b&quot;]";
        assertTrue(block.contains("<script type=\"text/plain\" class=\"mermaid-source\" id=\"mermaid-source-g1\">\n" + "flowchart TB\n    " + escaped + "\n</script>"), block);
        assertTrue(block.contains("<pre class=\"mermaid\">\nflowchart TB\n    " + escaped + "\n</pre>"), block);
    }

    @Test
    void mermaidBlockEscapesArrowsAndQuotesAsWell() {
        // The browser decodes &gt; and &quot; back before mermaid.js reads the text (it reads the <pre>
        // via innerHTML), so directed edges and quoted labels keep working; what matters is that no
        // raw < or > reaches the HTML parser.
        String definition = "flowchart LR\n    n0[\"Comp One\"] -->|\" 3 \"| n1[\"Comp Two\"]\n    linkStyle 0 stroke:#00b2ee,stroke-width:2px";
        String block = ReportRenderer.mermaidBlock("g", definition);
        assertTrue(block.contains("<pre class=\"mermaid\">\n" + definition.replace(">", "&gt;").replace("\"", "&quot;") + "\n</pre>"), block);
    }

    @Test
    void mermaidBlockLeavesDefinitionsWithoutMetacharactersUntouched() {
        String definition = "flowchart LR\n    n0[Comp One] ---|3| n1[Comp Two]\n    linkStyle 0 stroke:#00b2ee,stroke-width:2px";
        String block = ReportRenderer.mermaidBlock("g", definition);
        assertTrue(block.contains("<pre class=\"mermaid\">\n" + definition + "\n</pre>"), block);
    }

    @Test
    void mermaidBlockWithoutIdHasNoSourceStash() {
        String block = ReportRenderer.mermaidBlock("", "flowchart TB");
        assertFalse(block.contains("mermaid-source"), block);
        assertTrue(block.contains("<pre class=\"mermaid\">\nflowchart TB\n</pre>"), block);
    }
}
