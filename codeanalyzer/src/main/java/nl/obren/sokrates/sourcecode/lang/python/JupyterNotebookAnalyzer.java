package nl.obren.sokrates.sourcecode.lang.python;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.cleaners.CommentsAndEmptyLinesCleaner;
import nl.obren.sokrates.sourcecode.cleaners.SourceCodeCleanerUtils;
import nl.obren.sokrates.sourcecode.dependencies.DependenciesAnalysis;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

public class JupyterNotebookAnalyzer extends PythonAnalyzer {

    private static final Log LOG = LogFactory.getLog(JupyterNotebookAnalyzer.class);

    final JupyterNotebookCleaner preprocessor = new JupyterNotebookCleaner();

    public JupyterNotebookAnalyzer() {
    }

    @Override
    public CleanedContent cleanForLinesOfCodeCalculations(SourceFile sourceFile) {
        CleanedContent cc = getCleaner().clean(preprocessor.extract(sourceFile.getContent()));
        LOG.debug(String.format("%d lines in source %s",
                cc.getLines().size(),
                sourceFile.getFile().getAbsolutePath()));
        return cc;
    }

    private CommentsAndEmptyLinesCleaner getCleaner() {
        CommentsAndEmptyLinesCleaner cleaner = new CommentsAndEmptyLinesCleaner();

        cleaner.addCommentBlockHelper("\"\"\"", "\"\"\"");
        cleaner.addCommentBlockHelper("#", "\n");
        cleaner.addStringBlockHelper("\"", "\\");
        cleaner.addStringBlockHelper("'", "\\");
        cleaner.addStringBlockHelper("'''", "'''");

        return cleaner;
    }

    @Override
    public CleanedContent cleanForDuplicationCalculations(final SourceFile sourceFile) {
        String content = getCleaner().cleanKeepEmptyLines(preprocessor.extract(sourceFile.getContent()));

        content = SourceCodeCleanerUtils.trimLines(content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("from .*import.*", content);
        content = SourceCodeCleanerUtils.emptyLinesMatchingPattern("import .*", content);

        return SourceCodeCleanerUtils.cleanEmptyLinesWithLineIndexes(content);
    }

    @Override
    public List<UnitInfo> extractUnits(SourceFile sourceFile) {
        SourceFile unitCopy = new SourceFile(sourceFile.getFile());
        unitCopy.setContent(preprocessor.extract(sourceFile.getContent()));
        return new PythonHeuristicUnitsExtractor().extractUnits(unitCopy);
    }

    @Override
    public DependenciesAnalysis extractDependencies(List<SourceFile> sourceFiles, ProgressFeedback progressFeedback) {
        return new DependenciesAnalysis();
    }

    @Override
    public List<String> getFeaturesDescription() {
        final List<String> features = new ArrayList<>();

        features.add(FEATURE_ALL_STANDARD_ANALYSES);
        features.add(FEATURE_ADVANCED_CODE_CLEANING);
        features.add(FEATURE_UNIT_SIZE_ANALYSIS);
        features.add(FEATURE_CONDITIONAL_COMPLEXITY_ANALYSIS);
        features.add(FEATURE_BASIC_DEPENDENCIES_ANALYSIS);

        return features;
    }

}
