package nl.obren.sokrates.sourcecode.lang.go;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.dependencies.DependenciesAnalysis;
import nl.obren.sokrates.sourcecode.lang.LanguageAnalyzer;
import nl.obren.sokrates.sourcecode.units.CStyleHeuristicUnitParser;
import nl.obren.sokrates.sourcecode.units.UnitInfo;

import java.util.List;

public class GoLangAnalyzer extends LanguageAnalyzer {
    public GoLangAnalyzer() {
    }

    @Override
    public CleanedContent cleanForLinesOfCodeCalculations(SourceFile sourceFile) {
        return SourceCodeCleanerUtils.cleanCommentsAndEmptyLines(sourceFile.getContent(), "//", "/*", "*/");
    }

    @Override
    public CleanedContent cleanForDuplicationCalculations(SourceFile sourceFile) {
        String content = SourceCodeCleanerUtils.emptyComments(sourceFile.getContent(), "//", "/*", "*/").getCleanedContent();

        content = SourceCodeCleanerUtils.trimLines(content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("import .*", content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("package.*", content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("[{]", content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("[}]", content);

        return SourceCodeCleanerUtils.cleanEmptyLinesWithLineIndexes(content);
    }

    @Override
    public List<UnitInfo> extractUnits(SourceFile sourceFile) {
        CStyleHeuristicUnitParser heuristicUnitParser = new CStyleHeuristicUnitParser() {
            @Override
            public boolean isUnitSignature(String line) {
                return RegexUtils.matchesEntirely(".*func .*[(].*", line);
            }

            @Override
            public void setNameAndParameters(String line, UnitInfo unit, String cleandedBody) {
                if (isStaticUnit(line)) {
                    unit.setShortName("static");
                    unit.setNumberOfParameters(0);
                } else {
                    super.setNameAndParameters(line, unit, cleandedBody);
                }
            }

        };
        return heuristicUnitParser.extractUnits(sourceFile);
    }

    private boolean isStaticUnit(String line) {
        line = line.replace("\t", "");
        line = line.replace(" ", "");
        line = line.trim();
        return line.equalsIgnoreCase("static{");
    }


    @Override
    public DependenciesAnalysis extractDependencies(List<SourceFile> sourceFiles, ProgressFeedback progressFeedback) {
        return new GoLangHeuristicDependenciesExtractor().extractDependencies(sourceFiles, progressFeedback);
    }
}
