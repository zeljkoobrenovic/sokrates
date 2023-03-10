package nl.obren.sokrates.sourcecode.lang.cobol;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.cleaners.CommentsAndEmptyLinesCleaner;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.lang.DefaultLanguageAnalyzer;
import nl.obren.sokrates.sourcecode.units.UnitInfo;

import java.util.ArrayList;
import java.util.List;

public class CobolLangAnalyzer extends DefaultLanguageAnalyzer {

    String sourceFileContent;

    private CommentsAndEmptyLinesCleaner getCleaner() {
        CommentsAndEmptyLinesCleaner cleaner = new CommentsAndEmptyLinesCleaner();

        cleaner.addCommentBlockHelper("*", "\n");

        return cleaner;
    }

    @Override
    public CleanedContent cleanForLinesOfCodeCalculations(SourceFile sourceFile) {
        if (sourceFileContent == null) {
            sourceFileContent = CobolReformatter.getReformattedCobol(sourceFile.getContent());
        }

        String result = getCleaner().cleanKeepEmptyLines(sourceFileContent);

        result = SourceCodeCleanerUtils.trimLines(result);

        return SourceCodeCleanerUtils.cleanEmptyLinesWithLineIndexes(result);
    }

    @Override
    public CleanedContent cleanForDuplicationCalculations(SourceFile sourceFile) {
        if (sourceFileContent == null) {
            sourceFileContent = CobolReformatter.getReformattedCobol(sourceFile.getContent());
        }

        String result = getCleaner().cleanKeepEmptyLines(sourceFileContent);

        result = SourceCodeCleanerUtils.trimLines(result);

        return SourceCodeCleanerUtils.cleanEmptyLinesWithLineIndexes(result);
    }

    @Override
    public List<UnitInfo> extractUnits(SourceFile sourceFile) {
        CleanedContent cleanedContent = cleanForLinesOfCodeCalculations(sourceFile);

        List<String> normalLines = sourceFile.getLines();
        List<String> lines = SourceCodeCleanerUtils.splitInLines(cleanedContent.getCleanedContent());

        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            String line = lines.get(lineIndex).trim();

            if (isUnitSignature(line)) {
            }
        }

        return new ArrayList<>();
    }

    protected boolean isUnitSignature(String line) {
        return false;
    }

    @Override
    public List<String> getFeaturesDescription() {
        List<String> features = new ArrayList<>();

        features.add(FEATURE_ALL_STANDARD_ANALYSES);
        features.add(FEATURE_BASIC_CODE_CLEANING);
        features.add(FEATURE_NO_UNIT_SIZE_ANALYSIS);
        features.add(FEATURE_NO_CONDITIONAL_COMPLEXITY_ANALYSIS);
        features.add(FEATURE_NO_DEPENDENCIES_ANALYSIS);

        return features;
    }
}
