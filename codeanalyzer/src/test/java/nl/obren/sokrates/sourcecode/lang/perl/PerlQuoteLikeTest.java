/*
 * Copyright (c) 2026 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.perl;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

// issue #131: quotes and # inside Perl regexes and quote-like operators
public class PerlQuoteLikeTest {
    private static final String ISSUE_EXAMPLE = "sub html_quote {\n" +
            "  my ($var) = @_;\n" +
            "  $var =~ s/\"/&quot;/g;\n" +
            "  return $var;\n" +
            "}\n" +
            "\n" +
            "sub other {\n" +
            "  # a comment\n" +
            "  return 1;\n" +
            "}\n";

    private String clean(String code) {
        return new PerlAnalyzer().cleanForLinesOfCodeCalculations(new SourceFile(new File("a.pl"), code)).getCleanedContent()
                .replaceAll("[ \t]+\n", "\n").trim();
    }

    private List<UnitInfo> units(String code) {
        return new PerlAnalyzer().extractUnits(new SourceFile(new File("a.pl"), code));
    }

    @Test
    public void aQuoteInASubstitutionDoesNotDropTheRestOfTheFile() {
        assertEquals("sub html_quote {\n" +
                "  my ($var) = @_;\n" +
                "  $var =~ s/\"/&quot;/g;\n" +
                "  return $var;\n" +
                "}\n" +
                "sub other {\n" +
                "  return 1;\n" +
                "}", clean(ISSUE_EXAMPLE));

        List<UnitInfo> units = units(ISSUE_EXAMPLE);
        assertEquals("html_quote(), other()", units.stream().map(UnitInfo::getShortName).map(n -> n.replace("sub ", "")).collect(Collectors.joining(", ")));
        assertEquals(5, units.get(0).getLinesOfCode());
        assertEquals(3, units.get(1).getLinesOfCode());
    }

    @Test
    public void aClosedFalseStringNoLongerHidesComments() {
        // Bugzilla/DB/Mysql.pm: m/"/ followed later by another quote kept the comments in between as code
        assertEquals("return 1 if $word !~ m/\"/;\n" +
                "my $x = \"a\";", clean("return 1 if $word !~ m/\"/;\n" +
                "# a comment\n" +
                "my $x = \"a\";"));
    }

    @Test
    public void quoteLikeOperatorsWithAnyDelimiter() {
        String code = "s{\"}{&quot;}g;\n" +
                "s{'} {x}g; # c1\n" +
                "tr/'//d; # c2\n" +
                "y/\"/x/; # c3\n" +
                "my @w = qw(don't stop); # c4\n" +
                "my $q = qq{say \"hi\" # not a comment}; # c5\n" +
                "my $p = q'it\\'s'; # c6\n" +
                "my $r = qr/#\"/; # c7\n" +
                "s#\"#x#g; # c8\n" +
                "m!'!; # c9\n" +
                "my @parts = split /'/, $line; # c10\n" +
                "print 1 if /\"/; # c11\n" +
                "$x =~ m{a{2}\"}; # c12";
        assertEquals("s{\"}{&quot;}g;\n" +
                "s{'} {x}g;\n" +
                "tr/'//d;\n" +
                "y/\"/x/;\n" +
                "my @w = qw(don't stop);\n" +
                "my $q = qq{say \"hi\" # not a comment};\n" +
                "my $p = q'it\\'s';\n" +
                "my $r = qr/#\"/;\n" +
                "s#\"#x#g;\n" +
                "m!'!;\n" +
                "my @parts = split /'/, $line;\n" +
                "print 1 if /\"/;\n" +
                "$x =~ m{a{2}\"};", clean(code));
    }

    @Test
    public void identifiersThatLookLikeOperatorsStayCode() {
        String code = "my %h = (s => 1, y => 2, q => 3); # c1\n" +
                "$h{s} = $s / 2; # c2\n" +
                "$h{ m } = $obj->s(1) + Foo::y(2); # c3\n" +
                "print -s $file; # c4\n" +
                "my $last = $#items; # c5\n" +
                "my $n = $#{$ref} + $#$ref; # c6\n" +
                "print \"it's\"; # c7\n" +
                "@c = map { /\\s+AS\\s+(.*)$/ ? $1 : $_ } @c; # c8\n" +
                "@d = grep { /'/ } @d; # c9";
        assertEquals("my %h = (s => 1, y => 2, q => 3);\n" +
                "$h{s} = $s / 2;\n" +
                "$h{ m } = $obj->s(1) + Foo::y(2);\n" +
                "print -s $file;\n" +
                "my $last = $#items;\n" +
                "my $n = $#{$ref} + $#$ref;\n" +
                "print \"it's\";\n" +
                "@c = map { /\\s+AS\\s+(.*)$/ ? $1 : $_ } @c;\n" +
                "@d = grep { /'/ } @d;", clean(code));
    }

    @Test
    public void bracesInsideRegexesDoNotEndUnits() {
        String code = "sub a {\n" +
                "  $x =~ s/\\{//g;\n" +
                "  $x =~ s{\\}}{}g;\n" +
                "  $x =~ tr/{//d;\n" +
                "  return $x;\n" +
                "}\n" +
                "sub b {\n" +
                "  return 1;\n" +
                "}\n";
        List<UnitInfo> units = units(code);
        assertEquals(2, units.size());
        assertEquals(6, units.get(0).getLinesOfCode());
    }

    @Test
    public void hereDocTextDoesNotOpenStringsOrOperators() {
        // Bugzilla/DB/Pg.pm (SELECT $1[s.i]) and Bugzilla/Install/DB.pm (You'll in a warning)
        String code = "$dbh->do(<<END);\n" +
                "SELECT $1[s.i] AS each_item # not a comment\n" +
                "END\n" +
                "# c1\n" +
                "print <<\"EOT\";\n" +
                "You'll probably want to repair it.\n" +
                "EOT\n" +
                "# c2\n" +
                "print <<~'EOT';\n" +
                "    Don't\n" +
                "    EOT\n" +
                "my $shifted = 1 << 3; # c3";
        assertEquals("$dbh->do(<<END);\n" +
                "SELECT $1[s.i] AS each_item # not a comment\n" +
                "END\n" +
                "print <<\"EOT\";\n" +
                "You'll probably want to repair it.\n" +
                "EOT\n" +
                "print <<~'EOT';\n" +
                "    Don't\n" +
                "    EOT\n" +
                "my $shifted = 1 << 3;", clean(code));

        List<UnitInfo> units = units("sub a {\n" +
                "  print <<EOT;\n" +
                "You'll see } here\n" +
                "EOT\n" +
                "  return 1;\n" +
                "}\n" +
                "sub b {\n" +
                "  return 2;\n" +
                "}\n");
        assertEquals(2, units.size());
    }
}
