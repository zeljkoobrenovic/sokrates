/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.perl;

import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.sourcecode.cleaners.CommentsAndEmptyLinesCleaner;
import nl.obren.sokrates.sourcecode.units.CStyleHeuristicUnitsExtractor;
import nl.obren.sokrates.sourcecode.units.UnitInfo;

public class PerlHeuristicUnitsExtractor extends CStyleHeuristicUnitsExtractor {
    @Override
    public boolean isUnitSignature(String line) {
        return RegexUtils.matchesEntirely(".*( |\t|)sub( |\t).*", line);
    }

    // The language cleaner keeps regexes as code; their contents (a lone quote, a brace in s/\{//) would
    // confuse the C-style brace counting, so they are removed before the C-style cleaning. Strings are only
    // skipped here (so a regex-like text inside one stays), the C-style cleaning handles them line by line:
    // removing them across lines would let an apostrophe in a here-doc (You'll) swallow the code up to the next '.
    @Override
    protected String extraCleanContent(String content) {
        CommentsAndEmptyLinesCleaner cleaner = new CommentsAndEmptyLinesCleaner();
        PerlQuoteLikeParser quoteLikeParser = new PerlQuoteLikeParser();
        quoteLikeParser.setRemoveWhenCleaning(true);
        cleaner.getCodeBlockParsers().add(quoteLikeParser);
        cleaner.addStringBlockHelper("\"", "\\");
        cleaner.addStringBlockHelper("'", "\\");
        return super.extraCleanContent(cleaner.cleanKeepEmptyLines(content));
    }

    @Override
    public void setNameAndParameters(String line, UnitInfo unit, String cleandedBody) {
        super.setNameAndParameters(line, unit, cleandedBody);
        unit.setNumberOfParameters(0);
    }
}
