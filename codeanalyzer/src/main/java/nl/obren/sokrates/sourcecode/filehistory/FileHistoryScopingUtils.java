/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.filehistory;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.LogicalDecomposition;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;

import java.util.ArrayList;
import java.util.List;

public class FileHistoryScopingUtils {
    public static List<LogicalDecomposition> getLogicalDecompositionsFileUpdateFrequency(List<SourceFile> allFiles) {
        return createLogicalDecompositionList(getLogicalDecompositionByUpdateFrequency(allFiles));
    }

    public static List<LogicalDecomposition> getLogicalDecompositionsByAge(List<SourceFile> allFiles) {
        return createLogicalDecompositionList(getLogicalDecompositionByAge(allFiles));
    }

    public static List<LogicalDecomposition> getLogicalDecompositionsByFreshness(List<SourceFile> allFiles) {
        return createLogicalDecompositionList(getLogicalDecompositionByFresness(allFiles));
    }

    private static List<LogicalDecomposition> createLogicalDecompositionList(LogicalDecomposition logicalDecomposition) {
        List<LogicalDecomposition> logicalDecompositions = new ArrayList<>();

        logicalDecomposition.setComponentsFolderDepth(0);
        logicalDecomposition.getDependenciesFinder().setUseBuiltInDependencyFinders(false);

        logicalDecompositions.add(logicalDecomposition);

        return logicalDecompositions;
    }

    private static LogicalDecomposition getLogicalDecompositionByUpdateFrequency(List<SourceFile> allFiles) {
        LogicalDecomposition logicalDecomposition = new LogicalDecomposition("frequency of changes");

        NamedSourceCodeAspect componentChanges1To5 = new NamedSourceCodeAspect("1-5 changes");
        NamedSourceCodeAspect componentChanges6To20 = new NamedSourceCodeAspect("6-20 changes");
        NamedSourceCodeAspect componentChanges21To50 = new NamedSourceCodeAspect("21-50 changes");
        NamedSourceCodeAspect componentChanges51To100 = new NamedSourceCodeAspect("51-100 changes");
        NamedSourceCodeAspect componentChanges101Plus = new NamedSourceCodeAspect("101+ changes");

        logicalDecomposition.getComponents().add(componentChanges1To5);
        logicalDecomposition.getComponents().add(componentChanges6To20);
        logicalDecomposition.getComponents().add(componentChanges21To50);
        logicalDecomposition.getComponents().add(componentChanges51To100);
        logicalDecomposition.getComponents().add(componentChanges101Plus);

        allFiles.forEach(sourceFile -> {
            if (sourceFile.getFileModificationHistory() != null) {
                int updateCount = sourceFile.getFileModificationHistory().getDates().size();
                String relativePath = sourceFile.getRelativePath();
                if (updateCount <= 5) {
                    componentChanges1To5.getFiles().add(relativePath);
                } else if (updateCount <= 20) {
                    componentChanges6To20.getFiles().add(relativePath);
                } else if (updateCount <= 50) {
                    componentChanges21To50.getFiles().add(relativePath);
                } else if (updateCount <= 100) {
                    componentChanges51To100.getFiles().add(relativePath);
                } else {
                    componentChanges101Plus.getFiles().add(relativePath);
                }
            }
        });
        return logicalDecomposition;
    }

    private static LogicalDecomposition getLogicalDecompositionByAge(List<SourceFile> allFiles) {
        LogicalDecomposition logicalDecomposition = new LogicalDecomposition("file age");

        NamedSourceCodeAspect componentChangesNew = new NamedSourceCodeAspect("1-30 days old");
        NamedSourceCodeAspect componentChangesYoung = new NamedSourceCodeAspect("31-90 days old");
        NamedSourceCodeAspect componentChangesMature = new NamedSourceCodeAspect("91-180 days old");
        NamedSourceCodeAspect componentChangesOld = new NamedSourceCodeAspect("180 days to 1 year old");
        NamedSourceCodeAspect componentChangesVeryOld = new NamedSourceCodeAspect("> 1 year old");

        logicalDecomposition.getComponents().add(componentChangesNew);
        logicalDecomposition.getComponents().add(componentChangesYoung);
        logicalDecomposition.getComponents().add(componentChangesMature);
        logicalDecomposition.getComponents().add(componentChangesOld);
        logicalDecomposition.getComponents().add(componentChangesVeryOld);

        allFiles.forEach(sourceFile -> {
            if (sourceFile.getFileModificationHistory() != null) {
                int updateCount = sourceFile.getFileModificationHistory().daysSinceFirstUpdate();
                String relativePath = sourceFile.getRelativePath();
                if (updateCount <= 30) {
                    componentChangesNew.getFiles().add(relativePath);
                } else if (updateCount <= 90) {
                    componentChangesYoung.getFiles().add(relativePath);
                } else if (updateCount <= 180) {
                    componentChangesMature.getFiles().add(relativePath);
                } else if (updateCount <= 365) {
                    componentChangesOld.getFiles().add(relativePath);
                } else {
                    componentChangesVeryOld.getFiles().add(relativePath);
                }
            }
        });
        return logicalDecomposition;
    }

    private static LogicalDecomposition getLogicalDecompositionByFresness(List<SourceFile> allFiles) {
        LogicalDecomposition logicalDecomposition = new LogicalDecomposition("file fressness");

        NamedSourceCodeAspect componentChangesNew = new NamedSourceCodeAspect("changed less than 30 days ago");
        NamedSourceCodeAspect componentChangesYoung = new NamedSourceCodeAspect("changed 31-90 days ago");
        NamedSourceCodeAspect componentChangesMature = new NamedSourceCodeAspect("changed 91-180 days ago");
        NamedSourceCodeAspect componentChangesOld = new NamedSourceCodeAspect("changed 180 days to 1 year ago");
        NamedSourceCodeAspect componentChangesVeryOld = new NamedSourceCodeAspect("changed > 1 year ago");

        logicalDecomposition.getComponents().add(componentChangesNew);
        logicalDecomposition.getComponents().add(componentChangesYoung);
        logicalDecomposition.getComponents().add(componentChangesMature);
        logicalDecomposition.getComponents().add(componentChangesOld);
        logicalDecomposition.getComponents().add(componentChangesVeryOld);

        allFiles.forEach(sourceFile -> {
            if (sourceFile.getFileModificationHistory() != null) {
                int updateCount = sourceFile.getFileModificationHistory().daysSinceLatestUpdate();
                String relativePath = sourceFile.getRelativePath();
                if (updateCount <= 30) {
                    componentChangesNew.getFiles().add(relativePath);
                } else if (updateCount <= 90) {
                    componentChangesYoung.getFiles().add(relativePath);
                } else if (updateCount <= 180) {
                    componentChangesMature.getFiles().add(relativePath);
                } else if (updateCount <= 365) {
                    componentChangesOld.getFiles().add(relativePath);
                } else {
                    componentChangesVeryOld.getFiles().add(relativePath);
                }
            }
        });
        return logicalDecomposition;
    }

}
