package nl.obren.sokrates.cli;

import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.io.JsonMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * {@code _sokrates/post-analysis.json}: what the -postAnalysis hook last ran on this analysis —
 * the command, the head commit of the source tree at that time, the date and the exit code. It
 * travels with the kept analysis, and the next run skips the hook when the head commit and the
 * command are unchanged and the last run succeeded (an agent scan is minutes of work per
 * repository, so a nightly landscape must not redo untouched repositories). -aiForce overrides.
 */
public class PostAnalysisState {
    public static final String FILE_NAME = "post-analysis.json";

    private String command = "";
    private String head = "";
    private String ranOn = "";
    private int exitCode = 0;

    public PostAnalysisState() {
    }

    public PostAnalysisState(String command, String head, String ranOn, int exitCode) {
        this.command = command;
        this.head = StringUtils.defaultString(head);
        this.ranOn = ranOn;
        this.exitCode = exitCode;
    }

    /** The state kept in {@code folder}, or null when there is none (or it is unreadable). */
    public static PostAnalysisState read(File folder) {
        File file = new File(folder, FILE_NAME);
        if (!file.exists()) {
            return null;
        }
        try {
            return (PostAnalysisState) new JsonMapper().getObject(FileUtils.readFileToString(file, StandardCharsets.UTF_8), PostAnalysisState.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void save(File folder) throws IOException {
        FileUtils.write(new File(folder, FILE_NAME), new JsonGenerator().generate(this), StandardCharsets.UTF_8);
    }

    /** True when running {@code command} on a tree at {@code head} again would redo what this state records. */
    public boolean covers(String command, String head) {
        return exitCode == 0 && StringUtils.isNotBlank(head) && head.equals(this.head) && StringUtils.equals(command, this.command);
    }

    /** The HEAD commit of the git repository at {@code root}, or "" when it is not one (then nothing is ever skipped). */
    public static String headCommit(File root) {
        File gitDir = new File(root, ".git");
        if (!gitDir.exists()) {
            return "";
        }
        try (Repository repository = new FileRepositoryBuilder().setGitDir(gitDir).setMustExist(true).build()) {
            ObjectId head = repository.resolve("HEAD");
            return head != null ? head.name() : "";
        } catch (Exception e) {
            return "";
        }
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getHead() {
        return head;
    }

    public void setHead(String head) {
        this.head = head;
    }

    public String getRanOn() {
        return ranOn;
    }

    public void setRanOn(String ranOn) {
        this.ranOn = ranOn;
    }

    public int getExitCode() {
        return exitCode;
    }

    public void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }
}
