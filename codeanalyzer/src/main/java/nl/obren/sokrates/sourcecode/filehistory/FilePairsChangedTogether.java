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
    public static final int PAIR_LIST_LIMIT = 10000000;

    // A commit touching N files yields N*(N-1)/2 pairs, so this map can hold millions of entries. It is
    // keyed by the two files' indexes packed into one long (not by a "path1_path2" string) and is
    // dropped after populate(); only the sorted list is kept.
    private Map<Long, FilePairChangedTogether> filePairsMap = new LinkedHashMap<>();
    private List<FilePairChangedTogether> filePairsList = new ArrayList<>();
    private int rangeInDays = -1;
    private int maxFilesPerCommit = -1;
    private boolean limitReported = false;
    private int skippedCommitsCount = 0;

    public FilePairsChangedTogether(int rangeInDays) {
        this(rangeInDays, -1);
    }

    /**
     * @param maxFilesPerCommit commits touching more files of the aspect (within the range) than this are
     *                          left out; 0 or less means no limit
     */
    public FilePairsChangedTogether(int rangeInDays, int maxFilesPerCommit) {
        this.rangeInDays = rangeInDays;
        this.maxFilesPerCommit = maxFilesPerCommit;
    }

    public void populate(NamedSourceCodeAspect aspect, List<FileModificationHistory> fileHistories) {
        // Resolve each modification history to its SourceFile once, up front, instead of re-resolving on
        // every generated pair. Histories not part of the aspect are dropped here. Files are numbered by
        // their lower-cased path, so histories whose paths differ only in case still form one pair key.
        List<ResolvedFile> resolvedFiles = new ArrayList<>();
        Map<String, Integer> indexByLowerCasePath = new HashMap<>();
        fileHistories.forEach(fileHistory -> {
            SourceFile sourceFile = aspect.getSourceFileByPath(fileHistory.getPath());
            if (sourceFile != null) {
                int index = indexByLowerCasePath.computeIfAbsent(sourceFile.getRelativePath().toLowerCase(),
                        k -> indexByLowerCasePath.size());
                resolvedFiles.add(new ResolvedFile(fileHistory, sourceFile, index, fileHistory.getCommits().size()));
            }
        });

        Set<String> skippedCommits = commitsWithTooManyFiles(resolvedFiles);
        skippedCommitsCount = skippedCommits.size();
        if (skippedCommitsCount > 0) {
            LOG.info("Leaving " + skippedCommitsCount + " commit(s) with more than " + maxFilesPerCommit
                    + " files out of the files changed together"
                    + (rangeInDays > 0 ? " (past " + rangeInDays + " days)" : ""));
        }

        // commit id -> the files (positions in resolvedFiles) seen in that commit so far
        Map<String, IntList> filesPerCommit = new HashMap<>();

        for (int position = 0; position < resolvedFiles.size(); position++) {
            ResolvedFile resolvedFile = resolvedFiles.get(position);
            for (CommitInfo commitInfo : resolvedFile.history.getCommits()) {
                if (filePairsMap.size() < PAIR_LIST_LIMIT && isInRange(commitInfo)) {
                    String commitId = commitInfo.getId();
                    if (skippedCommits.contains(commitId)) {
                        continue;
                    }
                    IntList filesInCommit = filesPerCommit.computeIfAbsent(commitId, k -> new IntList());
                    // files are added in position order, so a file that lists the same commit twice is
                    // already the last entry of that commit's list
                    if (!filesInCommit.endsWith(position)) {
                        for (int i = 0; i < filesInCommit.size(); i++) {
                            addFilePair(resolvedFile, resolvedFiles.get(filesInCommit.get(i)), commitId, commitInfo.getDate());
                        }
                        filesInCommit.add(position);
                    }
                }
            }
        }

        filePairsList = new ArrayList<>(filePairsMap.values());
        filePairsMap = new LinkedHashMap<>();
        filePairsList.forEach(pair -> {
            if (pair.getCommits() instanceof ArrayList) {
                ((ArrayList<String>) pair.getCommits()).trimToSize();
            }
        });
        Collections.sort(filePairsList, (a, b) -> b.getCommits().size() - a.getCommits().size());
    }

    private boolean isInRange(CommitInfo commitInfo) {
        return rangeInDays <= 0 || DateUtils.isDateWithinRange(commitInfo.getDate(), rangeInDays);
    }

    // The commits touching more than maxFilesPerCommit distinct files (counted like the pairs: files of
    // the aspect, commits within the range).
    private Set<String> commitsWithTooManyFiles(List<ResolvedFile> resolvedFiles) {
        Set<String> commits = new HashSet<>();
        if (maxFilesPerCommit <= 0) {
            return commits;
        }
        // commit id -> {number of files, position of the last counted file}
        Map<String, int[]> filesCountPerCommit = new HashMap<>();
        for (int position = 0; position < resolvedFiles.size(); position++) {
            for (CommitInfo commitInfo : resolvedFiles.get(position).history.getCommits()) {
                if (isInRange(commitInfo)) {
                    int[] count = filesCountPerCommit.computeIfAbsent(commitInfo.getId(), k -> new int[]{0, -1});
                    if (count[1] != position) {
                        count[0]++;
                        count[1] = position;
                        if (count[0] > maxFilesPerCommit) {
                            commits.add(commitInfo.getId());
                        }
                    }
                }
            }
        }
        return commits;
    }

    /**
     * The number of commits that populate() left out because they touched more than maxFilesPerCommit files.
     */
    public int getSkippedCommitsCount() {
        return skippedCommitsCount;
    }

    private void addFilePair(ResolvedFile file1, ResolvedFile file2, String commitId, String date) {
        long key = Math.min(file1.index, file2.index) * 0x100000000L + Math.max(file1.index, file2.index);

        FilePairChangedTogether filePairChangedTogether = filePairsMap.get(key);

        if (filePairChangedTogether == null) {
            if (filePairsMap.size() >= PAIR_LIST_LIMIT) {
                if (!limitReported) {
                    limitReported = true;
                    LOG.info("Reached the limit of " + PAIR_LIST_LIMIT + " file pairs changed together; further pairs are ignored");
                }
                return;
            }
            filePairChangedTogether = new FilePairChangedTogether(file1.sourceFile, file2.sourceFile);
            // most pairs share just one commit, so start small rather than with ArrayList's default 10 slots
            filePairChangedTogether.setCommits(new ArrayList<>(1));

            filePairChangedTogether.setCommitsCountFile1(file1.commitsCount);
            filePairChangedTogether.setCommitsCountFile2(file2.commitsCount);

            filePairsMap.put(key, filePairChangedTogether);
        }

        filePairChangedTogether.getCommits().add(commitId);

        if (shouldUpdateLatestDate(date, filePairChangedTogether)) {
            filePairChangedTogether.setLatestCommit(date);
        }
    }

    private static class ResolvedFile {
        final FileModificationHistory history;
        final SourceFile sourceFile;
        final int index;
        final int commitsCount;

        ResolvedFile(FileModificationHistory history, SourceFile sourceFile, int index, int commitsCount) {
            this.history = history;
            this.sourceFile = sourceFile;
            this.index = index;
            this.commitsCount = commitsCount;
        }
    }

    // A growable int array: one entry per (commit, file), so boxed Integers or a HashSet per commit
    // would cost several times the memory.
    private static class IntList {
        private int[] values = new int[2];
        private int size = 0;

        void add(int value) {
            if (size == values.length) {
                values = Arrays.copyOf(values, size * 2);
            }
            values[size++] = value;
        }

        int get(int i) {
            return values[i];
        }

        int size() {
            return size;
        }

        boolean endsWith(int value) {
            return size > 0 && values[size - 1] == value;
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
