/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.age;

import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;

import java.util.*;

public class FilePairsChangedTogether {
    private Map<String, FilePairChangedTogether> filePairsMap = new HashMap<>();
    private List<FilePairChangedTogether> filePairs = new ArrayList<>();

    public void populate(NamedSourceCodeAspect aspect, List<FileModificationHistory> fileHistories) {
        Map<String, List<SourceFile>> datesMap = new HashMap<>();

        fileHistories.forEach(fileHistory -> {
            fileHistory.getDates().forEach(dateTime -> {
                if (dateTime.length() >= 10) {
                    SourceFile sourceFile = aspect.getSourceFileByPath(fileHistory.getPath());
                    if (sourceFile != null) {
                        String date = dateTime.substring(0, 10);
                        List<SourceFile> list = datesMap.get(date);
                        if (list == null) {
                            list = new ArrayList<>();
                            datesMap.put(date, list);
                            list.add(sourceFile);
                        } else if (!list.contains(sourceFile)) {
                            list.forEach(sourceFileOnSameDate -> addFilePair(sourceFile, sourceFileOnSameDate, date));
                            list.add(sourceFile);
                        }
                    }
                }
            });
        });

        Collections.sort(filePairs, (a, b) -> b.getDates().size() - a.getDates().size());
    }

    public void addFilePair(SourceFile sourceFile1, SourceFile sourceFile2, String date) {
        String path1 = sourceFile1.getRelativePath().toLowerCase();
        String path2 = sourceFile2.getRelativePath().toLowerCase();

        String key1 = path1 + "_" + path2;
        String key2 = path2 + "_" + path1;

        System.out.println(key1);

        FilePairChangedTogether filePairChangedTogether = filePairsMap.get(key1);
        if (filePairChangedTogether == null) {
            filePairChangedTogether = filePairsMap.get(key2);
        }

        if (filePairChangedTogether == null) {
            filePairChangedTogether = new FilePairChangedTogether(sourceFile1, sourceFile2);
            filePairsMap.put(key1, filePairChangedTogether);
            filePairs.add(filePairChangedTogether);
        }

        filePairChangedTogether.getDates().add(date);
    }

    public List<FilePairChangedTogether> getFilePairs() {
        return filePairs;
    }

    public void setFilePairs(List<FilePairChangedTogether> filePairs) {
        this.filePairs = filePairs;
    }
}
