/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.age.utils;

import nl.obren.sokrates.sourcecode.age.FileModificationHistory;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Assumes that you have generated the text file being read iusing the following git command:
 * git ls-files -z | xargs -0 -n1 -I{} -- git log --format="%ai {}" {} > git-history.txt
 */
public class GitLsFileUtil {
    private final static int DATE_PATH_SEPARATION_POISTION = 26;

    public static void main(String args[]) throws IOException {
        GitLsFileUtil util = new GitLsFileUtil();

        List<FileModificationHistory> fileInfos = util.importGitLsFilesExport(new File("/Users/zeljkoobrenovic/Documents/workspace/sokrates/changes.txt"));
    }

    public static String printGitLogCommand() {
        return "git ls-files -z | xargs -0 -n1 -I{} -- git log --format=\"%ai {}\" {} > git-history.txt";
    }

    public static List<FileModificationHistory> importGitLsFilesExport(File file) {
        List<FileModificationHistory> files = new ArrayList<>();

        List<String> lines;
        try {
            lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return files;
        }

        Map<String, FileModificationHistory> map = new HashMap<>();

        lines.stream().filter(line -> line.length() > DATE_PATH_SEPARATION_POISTION).forEach(line -> {
            String path = line.substring(DATE_PATH_SEPARATION_POISTION).trim();
            FileModificationHistory fileInfo = map.get(path);
            if (fileInfo == null) {
                fileInfo = new FileModificationHistory(path);
                files.add(fileInfo);
                map.put(path, fileInfo);
            }

            String lastModifiedDate = line.substring(0, DATE_PATH_SEPARATION_POISTION).trim();
            fileInfo.getDates().add(lastModifiedDate);
        });

        return files;
    }
}
