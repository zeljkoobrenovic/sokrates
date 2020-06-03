/*
 * Copyright (c) 2020 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.age.utils;

import nl.obren.sokrates.sourcecode.age.FileLastModifiedInfo;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/*
 * Assumes that you have generated the text file being read iusing the following git command:
 * git ls-files -z | xargs -0 -n1 -I{} -- git log -1 --format="%ai {}" {} > changes.txt
 */
public class GitLsFileUtil {
    public static void main(String args[]) throws IOException {
        GitLsFileUtil util = new GitLsFileUtil();

        List<FileLastModifiedInfo> fileInfos = util.importGitLsFilesExport(new File("/Users/zeljkoobrenovic/Documents/workspace/sokrates/changes.txt"));
    }

    public static String printGitCommand() {
        return "git ls-files -z | xargs -0 -n1 -I{} -- git log -1 --format=\"%ai {}\" {} > changes.txt";
    }

    public static List<FileLastModifiedInfo> importGitLsFilesExport(File file) {
        List<FileLastModifiedInfo> files = new ArrayList<>();

        List<String> lines;
        try {
            lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return files;
        }

        lines.stream().filter(line -> line.length() > 26).forEach(line -> {
            String lastModifiedDate = line.substring(0, 26).trim();
            String path = line.substring(26).trim();
            FileLastModifiedInfo fileInfo = new FileLastModifiedInfo(lastModifiedDate, path);
            files.add(fileInfo);

            System.out.println(fileInfo.ageInDays());
        });

        return files;
    }
}
