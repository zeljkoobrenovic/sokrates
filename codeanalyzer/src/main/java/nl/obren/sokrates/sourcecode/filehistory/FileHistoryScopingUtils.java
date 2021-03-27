/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.filehistory;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.threshold.Thresholds;

import java.util.ArrayList;
import java.util.List;

public class FileHistoryScopingUtils {
    public static List<LogicalDecomposition> getLogicalDecompositionsFileUpdateFrequency(CodeAnalysisResults analysisResults) {
        return createLogicalDecompositionList(getLogicalDecompositionByUpdateFrequency(analysisResults));
    }

    public static List<LogicalDecomposition> getLogicalDecompositionsByAge(CodeAnalysisResults analysisResults) {
        return createLogicalDecompositionList(getLogicalDecompositionByAge(analysisResults));
    }

    public static List<LogicalDecomposition> getLogicalDecompositionsByFreshness(CodeAnalysisResults analysisResults) {
        return createLogicalDecompositionList(getLogicalDecompositionByFreshness(analysisResults));
    }

    private static List<LogicalDecomposition> createLogicalDecompositionList(LogicalDecomposition logicalDecomposition) {
        List<LogicalDecomposition> logicalDecompositions = new ArrayList<>();

        logicalDecomposition.setComponentsFolderDepth(0);
        logicalDecomposition.getDependenciesFinder().setUseBuiltInDependencyFinders(false);

        logicalDecompositions.add(logicalDecomposition);

        return logicalDecompositions;
    }

    private static LogicalDecomposition getLogicalDecompositionByUpdateFrequency(CodeAnalysisResults analysisResults) {
        List<SourceFile> allFiles = analysisResults.getMainAspectAnalysisResults().getAspect().getSourceFiles();
        LogicalDecomposition logicalDecomposition = new LogicalDecomposition("frequency of changes");

        Thresholds thresholds = analysisResults.getCodeConfiguration().getAnalysis().getFileUpdateFrequencyThresholds();

        NamedSourceCodeAspect componentChangesNegligible = new NamedSourceCodeAspect(thresholds.getNegligibleRiskLabel() + " changes");
        NamedSourceCodeAspect componentChangesLow = new NamedSourceCodeAspect(thresholds.getLowRiskLabel() + " changes");
        NamedSourceCodeAspect componentChangesMedium = new NamedSourceCodeAspect(thresholds.getMediumRiskLabel() + " changes");
        NamedSourceCodeAspect componentChangesHigh = new NamedSourceCodeAspect(thresholds.getHighRiskLabel() + " changes");
        NamedSourceCodeAspect componentChangesVeryHigh = new NamedSourceCodeAspect(thresholds.getVeryHighRiskLabel() + " changes");

        logicalDecomposition.getComponents().add(componentChangesNegligible);
        logicalDecomposition.getComponents().add(componentChangesLow);
        logicalDecomposition.getComponents().add(componentChangesMedium);
        logicalDecomposition.getComponents().add(componentChangesHigh);
        logicalDecomposition.getComponents().add(componentChangesVeryHigh);

        allFiles.forEach(sourceFile -> {
            if (sourceFile.getFileModificationHistory() != null) {
                int updateCount = sourceFile.getFileModificationHistory().getDates().size();
                String relativePath = sourceFile.getRelativePath();
                if (updateCount <= thresholds.getLow()) {
                    componentChangesNegligible.getFiles().add(relativePath);
                } else if (updateCount <= thresholds.getMedium()) {
                    componentChangesLow.getFiles().add(relativePath);
                } else if (updateCount <= thresholds.getHigh()) {
                    componentChangesMedium.getFiles().add(relativePath);
                } else if (updateCount <= thresholds.getVeryHigh()) {
                    componentChangesHigh.getFiles().add(relativePath);
                } else {
                    componentChangesVeryHigh.getFiles().add(relativePath);
                }
            }
        });
        return logicalDecomposition;
    }

    private static LogicalDecomposition getLogicalDecompositionByAge(CodeAnalysisResults analysisResults) {
        List<SourceFile> allFiles = analysisResults.getMainAspectAnalysisResults().getAspect().getSourceFiles();
        LogicalDecomposition logicalDecomposition = new LogicalDecomposition("file age");

        Thresholds thresholds = analysisResults.getCodeConfiguration().getAnalysis().getFileAgeThresholds();

        NamedSourceCodeAspect componentChangesNew = new NamedSourceCodeAspect(thresholds.getNegligibleRiskLabel() + " days old");
        NamedSourceCodeAspect componentChangesYoung = new NamedSourceCodeAspect(thresholds.getLowRiskLabel() + " days old");
        NamedSourceCodeAspect componentChangesMature = new NamedSourceCodeAspect(thresholds.getMediumRiskLabel() + " days old");
        NamedSourceCodeAspect componentChangesOld = new NamedSourceCodeAspect(thresholds.getHighRiskLabel() + " year old");
        NamedSourceCodeAspect componentChangesVeryOld = new NamedSourceCodeAspect(thresholds.getVeryHighRiskLabel() + " year old");

        logicalDecomposition.getComponents().add(componentChangesNew);
        logicalDecomposition.getComponents().add(componentChangesYoung);
        logicalDecomposition.getComponents().add(componentChangesMature);
        logicalDecomposition.getComponents().add(componentChangesOld);
        logicalDecomposition.getComponents().add(componentChangesVeryOld);

        allFiles.forEach(sourceFile -> {
            if (sourceFile.getFileModificationHistory() != null) {
                int updateCount = sourceFile.getFileModificationHistory().daysSinceFirstUpdate();
                String relativePath = sourceFile.getRelativePath();
                if (updateCount <= thresholds.getLow()) {
                    componentChangesNew.getFiles().add(relativePath);
                } else if (updateCount <= thresholds.getMedium()) {
                    componentChangesYoung.getFiles().add(relativePath);
                } else if (updateCount <= thresholds.getHigh()) {
                    componentChangesMature.getFiles().add(relativePath);
                } else if (updateCount <= thresholds.getVeryHigh()) {
                    componentChangesOld.getFiles().add(relativePath);
                } else {
                    componentChangesVeryOld.getFiles().add(relativePath);
                }
            }
        });
        return logicalDecomposition;
    }

    private static LogicalDecomposition getLogicalDecompositionByFreshness(CodeAnalysisResults analysisResults) {
        List<SourceFile> allFiles = analysisResults.getMainAspectAnalysisResults().getAspect().getSourceFiles();
        LogicalDecomposition logicalDecomposition = new LogicalDecomposition("file freshness");

        Thresholds thresholds = analysisResults.getCodeConfiguration().getAnalysis().getFileAgeThresholds();

        NamedSourceCodeAspect componentChangesNew = new NamedSourceCodeAspect(thresholds.getNegligibleRiskLabel() + " days old");
        NamedSourceCodeAspect componentChangesYoung = new NamedSourceCodeAspect(thresholds.getLowRiskLabel() + " days old");
        NamedSourceCodeAspect componentChangesMature = new NamedSourceCodeAspect(thresholds.getMediumRiskLabel() + " days old");
        NamedSourceCodeAspect componentChangesOld = new NamedSourceCodeAspect(thresholds.getHighRiskLabel() + " year old");
        NamedSourceCodeAspect componentChangesVeryOld = new NamedSourceCodeAspect(thresholds.getVeryHighRiskLabel() + " year old");

        logicalDecomposition.getComponents().add(componentChangesNew);
        logicalDecomposition.getComponents().add(componentChangesYoung);
        logicalDecomposition.getComponents().add(componentChangesMature);
        logicalDecomposition.getComponents().add(componentChangesOld);
        logicalDecomposition.getComponents().add(componentChangesVeryOld);

        allFiles.forEach(sourceFile -> {
            if (sourceFile.getFileModificationHistory() != null) {
                int updateCount = sourceFile.getFileModificationHistory().daysSinceLatestUpdate();
                String relativePath = sourceFile.getRelativePath();
                if (updateCount <= thresholds.getLow()) {
                    componentChangesNew.getFiles().add(relativePath);
                } else if (updateCount <= thresholds.getMedium()) {
                    componentChangesYoung.getFiles().add(relativePath);
                } else if (updateCount <= thresholds.getHigh()) {
                    componentChangesMature.getFiles().add(relativePath);
                } else if (updateCount <= thresholds.getVeryHigh()) {
                    componentChangesOld.getFiles().add(relativePath);
                } else {
                    componentChangesVeryOld.getFiles().add(relativePath);
                }
            }
        });
        return logicalDecomposition;
    }

}
