/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.filehistory;

import nl.obren.sokrates.sourcecode.githistory.FileUpdate;
import nl.obren.sokrates.sourcecode.githistory.GitHistoryUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GitHistoryUtil {
    List<FileModificationHistory> files = new ArrayList<>();
    Map<String, FileModificationHistory> map = new HashMap<>();

    public List<FileModificationHistory> importGitLsFilesExport(File file, List<String> ignoreContributors) {
        files = new ArrayList<>();
        map = new HashMap<>();
        GitHistoryUtils.getHistoryFromFile(file)
                .stream()
                .filter(fileUpdate -> !GitHistoryUtils.shouldIgnore(fileUpdate.getAuthorEmail(), ignoreContributors))
                .forEach(fileUpdate -> {
                    processUpdate(fileUpdate);
                });
        return files;
    }

    private void processUpdate(FileUpdate fileUpdate) {
        String path = fileUpdate.getPath();
        FileModificationHistory fileInfo = map.get(path);
        if (fileInfo == null) {
            fileInfo = new FileModificationHistory(path);
            files.add(fileInfo);
            map.put(path, fileInfo);
        }

        CommitInfo commitInfo = new CommitInfo(fileUpdate.getCommitId(), fileUpdate.getDate());
        String authorEmail = fileUpdate.getAuthorEmail();
        commitInfo.setEmail(authorEmail);
        fileInfo.getCommits().add(commitInfo);

        String lastModifiedDate = fileUpdate.getDate();
        if (!fileInfo.getDates().contains(lastModifiedDate)) {
            fileInfo.getDates().add(lastModifiedDate);
        }
    }
}
