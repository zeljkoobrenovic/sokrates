package nl.obren.sokrates.sourcecode.core;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.SourceCodeAspect;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.core.SimpleCallback;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CodeConfigurationUtils {
    public static final String DEFAULT_CONFIGURATION_FOLDER = "_sokrates";
    private static final String DEFAULT_CONFIGURATION_FILE_NAME = "config.json";
    public static final String PERCENTAGE_2_VARIABLE = "${percentage2}";
    public static final String PERCENTAGE_1_VARIABLE = "${percentage1}";
    public static final String UNCLASSIFIED_FILES = "Unclassified";
    public static final String FILES_IN_MULTIPLE_CLASSIFICATIONS = "Multiple Classifications";

    public static File getDefaultSokratesFolder(File srcRoot) {
        File sokratesFolder = new File(srcRoot, CodeConfigurationUtils.DEFAULT_CONFIGURATION_FOLDER);
        sokratesFolder.mkdirs();
        return sokratesFolder;
    }

    public static File getDefaultSokratesConfigFile(File srcRoot) {
        return new File(getDefaultSokratesFolder(srcRoot), CodeConfigurationUtils.DEFAULT_CONFIGURATION_FILE_NAME);
    }

    public static File getDefaultSokratesReportsFolder(File srcRoot) {
        File reportsFolder = new File(getDefaultSokratesFolder(srcRoot), "reports");
        reportsFolder.mkdirs();
        return reportsFolder;
    }

    public static File getDefaultSokratesFindingsFile(File sokratesRoot) {
        File folder = new File(sokratesRoot, "findings");
        folder.mkdirs();
        return new File(folder, "findings.txt");
    }

    public static void populateUnclassifiedAndMultipleAspectsFiles(List<SourceCodeAspect> aspects, List<SourceFile> sourceFiles,
                                                                   SimpleCallback<Pair<SourceFile, SourceCodeAspect>, Void> sourceFileUpdateCallback) {
        SourceCodeAspect unclassified = new SourceCodeAspect(CodeConfigurationUtils.UNCLASSIFIED_FILES);
        SourceCodeAspect filesInMultipleScopes = new SourceCodeAspect(CodeConfigurationUtils.FILES_IN_MULTIPLE_CLASSIFICATIONS);

        for (SourceFile sourceFile : sourceFiles) {
            int fileAspectCount = 0;
            for (SourceCodeAspect aspect : aspects) {
                if (aspect.getSourceFiles().contains(sourceFile)) {
                    fileAspectCount++;
                }
            }
            if (fileAspectCount == 0) {
                unclassified.getSourceFiles().add(sourceFile);
                sourceFileUpdateCallback.call(new ImmutablePair<>(sourceFile, unclassified));
            } else if (fileAspectCount > 1) {
                filesInMultipleScopes.getSourceFiles().add(sourceFile);
            }
        }

        if (filesInMultipleScopes.getSourceFiles().size() > 0) {
            aspects.add(filesInMultipleScopes);
        }

        if (unclassified.getSourceFiles().size() > 0) {
            aspects.add(unclassified);
        }
    }

    public static void removeEmptyAspects(List<SourceCodeAspect> aspects) {
        List<SourceCodeAspect> aspectsWithFiles = new ArrayList<>();
        aspects.forEach(aspect -> {
            if (aspect.getSourceFiles().size() > 0) {
                aspectsWithFiles.add(aspect);
            }
        });
        aspects.clear();
        aspects.addAll(aspectsWithFiles);
    }
}
