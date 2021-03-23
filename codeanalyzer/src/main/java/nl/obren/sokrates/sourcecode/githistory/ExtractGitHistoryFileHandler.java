package nl.obren.sokrates.sourcecode.githistory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ExtractGitHistoryFileHandler {
    public void extractSubHistory(File gitHistoryFile, String prefix) throws IOException {
        File folder = gitHistoryFile.getParentFile();
        File splitFolder = new File(folder, prefix);
        splitFolder.mkdirs();

        String gitHistoryContent = FileUtils.readFileToString(gitHistoryFile, StandardCharsets.UTF_8);
        String splitContent = extractSubHistory(gitHistoryContent, prefix);

        FileUtils.writeStringToFile(new File(splitFolder, gitHistoryFile.getName()), splitContent, StandardCharsets.UTF_8);

        System.out.println("Extracted git history to " + new File(splitFolder, gitHistoryFile.getName()).getPath());
    }

    public String extractSubHistory(String gitHistoryContent, String prefix) {
        List<String> originalLines = Arrays.asList(gitHistoryContent.split("\n"));
        List<String> lines = new ArrayList<>();

        originalLines.forEach(line -> {
            String elements[] = line.split(" ");
            if (elements.length >= 4) {
                String path = elements[3];
                if (path.startsWith(prefix)) {
                    elements[3] = StringUtils.removeStart(path.substring(prefix.length()), "/");
                    lines.add(Arrays.asList(elements).stream().collect(Collectors.joining(" ")));
                }
            }
        });

        return lines.stream().collect(Collectors.joining("\n"));
    }
}
