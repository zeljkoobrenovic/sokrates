package nl.obren.sokrates.reports.dataexporters;

import nl.obren.sokrates.sourcecode.ExtensionGroupExtractor;
import nl.obren.sokrates.sourcecode.IgnoredFilesGroup;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.SourceFileWithSearchData;
import nl.obren.sokrates.sourcecode.analysis.results.AspectAnalysisResults;
import nl.obren.sokrates.sourcecode.analysis.results.CodeAnalysisResults;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.core.CodeConfiguration;
import nl.obren.sokrates.sourcecode.filehistory.DateUtils;
import nl.obren.sokrates.sourcecode.filehistory.FileModificationHistory;
import nl.obren.sokrates.sourcecode.search.FoundLine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.File;
import java.io.IOException;
import java.util.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.function.Consumer;

/**
 * The file-list text exports of data/text/: the files excluded by extension or by rule, the per-aspect file lists
 * (with and without history) and the features-of-interest matches. Moved out of {@link DataExporter}.
 */
class FileListsDataExporter {
    private static final Log LOG = LogFactory.getLog(FileListsDataExporter.class);
    private final CodeAnalysisResults analysisResults;
    private final File textDataFolder;
    private final Runnable saveSymbolicLinks;
    private final Consumer<String> info;
    private final Consumer<String> detailedInfo;

    FileListsDataExporter(CodeAnalysisResults analysisResults, File textDataFolder, Runnable saveSymbolicLinks, Consumer<String> info, Consumer<String> detailedInfo) {
        this.analysisResults = analysisResults;
        this.textDataFolder = textDataFolder;
        this.saveSymbolicLinks = saveSymbolicLinks;
        this.info = info;
        this.detailedInfo = detailedInfo;
    }

    void exportFileLists() {
        saveExcludedByExtensionFiles();
        saveExplicitlyIgnoredFiles();
        saveSymbolicLinks.run();
        saveSourceCodeAspect(analysisResults.getMainAspectAnalysisResults().getAspect(), "");
        saveSourceCodeAspect(analysisResults.getTestAspectAnalysisResults().getAspect(), "");
        saveSourceCodeAspect(analysisResults.getGeneratedAspectAnalysisResults().getAspect(), "");
        saveSourceCodeAspect(analysisResults.getBuildAndDeployAspectAnalysisResults().getAspect(), "");
        saveSourceCodeAspect(analysisResults.getOtherAspectAnalysisResults().getAspect(), "");

        analysisResults.getLogicalDecompositionsAnalysisResults().forEach(logicalDecomposition -> {
            logicalDecomposition.getComponents().forEach(component -> {
                saveSourceCodeAspect(component.getAspect(), DataExportUtils.getComponentFilePrefix(logicalDecomposition.getKey()));
            });
        });

        analysisResults.getConcernsAnalysisResults().forEach(group -> {
            group.getConcerns().forEach(concern -> {
                saveSourceCodeAspect(concern.getAspect(), DataExportUtils.getConcernFilePrefix(group.getKey()));
                saveFoundText(concern, DataExportUtils.getConcernFilePrefix(group.getKey()));
                saveFoundTextPerFile(concern, DataExportUtils.getConcernFilePrefix(group.getKey()));
            });
        });
    }

    private void saveExcludedByExtensionFiles() {
        StringBuilder content = new StringBuilder();

        Map<String, List<SourceFile>> extensionsMap = new HashMap<>();

        analysisResults.getFilesExcludedByExtension().forEach(sourceFile -> {
            String extension = ExtensionGroupExtractor.getExtension(sourceFile.getRelativePath());
            List<SourceFile> files = extensionsMap.get(extension);
            if (files == null) {
                files = new ArrayList<>();
                extensionsMap.put(extension, files);
            }
            files.add(sourceFile);
        });

        List<String> extensions = new ArrayList<>(extensionsMap.keySet());
        Collections.sort(extensions, (o1, o2) -> extensionsMap.get(o2).size() - extensionsMap.get(o1).size());

        extensions.forEach(extension -> {
            List<SourceFile> sourceFiles = extensionsMap.get(extension);
            content.append(DataExporter.SEPARATOR);
            content.append("*." + extension + " files (" + sourceFiles.size() + ")");
            content.append(":\n\n");
            sourceFiles.forEach(sourceFile -> {
                content.append(sourceFile.getRelativePath());
                content.append("\n");
            });
            content.append(DataExporter.SEPARATOR);
            content.append("\n\n\n");
        });

        try {
            FileUtils.write(new File(textDataFolder, "excluded_files_ignored_extensions.txt"), content.toString(), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveExplicitlyIgnoredFiles() {
        StringBuilder content = new StringBuilder();

        Map<String, IgnoredFilesGroup> ignoredFilesGroups = analysisResults.getIgnoredFilesGroups();
        List<String> keys = new ArrayList<>(ignoredFilesGroups.keySet());
        Collections.sort(keys, (o1, o2) -> ignoredFilesGroups.get(o2).getSourceFiles().size() - ignoredFilesGroups.get(o1).getSourceFiles().size());
        keys.forEach(key -> {
            IgnoredFilesGroup ignoredFilesGroup = ignoredFilesGroups.get(key);
            content.append(DataExporter.SEPARATOR);
            content.append(ignoredFilesGroup.getFilter().getNote());
            content.append("\n");
            content.append(key);
            content.append("\n");
            List<SourceFile> sourceFiles = ignoredFilesGroup.getSourceFiles();
            content.append(sourceFiles.size() + " files");
            content.append(":\n\n");
            sourceFiles.forEach(sourceFile -> {
                content.append(sourceFile.getRelativePath());
                content.append("\n");
            });
            content.append(DataExporter.SEPARATOR);
            content.append("\n\n\n");
        });

        try {
            FileUtils.write(new File(textDataFolder, "excluded_files_ignored_rules.txt"), content.toString(), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveSourceCodeAspect(NamedSourceCodeAspect aspect, String prefix) {
        StringBuilder content = new StringBuilder();

        List<SourceFile> files = new ArrayList<>(aspect.getSourceFiles());
        Collections.sort(files, Comparator.comparing(SourceFile::getRelativePath));

        content.append("Path\tLines of Code\n");
        files.forEach(sourceFile -> {
            content.append(sourceFile.getRelativePath());
            content.append("\t");
            content.append(sourceFile.getLinesOfCode());
            content.append("\n");
        });

        try {
            FileUtils.write(new File(textDataFolder, DataExportUtils.getAspectFileListFileName(aspect, prefix)), content.toString(), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveFoundText(AspectAnalysisResults aspectAnalysisResults, String prefix) {
        if (aspectAnalysisResults.getFoundTextList().size() == 0) {
            return;
        }

        StringBuilder content = new StringBuilder();

        content.append("Text\tCount\n");
        int total[] = {0};
        int unique[] = {0};
        aspectAnalysisResults.getFoundTextList().forEach(foundText -> {
            content.append(foundText.getText().trim());
            content.append("\t");
            content.append(foundText.getCount());
            content.append("\n");

            unique[0] += 1;
            total[0] += foundText.getCount();
        });

        try {
            String fileName = DataExportUtils.getAspectFileListFileName(aspectAnalysisResults.getAspect(), prefix, DataExporter.FOUND_TEXT_SUFFIX);
            String data = "Summary: " + total[0] + " " + (total[0] == 1 ? "instance" : "instances") + ", " + unique[0] + " unique\n\n";
            data += content.toString();
            FileUtils.write(new File(textDataFolder, fileName), data, UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveFoundTextPerFile(AspectAnalysisResults aspectAnalysisResults, String prefix) {
        Map<File, SourceFileWithSearchData> foundFiles = aspectAnalysisResults.getFoundFiles();
        if (foundFiles.size() == 0) {
            return;
        }

        StringBuilder content = new StringBuilder();

        List<SourceFileWithSearchData> list = new ArrayList<>(foundFiles.values());
        Collections.sort(list, (a, b) -> b.getFoundInstancesCount() - a.getFoundInstancesCount());
        list.forEach(data -> {
            if (content.length() > 0) {
                content.append("\n\n");
            }
            List<FoundLine> lines = data.getLinesWithSearchedContent();
            content.append(data.getSourceFile().getRelativePath() + " (" + lines.size() + " " + (lines.size() == 1 ? "line" : "lines") + "):\n");
            data.getLinesWithSearchedContent().forEach(line -> {
                content.append("\t");
                content.append("- line " + line.getLineNumber() + ": ");
                content.append(line.getFoundText().trim());
                content.append("\n");
            });
        });

        try {
            String fileName = DataExportUtils.getAspectFileListFileName(aspectAnalysisResults.getAspect(), prefix, DataExporter.FOUND_TEXT_PER_FILE_SUFFIX);
            FileUtils.write(new File(textDataFolder, fileName), content.toString(), UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * The five scope file lists, named the way they were written.
     *
     * <p>These were five literals - "aspect_main.txt" and so on - which is what the default aspect
     * names produce. The writer derives each name from the aspect's configured name, so renaming a
     * scope aspect in config.json left this opening a file nothing had written; the exception went
     * into the enclosing catch in the caller, taking the rest of that block with it.
     *
     * <p>Derived through the writer's own function rather than by repeating its rule, so the two
     * cannot drift apart again.
     */
    static String[][] aspectFileLists(CodeConfiguration configuration, File textDataFolder) throws IOException {
        List<NamedSourceCodeAspect> aspects = Arrays.asList(
                configuration.getMain(), configuration.getTest(), configuration.getGenerated(),
                configuration.getBuildAndDeployment(), configuration.getOther());

        String[][] entries = new String[aspects.size()][2];
        for (int i = 0; i < aspects.size(); i++) {
            String fileName = DataExportUtils.getAspectFileListFileName(aspects.get(i), "");
            entries[i] = new String[]{fileName, FileUtils.readFileToString(new File(textDataFolder, fileName), UTF_8)};
        }
        return entries;
    }

    static String getFilesAsTxt(List<SourceFile> sourceFiles) {
        StringBuilder builder = new StringBuilder();

        builder.append("path\t# lines of code").append("\n");

        sourceFiles.forEach(sourceFile -> {
            builder.append(sourceFile.getRelativePath())
                    .append("\t")
                    .append(sourceFile.getLinesOfCode())
                    .append("\n");
        });

        return builder.toString();
    }

    static String getFilesWithHistoryAsTxt(List<SourceFile> sourceFiles) {
        StringBuilder builder = new StringBuilder();

        builder.append("path\t# lines of code\t")
                .append("# active days\tdays since first update\tdays since last update\t")
                .append("# commits\t# commits (30d)\t# commits (90d)\t# contributors\t")
                .append("line churn\tline churn (30d)\tline churn (90d)\t")
                .append("first updated\tlast updated\tfirst contributor\tlast contributor")
                .append("\n");

        sourceFiles.forEach(sourceFile -> {
            FileModificationHistory history = sourceFile.getFileModificationHistory();
            if (history != null) {
                long commits30Days = history.getDates().stream()
                        .filter(date -> DateUtils.isCommittedLessThanDaysAgo(date, 30)).count();
                long commits90Days = history.getDates().stream()
                        .filter(date -> DateUtils.isCommittedLessThanDaysAgo(date, 90)).count();
                builder.append(sourceFile.getRelativePath()).append("\t")
                        .append(sourceFile.getLinesOfCode()).append("\t")
                        .append(history.getDates().size()).append("\t")
                        .append(history.daysSinceFirstUpdate()).append("\t")
                        .append(history.daysSinceLatestUpdate()).append("\t")
                        .append(history.getCommits().size()).append("\t")
                        .append(commits30Days).append("\t")
                        .append(commits90Days).append("\t")
                        .append(history.countContributors()).append("\t")
                        .append(history.getChurn()).append("\t")
                        .append(history.getChurn30Days()).append("\t")
                        .append(history.getChurn90Days()).append("\t")
                        .append(history.getOldestDate()).append("\t")
                        .append(history.getLatestDate()).append("\t")
                        .append(history.getOldestContributor()).append("\t")
                        .append(history.getLatestContributor()).append("\n");
            }
        });

        return builder.toString();
    }

    static String getFilesWithoutHistoryAsTxt(List<SourceFile> sourceFiles) {
        StringBuilder builder = new StringBuilder();

        builder.append("path\t# lines of code\n");

        sourceFiles.forEach(sourceFile -> {
            FileModificationHistory history = sourceFile.getFileModificationHistory();
            if (history == null) {
                builder.append(sourceFile.getRelativePath()).append("\t")
                        .append(sourceFile.getLinesOfCode()).append("\t")
                        .append("\n");
            }
        });

        return builder.toString();
    }
}
