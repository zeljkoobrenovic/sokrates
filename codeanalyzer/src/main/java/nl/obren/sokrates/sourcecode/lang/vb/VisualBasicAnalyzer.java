package nl.obren.sokrates.sourcecode.lang.vb;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.dependencies.DependenciesAnalysis;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzer;
import nl.obren.sokrates.sourcecode.units.UnitInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VisualBasicAnalyzer extends LanguageAnalyzer {
    public VisualBasicAnalyzer() {
    }

    @Override
    public CleanedContent cleanForLinesOfCodeCalculations(SourceFile sourceFile) {
        return SourceCodeCleanerUtils.cleanSingeLineCommentsAndEmptyLines(sourceFile.getContent(), Arrays.asList("'", "REM "));
    }

    @Override
    public CleanedContent cleanForDuplicationCalculations(SourceFile sourceFile) {
        String content = sourceFile.getContent();

        content = SourceCodeCleanerUtils.trimLines(content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("[ ]*End [A-Z][a-z]+[ ]*", content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("[ ]*Imports .*", content);

        return SourceCodeCleanerUtils.cleanSingeLineCommentsAndEmptyLines(content, Arrays.asList("'", "REM "));
    }

    @Override
    public List<UnitInfo> extractUnits(SourceFile sourceFile) {
        List<UnitInfo> units = new ArrayList<>();



        return units;
    }


    private int getMcCabeIndex(String body) {
        return 1;
    }


    @Override
    public DependenciesAnalysis extractDependencies(List<SourceFile> sourceFiles, ProgressFeedback progressFeedback) {
        return new DependenciesAnalysis();
    }
}
