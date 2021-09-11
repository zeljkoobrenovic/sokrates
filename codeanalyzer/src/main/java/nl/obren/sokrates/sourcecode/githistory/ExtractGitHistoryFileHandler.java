package nl.obren.sokrates.sourcecode.githistory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
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

        List<String> originalLines = new ArrayList<>();
        BufferedReader in = new BufferedReader(new FileReader(gitHistoryFile));
        String line;
        while ((line = in.readLine()) != null) {
            originalLines.add(line);
        }
        List<String> splitContent = extractSubHistory(originalLines, prefix);

        FileUtils.writeLines(new File(splitFolder, gitHistoryFile.getName()), splitContent);

        System.out.println("Extracted git history to " + new File(splitFolder, gitHistoryFile.getName()).getPath());
    }

    public  List<String> extractSubHistory(List<String> originalLines, String prefix) {
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

        return lines;
    }
}
