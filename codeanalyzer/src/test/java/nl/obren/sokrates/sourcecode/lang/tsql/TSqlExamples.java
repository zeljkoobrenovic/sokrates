/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.tsql;

public class TSqlExamples {

    // Simple T-SQL with a line comment and a block comment; after cleaning both should be gone and empty
    // lines removed. String literals are kept by the cleaner (they are emptied only for McCabe analysis).
    public static final String CONTENT_LOC = ""
            + "-- line comment\n"
            + "DECLARE @x INT;\n"
            + "/* block\n"
            + "   comment */\n"
            + "\n"
            + "SET @x = 1;\n";

    public static final String CONTENT_LOC_CLEANED = ""
            + "DECLARE @x INT;\n"
            + "SET @x = 1;";

    // Duplication cleaning strips comments and trims per-line whitespace (collapses runs of spaces).
    // String literals are kept intact; they are emptied only during McCabe analysis.
    public static final String CONTENT_DUP = ""
            + "-- comment\n"
            + "  SELECT   col, @name \n"
            + "  FROM   dbo.t\n";

    public static final String CONTENT_DUP_CLEANED = ""
            + "SELECT col, @name\n"
            + "FROM dbo.t";

    // Simple procedure with one IF/ELSE branch. McCabe = 1 + 1 (IF) = 2, params = 1.
    public static final String SIMPLE_PROCEDURE = ""
            + "CREATE PROCEDURE dbo.foo @a INT\n"
            + "AS\n"
            + "BEGIN\n"
            + "  IF @a > 0\n"
            + "    SELECT 1\n"
            + "  ELSE\n"
            + "    SELECT 0\n"
            + "END\n";

    // Procedure exercising WHILE + CASE/WHEN counting. McCabe = 1 + WHILE(1) + CASE(1) + WHEN(2) = 5.
    public static final String WHILE_AND_CASE_PROCEDURE = ""
            + "CREATE PROCEDURE dbo.bar\n"
            + "AS\n"
            + "BEGIN\n"
            + "  DECLARE @i INT = 0\n"
            + "  WHILE @i < 10\n"
            + "  BEGIN\n"
            + "    SELECT CASE WHEN @i = 0 THEN 'a' WHEN @i = 1 THEN 'b' ELSE 'c' END\n"
            + "    SET @i = @i + 1\n"
            + "  END\n"
            + "END\n";

    // TRY/CATCH pairs must NOT affect BEGIN/END depth (self-balancing). McCabe = 1 + BEGIN_CATCH(1) = 2.
    public static final String TRY_CATCH_PROCEDURE = ""
            + "CREATE PROCEDURE dbo.baz\n"
            + "AS\n"
            + "BEGIN\n"
            + "  BEGIN TRY\n"
            + "    SELECT 1\n"
            + "  END TRY\n"
            + "  BEGIN CATCH\n"
            + "    SELECT 2\n"
            + "  END CATCH\n"
            + "END\n";

    // Parenthesised parameter list: (@a INT, @b VARCHAR(10)).
    public static final String PAREN_PARAMS_PROCEDURE = ""
            + "CREATE PROCEDURE dbo.p1 (@a INT, @b VARCHAR(10))\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT 1\n"
            + "END\n";

    // Bareword parameter list: @a INT, @b VARCHAR(10) — T-SQL only.
    public static final String BAREWORD_PARAMS_PROCEDURE = ""
            + "CREATE PROCEDURE dbo.p2 @a INT, @b VARCHAR(10)\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT 1\n"
            + "END\n";

    // Inline table-valued function: no BEGIN/END, terminated by statement semicolon.
    public static final String INLINE_TVF = ""
            + "CREATE FUNCTION dbo.f1 (@a INT) RETURNS TABLE AS RETURN (SELECT @a AS x);\n";

    // Two procedures separated by a GO batch boundary — must be extracted as 2 distinct units.
    public static final String TWO_PROCS_WITH_GO = ""
            + "CREATE PROCEDURE dbo.p1\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT 1\n"
            + "END\n"
            + "GO\n"
            + "CREATE PROCEDURE dbo.p2\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT 2\n"
            + "END\n"
            + "GO\n";

    // CREATE OR ALTER introduced in SQL Server 2016+.
    public static final String CREATE_OR_ALTER = ""
            + "CREATE OR ALTER PROCEDURE dbo.p3 @a INT\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT @a\n"
            + "END\n";

    // Bracket-quoted name preserves the bracketed, schema-qualified form in shortName.
    public static final String BRACKETED_NAME = ""
            + "CREATE PROCEDURE [dbo].[weird name]\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT 1\n"
            + "END\n";

    // Byte-identical to CREATE_OR_ALTER apart from the DDL keyword. Pinning the pair together is what
    // makes it visible that the OR in "CREATE OR ALTER" is not a decision point.
    public static final String PLAIN_CREATE_COUNTERPART = ""
            + "CREATE PROCEDURE dbo.p3 @a INT\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT @a\n"
            + "END\n";

    // The PROC abbreviation is as common as the full keyword in hand-written T-SQL.
    public static final String PROC_ABBREVIATION = ""
            + "CREATE PROC dbo.abbr @a INT\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT 1\n"
            + "END\n";

    // A bare ALTER, with no CREATE — the form a deployment script uses for an object that already exists.
    public static final String BARE_ALTER = ""
            + "ALTER PROCEDURE dbo.alt @a INT\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT 1\n"
            + "END\n";

    // Trigger signature: FOR/AFTER/INSTEAD OF between the name and AS.
    public static final String TRIGGER = ""
            + "CREATE TRIGGER dbo.trg ON dbo.t AFTER INSERT\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT 1\n"
            + "END\n";

    // Scalar function: RETURNS <type> rather than RETURNS TABLE, so it takes the BEGIN/END route rather
    // than the inline table-valued one.
    public static final String SCALAR_FUNCTION = ""
            + "CREATE FUNCTION dbo.sc (@a INT) RETURNS INT\n"
            + "AS\n"
            + "BEGIN\n"
            + "  RETURN @a\n"
            + "END\n";

    // Multi-statement table-valued function: RETURNS @t TABLE (...) — despite the word TABLE this has a
    // BEGIN/END body and must NOT take the inline route.
    public static final String MULTI_STATEMENT_TVF = ""
            + "CREATE FUNCTION dbo.mstvf (@a INT) RETURNS @t TABLE (x INT)\n"
            + "AS\n"
            + "BEGIN\n"
            + "  INSERT @t SELECT @a\n"
            + "  RETURN\n"
            + "END\n";

    // A body with no BEGIN at all — a single statement, ended by the batch separator.
    public static final String NO_BEGIN_SINGLE_STATEMENT = ""
            + "CREATE PROCEDURE dbo.s\n"
            + "AS\n"
            + "  SELECT 1\n"
            + "GO\n"
            + "CREATE PROCEDURE dbo.t\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT 2\n"
            + "END\n";

    // IIF and CHOOSE are branch constructs written as function calls.
    public static final String IIF_AND_CHOOSE = ""
            + "CREATE PROCEDURE dbo.i\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT IIF(@x > 0, 1, 0), CHOOSE(@y, 'a', 'b')\n"
            + "END\n";

    // Keywords inside a string literal are data, not control flow.
    public static final String KEYWORDS_IN_STRING_LITERAL = ""
            + "CREATE PROCEDURE dbo.strlit\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT 'IF WHILE AND OR CASE WHEN'\n"
            + "END\n";

    // A body with no BEGIN of its own, whose inner IF blocks each have one. Closing the unit when the
    // first inner block closes would end it at line 6 and drop lines 7-10.
    public static final String NO_OUTER_BEGIN_WITH_INNER_BLOCKS = ""
            + "CREATE PROCEDURE dbo.p1\n"
            + "AS\n"
            + "IF @x = 1\n"
            + "BEGIN\n"
            + "  SELECT 1\n"
            + "END\n"
            + "IF @x = 2\n"
            + "BEGIN\n"
            + "  SELECT 2\n"
            + "END\n"
            + "GO\n";

    // BEGIN DISTRIBUTED TRANSACTION is closed by COMMIT, not by END, so it must not open a depth.
    public static final String DISTRIBUTED_TRANSACTION = ""
            + "CREATE PROCEDURE dbo.p1\n"
            + "AS\n"
            + "BEGIN\n"
            + "  BEGIN DISTRIBUTED TRANSACTION\n"
            + "  SELECT 1\n"
            + "  COMMIT\n"
            + "END\n"
            + "GO\n"
            + "CREATE PROCEDURE dbo.p2\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT 2\n"
            + "END\n"
            + "GO\n";

    // CASE and END are reserved words, so bracketing is the only way to use them as column names.
    public static final String BRACKETED_RESERVED_WORD_COLUMNS = ""
            + "CREATE PROCEDURE dbo.p1\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT [Case], [End], [When] FROM dbo.Legal\n"
            + "  SELECT 2\n"
            + "END\n"
            + "GO\n"
            + "CREATE PROCEDURE dbo.p2\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT 3\n"
            + "END\n"
            + "GO\n";

    // The DDL keyword on its own line, with the object keyword below it. Legal, and produced by real
    // exports; line-by-line matching alone does not see it.
    public static final String SPLIT_DDL_KEYWORD = ""
            + "CREATE \n"
            + "procedure [dbo].[split] @a INT as\n"
            + "BEGIN\n"
            + "  SELECT 1\n"
            + "END\n";

    // A temporary stored procedure. The # is part of the name and there is no schema.
    public static final String TEMPORARY_PROCEDURE = ""
            + "CREATE PROCEDURE #tempProc @a INT\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT 1\n"
            + "END\n";

    // A name and an identifier that are not ASCII. Both the name and the complexity count have to treat
    // a letter such as the Danish o-with-stroke as a word character rather than as a word boundary.
    public static final String NON_ASCII_IDENTIFIERS = ""
            + "CREATE PROCEDURE dbo.Opgørelse @a INT\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SET @ANDø = 1\n"
            + "  SET @ORø = 2\n"
            + "END\n";

    // A column aliased with a double-quoted reserved word. T-SQL accepts both delimiter forms, so this
    // is the same case as [Begin] and has to be masked the same way.
    public static final String QUOTED_RESERVED_WORD_ALIAS = ""
            + "CREATE PROCEDURE dbo.p1\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT col AS \"BEGIN\"\n"
            + "END\n"
            + "GO\n"
            + "CREATE PROCEDURE dbo.p2\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT 2\n"
            + "END\n"
            + "GO\n";

    // A delimited identifier containing an escaped closing bracket: [Value]]Begin] names Value]Begin.
    public static final String ESCAPED_BRACKET_IN_IDENTIFIER = ""
            + "CREATE PROCEDURE dbo.p1\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT [Value]]Begin] FROM dbo.t\n"
            + "END\n"
            + "GO\n"
            + "CREATE PROCEDURE dbo.p2\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT 2\n"
            + "END\n"
            + "GO\n";

    // The object's own name contains a bare AS, which must not be read as the end of the signature.
    public static final String KEYWORD_INSIDE_DELIMITED_NAME = ""
            + "CREATE PROCEDURE [dbo].[AS]\n"
            + "@a INT,\n"
            + "@b INT\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT 1\n"
            + "END\n";

    // An inline table-valued function written the way they normally are - across several lines.
    public static final String MULTILINE_INLINE_TVF = ""
            + "CREATE FUNCTION dbo.f1 (@a INT)\n"
            + "RETURNS TABLE\n"
            + "AS\n"
            + "RETURN\n"
            + "(\n"
            + "  SELECT @a AS x\n"
            + ")\n"
            + "GO\n"
            + "CREATE PROCEDURE dbo.p2\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT 2\n"
            + "END\n"
            + "GO\n";

    // A commented-out definition must not be mistaken for a real one.
    public static final String COMMENTED_OUT_DEFINITION = ""
            + "-- CREATE PROCEDURE dbo.notreal\n"
            + "CREATE PROCEDURE dbo.real\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT 1\n"
            + "END\n";

    // A literal ending in a backslash - a Windows path, which is routine in maintenance scripts. A
    // backslash is an ordinary character in T-SQL, so the string ends at the quote that follows it.
    public static final String BACKSLASH_TERMINATED_STRING = ""
            + "CREATE PROCEDURE dbo.p1\n"
            + "AS\n"
            + "BEGIN\n"
            + "  DECLARE @path NVARCHAR(200)\n"
            + "  SET @path = 'D:\\Backups\\'\n"
            + "END\n"
            + "GO\n"
            + "CREATE PROCEDURE dbo.p2\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT 2\n"
            + "END\n"
            + "GO\n";

    // T-SQL's actual escape: a quote is doubled inside a literal.
    public static final String DOUBLED_QUOTE_STRING = ""
            + "CREATE PROCEDURE dbo.p1\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT 'it''s -- not a comment'\n"
            + "END\n"
            + "GO\n"
            + "CREATE PROCEDURE dbo.p2\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT 2\n"
            + "END\n"
            + "GO\n";

    // BEGIN TRANSACTION with the keywords split across lines - legal, and invisible to a line-by-line
    // scan, so the depth count treats the BEGIN as a block that COMMIT never closes.
    public static final String BEGIN_TRANSACTION_SPLIT_ACROSS_LINES = ""
            + "CREATE PROCEDURE dbo.p1\n"
            + "AS\n"
            + "BEGIN\n"
            + "  BEGIN\n"
            + "  TRANSACTION\n"
            + "  UPDATE t SET c = 1\n"
            + "  COMMIT\n"
            + "END\n"
            + "GO\n"
            + "CREATE PROCEDURE dbo.p2\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT 2\n"
            + "END\n"
            + "GO\n";

    // Malformed: the first procedure never closes its BEGIN. The batch separator is all there is to
    // stop it consuming what follows.
    public static final String MISSING_END_BEFORE_GO = ""
            + "CREATE PROCEDURE dbo.p1\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT 1\n"
            + "GO\n"
            + "CREATE PROCEDURE dbo.p2\n"
            + "AS\n"
            + "BEGIN\n"
            + "  SELECT 2\n"
            + "END\n"
            + "GO\n";
}
