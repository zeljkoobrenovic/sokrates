/*
 * Copyright (c) 2019 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.sourcecode.Metadata;
import nl.obren.sokrates.sourcecode.SourceCodeFiles;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.SourceFileFilter;
import nl.obren.sokrates.sourcecode.analysis.ContributorsAnalysisConfig;
import nl.obren.sokrates.sourcecode.analysis.FileHistoryAnalysisConfig;
import nl.obren.sokrates.sourcecode.aspects.*;
import nl.obren.sokrates.sourcecode.metrics.MetricRangeControl;
import nl.obren.sokrates.sourcecode.metrics.MetricsWithGoal;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static nl.obren.sokrates.sourcecode.core.CodeConfigurationUtils.*;

public class CodeConfiguration {
    private Metadata metadata = new Metadata();
    private List<String> summary = new ArrayList<>();

    private String srcRoot = "..";
    private List<String> extensions = new ArrayList<>();
    private ArrayList<SourceFileFilter> ignore = new ArrayList<>();

    private NamedSourceCodeAspect main;
    private NamedSourceCodeAspect test;
    private NamedSourceCodeAspect generated;
    private NamedSourceCodeAspect buildAndDeployment;
    private NamedSourceCodeAspect other;

    private List<LogicalDecomposition> logicalDecompositions = new ArrayList<>();
    private List<ConcernsGroup> concernGroups = new ArrayList<>();

    private List<MetricsWithGoal> goalsAndControls = new ArrayList<>();

    private TrendAnalysisConfig trendAnalysis = new TrendAnalysisConfig();
    private FileHistoryAnalysisConfig fileHistoryAnalysis = new FileHistoryAnalysisConfig();

    private AnalysisConfig analysis = new AnalysisConfig();

    public CodeConfiguration() {
        createDefaultScope();
    }

    public static CodeConfiguration getDefaultConfiguration() {
        CodeConfiguration codeConfiguration = new CodeConfiguration();

        codeConfiguration.createDefaultConcerns();

        codeConfiguration.getGoalsAndControls().add(getDefaultMetricsWithGoal());

        return codeConfiguration;
    }

    private static MetricsWithGoal getDefaultMetricsWithGoal() {
        MetricsWithGoal metricsWithGoal = new MetricsWithGoal();
        metricsWithGoal.setGoal("Keep the system simple and easy to change");
        metricsWithGoal.setDescription("Aim at keeping the system size modest (less than 200,000 LOC is good), duplication low (less than 5% is good), files small (no files longer than 1000 LOC is good), and units simple (no units with more than 25 decision points is good).");
        metricsWithGoal.getControls().add(new MetricRangeControl("LINES_OF_CODE_MAIN", "Total number of lines of main code", new Range("0", "200000", "20000")));
        metricsWithGoal.getControls().add(new MetricRangeControl("DUPLICATION_PERCENTAGE", "System duplication", new Range("0", "5", "1")));
        metricsWithGoal.getControls().add(new MetricRangeControl("NUMBER_OF_FILES_FILE_SIZE_1001_PLUS", "The number of very large files", new Range("0", "0", "1")));
        metricsWithGoal.getControls().add(new MetricRangeControl("CONDITIONAL_COMPLEXITY_DISTRIBUTION_51_PLUS_COUNT", "Number of very complex units", new Range("0", "0", "1")));
        return metricsWithGoal;
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
        sourceCodeFiles.createBroadScope(extensions, ignore, analysis.getMaxLineLength());
        updateScopesFiles(sourceCodeFiles);
        logicalDecompositions.forEach(logicalDecomposition -> {
            logicalDecomposition.updateLogicalComponentsFiles(sourceCodeFiles, CodeConfiguration.this, codeConfigurationFile);
        });
        updateConcernFiles(sourceCodeFiles);
    }

    @JsonIgnore
    private List<NamedSourceCodeAspect> getScopeAspects() {
        List<NamedSourceCodeAspect> aspects = new ArrayList<>();

        addAspectIfNotNull(aspects, main);
        addAspectIfNotNull(aspects, test);
        addAspectIfNotNull(aspects, generated);
        addAspectIfNotNull(aspects, buildAndDeployment);
        addAspectIfNotNull(aspects, other);

        return aspects;
    }

    private void addAspectIfNotNull(List<NamedSourceCodeAspect> aspects, NamedSourceCodeAspect aspect) {
        if (aspect != null) {
            aspects.add(aspect);
        }
    }

    @JsonIgnore
    public List<NamedSourceCodeAspect> getScopesWithExtensions() {
        List<NamedSourceCodeAspect> aspects = new ArrayList<>();

        for (NamedSourceCodeAspect aspect : getScopeAspects()) {
            aspects.add(aspect);
            aspects.addAll(SourceCodeAspectUtils.getAspectsPerExtensions(aspect));
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
        other = new NamedSourceCodeAspect("other");
    }

    private void createBuildAndDeployment() {
        buildAndDeployment = new NamedSourceCodeAspect("build and deployment");
    }

    private void createGenerated() {
        generated = new NamedSourceCodeAspect("generated");
    }

    private void createTest() {
        test = new NamedSourceCodeAspect("test");
    }

    private void createMain() {
        main = new NamedSourceCodeAspect("main");
        main.getSourceFileFilters().add(new SourceFileFilter(".*", ""));
    }

    @JsonIgnore
    private void updateScopesFiles(SourceCodeFiles sourceCodeFiles) {
        for (NamedSourceCodeAspect aspect : getScopeAspects()) {
            sourceCodeFiles.getSourceFiles(aspect);
        }

        removeAspectIfNotNull(test);
        removeAspectIfNotNull(generated);
        removeAspectIfNotNull(buildAndDeployment);
        removeAspectIfNotNull(other);
    }

    private void removeAspectIfNotNull(NamedSourceCodeAspect aspect) {
        if (main != null && aspect != null) {
            main.remove(aspect);
        }
    }

    @JsonIgnore
    public void createDefaultConcerns() {
        Concern todos = new Concern("TODOs");
        todos.getSourceFileFilters().add(new SourceFileFilter("", ".*(TODO|FIXME)( |:|\t).*"));

        concernGroups.clear();
        ConcernsGroup general = new ConcernsGroup("general");
        concernGroups.add(general);

        general.getConcerns().add(todos);

        // Concern security = new Concern("Security");
        // security.getSourceFileFilters().add(new SourceFileFilter("", "(?i).*(Security|Authentication|Password).*"));
        // general.getConcerns().add(security);
    }

    @JsonIgnore
    private void populateUnclassifiedForConcern(List<Concern> concerns) {
        Concern unclassified = new Concern(UNCLASSIFIED_FILES);
        Concern filesInMultipleClassifications = new Concern(FILES_IN_MULTIPLE_CLASSIFICATIONS);

        for (SourceFile sourceFile : main.getSourceFiles()) {
            int fileAspectCount = 0;
            for (Concern aspect : concerns) {
                if (aspect.getSourceFiles().contains(sourceFile)) {
                    fileAspectCount++;
                }
            }
            if (fileAspectCount == 0) {
                unclassified.getSourceFiles().add(sourceFile);
                sourceFile.getConcerns().add(unclassified);
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
    private void updateConcernFiles(SourceCodeFiles sourceCodeFiles) {
        concernGroups.forEach(group -> {
            group.getConcerns().forEach(aspect -> {
                sourceCodeFiles.getSourceFiles(aspect, main.getSourceFiles());
                aspect.getSourceFiles().forEach(sourceFile -> {
                    sourceFile.getConcerns().add(aspect);
                });
            });

            MetaRulesProcessor helper = MetaRulesProcessor.getConcernsInstance();
            List<Concern> metaConcerns = helper.extractAspects(main.getSourceFiles(), group.getMetaConcerns());
            group.getConcerns().addAll(metaConcerns);

            populateUnclassifiedForConcern(group.getConcerns());
            List<DerivedConcern> overlaps = getOverlaps(group.getConcerns());

            group.getConcerns().addAll(overlaps);
        });
    }

    private List<DerivedConcern> getOverlaps(List<Concern> concerns) {
        List<DerivedConcern> overlaps = new ArrayList<>();
        Map<String, DerivedConcern> overlapsMap = new HashMap<>();

        getMain().getSourceFiles().forEach(sourceFile -> {
            if (sourceFile.getConcerns().size() > 1) {
                sourceFile.getConcerns().forEach(concern1 -> {
                    sourceFile.getConcerns().forEach(concern2 -> {
                        if (concern1 != concern2 && concerns.contains(concern1) && concerns.contains(concern2)) {
                            Concern overlapConcern = getOverlapSourceCodeAspect(concern1, concern2, overlapsMap, overlaps);
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

    private void replacePercentageInOverlapConcerns(List<Concern> concerns, Map<String, DerivedConcern> overlapsMap) {
        getMain().getSourceFiles().forEach(sourceFile -> {
            if (sourceFile.getConcerns().size() > 1) {
                sourceFile.getConcerns().forEach(concern1 -> {
                    sourceFile.getConcerns().forEach(concern2 -> {
                        if (concern1 != concern2 && concerns.contains(concern1) && concerns.contains(concern2)) {
                            Concern overlapConcern1 = getOverlapSourceCodeAspectIfExist(concern1, concern2, overlapsMap);
                            Concern overlapConcern2 = getOverlapSourceCodeAspectIfExist(concern2, concern1, overlapsMap);
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

    private void replacePercentageInOverlapConcernName(NamedSourceCodeAspect concern1, NamedSourceCodeAspect concern2, Concern overlapConcern) {
        int totalLinesOfCode = overlapConcern.getLinesOfCode();
        if (overlapConcern.getName().contains(PERCENTAGE_1_VARIABLE)) {
            overlapConcern.setName(overlapConcern.getName().replace(PERCENTAGE_1_VARIABLE, getPercentageString(concern1, totalLinesOfCode)));
            overlapConcern.setName(overlapConcern.getName().replace(PERCENTAGE_2_VARIABLE, getPercentageString(concern2, totalLinesOfCode)));
        }
    }

    private String getPercentageString(NamedSourceCodeAspect concern, int totalLinesOfCode) {
        double value = 100.0 * totalLinesOfCode / concern.getLinesOfCode();
        if (value > 0 && value < 1) {
            return "<1%";
        } else {
            return "" + ((int) (value + 0.5)) + "%";
        }
    }

    private DerivedConcern getOverlapSourceCodeAspect(NamedSourceCodeAspect concern1, NamedSourceCodeAspect concern2,
                                                      Map<String, DerivedConcern> overlapsMap, List<DerivedConcern> overlaps) {
        String key1 = getOverlapConcernKey(concern1, concern2);
        String key2 = getOverlapConcernKey(concern2, concern1);
        DerivedConcern aspect;
        if (overlapsMap.containsKey(key1)) {
            aspect = overlapsMap.get(key1);
        } else if (overlapsMap.containsKey(key2)) {
            aspect = overlapsMap.get(key2);
        } else {
            aspect = new DerivedConcern(key1);
            overlapsMap.put(key1, aspect);
            overlaps.add(aspect);
        }

        return aspect;
    }

    private String getOverlapConcernKey(NamedSourceCodeAspect concern1, NamedSourceCodeAspect concern2) {
        return " - " + concern1.getName() + " (" + PERCENTAGE_1_VARIABLE + ") AND " + concern2.getName() + " (" + PERCENTAGE_2_VARIABLE + ")";
    }

    private Concern getOverlapSourceCodeAspectIfExist(NamedSourceCodeAspect concern1, NamedSourceCodeAspect concern2,
                                                      Map<String, ? extends Concern> overlapsMap) {
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
    public NamedSourceCodeAspect getScope(String scope) {
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

    public NamedSourceCodeAspect getMain() {
        return main;
    }

    public void setMain(NamedSourceCodeAspect main) {
        if (main != null) {
            this.main = main;
        }
    }

    public NamedSourceCodeAspect getTest() {
        return test;
    }

    public void setTest(NamedSourceCodeAspect test) {
        this.test = test;
    }

    public NamedSourceCodeAspect getGenerated() {
        return generated;
    }

    public void setGenerated(NamedSourceCodeAspect generated) {
        this.generated = generated;
    }

    public NamedSourceCodeAspect getBuildAndDeployment() {
        return buildAndDeployment;
    }

    public void setBuildAndDeployment(NamedSourceCodeAspect buildAndDeployment) {
        this.buildAndDeployment = buildAndDeployment;
    }

    public NamedSourceCodeAspect getOther() {
        return other;
    }

    public void setOther(NamedSourceCodeAspect other) {
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

    public List<MetricsWithGoal> getGoalsAndControls() {
        return goalsAndControls;
    }

    public void setGoalsAndControls(List<MetricsWithGoal> goalsAndControls) {
        this.goalsAndControls = goalsAndControls;
    }

    @JsonIgnore
    public int countAllConcernsDefinitions() {
        int count = 0;
        for (ConcernsGroup group : getConcernGroups()) {
            count += group.getConcerns().size();
            count += group.getMetaConcerns().size();
        }
        return count;
    }

    public List<ConcernsGroup> getConcernGroups() {
        return concernGroups;
    }

    public void setConcernGroups(List<ConcernsGroup> concernGroups) {
        if (concernGroups != null) {
            this.concernGroups = concernGroups;
        } else {
            this.concernGroups = new ArrayList<>();
        }
        if (this.concernGroups.size() == 0) {
            ConcernsGroup group = new ConcernsGroup("general");
            this.concernGroups.add(group);
        }
    }

    // legacy support
    public void setConcerns(List<ConcernsGroup> concerns) {
        setConcernGroups(concerns);
    }

    public List<String> getSummary() {
        return summary;
    }

    public void setSummary(List<String> summary) {
        if (summary == null) {
            this.summary = new ArrayList<>();
        } else {
            this.summary = summary;
        }
    }

    public FileHistoryAnalysisConfig getFileHistoryAnalysis() {
        return fileHistoryAnalysis;
    }

    public void setFileHistoryAnalysis(FileHistoryAnalysisConfig fileHistoryAnalysis) {
        if (fileHistoryAnalysis != null) {
            this.fileHistoryAnalysis = fileHistoryAnalysis;
        }
    }

    public TrendAnalysisConfig getTrendAnalysis() {
        return trendAnalysis;
    }

    public void setTrendAnalysis(TrendAnalysisConfig trendAnalysis) {
        if (trendAnalysis != null) {
            this.trendAnalysis = trendAnalysis;
        }
    }
}
