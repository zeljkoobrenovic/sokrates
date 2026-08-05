/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.tsql;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TSqlAnalyzerTest {

    private TSqlAnalyzer analyzer;

    @Before
    public void init() {
        analyzer = new TSqlAnalyzer();
    }

    private SourceFile srcFile(String content) {
        return srcFile("test.tsql", content);
    }

    private SourceFile srcFile(String name, String content) {
        return new SourceFile(new File(name), content);
    }

    @Test
    public void cleanForLinesOfCodeCalculations_removesCommentsAndEmptyLines() {
        CleanedContent cleaned = analyzer.cleanForLinesOfCodeCalculations(srcFile(TSqlExamples.CONTENT_LOC));
        assertEquals(TSqlExamples.CONTENT_LOC_CLEANED, cleaned.getCleanedContent());
    }

    @Test
    public void cleanForDuplicationCalculations_trimsWhitespaceAndEmptiesStrings() {
        CleanedContent cleaned = analyzer.cleanForDuplicationCalculations(srcFile(TSqlExamples.CONTENT_DUP));
        assertEquals(TSqlExamples.CONTENT_DUP_CLEANED, cleaned.getCleanedContent());
    }

    @Test
    public void extractUnits_simpleProcedureWithIfElse() {
        List<UnitInfo> units = analyzer.extractUnits(srcFile(TSqlExamples.SIMPLE_PROCEDURE));
        assertEquals(1, units.size());
        UnitInfo unit = units.get(0);
        assertEquals("dbo.foo", unit.getShortName());
        assertEquals(1, unit.getNumberOfParameters());
        // McCabe: base 1 + IF(1). ELSE is not a decision point on its own.
        assertEquals(2, unit.getMcCabeIndex());
        assertEquals(8, unit.getLinesOfCode());
        assertEquals(1, unit.getStartLine());
        assertEquals(8, unit.getEndLine());
    }

    @Test
    public void extractUnits_whileAndCase() {
        List<UnitInfo> units = analyzer.extractUnits(srcFile(TSqlExamples.WHILE_AND_CASE_PROCEDURE));
        assertEquals(1, units.size());
        UnitInfo unit = units.get(0);
        assertEquals("dbo.bar", unit.getShortName());
        assertEquals(0, unit.getNumberOfParameters());
        // McCabe: 1 + WHILE(1) + CASE(1) + WHEN(2) = 5.
        assertEquals(5, unit.getMcCabeIndex());
        assertEquals(10, unit.getLinesOfCode());
    }

    @Test
    public void extractUnits_tryCatchDoesNotTerminateEarly() {
        List<UnitInfo> units = analyzer.extractUnits(srcFile(TSqlExamples.TRY_CATCH_PROCEDURE));
        assertEquals(1, units.size());
        UnitInfo unit = units.get(0);
        assertEquals("dbo.baz", unit.getShortName());
        // Without the TRY/CATCH exclusion the unit would close at 'END TRY' (line 6) instead of the
        // outer 'END' (line 10).
        assertEquals(10, unit.getLinesOfCode());
        // McCabe: 1 + BEGIN CATCH(1) = 2.
        assertEquals(2, unit.getMcCabeIndex());
    }

    @Test
    public void extractUnits_parenthesisedParameterList() {
        List<UnitInfo> units = analyzer.extractUnits(srcFile(TSqlExamples.PAREN_PARAMS_PROCEDURE));
        assertEquals(1, units.size());
        assertEquals(2, units.get(0).getNumberOfParameters());
        assertEquals("dbo.p1", units.get(0).getShortName());
    }

    @Test
    public void extractUnits_barewordParameterList() {
        List<UnitInfo> units = analyzer.extractUnits(srcFile(TSqlExamples.BAREWORD_PARAMS_PROCEDURE));
        assertEquals(1, units.size());
        assertEquals(2, units.get(0).getNumberOfParameters());
        assertEquals("dbo.p2", units.get(0).getShortName());
    }

    @Test
    public void extractUnits_inlineTableValuedFunction() {
        List<UnitInfo> units = analyzer.extractUnits(srcFile(TSqlExamples.INLINE_TVF));
        assertEquals(1, units.size());
        UnitInfo unit = units.get(0);
        assertEquals("dbo.f1", unit.getShortName());
        assertEquals(1, unit.getNumberOfParameters());
        assertEquals(1, unit.getMcCabeIndex());
        // Whole definition fits on one line.
        assertEquals(1, unit.getLinesOfCode());
        assertEquals(1, unit.getStartLine());
        assertEquals(1, unit.getEndLine());
    }

    @Test
    public void extractUnits_twoProceduresSeparatedByGo() {
        List<UnitInfo> units = analyzer.extractUnits(srcFile(TSqlExamples.TWO_PROCS_WITH_GO));
        assertEquals(2, units.size());
        assertEquals("dbo.p1", units.get(0).getShortName());
        assertEquals("dbo.p2", units.get(1).getShortName());
        assertEquals(5, units.get(0).getLinesOfCode());
        assertEquals(5, units.get(1).getLinesOfCode());
        // Second unit starts after the GO and any intermediate blank lines.
        assertEquals(1, units.get(0).getStartLine());
        assertEquals(5, units.get(0).getEndLine());
        assertEquals(7, units.get(1).getStartLine());
        assertEquals(11, units.get(1).getEndLine());
    }

    @Test
    public void extractUnits_createOrAlter() {
        List<UnitInfo> units = analyzer.extractUnits(srcFile(TSqlExamples.CREATE_OR_ALTER));
        assertEquals(1, units.size());
        assertEquals("dbo.p3", units.get(0).getShortName());
        assertEquals(1, units.get(0).getNumberOfParameters());
    }

    @Test
    public void extractUnits_theDdlKeywordDoesNotAffectComplexity() {
        // The OR in "CREATE OR ALTER" is part of the DDL statement, not a boolean operator in the code.
        // Counting it scored this procedure 2 against 1 for the byte-identical plain-CREATE form, so the
        // two are asserted against each other rather than against a literal.
        UnitInfo createOrAlter = only(analyzer.extractUnits(srcFile(TSqlExamples.CREATE_OR_ALTER)));
        UnitInfo plainCreate = only(analyzer.extractUnits(srcFile(TSqlExamples.PLAIN_CREATE_COUNTERPART)));

        assertEquals(1, plainCreate.getMcCabeIndex());
        assertEquals(plainCreate.getMcCabeIndex(), createOrAlter.getMcCabeIndex());
    }

    @Test
    public void extractUnits_procAbbreviationAndBareAlter() {
        UnitInfo proc = only(analyzer.extractUnits(srcFile(TSqlExamples.PROC_ABBREVIATION)));
        assertEquals("dbo.abbr", proc.getShortName());
        assertEquals(1, proc.getNumberOfParameters());
        assertEquals(5, proc.getEndLine());

        UnitInfo altered = only(analyzer.extractUnits(srcFile(TSqlExamples.BARE_ALTER)));
        assertEquals("dbo.alt", altered.getShortName());
        assertEquals(1, altered.getNumberOfParameters());
        assertEquals(5, altered.getEndLine());
    }

    @Test
    public void extractUnits_trigger() {
        // The signature runs name -> ON -> AFTER INSERT -> AS, so the body must not be taken to start
        // before the AS.
        UnitInfo unit = only(analyzer.extractUnits(srcFile(TSqlExamples.TRIGGER)));
        assertEquals("dbo.trg", unit.getShortName());
        assertEquals(0, unit.getNumberOfParameters());
        assertEquals(1, unit.getStartLine());
        assertEquals(5, unit.getEndLine());
    }

    @Test
    public void extractUnits_scalarFunctionTakesTheBeginEndRoute() {
        UnitInfo unit = only(analyzer.extractUnits(srcFile(TSqlExamples.SCALAR_FUNCTION)));
        assertEquals("dbo.sc", unit.getShortName());
        assertEquals(1, unit.getNumberOfParameters());
        assertEquals(5, unit.getEndLine());
    }

    @Test
    public void extractUnits_multiStatementTvfIsNotTreatedAsInline() {
        // "RETURNS @t TABLE (...)" contains the word TABLE but has a BEGIN/END body, so it must take the
        // BEGIN/END route; the inline route would end it at the first statement terminator.
        UnitInfo unit = only(analyzer.extractUnits(srcFile(TSqlExamples.MULTI_STATEMENT_TVF)));
        assertEquals("dbo.mstvf", unit.getShortName());
        assertEquals(1, unit.getNumberOfParameters());
        assertEquals(1, unit.getStartLine());
        assertEquals(6, unit.getEndLine());
    }

    @Test
    public void extractUnits_bodyWithNoBeginEndsAtTheBatchSeparator() {
        List<UnitInfo> units = analyzer.extractUnits(srcFile(TSqlExamples.NO_BEGIN_SINGLE_STATEMENT));
        assertEquals(2, units.size());
        assertEquals("dbo.s", units.get(0).getShortName());
        // Ends at the statement before GO, not swallowing the procedure that follows it.
        assertEquals(1, units.get(0).getStartLine());
        assertEquals(3, units.get(0).getEndLine());
        assertEquals("dbo.t", units.get(1).getShortName());
        assertEquals(5, units.get(1).getStartLine());
        assertEquals(9, units.get(1).getEndLine());
    }

    @Test
    public void extractUnits_iifAndChooseAreBranches() {
        // Both are branch constructs written as function calls. IIF must also not be counted twice by the
        // plain IF pattern - "IIF" has no word boundary before its trailing "IF".
        assertEquals(3, only(analyzer.extractUnits(srcFile(TSqlExamples.IIF_AND_CHOOSE))).getMcCabeIndex());
    }

    @Test
    public void extractUnits_keywordsInsideStringLiteralsAreNotCounted() {
        assertEquals(1, only(analyzer.extractUnits(
                srcFile(TSqlExamples.KEYWORDS_IN_STRING_LITERAL))).getMcCabeIndex());
    }

    @Test
    public void extractUnits_commentedOutDefinitionIsIgnored() {
        UnitInfo unit = only(analyzer.extractUnits(srcFile(TSqlExamples.COMMENTED_OUT_DEFINITION)));
        assertEquals("dbo.real", unit.getShortName());
        // Line 1 is the commented-out definition, so a correct extractor cannot start the unit there.
        assertEquals(2, unit.getStartLine());
        assertEquals(6, unit.getEndLine());
    }

    @Test
    public void extractUnits_bodyWithNoOuterBeginRunsPastItsInnerBlocks() {
        // The first BEGIN here belongs to the IF on line 3, not to the procedure. Treating it as the
        // procedure's own closed the unit at line 6 and dropped the second IF from size and complexity.
        UnitInfo unit = only(analyzer.extractUnits(srcFile(TSqlExamples.NO_OUTER_BEGIN_WITH_INNER_BLOCKS)));
        assertEquals("dbo.p1", unit.getShortName());
        assertEquals(1, unit.getStartLine());
        assertEquals(10, unit.getEndLine());
        // Base 1 + IF + IF. Both branches are inside the unit, so both are counted.
        assertEquals(3, unit.getMcCabeIndex());
    }

    @Test
    public void extractUnits_distributedTransactionDoesNotOpenABlock() {
        // A transaction is closed by COMMIT, not END. Counting BEGIN DISTRIBUTED TRANSACTION as an open
        // block left a depth that never returned to zero, so dbo.p1 swallowed dbo.p2 and the file
        // reported one unit instead of two.
        List<UnitInfo> units = analyzer.extractUnits(srcFile(TSqlExamples.DISTRIBUTED_TRANSACTION));
        assertEquals(2, units.size());
        assertEquals("dbo.p1", units.get(0).getShortName());
        assertEquals(7, units.get(0).getEndLine());
        assertEquals("dbo.p2", units.get(1).getShortName());
        assertEquals(9, units.get(1).getStartLine());
        assertEquals(13, units.get(1).getEndLine());
    }

    @Test
    public void extractUnits_bracketedReservedWordsAreNotKeywords() {
        // [Case] and [End] are column names. Reading them as block delimiters both corrupted the depth -
        // dbo.p1 ran to the end of the file - and inflated complexity.
        List<UnitInfo> units = analyzer.extractUnits(srcFile(TSqlExamples.BRACKETED_RESERVED_WORD_COLUMNS));
        assertEquals(2, units.size());
        assertEquals("dbo.p1", units.get(0).getShortName());
        assertEquals(1, units.get(0).getStartLine());
        assertEquals(6, units.get(0).getEndLine());
        assertEquals(1, units.get(0).getMcCabeIndex());
        assertEquals("dbo.p2", units.get(1).getShortName());
        assertEquals(8, units.get(1).getStartLine());
        assertEquals(12, units.get(1).getEndLine());
    }

    @Test
    public void extractUnits_ddlKeywordOnItsOwnLine() {
        // "CREATE" alone, with "procedure ..." on the next line. The unit still starts at the CREATE.
        UnitInfo unit = only(analyzer.extractUnits(srcFile(TSqlExamples.SPLIT_DDL_KEYWORD)));
        assertEquals("[dbo].[split]", unit.getShortName());
        assertEquals(1, unit.getNumberOfParameters());
        assertEquals(1, unit.getStartLine());
        assertEquals(5, unit.getEndLine());
        assertEquals(1, unit.getMcCabeIndex());
    }

    @Test
    public void extractUnits_temporaryProcedureKeepsItsName() {
        UnitInfo unit = only(analyzer.extractUnits(srcFile(TSqlExamples.TEMPORARY_PROCEDURE)));
        assertEquals("#tempProc", unit.getShortName());
        assertEquals(1, unit.getNumberOfParameters());
    }

    @Test
    public void extractUnits_nonAsciiIdentifiers() {
        // Java's \w is ASCII only unless told otherwise, and \b is defined in terms of it. Without the
        // Unicode flag the name truncated at the first non-ASCII letter, and an identifier such as @ANDø
        // ended the \bAND\b boundary early and counted as a branch.
        UnitInfo unit = only(analyzer.extractUnits(srcFile(TSqlExamples.NON_ASCII_IDENTIFIERS)));
        assertEquals("dbo.Opgørelse", unit.getShortName());
        assertEquals(1, unit.getMcCabeIndex());
    }

    @Test
    public void extractUnits_doubleQuotedReservedWordAlias() {
        // Masking only [...] left the other delimiter form exposed: the word inside "BEGIN" was read as
        // an opening block, depth never returned to zero, and dbo.p2 disappeared from the file.
        List<UnitInfo> units = analyzer.extractUnits(srcFile(TSqlExamples.QUOTED_RESERVED_WORD_ALIAS));
        assertEquals(2, units.size());
        assertEquals("dbo.p1", units.get(0).getShortName());
        assertEquals(5, units.get(0).getEndLine());
        assertEquals("dbo.p2", units.get(1).getShortName());
        assertEquals(7, units.get(1).getStartLine());
        assertEquals(11, units.get(1).getEndLine());
    }

    @Test
    public void extractUnits_escapedClosingBracketInIdentifier() {
        // [Value]]Begin] is one identifier naming Value]Begin. Stopping the mask at the first ] left
        // "Begin" exposed, with the same disappearing-procedure result.
        List<UnitInfo> units = analyzer.extractUnits(srcFile(TSqlExamples.ESCAPED_BRACKET_IN_IDENTIFIER));
        assertEquals(2, units.size());
        assertEquals("dbo.p1", units.get(0).getShortName());
        assertEquals(5, units.get(0).getEndLine());
        assertEquals("dbo.p2", units.get(1).getShortName());
        assertEquals(7, units.get(1).getStartLine());
        assertEquals(11, units.get(1).getEndLine());
    }

    @Test
    public void extractUnits_keywordInsideTheObjectName() {
        // The AS in [dbo].[AS] is part of the name, not the end of the signature. Reading it as the end
        // cut the parameter list off before it was counted.
        UnitInfo unit = only(analyzer.extractUnits(srcFile(TSqlExamples.KEYWORD_INSIDE_DELIMITED_NAME)));
        assertEquals("[dbo].[AS]", unit.getShortName());
        assertEquals(2, unit.getNumberOfParameters());
    }

    @Test
    public void extractUnits_multiLineInlineTableValuedFunction() {
        // The single-line fixture never exercises the cross-line RETURN/paren tracking, which is how
        // inline table-valued functions are normally written.
        List<UnitInfo> units = analyzer.extractUnits(srcFile(TSqlExamples.MULTILINE_INLINE_TVF));
        assertEquals(2, units.size());
        assertEquals("dbo.f1", units.get(0).getShortName());
        assertEquals(1, units.get(0).getStartLine());
        assertEquals(7, units.get(0).getEndLine());
        assertEquals(1, units.get(0).getNumberOfParameters());
        assertEquals("dbo.p2", units.get(1).getShortName());
    }

    @Test
    public void extractUnits_longDelimitedIdentifierDoesNotExhaustTheStack() {
        // Matching a delimited identifier with a group repeated per character makes the regex engine
        // recurse per character, which overflowed the stack on a long one - an Error, not an Exception,
        // so nothing upstream would have caught it. Both a closed and an unclosed delimiter are checked,
        // since a truncated export produces the second.
        String filler = new String(new char[40000]).replace('\0', 'x');

        analyzer.extractUnits(srcFile("CREATE PROCEDURE dbo.p\nAS\nBEGIN\n  SELECT [ok" + filler
                + "] FROM t\nEND\n"));
        analyzer.extractUnits(srcFile("CREATE PROCEDURE dbo.p\nAS\nBEGIN\n  SELECT [unterminated"
                + filler + "\nEND\n"));
        analyzer.extractUnits(srcFile("CREATE PROCEDURE dbo.p\nAS\nBEGIN\n  SELECT \"unterminated"
                + filler + "\nEND\n"));
    }

    private UnitInfo only(List<UnitInfo> units) {
        assertEquals(1, units.size());
        return units.get(0);
    }

    @Test
    public void extractUnits_bracketedName() {
        List<UnitInfo> units = analyzer.extractUnits(srcFile(TSqlExamples.BRACKETED_NAME));
        assertEquals(1, units.size());
        assertEquals("[dbo].[weird name]", units.get(0).getShortName());
    }

    @Test
    public void computeMcCabeIndex_directWordBoundaryBehaviour() {
        TSqlHeuristicUnitsExtractor extractor = new TSqlHeuristicUnitsExtractor();
        // identifiers that merely *contain* the keyword substring must NOT count (word boundary).
        assertEquals(1, extractor.computeMcCabeIndex("SET @ANDY = 1; SET @IFACE = 2; SET @ORDER = 3;"));
        // Each genuine keyword contributes +1. Base 1 + IF + AND + OR = 4.
        assertEquals(4, extractor.computeMcCabeIndex("IF @x = 1 AND @y = 2 OR @z = 3 SELECT 1"));
    }
}
