package nl.obren.sokrates.sourcecode.githistory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ExtractGitHistoryFileHandler {
    private static final Log LOG = LogFactory.getLog(ExtractGitHistoryFileHandler.class);

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

        // Copy the optional commit-messages sidecar along: it is keyed by sha, so the (superset)
        // map stays valid for the split history; older extractions simply have no sidecar.
        copySidecar(folder, splitFolder, GitHistoryUtils.GIT_COMMITS_FILE_NAME);
        copySidecar(folder, splitFolder, GitHistoryUtils.GIT_COMMIT_TRAILERS_FILE_NAME);

        LOG.info("Extracted git history to " + new File(splitFolder, gitHistoryFile.getName()).getPath());
    }

    private void copySidecar(File folder, File splitFolder, String fileName) throws IOException {
        File sidecar = new File(folder, fileName);
        if (sidecar.exists()) {
            FileUtils.copyFile(sidecar, new File(splitFolder, fileName));
        }
    }

    public List<String> extractSubHistory(List<String> originalLines, String prefix) {
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
