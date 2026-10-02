package nl.obren.sokrates.cli;

import nl.obren.sokrates.cli.skills.SkillsInstaller;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code installSkills} against a local skills checkout and a local bare git repository (no network): the
 * skill folders (a SKILL.md under skills/, one or two levels deep) are linked into every target, a re-run
 * updates the clone and picks up new skills, -copy copies, -listOnly writes nothing.
 */
class InstallSkillsCommandTest {

    private static File skillsCheckout(File root) throws Exception {
        for (String skill : new String[]{"skills/sokrates", "skills/scanners/tech-stack-scan", "skills/config/sokrates-repo-config"}) {
            FileUtils.write(new File(root, skill + "/SKILL.md"), "---\nname: " + new File(skill).getName() + "\n---\n", UTF_8);
            FileUtils.write(new File(root, skill + "/scripts/tool.py"), "print('hi')\n", UTF_8);
        }
        FileUtils.write(new File(root, "skills/scanners/README.md"), "not a skill\n", UTF_8);   // a family README is not a skill
        FileUtils.write(new File(root, "README.md"), "# skills\n", UTF_8);
        return root;
    }

    private static List<String> names(File folder) {
        File[] files = folder.listFiles();
        return files == null ? List.of() : java.util.Arrays.stream(files).map(File::getName).sorted().collect(Collectors.toList());
    }

    @Test
    void linksEverySkillOfALocalCheckoutIntoEveryTarget(@TempDir Path tmp) throws Exception {
        File checkout = skillsCheckout(tmp.resolve("sokrates-skills").toFile());
        File claude = tmp.resolve("claude-skills").toFile();
        File agents = tmp.resolve("agents-skills").toFile();

        new CommandLineInterface().run(new String[]{"installSkills", "-source", checkout.getPath(), "-target", claude.getPath(), "-target", agents.getPath()});

        assertEquals(List.of("sokrates", "sokrates-repo-config", "tech-stack-scan"), names(claude));
        assertEquals(names(claude), names(agents));
        Path link = new File(claude, "tech-stack-scan").toPath();
        assertTrue(Files.isSymbolicLink(link), "skills are linked, so a later update of the checkout is visible at once");
        assertEquals(new File(checkout, "skills/scanners/tech-stack-scan").getCanonicalPath(), link.toRealPath().toString());
        assertTrue(new File(claude, "sokrates/scripts/tool.py").isFile());
        assertFalse(new File(claude, "README.md").exists(), "a family README is not a skill");

        // Re-running replaces the links and leaves a real folder of the same name alone unless copying.
        FileUtils.deleteDirectory(new File(claude, "sokrates"));
        FileUtils.write(new File(claude, "sokrates/SKILL.md"), "my own\n", UTF_8);
        new CommandLineInterface().run(new String[]{"installSkills", "-source", checkout.getPath(), "-target", claude.getPath()});
        assertEquals("my own\n", FileUtils.readFileToString(new File(claude, "sokrates/SKILL.md"), UTF_8), "a user's own folder is not overwritten by a link");
        assertTrue(Files.isSymbolicLink(new File(claude, "tech-stack-scan").toPath()));
    }

    @Test
    void copiesInsteadOfLinkingWithCopy(@TempDir Path tmp) throws Exception {
        File checkout = skillsCheckout(tmp.resolve("sokrates-skills").toFile());
        File target = tmp.resolve("target").toFile();
        new CommandLineInterface().run(new String[]{"installSkills", "-source", checkout.getPath(), "-target", target.getPath(), "-copy"});
        File skill = new File(target, "tech-stack-scan");
        assertTrue(skill.isDirectory() && !Files.isSymbolicLink(skill.toPath()));
        assertTrue(new File(skill, "scripts/tool.py").isFile());
    }

    @Test
    void clonesAGitSourceIntoTheCacheAndUpdatesItOnRerun(@TempDir Path tmp) throws Exception {
        File work = skillsCheckout(tmp.resolve("work").toFile());
        File bare = tmp.resolve("sokrates-skills.git").toFile();
        Git.init().setBare(true).setDirectory(bare).call().close();
        try (Git git = Git.init().setInitialBranch("main").setDirectory(work).call()) {
            git.add().addFilepattern(".").call();
            git.commit().setMessage("skills").setAuthor("Ada", "ada@example.com").setCommitter("Ada", "ada@example.com").call();
            git.remoteAdd().setName("origin").setUri(new org.eclipse.jgit.transport.URIish(bare.toURI().toString())).call();
            git.push().setRemote("origin").setPushAll().call();
        }
        File cache = tmp.resolve("cache/sokrates-skills").toFile();
        File target = tmp.resolve("target").toFile();
        String url = bare.toURI().toString();

        new CommandLineInterface().run(new String[]{"installSkills", "-source", url, "-ref", "main", "-cacheFolder", cache.getPath(), "-target", target.getPath()});
        assertTrue(new File(cache, ".git").isDirectory(), "the skills repository is cloned into the cache");
        assertEquals(List.of("sokrates", "sokrates-repo-config", "tech-stack-scan"), names(target));

        // A new skill published later arrives with the next run (fetch + reset in the cache).
        FileUtils.write(new File(work, "skills/improve/sokrates-improve/SKILL.md"), "---\nname: sokrates-improve\n---\n", UTF_8);
        try (Git git = Git.open(work)) {
            git.add().addFilepattern(".").call();
            git.commit().setMessage("improve").setAuthor("Ada", "ada@example.com").setCommitter("Ada", "ada@example.com").call();
            git.push().setRemote("origin").setPushAll().call();
        }
        new CommandLineInterface().run(new String[]{"installSkills", "-source", url, "-cacheFolder", cache.getPath(), "-target", target.getPath()});
        assertEquals(List.of("sokrates", "sokrates-improve", "sokrates-repo-config", "tech-stack-scan"), names(target));
    }

    @Test
    void listOnlyWritesNothing(@TempDir Path tmp) throws Exception {
        File checkout = skillsCheckout(tmp.resolve("sokrates-skills").toFile());
        File target = tmp.resolve("target").toFile();
        new CommandLineInterface().run(new String[]{"installSkills", "-source", checkout.getPath(), "-target", target.getPath(), "-listOnly"});
        assertFalse(target.exists());
        assertEquals(3, SkillsInstaller.findSkills(checkout).size());
    }

    @Test
    void missingSkillsHintNamesTheAgentFolder() {
        assertNull(SkillsInstaller.missingSkillsHint("unknown-agent"));
        String hint = SkillsInstaller.missingSkillsHint("claude");
        File entry = new File(SkillsInstaller.skillsFolderOf("claude"), SkillsInstaller.ENTRY_SKILL);
        assertEquals(entry.exists(), hint == null, "a hint exactly when the entry skill is not installed for the agent");
        if (hint != null) {
            assertTrue(hint.contains("sokrates installSkills"));
        }
    }
}
