package nl.obren.sokrates.cli;

import nl.obren.sokrates.common.utils.RegexUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SokratesFileUtils {
    public static final String GIT_HISTORY_FILE_NAME = "git-history.txt";

    public static void extractFiles(File srcRoot, File dest, File parentDest, String pattern) throws IOException {
        dest.mkdirs();
        Path rootPath = Paths.get(srcRoot.getPath());
        FileUtils.deleteDirectory(dest);
        dest.mkdirs();
        int counter[] = {0};
        SokratesFileUtils.listFiles(pattern, rootPath).forEach(path -> {
            counter[0] += 1;
            File srcFile = path.toFile();
            String relPath = rootPath.relativize(path).toFile().getPath();
            File destFile = new File(dest, relPath);
            System.out.println(counter[0] + ". " + relPath);
            try {
                FileUtils.copyFile(srcFile, destFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        File gitHistoryFile = new File(srcRoot, GIT_HISTORY_FILE_NAME);
        if (gitHistoryFile.exists()) {
            if (parentDest != null && !parentDest.equals(dest)) {
                String relativize = Paths.get(parentDest.getAbsolutePath()).relativize(Paths.get(dest.getAbsolutePath())).toString();
                System.out.println(relativize);
                extractSubHistory(gitHistoryFile, pattern, parentDest, relativize + "/");
            } else {
                extractSubHistory(gitHistoryFile, pattern, dest, "");
            }
        }
    }

    public static List<Path> listFiles(String pattern, Path path) throws IOException {
        List<Path> result;
        try (Stream<Path> walk = Files.walk(path)) {
            result = walk.filter(Files::isRegularFile)
                    .filter(f -> RegexUtils.matchesEntirely(pattern, f.toFile().getPath()))
                    .collect(Collectors.toList());
        }
        return result;
    }

    public static void extractSubHistory(File gitHistoryFile, String pattern, File splitFolder, String addPrefix) throws IOException {
        String gitHistoryContent = FileUtils.readFileToString(gitHistoryFile, StandardCharsets.UTF_8);
        String splitContent = extractSubHistory(gitHistoryContent, pattern, addPrefix);

        FileUtils.writeStringToFile(new File(splitFolder, gitHistoryFile.getName()), splitContent, StandardCharsets.UTF_8);

        System.out.println("Extracted git history to " + new File(splitFolder, gitHistoryFile.getName()).getPath());
    }

    public static String extractSubHistory(String gitHistoryContent, String pattern, String addPrefix) {
        List<String> originalLines = Arrays.asList(gitHistoryContent.split("\n"));
        List<String> lines = new ArrayList<>();

        originalLines.forEach(line -> {
            String elements[] = line.split(" ");
            if (elements.length >= 4) {
                String path = elements[3];
                if (RegexUtils.matchesEntirely(pattern, path)) {
                    elements[3] = addPrefix + elements[3];
                    lines.add(Arrays.asList(elements).stream().collect(Collectors.joining(" ")));
                }
            }
        });

        return lines.stream().collect(Collectors.joining("\n"));
    }
}
