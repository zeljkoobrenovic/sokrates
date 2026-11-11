package nl.obren.sokrates.cli.skills;

import nl.obren.sokrates.cli.git.GitRepoCloner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Installs the sokrates-skills (the AI agent skills that configure Sokrates, scan a finished analysis and act on
 * it) into the skills folders of the agent CLIs, so that {@code -ai claude|codex|gemini} finds them without a
 * second repository to clone by hand. The skills repository is cloned (JGit, no git binary) into a cache folder
 * and updated on every run; each skill folder — a folder holding a {@code SKILL.md} under {@code skills/},
 * one or two levels deep — is symlinked into every target (a link follows later updates of the cache), or
 * copied with {@code copy} where links are not wanted (a Docker volume, Windows without symlink rights).
 * A local folder can be the source too (a checkout of the skills repository), in which case nothing is cloned.
 */
public class SkillsInstaller {
    public static final String DEFAULT_SOURCE = "https://github.com/zeljkoobrenovic/sokrates-skills.git";
    public static final String DEFAULT_REF = "main";
    public static final String ENTRY_SKILL = "sokrates";

    private static final Log LOG = LogFactory.getLog(SkillsInstaller.class);

    public static File defaultCacheFolder() {
        return new File(new File(FileUtils.getUserDirectory(), ".sokrates"), "skills/sokrates-skills");
    }

    /** Claude Code reads ~/.claude/skills; Codex, Gemini CLI, Cursor, Copilot and others read ~/.agents/skills. */
    public static List<File> defaultTargets() {
        File home = FileUtils.getUserDirectory();
        return Arrays.asList(new File(home, ".claude/skills"), new File(home, ".agents/skills"));
    }

    /** The project-level folders the same tools read, relative to a project root. */
    public static List<File> projectTargets(File project) {
        return Arrays.asList(new File(project, ".claude/skills"), new File(project, ".agents/skills"));
    }

    /** The agent's personal skills folder, by preset name; null for an unknown agent. */
    public static File skillsFolderOf(String agent) {
        switch (StringUtils.defaultString(agent).trim().toLowerCase()) {
            case "claude":
                return new File(FileUtils.getUserDirectory(), ".claude/skills");
            case "codex":
            case "gemini":
                return new File(FileUtils.getUserDirectory(), ".agents/skills");
            default:
                return null;
        }
    }

    /** A hint for the log when the entry skill is not installed for the agent {@code -ai} is about to run; null when it is. */
    public static String missingSkillsHint(String agent) {
        File folder = skillsFolderOf(agent);
        if (folder == null || new File(folder, ENTRY_SKILL).exists()) {
            return null;
        }
        return "The sokrates skills are not installed for " + agent + " (" + new File(folder, ENTRY_SKILL).getPath()
                + " is missing): the agent will work without them, but it is much better with them — run `sokrates installSkills` once.";
    }

    /**
     * The folder holding the skills: {@code source} itself when it is a local checkout (a folder with a
     * {@code skills/} subfolder), else the git clone of {@code source} at {@code ref} in {@code cache}, updated when
     * it already exists.
     */
    public File fetch(String source, String ref, File cache) throws GitAPIException, IOException {
        File local = new File(source);
        if (local.isDirectory() && new File(local, "skills").isDirectory()) {
            LOG.info("Using the local skills checkout " + local.getPath());
            return local;
        }
        return new GitRepoCloner().cloneOrUpdate(source, cache, StringUtils.defaultIfBlank(ref, DEFAULT_REF), 1);
    }

    /** Every skill folder under {@code root/skills}: a folder with a SKILL.md, directly or one level deeper, sorted by name. */
    public static List<File> findSkills(File root) {
        List<File> skills = new ArrayList<>();
        File[] families = new File(root, "skills").listFiles(File::isDirectory);
        if (families == null) {
            return skills;
        }
        for (File family : families) {
            if (new File(family, "SKILL.md").isFile()) {
                skills.add(family);
                continue;
            }
            File[] children = family.listFiles(File::isDirectory);
            if (children != null) {
                for (File child : children) {
                    if (new File(child, "SKILL.md").isFile()) {
                        skills.add(child);
                    }
                }
            }
        }
        skills.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return skills;
    }

    /**
     * Links (or copies) every skill into {@code target} under the skill's name and returns what was installed. An
     * existing link is replaced; an existing real folder is replaced only when copying (a link would silently
     * shadow someone's own skill of the same name), otherwise it is left alone and reported.
     */
    public List<File> installInto(List<File> skills, File target, boolean copy) throws IOException {
        FileUtils.forceMkdir(target);
        List<File> installed = new ArrayList<>();
        for (File skill : skills) {
            File destination = new File(target, skill.getName());
            Path path = destination.toPath();
            if (Files.isSymbolicLink(path)) {
                Files.delete(path);
            } else if (destination.exists()) {
                if (!copy) {
                    LOG.warn("Skipping " + destination.getPath() + ": a folder of that name exists and is not a link (use -copy to replace it)");
                    continue;
                }
                FileUtils.deleteDirectory(destination);
            }
            if (copy) {
                FileUtils.copyDirectory(skill, destination);
            } else {
                try {
                    Files.createSymbolicLink(path, skill.getAbsoluteFile().toPath());
                } catch (IOException | UnsupportedOperationException e) {
                    LOG.warn("Cannot link " + destination.getPath() + " (" + e.getMessage() + "): copying instead");
                    FileUtils.copyDirectory(skill, destination);
                }
            }
            installed.add(destination);
        }
        return installed;
    }
}
