/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.filehistory;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

public class FilePairsChangedTogether {
    private static final Log LOG = LogFactory.getLog(FilePairsChangedTogether.class);
    public static final int PAIR_LIST_LIMIT = 1000000;

    private Map<String, FilePairChangedTogether> filePairsMap = new HashMap<>();
    private Set<FilePairChangedTogether> filePairs = new HashSet<>();
    private List<FilePairChangedTogether> filePairsList = new ArrayList<>();
    private int rangeInDays = -1;

    public FilePairsChangedTogether(int rangeInDays) {
        this.rangeInDays = rangeInDays;
    }

    public void populate(NamedSourceCodeAspect aspect, List<FileModificationHistory> fileHistories) {
        // Resolve each modification history to its SourceFile (and lower-cased path) once, up front,
        // instead of re-resolving on every generated pair. Histories not part of the aspect are dropped here.
        Map<FileModificationHistory, ResolvedFile> resolved = new HashMap<>();
        fileHistories.forEach(fileHistory -> {
            SourceFile sourceFile = aspect.getSourceFileByPath(fileHistory.getPath());
            if (sourceFile != null) {
                resolved.put(fileHistory, new ResolvedFile(sourceFile,
                        sourceFile.getRelativePath().toLowerCase(), fileHistory.getCommits().size()));
            }
        });

        Map<String, Set<FileModificationHistory>> commitsIdMap = new HashMap<>();

        fileHistories.forEach(fileHistory -> {
            if (resolved.containsKey(fileHistory)) {
                fileHistory.getCommits().forEach(commitInfo -> {
                    if (filePairs.size() < PAIR_LIST_LIMIT && (rangeInDays <= 0 || DateUtils.isDateWithinRange(commitInfo.getDate(), rangeInDays))) {
                        String commitId = commitInfo.getId();
                        String commitDate = commitInfo.getDate();
                        Set<FileModificationHistory> list = commitsIdMap.get(commitId);
                        if (list == null) {
                            list = new HashSet<>();
                            commitsIdMap.put(commitId, list);
                            list.add(fileHistory);
                        } else if (!list.contains(fileHistory)) {
                            ResolvedFile resolvedFile = resolved.get(fileHistory);
                            list.forEach(sourceFileInSameCommit -> addFilePair(resolvedFile,
                                    resolved.get(sourceFileInSameCommit), commitId, commitDate));
                            list.add(fileHistory);
                        }
                    }
                });
            }
        });

        filePairsList = new ArrayList<>(filePairs);
        Collections.sort(filePairsList, (a, b) -> b.getCommits().size() - a.getCommits().size());
    }

    private void addFilePair(ResolvedFile file1, ResolvedFile file2, String commitId, String date) {
        if (file1 != null && file2 != null) {
            String key1 = file1.lowerCasePath + "_" + file2.lowerCasePath;
            String key2 = file2.lowerCasePath + "_" + file1.lowerCasePath;

            FilePairChangedTogether filePairChangedTogether = filePairsMap.get(key1);
            if (filePairChangedTogether == null) {
                filePairChangedTogether = filePairsMap.get(key2);
            }

            if (filePairChangedTogether == null) {
                filePairChangedTogether = new FilePairChangedTogether(file1.sourceFile, file2.sourceFile);

                filePairChangedTogether.setCommitsCountFile1(file1.commitsCount);
                filePairChangedTogether.setCommitsCountFile2(file2.commitsCount);

                filePairsMap.put(key1, filePairChangedTogether);
                filePairs.add(filePairChangedTogether);
            }

            filePairChangedTogether.getCommits().add(commitId);

            if (shouldUpdateLatestDate(date, filePairChangedTogether)) {
                filePairChangedTogether.setLatestCommit(date);
            }
        }
    }

    private static class ResolvedFile {
        final SourceFile sourceFile;
        final String lowerCasePath;
        final int commitsCount;

        ResolvedFile(SourceFile sourceFile, String lowerCasePath, int commitsCount) {
            this.sourceFile = sourceFile;
            this.lowerCasePath = lowerCasePath;
            this.commitsCount = commitsCount;
        }
    }

    private boolean shouldUpdateLatestDate(String date, FilePairChangedTogether filePairChangedTogether) {
        return StringUtils.isBlank(filePairChangedTogether.getLatestCommit()) ||
                date.compareTo(filePairChangedTogether.getLatestCommit()) > 0;
    }

    public List<FilePairChangedTogether> getFilePairsList() {
        return filePairsList;
    }
}
