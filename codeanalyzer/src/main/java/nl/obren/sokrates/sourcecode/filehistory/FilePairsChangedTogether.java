/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.filehistory;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class FilePairsChangedTogether {
    private Map<String, FilePairChangedTogether> filePairsMap = new HashMap<>();
    private List<FilePairChangedTogether> filePairs = new ArrayList<>();
    private int rangeInDays = -1;

    public FilePairsChangedTogether(int rangeInDays) {
        this.rangeInDays = rangeInDays;
    }

    public void populate(NamedSourceCodeAspect aspect, List<FileModificationHistory> fileHistories) {
        Map<String, List<FileModificationHistory>> commitsIdMap = new HashMap<>();

        fileHistories.forEach(fileHistory -> {
            fileHistory.getCommits().forEach(commitInfo -> {
                if (rangeInDays <= 0 || DateUtils.isDateWithinRange(commitInfo.getDate(), rangeInDays)) {
                    String commitId = commitInfo.getId();
                    String commitDate = commitInfo.getDate();
                    List<FileModificationHistory> list = commitsIdMap.get(commitId);
                    if (list == null) {
                        list = new ArrayList<>();
                        commitsIdMap.put(commitId, list);
                        list.add(fileHistory);
                    } else if (!list.contains(fileHistory)) {
                        list.forEach(sourceFileInSameCommit -> addFilePair(aspect, fileHistory,
                                sourceFileInSameCommit, commitId, commitDate));
                        list.add(fileHistory);
                    }
                }
            });
        });

        Collections.sort(filePairs, (a, b) -> b.getCommits().size() - a.getCommits().size());
    }

    private void addFilePair(NamedSourceCodeAspect aspect, FileModificationHistory fileHistory1, FileModificationHistory fileHistory2, String commitId, String date) {
        SourceFile sourceFile1 = aspect.getSourceFileByPath(fileHistory1.getPath());
        SourceFile sourceFile2 = aspect.getSourceFileByPath(fileHistory2.getPath());

        if (sourceFile1 != null && sourceFile2 != null) {
            String path1 = sourceFile1.getRelativePath().toLowerCase();
            String path2 = sourceFile2.getRelativePath().toLowerCase();

            String key1 = path1 + "_" + path2;
            String key2 = path2 + "_" + path1;

            FilePairChangedTogether filePairChangedTogether = filePairsMap.get(key1);
            if (filePairChangedTogether == null) {
                filePairChangedTogether = filePairsMap.get(key2);
            }

            if (filePairChangedTogether == null) {
                filePairChangedTogether = new FilePairChangedTogether(sourceFile1, sourceFile2);

                filePairChangedTogether.setCommitsCountFile1(fileHistory1.getCommits().size());
                filePairChangedTogether.setCommitsCountFile2(fileHistory2.getCommits().size());

                filePairsMap.put(key1, filePairChangedTogether);
                filePairs.add(filePairChangedTogether);
            }

            filePairChangedTogether.getCommits().add(commitId);

            if (shouldUpdateLatestDate(date, filePairChangedTogether)) {
                filePairChangedTogether.setLatestCommit(date);
            }
        }
    }

    private boolean shouldUpdateLatestDate(String date, FilePairChangedTogether filePairChangedTogether) {
        return StringUtils.isBlank(filePairChangedTogether.getLatestCommit()) ||
                date.compareTo(filePairChangedTogether.getLatestCommit()) > 0;
    }

    public List<FilePairChangedTogether> getFilePairs() {
        return filePairs;
    }

    public void setFilePairs(List<FilePairChangedTogether> filePairs) {
        this.filePairs = filePairs;
    }
}
