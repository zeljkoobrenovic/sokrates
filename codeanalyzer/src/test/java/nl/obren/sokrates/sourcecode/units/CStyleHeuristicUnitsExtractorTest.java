/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.units;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CStyleHeuristicUnitsExtractorTest {
    @Test
    public void cleanContent() throws Exception {
        CStyleHeuristicUnitsExtractor unitParser = new CStyleHeuristicUnitsExtractor();

        String code = "    String getEndOfUnitBodyIndex(String[] lines, int i) {\n" +
                "        StringBuilder unitBody = new StringBuilder();\n" +
                "        int startCount = 0;\n" +
                "        int endCount = 0;\n" +
                "        for (; i < lines.length; i++) {\n" +
                "            String line = lines[i].trim();\n" +
                "            unitBody.append(line + \"\\n\");\n" +
                "            startCount += StringUtils.countMatches(line, \"{\");\n" +
                "            endCount += StringUtils.countMatches(line, \"}\");\n" +
                "            if (startCount > 0 && startCount == endCount) {\n" +
                "                break;\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        return unitBody.toString();\n" +
                "    }\n";

        assertEquals(unitParser.extraCleanContent(code), "    String getEndOfUnitBodyIndex(String[] lines, int i) {\n" +
                "        StringBuilder unitBody = new StringBuilder();\n" +
                "        int startCount = 0;\n" +
                "        int endCount = 0;\n" +
                "        for (; i < lines.length; i++) {\n" +
                "            String line = lines[i].trim();\n" +
                "            unitBody.append(line + \"\");\n" +
                "            startCount += StringUtils.countMatches(line, \"\");\n" +
                "            endCount += StringUtils.countMatches(line, \"\");\n" +
                "            if (startCount > 0 && startCount == endCount) {\n" +
                "                break;\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        return unitBody.toString();\n" +
                "    }\n");
    }

    @Test
    public void getEndOfUnitBodyIndex1() throws Exception {
        CStyleHeuristicUnitsExtractor unitParser = new CStyleHeuristicUnitsExtractor();

        String code = "    String getEndOfUnitBodyIndex(String[] lines, int i) {\n" +
                "        StringBuilder unitBody = new StringBuilder();\n" +
                "        int startCount = 0;\n" +
                "        int endCount = 0;\n" +
                "        for (; i < lines.length; i++) {\n" +
                "            String line = lines[i].trim();\n" +
                "            unitBody.append(line + \"\\n\");\n" +
                "            startCount += StringUtils.countMatches(line, \"{\");\n" +
                "            endCount += StringUtils.countMatches(line, \"}\");\n" +
                "            if (startCount > 0 && startCount == endCount) {\n" +
                "                break;\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        return unitBody.toString();\n" +
                "    }\n";

        assertEquals(unitParser.getEndOfUnitBodyIndex(SourceCodeCleanerUtils.splitInLines(code), 0), 15);
    }

    @Test
    public void getEndOfUnitBodyIndex2() throws Exception {
        CStyleHeuristicUnitsExtractor unitParser = new CStyleHeuristicUnitsExtractor();

        String code = "\n\nprotected String getEndOfUnitBodyIndex(String[] lines, int i) {\n" +
                "        StringBuilder unitBody = new StringBuilder();\n" +
                "        int startCount = 0;\n" +
                "        int endCount = 0;\n" +
                "        for (; i < lines.length; i++) {\n" +
                "            String line = lines[i].trim();\n" +
                "            unitBody.append(line + \"\\n\");\n" +
                "            startCount += StringUtils.countMatches(line, \"{\");\n" +
                "            endCount += StringUtils.countMatches(line, \"}\");\n" +
                "            if (startCount > 0 && startCount == endCount) {\n" +
                "                break;\n" +
                "            }\n" +
                "        }\n" +
                "\n" +
                "        return unitBody.toString();\n" +
                "    }\n";

        assertEquals(unitParser.getEndOfUnitBodyIndex(SourceCodeCleanerUtils.splitInLines(code), 2), 17);
    }

    @Test
    public void matchesStartOfUnit() throws Exception {
        CStyleHeuristicUnitsExtractor unitParser = new CStyleHeuristicUnitsExtractor();

        assertTrue(unitParser.isUnitSignature("private void test() {"));
        assertTrue(unitParser.isUnitSignature("public void test() {"));
        assertTrue(unitParser.isUnitSignature("protected void test() {"));
        assertTrue(unitParser.isUnitSignature("void test() {"));

        assertTrue(unitParser.isUnitSignature("private void test()"));
        assertTrue(unitParser.isUnitSignature("public void test()"));
        assertTrue(unitParser.isUnitSignature("protected void test()"));
        assertTrue(unitParser.isUnitSignature("public PersonenautoVerzekeringParams("));
        assertTrue(unitParser.isUnitSignature("private static Map<String, Filter> getG() {"));
        assertTrue(unitParser.isUnitSignature("private boolean isTG(A a) {"));


        assertTrue(unitParser.isUnitSignature("private String test() {"));
        assertTrue(unitParser.isUnitSignature("public String test() {"));
        assertTrue(unitParser.isUnitSignature("protected String test() {"));

        assertTrue(unitParser.isUnitSignature("private CustomClass test() {"));
        assertTrue(unitParser.isUnitSignature("public CustomClass test() {"));
        assertTrue(unitParser.isUnitSignature("protected CustomClass test() {"));

        assertTrue(unitParser.isUnitSignature("private void test(String param)"));
        assertTrue(unitParser.isUnitSignature("public void test(String param1, String param2)"));
        assertTrue(unitParser.isUnitSignature("protected void test(String param1, String param2, String param3)"));

        assertTrue(unitParser.isUnitSignature("public PVM(\n" +
                "            Guid id,\n" +
                "            DateTime is,\n" +
                "            int tK,\n" +
                "            int tV)"));
    }

    @Test
    public void matchesStartOfUnitFalse() throws Exception {
        CStyleHeuristicUnitsExtractor unitParser = new CStyleHeuristicUnitsExtractor();

        assertFalse(unitParser.isUnitSignature(""));
        assertFalse(unitParser.isUnitSignature("()"));
        assertFalse(unitParser.isUnitSignature("() {"));
        assertFalse(unitParser.isUnitSignature("{"));
        assertFalse(unitParser.isUnitSignature("test() {"));
        assertFalse(unitParser.isUnitSignature("String s = new Test()"));
        assertFalse(unitParser.isUnitSignature("return new Test();"));
        assertFalse(unitParser.isUnitSignature("return test();"));
    }

    @Test
    public void parse1() throws Exception {
        CStyleHeuristicUnitsExtractor unitParser = new CStyleHeuristicUnitsExtractor();

        List<UnitInfo> units = unitParser.extractUnits(new SourceFile(new File("test"), "class A {\n" +
                "    public void a() {\n" +
                "        if (a) {\n" +
                "            doA();\n" +
                "        } else {\n" +
                "            doNotA();\n" +
                "        }\n" +
                "    }\n" +
                "}"));

        Assert.assertEquals(units.size(), 1);
        Assert.assertEquals(units.get(0).getShortName(), "public void a()");
        Assert.assertEquals(units.get(0).getLinesOfCode(), 7);
        Assert.assertEquals(units.get(0).getMcCabeIndex(), 2);
        Assert.assertEquals(units.get(0).getNumberOfParameters(), 0);
    }

    @Test
    public void parse2() throws Exception {
        CStyleHeuristicUnitsExtractor unitParser = new CStyleHeuristicUnitsExtractor();

        List<UnitInfo> units = unitParser.extractUnits(new SourceFile(new File("test"), "class A {\n" +
                "    public void a(int a) {\n" +
                "        switch (a) {\n" +
                "        case 0:\n" +
                "            do0();\n" +
                "            break;\n" +
                "        case 1:\n" +
                "            do1();\n" +
                "            break;\n" +
                "        default:\n" +
                "            doDefault();\n" +
                "            break;\n" +
                "        }\n" +
                "    }\n" +
                "}"));

        Assert.assertEquals(units.size(), 1);
        Assert.assertEquals(units.get(0).getShortName(), "public void a()");
        Assert.assertEquals(units.get(0).getLinesOfCode(), 13);
        Assert.assertEquals(units.get(0).getMcCabeIndex(), 3);
        Assert.assertEquals(units.get(0).getNumberOfParameters(), 1);
    }

    @Test
    public void parse3() throws Exception {
        CStyleHeuristicUnitsExtractor unitParser = new CStyleHeuristicUnitsExtractor();

        List<UnitInfo> units = unitParser.extractUnits(new SourceFile(new File("test"), "   public void setSize(int size) {\n        this.size = size;    }\n"));

        Assert.assertEquals(units.size(), 1);
        Assert.assertEquals(units.get(0).getShortName(), "public void setSize()");
        Assert.assertEquals(units.get(0).getLinesOfCode(), 2);
        Assert.assertEquals(units.get(0).getMcCabeIndex(), 1);
        Assert.assertEquals(units.get(0).getNumberOfParameters(), 1);
    }

    @Test
    public void parseComplexExample() throws Exception {
        CStyleHeuristicUnitsExtractor unitParser = new CStyleHeuristicUnitsExtractor();

        List<UnitInfo> units = unitParser.extractUnits(new SourceFile(new File("test"), getContent()));

        Assert.assertEquals(units.size(), 6);
        Assert.assertEquals(units.get(0).getShortName(), "public static void main()");
        Assert.assertEquals(units.get(0).getLinesOfCode(), 3);
        Assert.assertEquals(units.get(0).getMcCabeIndex(), 1);
        Assert.assertEquals(units.get(0).getNumberOfParameters(), 1);

        Assert.assertEquals(units.get(1).getShortName(), "public void parseComplexExample()");
        Assert.assertEquals(units.get(1).getLinesOfCode(), 12);
        Assert.assertEquals(units.get(1).getMcCabeIndex(), 2);
        Assert.assertEquals(units.get(1).getNumberOfParameters(), 1);

        Assert.assertEquals(units.get(2).getShortName(), "public void processTree()");
        Assert.assertEquals(units.get(3).getShortName(), "private static void printUnit()");
        Assert.assertEquals(units.get(4).getShortName(), "public UnitInfo processUnit()");
        Assert.assertEquals(units.get(5).getShortName(), "private int countLines()");
    }

    private String getContent() {
        return "package nl.obren.codeexplorer.sourcecode.units;\n" +
                "\n" +
                "import nl.obren.grammars.java8.Java8Lexer;\n" +
                "import nl.obren.grammars.java8.Java8Parser;\n" +
                "import org.antlr.v4.runtime.*;\n" +
                "import org.antlr.v4.runtime.misc.Interval;\n" +
                "import org.antlr.v4.runtime.tree.ParseTree;\n" +
                "import org.apache.commons.lang3.StringUtils;\n" +
                "\n" +
                "import java.util.*;\n" +
                "\n" +
                "public class CStyleHeuristicUnitParser {\n" +
                "    public static void main(String args[]) {\n" +
                "        new CStyleHeuristicUnitParser().parseComplexExample(\"package A;\\nclass A {\\n public int a;public int b; public void a(int p1, int p2, List<String> test, String g) {if (true && false) {} }}\");\n" +
                "    }\n" +
                "\n" +
                "    public void parseComplexExample(String content) {\n" +
                "        try {\n" +
                "            Lexer lexer = new Java8Lexer(new ANTLRInputStream(content));\n" +
                "\n" +
                "            CommonTokenStream tokens = new CommonTokenStream(lexer);\n" +
                "            tokens.fill();\n" +
                "            Java8Parser parser = new Java8Parser(tokens);\n" +
                "            ParserRuleContext parseTree = parser.compilationUnit();\n" +
                "\n" +
                "            processTree(parseTree, parser, Arrays.asList(content.split(\"\\n\")), tokens.getTokens());\n" +
                "        } catch (Exception e) {\n" +
                "            e.printStackTrace();\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    public void processTree(ParseTree tree, Java8Parser parser, List<String> fileLines, List<Token> tokens) {\n" +
                "        String text = tree.toStringTree(parser);\n" +
                "        if (text.startsWith(\"(classBodyDeclaration (classMemberDeclaration (methodDeclaration \")) {\n" +
                "            Interval interval = tree.getSourceInterval();\n" +
                "\n" +
                "            UnitInfo unitInfo = processUnit(tokens.subList(interval.a, interval.b), text);\n" +
                "            printUnit(unitInfo);\n" +
                "        } else {\n" +
                "            for (int i = 0; i < tree.getChildCount(); i++) {\n" +
                "                processTree(tree.getChild(i), parser, fileLines, tokens);\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "\n" +
                "    private static void printUnit(UnitInfo unitInfo) {\n" +
                "        System.out.println();\n" +
                "        System.out.printf(\"loc = %d\\n\", unitInfo.getLinesOfCode());\n" +
                "        System.out.printf(\"params = %d\\n\", unitInfo.getNumberOfParameters());\n" +
                "        System.out.printf(\"mcCabe = %d\\n\", unitInfo.getMcCabeIndex());\n" +
                "        System.out.printf(\"statement = %d\\n\", unitInfo.getNumberOfStatements());\n" +
                "        System.out.printf(\"expression = %d\\n\", unitInfo.getNumberOfExpressions());\n" +
                "        System.out.printf(\"literals = %d\\n\", unitInfo.getNumberOfLiterals());\n" +
                "    }\n" +
                "\n" +
                "\n" +
                "    public UnitInfo processUnit(List<Token> tokens, String parserText) {\n" +
                "        UnitInfo unitInfo = new UnitInfo();\n" +
                "\n" +
                "        unitInfo.setLinesOfCode(countLines(tokens));\n" +
                "\n" +
                "        unitInfo.setMcCabeIndex(unitInfo.getMcCabeIndex() + StringUtils.countMatches(parserText, \"(ifThenStatement \"));\n" +
                "        unitInfo.setMcCabeIndex(unitInfo.getMcCabeIndex() + StringUtils.countMatches(parserText, \"(ifThenElseStatement \"));\n" +
                "        unitInfo.setMcCabeIndex(unitInfo.getMcCabeIndex() + StringUtils.countMatches(parserText, \"(whileStatement \"));\n" +
                "        unitInfo.setMcCabeIndex(unitInfo.getMcCabeIndex() + StringUtils.countMatches(parserText, \"(forStatement \"));\n" +
                "        unitInfo.setMcCabeIndex(unitInfo.getMcCabeIndex() + StringUtils.countMatches(parserText, \"(doStatement \"));\n" +
                "        unitInfo.setMcCabeIndex(unitInfo.getMcCabeIndex() + StringUtils.countMatches(parserText, \"(ifThenElseStatementNoShortIf \"));\n" +
                "        unitInfo.setMcCabeIndex(unitInfo.getMcCabeIndex() + StringUtils.countMatches(parserText, \"(whileStatementNoShortIf \"));\n" +
                "        unitInfo.setMcCabeIndex(unitInfo.getMcCabeIndex() + StringUtils.countMatches(parserText, \"(forStatementNoShortIf \"));\n" +
                "        unitInfo.setMcCabeIndex(unitInfo.getMcCabeIndex() + StringUtils.countMatches(parserText, \" || )\"));\n" +
                "        unitInfo.setMcCabeIndex(unitInfo.getMcCabeIndex() + StringUtils.countMatches(parserText, \" && (\"));\n" +
                "\n" +
                "        unitInfo.setNumberOfParameters(StringUtils.countMatches(parserText, \"(formalParameter \"));\n" +
                "        unitInfo.setNumberOfStatements(StringUtils.countMatches(parserText, \"(statement \"));\n" +
                "        unitInfo.setNumberOfExpressions(StringUtils.countMatches(parserText, \"(expression \"));\n" +
                "        unitInfo.setNumberOfLiterals(StringUtils.countMatches(parserText, \"(literal \"));\n" +
                "\n" +
                "        return unitInfo;\n" +
                "    }\n" +
                "\n" +
                "    private int countLines(List<Token> tokens) {\n" +
                "        Map<Integer,Integer> lineNumbers = new HashMap<>();\n" +
                "        for (Token token : tokens) {\n" +
                "            int lineNumber = token.getLine();\n" +
                "            lineNumbers.put(lineNumber, lineNumber);\n" +
                "        }\n" +
                "        return lineNumbers.size();\n" +
                "    }\n" +
                "}\n";
    }


}
