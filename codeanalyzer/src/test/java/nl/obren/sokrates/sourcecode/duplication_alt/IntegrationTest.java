package nl.obren.sokrates.sourcecode.duplication_alt;

import nl.obren.sokrates.sourcecode.duplication.impl.Blocks;
import nl.obren.sokrates.sourcecode.duplication.impl.Files;
import org.apache.log4j.BasicConfigurator;
import org.junit.Test;

import java.io.IOException;

public class IntegrationTest {
    public static final int MIN_DUPLICATION_BLOCK_SIZE = 12;

    @Test
    public void test() throws IOException {
        BasicConfigurator.configure();
        System.out.println("Loading files...");
        Files files = new Files(null);
        getFiles(files);
        Blocks blocks = new Blocks(files, MIN_DUPLICATION_BLOCK_SIZE);
        blocks.extractDuplicatedBlocks(null);
    }

    private void getFiles(Files files) throws IOException {
        //String rootPath = "/legacy/exact/globe/";
        //String rootPath = "/legacy/exact/globe/globe-20171106-v415/Exact Globe 414SPDEV/GLB_Local";
        /*String rootPath = "/legacy/opensource/kubernetes/";

        List<SourceFile> sourceFiles = new ArrayList<>();
        java.nio.file.Files.walk(Paths.get(rootPath))
                .filter(p -> p.toString().endsWith(".go"))
                .filter(p -> !(p.toString().contains(CodeConfigurationUtils.DEFAULT_CONFIGURATION_FOLDER)))
                .forEach(p -> {
                    SourceFile sourceFile = new SourceFile(p.toFile());
                    sourceFile.setRelativePath(p.toFile().getPath().replace("/legacy/opensource/kubernetes/kubernetes-20180205/", ""));
                    sourceFiles.add(sourceFile);
                });

        files.addAll(sourceFiles);*/
    }

}
