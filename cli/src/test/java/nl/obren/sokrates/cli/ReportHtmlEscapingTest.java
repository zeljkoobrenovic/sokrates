package nl.obren.sokrates.cli;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end regression test: analyzes a fixture repository whose file names, folder names, unit
 * names and git author identity carry HTML metacharacters, then checks that none of them reach a
 * generated HTML report as live markup — while their escaped forms do appear, which proves the
 * fixture really flowed through the reports rather than being skipped.
 */
class ReportHtmlEscapingTest {

    // Repository-controlled strings that used to be emitted raw.
    private static final String FILE_NAME_PAYLOAD = "<img src=x onerror=XSSNAME>.ts";
    private static final String FOLDER_PAYLOAD = "pwn\" onmouseover=\"XSSATTR";
    private static final String AUTHOR_NAME_PAYLOAD = "pwn' onmouseover='XSSAUTHOR";
    private static final String AUTHOR_EMAIL_PAYLOAD = "x\"y'z@example.com";
    private static final String SECOND_AUTHOR_EMAIL = "second@example.com";

    @Test
    void repositoryControlledStringsAreEscapedInEveryHtmlReport(@TempDir Path tmp) throws IOException {
        // The fixture's file names carry < > " which NTFS does not allow; the sinks under test are
        // platform-independent, so the check on other systems covers Windows too.
        Assumptions.assumeFalse(SystemUtils.IS_OS_WINDOWS, "fixture file names are not legal on Windows");
        File repo = tmp.resolve("repo").toFile();
        writeFixture(repo);

        CommandLineInterface cli = new CommandLineInterface();
        File configFile = new File(repo, "_sokrates/config.json");
        cli.run(new String[]{"init", "-srcRoot", repo.getPath(), "-confFile", configFile.getPath()});
        assertTrue(configFile.exists(), "init should have written " + configFile);
        // Depth 2 makes the payload folder (src/pwn" onmouseover="XSSATTR) a logical component, so the
        // Components report, the scope charts and the concern-per-component charts see it.
        String config = FileUtils.readFileToString(configFile, UTF_8);
        assertTrue(config.contains("\"componentsFolderDepth\" : 1"), "expected the default depth-1 decomposition in " + configFile);
        FileUtils.write(configFile, config.replace("\"componentsFolderDepth\" : 1", "\"componentsFolderDepth\" : 2"), UTF_8);
        File reportsFolder = new File(repo, "_sokrates/reports");
        cli.run(new String[]{"generateReports", "-confFile", configFile.getPath(), "-outputFolder", reportsFolder.getPath()});

        List<File> htmlFiles;
        try (Stream<Path> paths = Files.walk(reportsFolder.toPath())) {
            htmlFiles = paths.filter(p -> p.toString().endsWith(".html")).map(Path::toFile).collect(Collectors.toList());
        }
        assertTrue(htmlFiles.size() > 10, "expected a full set of reports, got " + htmlFiles.size());

        // The sweep detects verbatim copies of a payload only: a sink that transforms the string
        // first (case, abbreviation, safe-file-name) or embeds it compressed (the client-rendered
        // explorers, viewer and visuals) is invisible to it, which is what the unit tests are for.
        for (File html : htmlFiles) {
            String content = FileUtils.readFileToString(html, UTF_8);
            String where = html.getPath().substring(reportsFolder.getPath().length());
            assertFalse(content.contains(FILE_NAME_PAYLOAD), "raw file name in " + where);
            assertFalse(content.contains(FOLDER_PAYLOAD), "raw folder name in " + where);
            assertFalse(content.contains(AUTHOR_NAME_PAYLOAD), "raw author name in " + where);
            assertFalse(content.contains(AUTHOR_EMAIL_PAYLOAD), "raw author email in " + where);
        }

        // The escaped forms must be there: a fixture that never reached the reports would pass the
        // negative checks vacuously.
        String fileSize = read(reportsFolder, "html/FileSize.html");
        assertTrue(fileSize.contains("&lt;img src=x onerror=XSSNAME&gt;.ts"), "escaped file name in FileSize.html");
        assertTrue(fileSize.contains("in src/pwn&quot; onmouseover=&quot;XSSATTR"), "escaped folder name in FileSize.html");
        assertTrue(fileSize.contains("href='../src/viewer.html#aspect=main&file=src/%3Cimg%20src%3Dx%20onerror%3DXSSNAME%3E.ts'"),
                "percent-encoded viewer link in FileSize.html");
        assertTrue(fileSize.contains("href='../src/viewer.html#aspect=main&file=src/odd%20%23%26%25%20dir/plus%2Bfile.ts'"),
                "percent-encoded viewer link for # & % + and spaces in FileSize.html");

        String fileChurn = read(reportsFolder, "html/FileChurn.html");
        assertTrue(fileChurn.contains("<title>src/&lt;img src=x onerror=XSSNAME&gt;.ts"), "escaped SVG point title in FileChurn.html");

        String unitSize = read(reportsFolder, "html/UnitSize.html");
        assertTrue(unitSize.contains("&lt;img src=x onerror=XSSNAME&gt;.ts"), "escaped file path in UnitSize.html");

        String duplication = read(reportsFolder, "html/Duplication.html");
        assertTrue(duplication.contains("&lt;img src=x onerror=XSSNAME&gt;.ts"), "escaped file name in Duplication.html");

        String contributors = read(reportsFolder, "html/Contributors.html");
        assertTrue(contributors.contains("pwn&#39; onmouseover=&#39;XSSAUTHOR"), "escaped author name in Contributors.html");
        assertTrue(contributors.contains("x&quot;y&#39;z@example.com"), "escaped author email in Contributors.html");
        assertTrue(contributors.contains("<br>src/&lt;img src=x onerror=XSSNAME&gt;.ts"), "escaped shared file path in the contributor dependencies of Contributors.html");

        String components = read(reportsFolder, "html/Components.html");
        assertTrue(components.contains("pwn&quot; onmouseover=&quot;XSSATTR"), "escaped component name in Components.html");
    }

    private static String read(File reportsFolder, String relativePath) throws IOException {
        File file = new File(reportsFolder, relativePath);
        assertTrue(file.exists(), "expected report " + relativePath);
        return FileUtils.readFileToString(file, UTF_8);
    }

    private static void writeFixture(File repo) throws IOException {
        File src = new File(repo, "src");
        File payloadFile = new File(src, FILE_NAME_PAYLOAD);
        File insideFile = new File(new File(src, FOLDER_PAYLOAD), "inside.ts");
        File oddFile = new File(new File(src, "odd #&% dir"), "plus+file.ts");
        File normalFile = new File(src, "normal.ts");

        // A block duplicated across two files so the Duplication report gets rows (and a
        // duplication-between-files graph).
        StringBuilder duplicated = new StringBuilder();
        for (int i = 1; i <= 9; i++) {
            duplicated.append("export function dup").append(i).append("(q: number): number {\n")
                    .append("  const r = q * ").append(i).append(";\n")
                    .append("  return r + ").append(i).append(";\n}\n");
        }
        FileUtils.write(payloadFile, "export function a(x: number): number { return x + 1; }\n" + duplicated, UTF_8);
        FileUtils.write(insideFile, "export function b(y: string): string { return y.trim(); }\n" + duplicated, UTF_8);
        FileUtils.write(oddFile, "export function gen<T>(v: T): T {\n  if (v) { return v; }\n  return v;\n}\n", UTF_8);
        FileUtils.write(normalFile, "export function c(): string { return \"ok\"; }\n", UTF_8);

        // git-history.txt as extractGitHistory writes it: "<date> <email> <sha> <path> <name> <added> <deleted>",
        // spaces inside the path and the author name written as &nbsp;.
        String date = LocalDate.now().minusDays(3).toString();
        String sha = "b76525ba4bbf55b38a6a18b6376c07eb62fbc4de";
        String author = AUTHOR_EMAIL_PAYLOAD + " " + sha;
        String name = AUTHOR_NAME_PAYLOAD.replace(" ", "&nbsp;");
        // A second author touching the same files in a second commit, so the two form a contributor
        // pair with shared files.
        String date2 = LocalDate.now().minusDays(2).toString();
        String author2 = SECOND_AUTHOR_EMAIL + " a1b2c3d4e5f60718293a4b5c6d7e8f9012345678";
        String history = String.join("\n",
                date + " " + author + " " + relative(repo, payloadFile).replace(" ", "&nbsp;") + " " + name + " 3 0",
                date + " " + author + " " + relative(repo, insideFile).replace(" ", "&nbsp;") + " " + name + " 3 0",
                date + " " + author + " " + relative(repo, oddFile).replace(" ", "&nbsp;") + " " + name + " 4 0",
                date + " " + author + " " + relative(repo, normalFile) + " " + name + " 1 0",
                date2 + " " + author2 + " " + relative(repo, payloadFile).replace(" ", "&nbsp;") + " Second 1 0",
                date2 + " " + author2 + " " + relative(repo, normalFile) + " Second 1 0",
                "") ;
        FileUtils.write(new File(repo, "git-history.txt"), history, UTF_8);
    }

    private static String relative(File repo, File file) {
        return repo.toPath().relativize(file.toPath()).toString().replace('\\', '/');
    }
}
