package nl.obren.sokrates.sourcecode.aspects;

import javafx.util.Callback;
import nl.obren.sokrates.common.utils.RegexUtils;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.SourceFileFilter;
import nl.obren.sokrates.sourcecode.operations.ComplexOperation;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

public class MetaRulesProcessor<T extends NamedSourceCodeAspect> {
    private static final Log LOG = LogFactory.getLog(MetaRulesProcessor.class);
    private List<T> concerns = new ArrayList<>();
    private List<SourceFile> alreadyAddedFiles = new ArrayList<>();
    private Map<String, T> map = new HashMap<>();
    private Callback<String, T> sourceCodeAspectFactory;
    private Callback<Pair<SourceFile, NamedSourceCodeAspect>, Boolean> sourceFileUpdate;
    private boolean uniqueClassification;

    private MetaRulesProcessor(boolean uniqueClassification,
                               Callback<String, T> sourceCodeAspectFactory,
                               Callback<Pair<SourceFile, NamedSourceCodeAspect>, Boolean> sourceFileUpdate) {
        this.uniqueClassification = uniqueClassification;
        this.sourceCodeAspectFactory = sourceCodeAspectFactory;
        this.sourceFileUpdate = sourceFileUpdate;
    }

    public static MetaRulesProcessor getCrossCurringConcernsInstance() {
        return new MetaRulesProcessor<CrossCuttingConcern>(false,
                name -> new CrossCuttingConcern(name),
                updateInfo -> updateInfo.getLeft().getCrossCuttingConcerns().add(updateInfo.getRight()));
    }

    public static MetaRulesProcessor getLogicalDecompositionInstance() {
        return new MetaRulesProcessor<NamedSourceCodeAspect>(true,
                name -> new NamedSourceCodeAspect(name),
                updateInfo -> updateInfo.getLeft().getLogicalComponents().add(updateInfo.getRight()));
    }

    public boolean isUniqueClassification() {
        return uniqueClassification;
    }

    public void setUniqueClassification(boolean uniqueClassification) {
        this.uniqueClassification = uniqueClassification;
    }

    public List<T> extractAspects(NamedSourceCodeAspect aspect, List<MetaRule> metaRules) {
        concerns = new ArrayList<>();
        map = new HashMap<>();
        alreadyAddedFiles = new ArrayList<>();

        aspect.getSourceFiles().forEach(sourceFile -> {
            processSourceFile(metaRules, sourceFile);
        });

        return concerns;
    }

    private void processSourceFile(List<MetaRule> metaRules, SourceFile sourceFile) {
        metaRules.forEach(metaRule -> {
            SourceFileFilter sourceFileFilter = new SourceFileFilter(metaRule.getPathPattern(), "");
            if (sourceFileFilter.pathMatches(sourceFile.getRelativePath())) {
                processSourceFileContent(sourceFile, metaRule);
            }
        });
    }

    private void processSourceFileContent(SourceFile sourceFile, MetaRule metaRule) {
        if (StringUtils.isBlank(metaRule.getContentPattern())) {
            return;
        }
        getLines(sourceFile, metaRule).forEach(line -> {
            if (RegexUtils.matchesEntirely(metaRule.getContentPattern(), line)) {
                if (shouldProcessFile(sourceFile)) {
                    processMatchingString(sourceFile, metaRule, line);
                }
            }
        });
    }

    private List<String> getLines(SourceFile sourceFile, MetaRule metaRule) {
        if (metaRule.getUse().equalsIgnoreCase("path")) {
            return Arrays.asList(sourceFile.getRelativePath());
        } else {
            return metaRule.isIgnoreComments() ? sourceFile.getCleanedLines() : sourceFile.getLines();
        }
    }

    private boolean shouldProcessFile(SourceFile sourceFile) {
        return !uniqueClassification || !alreadyAddedFiles.contains(sourceFile);
    }

    private void processMatchingString(SourceFile sourceFile, MetaRule metaRule, String matchingString) {
        updateAlreadyPrcessedFiles(sourceFile);
        String name = new ComplexOperation(metaRule.getNameOperations()).exec(matchingString);
        name = StringUtils.defaultIfBlank(name, "ROOT");
        if (map.containsKey(name)) {
            T sourceCodeAspect = map.get(name);
            List<SourceFile> sourceFiles = sourceCodeAspect.getSourceFiles();
            if (!sourceFiles.contains(sourceFile)) {
                sourceFiles.add(sourceFile);
                sourceFileUpdate.call(Pair.of(sourceFile, sourceCodeAspect));
            }
        } else {
            T sourceCodeAspect = sourceCodeAspectFactory.call(name);
            sourceCodeAspect.getSourceFiles().add(sourceFile);

            sourceFileUpdate.call(Pair.of(sourceFile, sourceCodeAspect));

            map.put(name, sourceCodeAspect);
            concerns.add(sourceCodeAspect);
        }
    }

    private void updateAlreadyPrcessedFiles(SourceFile sourceFile) {
        if (uniqueClassification && !alreadyAddedFiles.contains(sourceFile)) {
            alreadyAddedFiles.add(sourceFile);
        }
    }
}
