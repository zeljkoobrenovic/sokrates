package nl.obren.sokrates.sourcecode.core;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The source root of an analysis is resolved relative to the configuration file — also when the
 * configuration file itself is given as a relative path (generateReports -confFile _sokrates/config.json
 * run from the repository root), where ".." used to escape to the working directory's parent. The result
 * stays relative when the configuration path is relative: it prefixes every analyzed file's path, which the
 * scope patterns match as a whole, so an absolute prefix would let the folders above the repository (a
 * "tests" or "docs" in the user's path) classify its files.
 */
class CodeConfigurationSrcRootTest {

    private static String canonical(String path) throws Exception {
        return new File(path).getCanonicalPath();
    }

    @Test
    void relativeConfigPathResolvesDotDotToTheRepositoryNotTheWorkingDirectorysParent() throws Exception {
        File repo = new File("target/srcroot-test-" + System.nanoTime());
        File config = new File(repo, "_sokrates/config.json");
        FileUtils.write(config, "{}", "UTF-8");
        FileUtils.forceMkdir(new File(repo, "src"));
        try {
            assertEquals(canonical(repo.getPath()), canonical(CodeConfiguration.getAbsoluteSrcRoot("..", config)));
            assertEquals(canonical(new File(repo, "src").getPath()), canonical(CodeConfiguration.getAbsoluteSrcRoot("../src", config)));
            assertEquals(canonical(repo.getPath()), canonical(CodeConfiguration.getAbsoluteSrcRoot("..", config.getAbsoluteFile())));
        } finally {
            FileUtils.deleteDirectory(repo);
        }
    }

    @Test
    void aRelativeConfigPathWithoutGrandparentYieldsARelativeRootInTheWorkingDirectory() throws Exception {
        File sokrates = new File("_sokrates-srcroot-test-" + System.nanoTime());
        File config = new File(sokrates, "config.json");       // _sokrates/config.json: the parent has no parent
        FileUtils.write(config, "{}", "UTF-8");
        try {
            String root = CodeConfiguration.getAbsoluteSrcRoot("..", config);
            assertFalse(new File(root).isAbsolute(), "a relative configuration path must not produce an absolute source root: " + root);
            assertEquals(canonical("."), canonical(root));
        } finally {
            FileUtils.deleteDirectory(sokrates);
        }
    }
}
