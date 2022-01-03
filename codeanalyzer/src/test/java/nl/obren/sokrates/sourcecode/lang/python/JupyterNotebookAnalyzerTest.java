package nl.obren.sokrates.sourcecode.lang.python;

import nl.obren.sokrates.sourcecode.SourceFile;

import java.io.File;
import java.util.List;

import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

class JupyterNotebookAnalyzerTest {

    final String HEADER = "{ \"cells\": [";
    final String CODE_CELL_START = "{ \"cell_type\": \"code\", \"source\": [";
    final String CODE_CELL_END = "] }";
    final String FOOTER = "]}";

    @Test
    public void cleanForLinesOfCodeCalculations() throws Exception {
        JupyterNotebookAnalyzer analyzer = new JupyterNotebookAnalyzer();
        String code = create_single_cell_json("x=1");
        assertEquals("x=1", analyzer.cleanForLinesOfCodeCalculations(new SourceFile(new File("test.ipynb"), code)).getCleanedContent());
    }

    @Test
    public void cleanForLinesOfCodeCalculationsComplex() throws Exception {
        JupyterNotebookAnalyzer analyzer = new JupyterNotebookAnalyzer();
        String code = create_single_cell_json("x=1\n\nprint(x)");
        assertEquals("x=1\nprint(x)", analyzer.cleanForLinesOfCodeCalculations(new SourceFile(new File("test.ipynb"), code)).getCleanedContent());
    }

    @Test
    public void extractTwoUnits() throws Exception {
        JupyterNotebookAnalyzer analyzer = new JupyterNotebookAnalyzer();
        String code = create_single_cell_json("def bla():\n"
                + "    pass\n\n"
                + "def foo(x):\n"
                + "    x = x + x\n"
                + "    return x\n");
        SourceFile sourceFile = new SourceFile(new File("dummy.ipynb"), code);
        CleanedContent cc = analyzer.cleanForLinesOfCodeCalculations(sourceFile);
        assertEquals(5, cc.getLines().size());
        List<UnitInfo> units = analyzer.extractUnits(sourceFile);
        assertEquals(2, units.size());
        assertEquals("def bla()", units.get(0).getShortName());
        assertEquals("def foo()", units.get(1).getShortName());
        assertEquals(2, units.get(0).getLinesOfCode());
        assertEquals(3, units.get(1).getLinesOfCode());
        assertEquals(1, units.get(0).getMcCabeIndex());
        assertEquals(1, units.get(1).getMcCabeIndex());
        assertEquals(0, units.get(0).getNumberOfParameters());
        assertEquals(1, units.get(1).getNumberOfParameters());
    }

    @Test
    public void extractRealWorld() throws Exception {
        JupyterNotebookAnalyzer analyzer = new JupyterNotebookAnalyzer();
        SourceFile sourceFile = new SourceFile(new File("real-world.ipynb"), REAL_WORLD_EXAMPLE);
        CleanedContent cc = analyzer.cleanForLinesOfCodeCalculations(sourceFile);
        assertEquals(3, cc.getLines().size());
    }

    private String create_single_cell_json(String code) {
        StringBuilder builder = new StringBuilder();
        builder.append(HEADER);
        builder.append(CODE_CELL_START);
        String[] lines = code.split("\n");
        for (String line : lines) {
            builder.append("\"");
            builder.append(line);
            builder.append("\",\n");
        }
        if (lines.length > 0) {
            int current = builder.length();
            builder.delete(current - 2, current - 1);
        }
        builder.append(CODE_CELL_END);
        builder.append(FOOTER);
        return builder.toString();
    }

    final String REAL_WORLD_EXAMPLE = String.join("\n",
            "{ \"cells\":",
            "  [",
            "   {",
            "       \"cell_type\": \"markdown\",",
            "       \"metadata\": {},",
            "       \"source\": [ \"# Markdown block\\n\",",
            "           \"\\n\"]",
            "   },",
            "   {",
            "       \"cell_type\": \"code\",",
            "       \"execution_count\": null,",
            "       \"outputs\": [],",
            "       \"source\": [ \"x=1\"]",
            "   },",
            "   {",
            "       \"cell_type\": \"code\",",
            "       \"source\": [",
            "           \"a=1\", \"print(a)\"]",
            "   }",
            "  ],",
            " \"nbformat\": 4,",
            " \"nbformat_minor\": 4 }");
}