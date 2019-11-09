package nl.obren.sokrates.integrationtests.codeanalysis;

import com.fasterxml.jackson.core.JsonProcessingException;
import nl.obren.sokrates.codeexplorer.CommandLineInterface;
import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.utils.UnzipToTempFilesUtil;
import nl.obren.sokrates.sourcecode.CodeConfigurationUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.BasicConfigurator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static junit.framework.TestCase.*;

public class RegressionTestUtils {
    private static String regressionDataRoot = "/Users/zeljko/Documents/workspace/sokrates/integrationtests/src/test/resources/regressioncases/";

    public static final String ANALYSIS_RESULTS_JSON = "analysisResults.json";

    public static void main(String args[]) throws IOException {
        BasicConfigurator.configure();
        createRegressionData("cherami-client-java", regressionDataRoot);
        createRegressionData("deck.gl", regressionDataRoot);
        createRegressionData("kafka", regressionDataRoot);
        createRegressionData("spark", regressionDataRoot);
        createRegressionData("bitcoin", regressionDataRoot);
    }

    public static void createRegressionData(String systemName, String regressionDataRoot) throws IOException {
        File srcRoot = createSourceFolder(systemName);

        File conf = CodeConfigurationUtils.getDefaultSokratesConfigFile(srcRoot);

        CommandLineInterface commandLineInterface = initConfiguration(srcRoot, conf);

        File sokrates_reports = generateReports(srcRoot, conf, commandLineInterface);

        FileUtils.copyFile(
                new File(sokrates_reports, ANALYSIS_RESULTS_JSON),
                new File(regressionDataRoot, systemName + "/" + ANALYSIS_RESULTS_JSON));

        FileUtils.deleteDirectory(srcRoot);
    }

    public static File generateReports(File srcRoot, File conf, CommandLineInterface commandLineInterface) throws IOException {
        File sokrates_reports = new File(srcRoot, "sokrates_reports");
        commandLineInterface.run(new String[]{"generateReports",
                "-reportAll", srcRoot.getPath(),
                "-outputFolder", sokrates_reports.getPath(),
                "-confFile", conf.getPath()}
        );
        return sokrates_reports;
    }

    public static String json(Object object) throws JsonProcessingException {
        return new JsonGenerator().generate(object);
    }

    public static CommandLineInterface initConfiguration(File srcRoot, File conf) throws IOException {
        CommandLineInterface commandLineInterface = new CommandLineInterface();
        commandLineInterface.run(new String[]{"init",
                "-srcRoot", srcRoot.getPath(),
                "-confFile", conf.getPath()});
        return commandLineInterface;
    }

    public static File createSourceFolder(String systemName) throws IOException {
        File tempFolder = createTempFolder(systemName);

        ClassLoader clazz = RegressionTestUtils.class.getClassLoader();
        InputStream inputStream = clazz.getResourceAsStream("regressioncases/" + systemName + "/src.zip");

        assertNotNull(inputStream);

        UnzipToTempFilesUtil.unzip(inputStream, tempFolder);

        return tempFolder;
    }

    public static File createTempFolder(String systemName) throws IOException {
        File tempFolder = File.createTempFile(systemName, "");
        tempFolder.delete();
        tempFolder.mkdirs();
        return tempFolder;
    }
}
