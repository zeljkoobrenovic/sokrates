/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode;

import com.fasterxml.jackson.annotation.JsonIgnore;
import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.core.AnalysisConfig;
import nl.obren.sokrates.sourcecode.core.CodeConfigurationUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class SourceCodeFiles {
    private static final Log LOG = LogFactory.getLog(SourceCodeFiles.class);

    private List<SourceFile> allFiles = new ArrayList<>();
    private List<SourceFile> filesInBroadScope = new ArrayList<>();
    private File root;
    private ProgressFeedback progressFeedback = new ProgressFeedback();
    @JsonIgnore
    private Map<String, IgnoredFilesGroup> ignoredFilesGroups = new HashMap<>();
    @JsonIgnore
    private List<SourceFile> filesExcludedByExtension = new ArrayList<>();
    @JsonIgnore
    private List<SymbolicLink> skippedSymbolicLinks = new ArrayList<>();
    // Resolved once per run rather than per link: a tree can hold tens of thousands of links, and
    // canonicalising the root is a syscall each time. Null when the root cannot be canonicalised,
    // which makes every link classify as outside.
    @JsonIgnore
    private Path canonicalRoot;

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
        skippedSymbolicLinks.clear();
        canonicalRoot = canonicalPathOf(root);
        progressFeedback.start();
        addFile(root, true);
        progressFeedback.end();
        if (skippedSymbolicLinks.size() > 0) {
            LOG.info("Did not follow " + skippedSymbolicLinks.size() + " symbolic link(s) under "
                    + root.getAbsolutePath() + "; found " + allFiles.size() + " file(s).");
        }
    }

    public List<SourceFile> getSourceFiles(NamedSourceCodeAspect aspect) {
        return getSourceFiles(aspect, getFilesInBroadScope());
    }

    public List<SourceFile> getSourceFiles(NamedSourceCodeAspect aspect, List<SourceFile> scopeSourceFiles) {
        progressFeedback.start();
        progressFeedback.setDetailedText("Updating \"" + aspect.getName() + "\"...");
        aspect.getSourceFiles().clear();

        List<SourceFile> sourceFiles = new ArrayList<>();

        int fileIndex[] = {0};
        final int allFilesCount = scopeSourceFiles.size();
        scopeSourceFiles.forEach(sourceFile -> {
            if (progressFeedback.canceled()) {
                return;
            }
            if (aspectIncludes(aspect, sourceFile)) {
                if (!sourceFiles.contains(sourceFile)) {
                    sourceFiles.add(sourceFile);
                }
                if (!aspect.getSourceFiles().contains(sourceFile)) {
                    aspect.getSourceFiles().add(sourceFile);
                }
            }
            progressFeedback.progress(++fileIndex[0], allFilesCount);
        });
        progressFeedback.end();

        return sourceFiles;
    }

    /** Listed by path or matched by an including filter, and matched by no excepting filter. */
    private boolean aspectIncludes(NamedSourceCodeAspect aspect, SourceFile sourceFile) {
        boolean included[] = {false};
        boolean excluded[] = {false};
        if (aspect.getFiles().contains(sourceFile.getRelativePath())) {
            included[0] = true;
        }
        aspect.getSourceFileFilters().forEach(filter -> {
            if (progressFeedback.canceled()) {
                return;
            }
            if (filter.matches(sourceFile)) {
                if (!filter.getException()) {
                    included[0] = true;
                } else {
                    excluded[0] = true;
                }
            }
        });
        return included[0] && !excluded[0];
    }

    public void createBroadScope(List<String> extensions, List<SourceFileFilter> exclusions, AnalysisConfig analysisConfig) {
        createBroadScope(extensions, exclusions, true, analysisConfig);
    }

    public void createBroadScope(List<String> extensions, List<SourceFileFilter> exclusions, boolean addLoc, AnalysisConfig analysisConfig) {
        progressFeedback.start();
        filesInBroadScope.clear();

        int fileIndex[] = {0};
        progressFeedback.setText("Loading files...");
        progressFeedback.setText("");
        int displayCounter[] = {0};
        allFiles.forEach(sourceFile -> {
            if (progressFeedback.canceled()) {
                return;
            }
            displayCounter[0] += 1;
            boolean lastFile = displayCounter[0] == allFiles.size();
            if (displayCounter[0] % 1000 == 1 || lastFile) {
                progressFeedback.setDetailedText("Loading file " + displayCounter[0] + "/" + allFiles.size()
                        + ": " + sourceFile.getFile().getName());
            }
            if (FilenameUtils.isExtension(sourceFile.getFile().getPath(), extensions)) {
                if (!shouldExcludeFile(sourceFile, exclusions, analysisConfig)) {
                    if (addLoc) {
                        sourceFile.setLinesOfCodeFromContent();
                    }
                    filesInBroadScope.add(sourceFile);
                }
                progressFeedback.progress(++fileIndex[0], allFiles.size());
            } else {
                filesExcludedByExtension.add(sourceFile);
            }
        });
        progressFeedback.end();
    }

    boolean shouldExcludeFile(SourceFile sourceFile, List<SourceFileFilter> exclusions, AnalysisConfig analysisConfig) {
        if (sourceFile.getFile().length() > analysisConfig.getMaxFileSizeBytes()) {
            ignore(sourceFile, "Too long file (" + analysisConfig.getMaxFileSizeBytes() + "+ bytes)", new SourceFileFilter());
            return true;
        } else if (hasTooManyLines(sourceFile, analysisConfig.getMaxLines())) {
            ignore(sourceFile, "Too many lines (" + analysisConfig.getMaxLines() + ")", new SourceFileFilter());
            return true;
        } else if (hasTooLongLines(sourceFile, analysisConfig.getMaxLineLength())) {
            ignore(sourceFile, "Too long lines (" + analysisConfig.getMaxLineLength() + "+ characters)", new SourceFileFilter());
            return true;
        } else {
            for (SourceFileFilter filter : exclusions) {
                if (filter.matches(sourceFile)) {
                    ignore(sourceFile, filter.toString(), filter);
                    return true;
                }
            }
            return false;
        }
    }

    /** Files the source file under the ignored-files group of the given key (created with the filter on first use). */
    private void ignore(SourceFile sourceFile, String key, SourceFileFilter filter) {
        IgnoredFilesGroup ignoredFilesGroup = ignoredFilesGroups.get(key);
        if (ignoredFilesGroup == null) {
            ignoredFilesGroup = new IgnoredFilesGroup(filter);
            ignoredFilesGroups.put(key, ignoredFilesGroup);
        }
        ignoredFilesGroup.getSourceFiles().add(sourceFile);
    }

    private boolean hasTooManyLines(SourceFile sourceFile, int maxLines) {
        if (sourceFile.getLines().size() > maxLines) {
            return true;
        }

        return false;
    }

    private boolean hasTooLongLines(SourceFile sourceFile, int maxLineLength) {
        for (String line : sourceFile.getLines()) {
            if (line.length() > maxLineLength) {
                return true;
            }
        }

        return false;
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

    private void addFile(File file, boolean isAnalysisRoot) {
        if (!isAnalysisRoot && isSymbolicLink(file)) {
            // A symlinked .git or _sokrates is not evidence of missing code: the walk skips those
            // folders whether they are links or not, so naming them would send the reader looking
            // for source that was never in scope.
            if (isNotVCSFolder(file)) {
                skippedSymbolicLinks.add(describeSymbolicLink(file));
            }
            return;
        }
        if (file.isDirectory()) {
            if (isNotVCSFolder(file)) {
                for (File child : file.listFiles()) {
                    addFile(child, false);
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

    /**
     * Not every path {@link File} accepts can be turned into a {@link java.nio.file.Path}:
     * {@code SourceFile.relativize} catches {@link InvalidPathException} on files this walk had
     * already collected. Such a path cannot be tested for a link, so it is walked as before rather
     * than dropped.
     */
    static boolean isSymbolicLink(File file) {
        try {
            return Files.isSymbolicLink(file.toPath());
        } catch (InvalidPathException e) {
            return false;
        }
    }

    /**
     * Describes a link the walk is about to skip. Nothing here may throw: this runs inside the walk,
     * so an escaping exception would abort the whole analysis - strictly worse than the plain
     * counter it replaced, which could not fail. A link whose details cannot be worked out is still
     * worth naming, so the fallback keeps the path and gives up only on the rest.
     */
    private SymbolicLink describeSymbolicLink(File file) {
        try {
            String target = linkTarget(file);
            return new SymbolicLink(relativePath(file), target, pointsInsideRoot(file, target));
        } catch (RuntimeException e) {
            return new SymbolicLink(file.getPath(), "", false);
        }
    }

    private String relativePath(File file) {
        try {
            return Paths.get(root.getPath()).relativize(Paths.get(file.getPath())).toString();
        } catch (IllegalArgumentException e) {
            // Covers InvalidPathException, and relativize's own refusal when the two paths cannot
            // be expressed relative to one another.
            return file.getPath();
        }
    }

    /**
     * The target as written on disk, so a relative link stays relative. Resolving it here would
     * hide the form the reader recognises from their own repository.
     */
    private String linkTarget(File file) {
        try {
            return Files.readSymbolicLink(file.toPath()).toString();
        } catch (IOException | InvalidPathException e) {
            return "";
        }
    }

    /**
     * Answers where the link <em>points</em>, which is not the same as whether anything is measured
     * there: a link pointing inside the root at a target that does not exist still counts as
     * pointing inside. The report says so in those words rather than promising the code was
     * measured under its real path.
     *
     * <p>Canonicalises the link's <em>target</em>, not the link itself. Canonicalising the link
     * resolves it only while the target exists; for a dangling link it yields the link's own path,
     * so every broken link anywhere would look like it pointed inside.
     *
     * <p>{@code getCanonicalFile} rather than {@code Path.toRealPath}: it resolves the part of the
     * path that exists and leaves the rest, so a target that was never created still classifies
     * instead of throwing. A relative target is resolved against the link's own directory - not
     * against the root, which would misplace any link below the top level.
     *
     * <p>A link whose target cannot be read at all is reported as outside: with no target there is
     * nothing to place inside the tree. The test is emptiness, not blankness - " " is a legal file
     * name, and a link pointing at it was read perfectly well.
     */
    // Package-private: the blank-target branch cannot be reached from a real file system (it needs
    // readSymbolicLink to fail), and a branch the test environment cannot reach is an untested one.
    boolean pointsInsideRoot(File file, String target) {
        if (target.isEmpty() || canonicalRoot == null) {
            return false;
        }
        File targetFile = new File(target);
        if (!targetFile.isAbsolute()) {
            targetFile = new File(file.getParentFile(), target);
        }
        Path canonicalTarget = canonicalPathOf(targetFile);
        // startsWith on Path compares whole name elements, so a sibling "repo-backup" is not
        // mistaken for something inside "repo".
        return canonicalTarget != null && canonicalTarget.startsWith(canonicalRoot);
    }

    private Path canonicalPathOf(File file) {
        try {
            return file.getCanonicalFile().toPath();
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    int getSkippedSymbolicLinksCount() {
        return skippedSymbolicLinks.size();
    }

    /**
     * A copy: {@code load} clears the internal list, so handing out the live one would let a
     * reload silently empty a results object that already holds it.
     */
    @JsonIgnore
    public List<SymbolicLink> getSkippedSymbolicLinks() {
        return new ArrayList<>(skippedSymbolicLinks);
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

    @JsonIgnore
    public Map<String, IgnoredFilesGroup> getIgnoredFilesGroups() {
        return ignoredFilesGroups;
    }

    @JsonIgnore
    public void setIgnoredFilesGroups(Map<String, IgnoredFilesGroup> ignoredFilesGroups) {
        this.ignoredFilesGroups = ignoredFilesGroups;
    }

    @JsonIgnore
    public List<SourceFile> getFilesExcludedByExtension() {
        return filesExcludedByExtension;
    }

    @JsonIgnore
    public void setFilesExcludedByExtension(List<SourceFile> filesExcludedByExtension) {
        this.filesExcludedByExtension = filesExcludedByExtension;
    }
}
