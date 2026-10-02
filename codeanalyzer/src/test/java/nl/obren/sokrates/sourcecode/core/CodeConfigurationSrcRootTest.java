package nl.obren.sokrates.sourcecode.core;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The source root of an analysis is resolved relative to the configuration file — also when the
 * configuration file itself is given as a relative path (generateReports -confFile _sokrates/config.json
 * run from the repository root), where ".." used to escape to the working directory's parent.
 */
class CodeConfigurationSrcRootTest {

    @Test
    void relativeConfigPathResolvesDotDotToTheRepositoryNotTheWorkingDirectorysParent() throws Exception {
        File repo = new File("target/srcroot-test-" + System.nanoTime());
        File config = new File(repo, "_sokrates/config.json");
        FileUtils.write(config, "{}", "UTF-8");
        try {
            String resolved = CodeConfiguration.getAbsoluteSrcRoot("..", config);           // relative config path
            assertEquals(repo.getAbsoluteFile().getPath(), new File(resolved).getAbsoluteFile().getPath());

            FileUtils.forceMkdir(new File(repo, "src"));
            assertEquals(new File(repo, "src").getAbsoluteFile().getPath(), new File(CodeConfiguration.getAbsoluteSrcRoot("../src", config)).getAbsoluteFile().getPath());

            String absolute = CodeConfiguration.getAbsoluteSrcRoot("..", config.getAbsoluteFile());  // absolute config path, as before
            assertEquals(repo.getAbsoluteFile().getPath(), new File(absolute).getAbsoluteFile().getPath());
        } finally {
            FileUtils.deleteDirectory(repo);
        }
    }
}
