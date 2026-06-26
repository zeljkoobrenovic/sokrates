package nl.obren.sokrates.reports.landscape.statichtml;

import nl.obren.sokrates.sourcecode.landscape.PeopleConfig;
import nl.obren.sokrates.sourcecode.landscape.PersonConfig;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositoryPeopleConfigCommandsTest {

    private File writeRepo(File repoRoot, String history) throws IOException {
        File sokratesFolder = new File(repoRoot, "_sokrates");
        sokratesFolder.mkdirs();
        File configFile = new File(sokratesFolder, "config.json");
        FileUtils.write(configFile,
                "{ \"metadata\": { \"name\": \"test\" }, "
                        + "\"fileHistoryAnalysis\": { \"importPath\": \"../git-history.txt\" } }",
                StandardCharsets.UTF_8);
        if (history != null) {
            FileUtils.write(new File(repoRoot, "git-history.txt"), history, StandardCharsets.UTF_8);
        }
        return configFile;
    }

    private PeopleConfig read(File peopleConfigFile) throws IOException {
        return new nl.obren.sokrates.common.io.JsonMapper().getObject(
                FileUtils.readFileToString(peopleConfigFile, StandardCharsets.UTF_8),
                new com.fasterxml.jackson.core.type.TypeReference<PeopleConfig>() {
                });
    }

    @Test
    void groupsEmailsByUserNameFromGitHistory(@TempDir File repoRoot) throws IOException {
        File configFile = writeRepo(repoRoot,
                "2024-01-01 guido@python.org c1 main/a.py Guido&nbsp;van&nbsp;Rossum\n"
                        + "2024-01-02 guido@dropbox.com c2 main/b.py Guido&nbsp;van&nbsp;Rossum\n"
                        + "2024-01-03 solo@example.com c3 main/c.py Solo&nbsp;Dev\n");

        File peopleConfigFile = RepositoryPeopleConfigCommands.updatePeopleConfigByUserName(configFile);

        assertNotNull(peopleConfigFile);
        // Written next to config.json, i.e. _sokrates/config-people.json.
        assertEquals("config-people.json", peopleConfigFile.getName());
        assertEquals(new File(repoRoot, "_sokrates"), peopleConfigFile.getParentFile());

        PeopleConfig config = read(peopleConfigFile);
        assertEquals(2, config.getPeople().size());
        PersonConfig guido = config.getPeople().stream()
                .filter(p -> p.getUserName().equals("Guido van Rossum")).findFirst().orElseThrow();
        // email = latest-used (2024-01-02 = dropbox); both emails become patterns.
        assertEquals("guido@dropbox.com", guido.getEmail());
        assertEquals(2, guido.getEmailPatterns().size());
    }

    @Test
    void isAdditiveOnRerun(@TempDir File repoRoot) throws IOException {
        File configFile = writeRepo(repoRoot,
                "2024-01-01 guido@python.org c1 main/a.py Guido&nbsp;van&nbsp;Rossum\n");

        RepositoryPeopleConfigCommands.updatePeopleConfigByUserName(configFile);
        // Append a newer commit with a different email for the same person, then re-run.
        FileUtils.write(new File(repoRoot, "git-history.txt"),
                "2024-02-01 gvanrossum@gmail.com c2 main/b.py Guido&nbsp;van&nbsp;Rossum\n",
                StandardCharsets.UTF_8, true);
        File peopleConfigFile = RepositoryPeopleConfigCommands.updatePeopleConfigByUserName(configFile);

        PeopleConfig config = read(peopleConfigFile);
        assertEquals(1, config.getPeople().size());
        PersonConfig guido = config.getPeople().get(0);
        // email field was set on the first run and is NOT overwritten by the newer email; the new
        // email is still added as a pattern.
        assertEquals("guido@python.org", guido.getEmail());
        assertEquals(2, guido.getEmailPatterns().size());
    }

    @Test
    void returnsNullWhenGitHistoryMissing(@TempDir File repoRoot) throws IOException {
        File configFile = writeRepo(repoRoot, null); // no git-history.txt

        assertNull(RepositoryPeopleConfigCommands.updatePeopleConfigByUserName(configFile));
        assertFalse(new File(repoRoot, "_sokrates/config-people.json").exists());
    }

    @Test
    void returnsNullWhenConfigMissing(@TempDir File repoRoot) {
        File missing = new File(repoRoot, "_sokrates/config.json");
        assertNull(RepositoryPeopleConfigCommands.updatePeopleConfigByUserName(missing));
    }

    @Test
    void populatesEmailPatternsPerEmail(@TempDir File repoRoot) throws IOException {
        File configFile = writeRepo(repoRoot,
                "2024-01-01 a.b@x.io c1 main/a.py Dotted&nbsp;Name\n");

        File peopleConfigFile = RepositoryPeopleConfigCommands.updatePeopleConfigByUserName(configFile);

        PeopleConfig config = read(peopleConfigFile);
        assertEquals(1, config.getPeople().get(0).getEmailPatterns().size());
        assertTrue(nl.obren.sokrates.common.utils.RegexUtils.matchesAnyPattern(
                "a.b@x.io", config.getPeople().get(0).getEmailPatterns()));
    }
}
