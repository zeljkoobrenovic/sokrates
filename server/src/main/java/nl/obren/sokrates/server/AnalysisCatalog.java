package nl.obren.sokrates.server;

import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalysisUtils;
import nl.obren.sokrates.sourcecode.landscape.analysis.LandscapeAnalyzer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * The Sokrates analyses below a root folder, found the same way the landscape finds them: a repository analysis
 * is a {@code data/data.zip} (or, for reports older than the zip, a loose {@code data/analysisResults.json})
 * outside any {@code _sokrates_landscape} folder; a landscape is a {@code _sokrates_landscape/config.json}
 * outside the generated virtual-landscape trees. The folder is rescanned at most every {@code rescanMillis},
 * so analyses added or refreshed by a later run show up without a restart.
 */
public class AnalysisCatalog {
    private static final Log LOG = LogFactory.getLog(AnalysisCatalog.class);
    // Never descended into: they cannot hold analyses and can be huge.
    private static final Set<String> SKIPPED_FOLDERS = Set.of(".git", "node_modules");
    // The report folders of an analysis (below "reports"), which hold many files and no analyses.
    private static final Set<String> SKIPPED_REPORT_FOLDERS = Set.of("html", "src", "explorers", "visuals", "ai-insights");

    /** A repository analysis: {@code id} is the path of its analysis folder (the reports folder's parent) relative to the root. */
    public record RepositoryLocation(String id, File analysisFolder, File reportsFolder, File dataFolder) {
        /** Changes when the data is regenerated. */
        public long stamp() {
            File zip = new File(dataFolder, LandscapeAnalyzer.DATA_ZIP_FILE_NAME);
            return zip.exists() ? zip.lastModified() : new File(dataFolder, "analysisResults.json").lastModified();
        }
    }

    /** A landscape: {@code id} is the path of the folder holding {@code _sokrates_landscape}, relative to the root. */
    public record LandscapeLocation(String id, File rootFolder, File landscapeFolder) {
    }

    public record Snapshot(List<RepositoryLocation> repositories, List<LandscapeLocation> landscapes, long scannedAt) {
    }

    private final File root;
    private final long rescanMillis;
    private Snapshot snapshot;

    public AnalysisCatalog(File root, long rescanMillis) {
        this.root = root.getAbsoluteFile().toPath().normalize().toFile();
        this.rescanMillis = rescanMillis;
    }

    public File getRoot() {
        return root;
    }

    public synchronized Snapshot snapshot() {
        if (snapshot == null || System.currentTimeMillis() - snapshot.scannedAt() > rescanMillis) {
            snapshot = scan();
        }
        return snapshot;
    }

    private Snapshot scan() {
        List<RepositoryLocation> repositories = new ArrayList<>();
        List<LandscapeLocation> landscapes = new ArrayList<>();
        Path rootPath = root.toPath();
        try {
            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    String name = dir.getFileName() == null ? "" : dir.getFileName().toString();
                    if (!dir.equals(rootPath) && SKIPPED_FOLDERS.contains(name)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    Path parent = dir.getParent();
                    if (parent != null && parent.getFileName() != null && parent.getFileName().toString().equals("reports")
                            && SKIPPED_REPORT_FOLDERS.contains(name)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    if (LandscapeAnalysisUtils.isInGeneratedVirtualLandscape(dir.resolve("x"))) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (LandscapeAnalysisUtils.isSokratesLandscapeConfigFile(file)) {
                        File landscapeFolder = file.getParent().toFile();
                        File landscapeRoot = landscapeFolder.getParentFile();
                        landscapes.add(new LandscapeLocation(relative(rootPath, landscapeRoot.toPath()), landscapeRoot, landscapeFolder));
                    } else if (isRepositoryData(file)) {
                        File dataFolder = file.getParent().toFile();
                        File reportsFolder = dataFolder.getParentFile();
                        File analysisFolder = reportsFolder.getParentFile();
                        repositories.add(new RepositoryLocation(relative(rootPath, analysisFolder.toPath()), analysisFolder, reportsFolder, dataFolder));
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOG.warn("Could not scan " + root.getPath() + ": " + e.getMessage());
        }
        repositories.sort(Comparator.comparing(RepositoryLocation::id));
        landscapes.sort(Comparator.comparing(LandscapeLocation::id));
        LOG.info("Found " + repositories.size() + " repository analyses and " + landscapes.size() + " landscapes in " + root.getPath());
        return new Snapshot(repositories, landscapes, System.currentTimeMillis());
    }

    // data/data.zip, or data/analysisResults.json when there is no data.zip next to it; never a landscape's own data.
    static boolean isRepositoryData(Path file) {
        Path parent = file.getParent();
        if (parent == null || parent.getFileName() == null || !parent.getFileName().toString().equals("data")
                || parent.getParent() == null || LandscapeAnalysisUtils.isInLandscapeFolder(file)) {
            return false;
        }
        String name = file.getFileName().toString();
        if (name.equals(LandscapeAnalyzer.DATA_ZIP_FILE_NAME)) {
            return true;
        }
        return name.equals("analysisResults.json") && !Files.exists(parent.resolve(LandscapeAnalyzer.DATA_ZIP_FILE_NAME));
    }

    static String relative(Path root, Path path) {
        String relative = root.relativize(path.toAbsolutePath().normalize()).toString().replace("\\", "/");
        return relative.isEmpty() ? "." : relative;
    }

    /** The path of a file relative to the root, as it is served under /reports/ (forward slashes). */
    public String servedPath(File file) {
        return relative(root.toPath(), file.toPath());
    }
}
