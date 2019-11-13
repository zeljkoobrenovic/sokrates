package nl.obren.sokrates.sourcecode.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.SourceCodeFiles;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.SourceFileFilter;
import nl.obren.sokrates.sourcecode.aspects.*;
import nl.obren.sokrates.sourcecode.metrics.MetricRangeControl;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static nl.obren.sokrates.sourcecode.core.CodeConfigurationUtils.*;

public class CodeConfiguration {
    private Metadata metadata = new Metadata();
    private String srcRoot = "..";

    private List<String> extensions = new ArrayList<>();
    private AnalysisConfig analysis = new AnalysisConfig();
    private ArrayList<SourceFileFilter> ignore = new ArrayList<>();

    private SourceCodeAspect main;
    private SourceCodeAspect test;
    private SourceCodeAspect generated;
    private SourceCodeAspect buildAndDeployment;
    private SourceCodeAspect other;

    private List<LogicalDecomposition> logicalDecompositions = new ArrayList<>();
    private List<CrossCuttingConcernsGroup> crossCuttingConcerns = new ArrayList<>();

    private List<MetricRangeControl> controls = new ArrayList<>();

    private List<String> summaryFindings = new ArrayList<>();

    public CodeConfiguration() {
        createDefaultScope();
    }

    public static CodeConfiguration getDefaultConfiguration() {
        CodeConfiguration codeConfiguration = new CodeConfiguration();

        codeConfiguration.createDefaultCrossCuttingConcerns();

        return codeConfiguration;
    }

    @JsonIgnore
    public static String getAbsoluteSrcRoot(String srcRoot, File configurationFile) {
        if (configurationFile != null) {
            File fileRelative;
            if (srcRoot.startsWith("..")) {
                fileRelative = new File(configurationFile.getParentFile().getParentFile(), srcRoot.substring(2));
            } else {
                fileRelative = new File(configurationFile.getParent(), srcRoot);
            }
            if (fileRelative.exists()) {
                return fileRelative.getPath();
            }
        }
        return srcRoot;
    }

    @JsonIgnore
    public void load(SourceCodeFiles sourceCodeFiles, File codeConfigurationFile) {
        sourceCodeFiles.createBroadScope(extensions, ignore);
        updateScopesFiles(sourceCodeFiles);
        logicalDecompositions.forEach(logicalDecomposition -> {
            logicalDecomposition.updateLogicalComponentsFiles(sourceCodeFiles, CodeConfiguration.this, codeConfigurationFile);
        });
        updateCrossCuttingConcernFiles(sourceCodeFiles);
    }

    @JsonIgnore
    private List<SourceCodeAspect> getScopeAspects() {
        List<SourceCodeAspect> aspects = new ArrayList<>();

        addAspectIfNotNull(aspects, main);
        addAspectIfNotNull(aspects, test);
        addAspectIfNotNull(aspects, generated);
        addAspectIfNotNull(aspects, buildAndDeployment);
        addAspectIfNotNull(aspects, other);

        return aspects;
    }

    private void addAspectIfNotNull(List<SourceCodeAspect> aspects, SourceCodeAspect aspect) {
        if (aspect != null) {
            aspects.add(aspect);
        }
    }

    @JsonIgnore
    public List<SourceCodeAspect> getScopesWithExtensions() {
        List<SourceCodeAspect> aspects = new ArrayList<>();

        for (SourceCodeAspect aspect : getScopeAspects()) {
            aspects.add(aspect);
            aspects.addAll(aspect.getAspectsPerExtensions());
        }

        return aspects;
    }

    @JsonIgnore
    public void createDefaultScope() {
        logicalDecompositions.add(new LogicalDecomposition("primary"));
        createMain();
        createTest();
        createGenerated();
        createBuildAndDeployment();
        createOther();
    }

    private void createOther() {
        other = new SourceCodeAspect("other");
    }

    private void createBuildAndDeployment() {
        buildAndDeployment = new SourceCodeAspect("build and deployment");
    }

    private void createGenerated() {
        generated = new SourceCodeAspect("generated");
    }

    private void createTest() {
        test = new SourceCodeAspect("test");
    }

    private void createMain() {
        main = new SourceCodeAspect("main");
        main.getSourceFileFilters().add(new SourceFileFilter(".*", ""));
    }

    @JsonIgnore
    private void updateScopesFiles(SourceCodeFiles sourceCodeFiles) {
        for (SourceCodeAspect aspect : getScopeAspects()) {
            sourceCodeFiles.getSourceFiles(aspect);
        }

        removeAspectIfNotNull(test);
        removeAspectIfNotNull(generated);
        removeAspectIfNotNull(buildAndDeployment);
        removeAspectIfNotNull(other);
    }

    private void removeAspectIfNotNull(SourceCodeAspect aspect) {
        if (main != null && aspect != null) {
            main.remove(aspect);
        }
    }

    @JsonIgnore
    public void createDefaultCrossCuttingConcerns() {
        crossCuttingConcerns.clear();
    }

    @JsonIgnore
    private void populateUnclassifiedForCrossCuttingConcern(List<CrossCuttingConcern> concerns) {
        CrossCuttingConcern unclassified = new CrossCuttingConcern(UNCLASSIFIED_FILES);
        CrossCuttingConcern filesInMultipleClassifications = new CrossCuttingConcern(FILES_IN_MULTIPLE_CLASSIFICATIONS);

        for (SourceFile sourceFile : main.getSourceFiles()) {
            int fileAspectCount = 0;
            for (CrossCuttingConcern aspect : concerns) {
                if (aspect.getSourceFiles().contains(sourceFile)) {
                    fileAspectCount++;
                }
            }
            if (fileAspectCount == 0) {
                unclassified.getSourceFiles().add(sourceFile);
                sourceFile.getCrossCuttingConcerns().add(unclassified);
            } else if (fileAspectCount > 1) {
                filesInMultipleClassifications.getSourceFiles().add(sourceFile);
            }
        }

        if (unclassified.getSourceFiles().size() > 0) {
            concerns.add(unclassified);
        }

        if (filesInMultipleClassifications.getSourceFiles().size() > 0) {
            concerns.add(filesInMultipleClassifications);
        }
    }

    @JsonIgnore
    private void updateCrossCuttingConcernFiles(SourceCodeFiles sourceCodeFiles) {
        crossCuttingConcerns.forEach(group -> {
            group.getConcerns().forEach(aspect -> {
                sourceCodeFiles.getSourceFiles(aspect, main.getSourceFiles());
                aspect.getSourceFiles().forEach(sourceFile -> {
                    sourceFile.getCrossCuttingConcerns().add(aspect);
                });
            });
            populateUnclassifiedForCrossCuttingConcern(group.getConcerns());
            List<DerivedCrossCuttingConcern> overlaps = getOverlaps(group.getConcerns());
            group.getConcerns().addAll(overlaps);
        });
    }

    private List<DerivedCrossCuttingConcern> getOverlaps(List<CrossCuttingConcern> concerns) {
        List<DerivedCrossCuttingConcern> overlaps = new ArrayList<>();
        Map<String, DerivedCrossCuttingConcern> overlapsMap = new HashMap<>();

        getMain().getSourceFiles().forEach(sourceFile -> {
            if (sourceFile.getCrossCuttingConcerns().size() > 1) {
                sourceFile.getCrossCuttingConcerns().forEach(concern1 -> {
                    sourceFile.getCrossCuttingConcerns().forEach(concern2 -> {
                        if (concern1 != concern2 && concerns.contains(concern1) && concerns.contains(concern2)) {
                            CrossCuttingConcern overlapConcern = getOverlapSourceCodeAspect(concern1, concern2, overlapsMap, overlaps);
                            if (!overlapConcern.getSourceFiles().contains(sourceFile)) {
                                overlapConcern.getSourceFiles().add(sourceFile);
                            }
                        }
                    });
                });
            }
        });

        replacePercentageInOverlapConcerns(concerns, overlapsMap);

        return overlaps;
    }

    private void replacePercentageInOverlapConcerns(List<CrossCuttingConcern> concerns, Map<String, DerivedCrossCuttingConcern> overlapsMap) {
        getMain().getSourceFiles().forEach(sourceFile -> {
            if (sourceFile.getCrossCuttingConcerns().size() > 1) {
                sourceFile.getCrossCuttingConcerns().forEach(concern1 -> {
                    sourceFile.getCrossCuttingConcerns().forEach(concern2 -> {
                        if (concern1 != concern2 && concerns.contains(concern1) && concerns.contains(concern2)) {
                            CrossCuttingConcern overlapConcern1 = getOverlapSourceCodeAspectIfExist(concern1, concern2, overlapsMap);
                            CrossCuttingConcern overlapConcern2 = getOverlapSourceCodeAspectIfExist(concern2, concern1, overlapsMap);
                            if (overlapConcern1 != null) {
                                replacePercentageInOverlapConcernName(concern1, concern2, overlapConcern1);
                            } else {
                                if (overlapConcern2 != null) {
                                    replacePercentageInOverlapConcernName(concern2, concern1, overlapConcern2);
                                }

                            }
                        }
                    });
                });
            }
        });
    }

    private void replacePercentageInOverlapConcernName(SourceCodeAspect concern1, SourceCodeAspect concern2, CrossCuttingConcern overlapConcern) {
        int totalLinesOfCode = overlapConcern.getLinesOfCode();
        if (overlapConcern.getName().contains(PERCENTAGE_1_VARIABLE)) {
            overlapConcern.setName(overlapConcern.getName().replace(PERCENTAGE_1_VARIABLE, getPercentageString(concern1, totalLinesOfCode)));
            overlapConcern.setName(overlapConcern.getName().replace(PERCENTAGE_2_VARIABLE, getPercentageString(concern2, totalLinesOfCode)));
        }
    }

    private String getPercentageString(SourceCodeAspect concern, int totalLinesOfCode) {
        double value = 100.0 * totalLinesOfCode / concern.getLinesOfCode();
        if (value > 0 && value < 1) {
            return "<1%";
        } else {
            return "" + ((int) (value + 0.5)) + "%";
        }
    }

    private DerivedCrossCuttingConcern getOverlapSourceCodeAspect(SourceCodeAspect concern1, SourceCodeAspect concern2,
                                                                  Map<String, DerivedCrossCuttingConcern> overlapsMap, List<DerivedCrossCuttingConcern> overlaps) {
        String key1 = getOverlapConcernKey(concern1, concern2);
        String key2 = getOverlapConcernKey(concern2, concern1);
        DerivedCrossCuttingConcern aspect;
        if (overlapsMap.containsKey(key1)) {
            aspect = overlapsMap.get(key1);
        } else if (overlapsMap.containsKey(key2)) {
            aspect = overlapsMap.get(key2);
        } else {
            aspect = new DerivedCrossCuttingConcern(key1);
            overlapsMap.put(key1, aspect);
            overlaps.add(aspect);
        }

        return aspect;
    }

    private String getOverlapConcernKey(SourceCodeAspect concern1, SourceCodeAspect concern2) {
        return " - " + concern1.getName() + " (" + PERCENTAGE_1_VARIABLE + ") AND " + concern2.getName() + " (" + PERCENTAGE_2_VARIABLE + ")";
    }

    private CrossCuttingConcern getOverlapSourceCodeAspectIfExist(SourceCodeAspect concern1, SourceCodeAspect concern2,
                                                                  Map<String, ? extends CrossCuttingConcern> overlapsMap) {
        String key = getOverlapConcernKey(concern1, concern2);
        return overlapsMap.get(key);
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public String getSrcRoot() {
        return srcRoot;
    }

    public void setSrcRoot(String srcRoot) {
        this.srcRoot = srcRoot;
    }

    public ArrayList<SourceFileFilter> getIgnore() {
        return ignore;
    }

    public void setIgnore(ArrayList<SourceFileFilter> ignore) {
        this.ignore = ignore;
    }

    public List<String> getExtensions() {
        return extensions;
    }

    public void setExtensions(List<String> extensions) {
        this.extensions = extensions;
    }

    public AnalysisConfig getAnalysis() {
        return analysis;
    }

    public void setAnalysis(AnalysisConfig analysis) {
        this.analysis = analysis;
    }

    @JsonIgnore
    public SourceCodeAspect getScope(String scope) {
        switch (scope.toLowerCase()) {
            case "main":
                return main;
            case "test":
                return test;
            case "buildAndDeployment":
                return buildAndDeployment;
            case "other":
                return other;
        }
        return main;
    }

    public SourceCodeAspect getMain() {
        return main;
    }

    public void setMain(SourceCodeAspect main) {
        if (main != null) {
            this.main = main;
        }
    }

    public SourceCodeAspect getTest() {
        return test;
    }

    public void setTest(SourceCodeAspect test) {
        this.test = test;
    }

    public SourceCodeAspect getGenerated() {
        return generated;
    }

    public void setGenerated(SourceCodeAspect generated) {
        this.generated = generated;
    }

    public SourceCodeAspect getBuildAndDeployment() {
        return buildAndDeployment;
    }

    public void setBuildAndDeployment(SourceCodeAspect buildAndDeployment) {
        this.buildAndDeployment = buildAndDeployment;
    }

    public SourceCodeAspect getOther() {
        return other;
    }

    public void setOther(SourceCodeAspect other) {
        this.other = other;
    }

    public List<LogicalDecomposition> getLogicalDecompositions() {
        return logicalDecompositions;
    }

    public void setLogicalDecompositions(List<LogicalDecomposition> logicalDecompositions) {
        if (logicalDecompositions != null) {
            this.logicalDecompositions = logicalDecompositions;
        } else {
            this.logicalDecompositions = new ArrayList<>();
        }
        if (this.logicalDecompositions.size() == 0) {
            LogicalDecomposition logicalDecomposition = new LogicalDecomposition("primary");
            this.logicalDecompositions.add(logicalDecomposition);
        }
    }

    public List<MetricRangeControl> getControls() {
        return controls;
    }

    public void setControls(List<MetricRangeControl> controls) {
        this.controls = controls;
    }

    public List<CrossCuttingConcernsGroup> getCrossCuttingConcerns() {
        return crossCuttingConcerns;
    }

    public void setCrossCuttingConcerns(List<CrossCuttingConcernsGroup> crossCuttingConcerns) {
        if (crossCuttingConcerns != null) {
            this.crossCuttingConcerns = crossCuttingConcerns;
        } else {
            this.crossCuttingConcerns = new ArrayList<>();
        }
        if (this.crossCuttingConcerns.size() == 0) {
            CrossCuttingConcernsGroup group = new CrossCuttingConcernsGroup("general");
            this.crossCuttingConcerns.add(group);
        }
    }

    public List<String> getSummaryFindings() {
        return summaryFindings;
    }

    public void setSummaryFindings(List<String> summaryFindings) {
        if (summaryFindings == null) {
            this.summaryFindings = new ArrayList<>();
        } else {
            this.summaryFindings = summaryFindings;
        }
    }
}
