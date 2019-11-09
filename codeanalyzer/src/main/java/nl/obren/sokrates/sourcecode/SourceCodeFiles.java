package nl.obren.sokrates.sourcecode;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.aspects.SourceCodeAspect;
import org.apache.commons.io.FilenameUtils;
import nl.obren.sokrates.sourcecode.core.CodeConfigurationUtils;

import java.io.File;
import java.util.*;

public class SourceCodeFiles {
    private List<SourceFile> allFiles = new ArrayList<>();
    private List<SourceFile> filesInBroadScope = new ArrayList<>();
    private File root;
    private ProgressFeedback progressFeedback = new ProgressFeedback();

    public SourceCodeFiles() {
    }

    public static int getLinesOfCode(List<SourceFile> sourceFiles) {
        int loc = 0;

        for (SourceFile sourceFile : sourceFiles) {
            loc += sourceFile.getLinesOfCode();
        }

        return loc;
    }

    public void load(File root, ProgressFeedback progressFeedback) {
        this.root = root;
        this.progressFeedback = progressFeedback;
        loadAllFiles(root, progressFeedback);
    }

    private void loadAllFiles(File root, ProgressFeedback progressFeedback) {
        this.progressFeedback = progressFeedback;
        allFiles.clear();
        progressFeedback.start();
        addFile(root);
        progressFeedback.end();
    }

    public List<SourceFile> getSourceFiles(SourceCodeAspect aspect) {
        return getSourceFiles(aspect, getFilesInBroadScope());
    }

    public List<SourceFile> getSourceFiles(SourceCodeAspect aspect, List<SourceFile> scopeSourceFiles) {
        progressFeedback.start();
        progressFeedback.setDetailedText("Updating \"" + aspect.getName() + "\"...");
        aspect.getSourceFiles().clear();

        List<SourceFile> sourceFiles = new ArrayList<>();

        int fileIndex[] = {0};
        scopeSourceFiles.forEach(sourceFile -> {
            if (progressFeedback.canceled()) {
                return;
            }
            boolean included[] = {false};
            boolean excluded[] = {false};
            aspect.getSourceFileFilters().forEach(filter -> {
                if (progressFeedback.canceled()) {
                    return;
                }
                if (filter.matches(sourceFile)) {
                    if (filter.getInclude()) {
                        included[0] = true;
                    } else {
                        excluded[0] = true;
                    }
                }
                progressFeedback.progress(++fileIndex[0], allFiles.size());
            });
            if (included[0] && !excluded[0]) {
                if (!sourceFiles.contains(sourceFile)) {
                    sourceFiles.add(sourceFile);
                }
                if (!aspect.getSourceFiles().contains(sourceFile)) {
                    aspect.getSourceFiles().add(sourceFile);
                }
            }
        });
        progressFeedback.end();

        return sourceFiles;
    }

    public void createBroadScope(List<String> extensions, List<SourceFileFilter> exclusions) {
        createBroadScope(extensions, exclusions, true);
    }

    public void createBroadScope(List<String> extensions, List<SourceFileFilter> exclusions, boolean addLoc) {
        progressFeedback.start();
        filesInBroadScope.clear();

        int fileIndex[] = {0};
        progressFeedback.setText("Loading files...");
        progressFeedback.setText("");
        allFiles.forEach(sourceFile -> {
            if (progressFeedback.canceled()) {
                return;
            }
            progressFeedback.setDetailedText("Loading " + sourceFile.getFile().getName());
            if (FilenameUtils.isExtension(sourceFile.getFile().getPath(), extensions)) {
                if (!shouldExcludeFile(sourceFile, exclusions)) {
                    if (addLoc) {
                        sourceFile.setLinesOfCodeFromContent();
                    }
                    filesInBroadScope.add(sourceFile);
                }
                progressFeedback.progress(++fileIndex[0], allFiles.size());
            }
        });
        progressFeedback.end();
    }

    boolean shouldExcludeFile(SourceFile sourceFile, List<SourceFileFilter> exclusions) {
        boolean exclude = false;
        for (SourceFileFilter filter : exclusions) {
            if (filter.matches(sourceFile)) {
                exclude = true;
                break;
            }
        }
        return exclude;
    }

    public List<SourceFile> getAllFiles() {
        return allFiles;
    }

    public void setAllFiles(List<SourceFile> allFiles) {
        this.allFiles = allFiles;
    }

    public List<SourceFile> getFilesInBroadScope() {
        return filesInBroadScope;
    }

    public void setFilesInBroadScope(List<SourceFile> filesInBroadScope) {
        this.filesInBroadScope = filesInBroadScope;
    }

    private void addFile(File file) {
        if (file.isDirectory()) {
            if (isNotVCSFolder(file)) {
                for (File child : file.listFiles()) {
                    addFile(child);
                }
            }
        } else {
            SourceFile sourceFile = new SourceFile(file);
            sourceFile.relativize(root);
            allFiles.add(sourceFile);
        }
    }

    public Map<String, Integer> getExtensionsCountMap(List<SourceFile> sourceFiles) {
        Map<String, Integer> map = new HashMap<>();

        sourceFiles.forEach(sourceFile -> {
            String key = sourceFile.getExtension();
            map.put(key, map.containsKey(key) ? map.get(key) + 1 : 1);
        });

        return map;
    }

    boolean isNotVCSFolder(File folder) {
        List<String> vcsFolderNames = Arrays.asList(".svn", ".git", CodeConfigurationUtils.DEFAULT_CONFIGURATION_FOLDER);

        for (String vcsFolderName : vcsFolderNames) {
            if (vcsFolderName.equalsIgnoreCase(folder.getName())) {
                return false;
            }
        }

        return true;
    }

    public List<SourceFile> getExcludedFiles() {
        List<SourceFile> excludedFiles = new ArrayList<>();

        allFiles.forEach(sourceFile -> {
            if (!filesInBroadScope.contains(sourceFile)) {
                excludedFiles.add(sourceFile);
            }
        });

        return excludedFiles;
    }
}
