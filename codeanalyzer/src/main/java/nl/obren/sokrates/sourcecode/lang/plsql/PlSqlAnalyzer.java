package nl.obren.sokrates.sourcecode.lang.plsql;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.cleaners.CommentsAndEmptyLinesCleaner;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.dependencies.DependenciesAnalysis;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzer;
import nl.obren.sokrates.sourcecode.lang.java.JavaHeuristicDependenciesExtractor;
import nl.obren.sokrates.sourcecode.units.UnitInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PlSqlAnalyzer extends LanguageAnalyzer {
    public PlSqlAnalyzer() {
    }

    @Override
    public CleanedContent cleanForLinesOfCodeCalculations(SourceFile sourceFile) {

        return getCleaner().clean(
                SourceCodeCleanerUtils.emptyLinesMatchingPattern("/\n", sourceFile.getContent())
        );
//        return getCleaner().clean(getLinesWithoutComments(sourceFile));
    }

    private String getLinesWithoutComments(SourceFile sourceFile) {
        List<String> lines = sourceFile.getLines();
        List<String> linesWithoutComments = new ArrayList<>();
        lines.forEach(line -> {
            if (line.trim().startsWith("/*") || line.trim().startsWith("--")
                    || line.trim().startsWith("*") || line.trim().startsWith("*/")
                    || line.trim().startsWith("/")) {
                linesWithoutComments.add("");
            } else {
                linesWithoutComments.add(line);
            }
        });
        return String.join("\n", linesWithoutComments);
    }

    private CommentsAndEmptyLinesCleaner getCleaner() {

        CommentsAndEmptyLinesCleaner cleaner = new CommentsAndEmptyLinesCleaner();

        cleaner.addCommentBlockHelper("/*", "*/");
        cleaner.addCommentBlockHelper("--", "\n");
        // PL/SQL escapes a quote by doubling it ('I''m'), and a backslash is an ordinary character with
        // no special meaning in a string literal. Naming backslash as the escape marker misreads any
        // literal ending in one - 'C:\exports\', a routine path for UTL_FILE work - as an escaped
        // quote, so the parser runs on to the next quote in the file. Every literal after that point is
        // paired with the wrong partner, and the tail of the file is dropped from the cleaned content
        // entirely: lines of code are undercounted and unit boundaries understated, with nothing in the
        // report to say so. Passing the quote as its own escape marker is what CodeBlockParser reads as
        // the doubling rule.
        cleaner.addStringBlockHelper("'", "'");

        return cleaner;
    }

    @Override
    public CleanedContent cleanForDuplicationCalculations(SourceFile sourceFile) {
        String content = getCleaner().cleanKeepEmptyLines(sourceFile.getContent());
//        String content = getCleaner().cleanKeepEmptyLines(getLinesWithoutComments(sourceFile));

        content = SourceCodeCleanerUtils.trimLines(content);

        return SourceCodeCleanerUtils.cleanEmptyLinesWithLineIndexes(content);
    }

    @Override
    public List<UnitInfo> extractUnits(SourceFile sourceFile) {
        return new PlSqlHeuristicUnitsExtractor().extractUnits(sourceFile);
    }

    @Override
    public DependenciesAnalysis extractDependencies(List<SourceFile> sourceFiles, ProgressFeedback progressFeedback) {
        return new PlSqlHeuristicDependenciesExtractor().extractDependencies(sourceFiles, progressFeedback);
    }

    @Override
    public List<String> getFeaturesDescription() {
        List<String> features = new ArrayList<>();

        features.add(FEATURE_ALL_STANDARD_ANALYSES);
        features.add(FEATURE_ADVANCED_CODE_CLEANING);
        features.add(FEATURE_UNIT_SIZE_ANALYSIS);
        features.add(FEATURE_CONDITIONAL_COMPLEXITY_ANALYSIS);
        features.add(FEATURE_ADVANCED_DEPENDENCIES_ANALYSIS + " (based on package names)");

        return features;
    }
}
