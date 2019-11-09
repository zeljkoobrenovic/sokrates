package nl.obren.sokrates.integrationtests.uitests;

import com.fasterxml.jackson.core.JsonProcessingException;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.stage.Stage;
import nl.obren.sokrates.codeexplorer.codebrowser.AspectsTablePane;
import nl.obren.sokrates.codeexplorer.codebrowser.CodeBrowserPane;
import nl.obren.sokrates.codeexplorer.dependencies.DependenciesPane;
import nl.obren.sokrates.codeexplorer.duplication.DuplicatesPane;
import nl.obren.sokrates.codeexplorer.units.UnitsPane;
import nl.obren.sokrates.common.io.JsonGenerator;
import nl.obren.sokrates.common.io.JsonMapper;
import nl.obren.sokrates.common.io.UserProperties;
import nl.obren.sokrates.sourcecode.CodeConfiguration;
import nl.obren.sokrates.sourcecode.SourceFileFilter;
import nl.obren.sokrates.sourcecode.aspects.CrossCuttingConcernsGroup;
import nl.obren.sokrates.sourcecode.aspects.SourceCodeAspect;
import nl.obren.sokrates.sourcecode.dependencies.Dependency;
import nl.obren.sokrates.sourcecode.duplication.DuplicationInstance;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.BasicConfigurator;
import org.junit.Test;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.matcher.base.NodeMatchers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static nl.obren.sokrates.codeexplorer.configuration.CodeConfigurationView.LAST_CONFIGURATION_FILE_PROPERTY;

public class BasciHappyFlowTest extends ApplicationTest {
    public static final int SMALL_DELAY = 100;
    private static final String JAVA_CLASS_EXAMPLE = "package ${package};\n"
            + "${imports}\n" +
            "public class Dummy {\n" +
            "   int a;\n" +
            "   int b;\n" +
            "   int c;\n" +
            "   public Dummy(){\n" +
            "   }\n" +
            "\n" +
            "   public Dummy(String name){\n" +
            "      // The constructor.\n" +
            "   }\n" +
            "}";
    private static final String GENERATED_JAVA_CLASS_EXAMPLE = "package dummy;\n\n"
            + "/* auto-generated */\n"
            + "public class GeneratedDummy{\n"
            + "   public GeneratedDummy(){\n"
            + "   }\n" +
            "}";
    private static final String JAVASCRIPT_EXAMPLE = "/* Declare the function 'myFunc' */\n" +
            "function myFunc(theObject) {\n" +
            "   theObject.brand = \"Toyota\";\n" +
            " }\n" +
            " \n" +
            " /*\n" +
            "  * Declare variable 'mycar';\n" +
            "  * create and initialize a new Object;\n" +
            "  * assign reference to it to 'mycar'\n" +
            "  */\n" +
            " var mycar = {\n" +
            "   brand: \"Honda\",\n" +
            "   model: \"Accord\",\n" +
            "   year: 1998\n" +
            " };\n" +
            "\n" +
            " /* Logs 'Honda' */\n" +
            " console.log(mycar.brand);\n" +
            "\n" +
            " /* Pass object reference to the function */\n" +
            " myFunc(mycar);\n" +
            "\n" +
            " /*\n" +
            "  * Logs 'Toyota' as the value of the 'brand' property\n" +
            "  * of the object, as changed to by the function.\n" +
            "  */\n" +
            " console.log(mycar.brand);";
    private CodeConfiguration testCodeConfiguration;
    private CodeBrowserPane codeBrowserPane;
    private Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        BasicConfigurator.configure();
        UserProperties testUserProperties = new UserProperties("sokrates");
        testUserProperties.setReadOnly(true);
        UserProperties.setInstance(testUserProperties);
        testCodeConfiguration = getTestCodeConfiguration();
        UserProperties.getInstance("sokrates").setProperty(LAST_CONFIGURATION_FILE_PROPERTY, getTestConfigurationFile(testCodeConfiguration));

        stage.setTitle("Code Explorer");
        codeBrowserPane = new CodeBrowserPane(stage);
        scene = new Scene(codeBrowserPane, 800, 600);
        stage.setScene(scene);

        stage.show();
    }

    @Test
    public void testStandardInteraction() throws InterruptedException, IOException {
        testLoadingOfConfiguration();
        testRunAnalysisDisplayOfResults();
        testAspectsSelectionAndPreview();
        testUnitsPane();
        testDuplicatesPane();
        testDependenciesPane();
    }

    private void testLoadingOfConfiguration() throws InterruptedException, IOException {
        Thread.sleep(2000);
        FxAssert.verifyThat("#new_project_dialog", NodeMatchers.isNull());
        new FxRobot().interact(() -> {
            try {
                String text = codeBrowserPane.getCodeConfigurationView().getText();
                text = new JsonGenerator().generate(new JsonMapper().getObject(text, CodeConfiguration.class));
                assertEquals(text, new JsonGenerator().generate(testCodeConfiguration));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void testRunAnalysisDisplayOfResults() throws InterruptedException {
        Thread.sleep(SMALL_DELAY);
        clickOn("#run_analysis");
        Thread.sleep(SMALL_DELAY);
        new FxRobot().interact(() -> {
            AspectsTablePane scopeAspectsTablePane = codeBrowserPane.getScopeAspectsTablePane();
            ObservableList<SourceCodeAspect> items = scopeAspectsTablePane.getTable().getItems();
            assertEquals(items.size(), 9);
            assertEquals(items.get(0).getName(), "main");
            assertEquals(items.get(0).getSourceFiles().size(), 8);
            assertEquals(items.get(1).getName(), "  *.java");
            assertEquals(items.get(1).getSourceFiles().size(), 6);
            assertEquals(items.get(2).getName(), "  *.js");
            assertEquals(items.get(2).getSourceFiles().size(), 2);
            assertEquals(items.get(3).getName(), "test");
            assertEquals(items.get(3).getSourceFiles().size(), 5);
            assertEquals(items.get(4).getName(), "  *.java");
            assertEquals(items.get(4).getSourceFiles().size(), 5);
            assertEquals(items.get(5).getName(), "generated");
            assertEquals(items.get(5).getSourceFiles().size(), 5);
            assertEquals(items.get(6).getName(), "  *.java");
            assertEquals(items.get(6).getSourceFiles().size(), 5);
            assertEquals(items.get(7).getName(), "build and deployment");
            assertEquals(items.get(7).getSourceFiles().size(), 0);
        });
        new FxRobot().interact(() -> {
            AspectsTablePane scopeAspectsTablePane = codeBrowserPane.getScopeAspectsTablePane();
            scopeAspectsTablePane.getTable().getSelectionModel().select(0);
        });
        Thread.sleep(SMALL_DELAY);
    }

    private void testDuplicatesPane() throws InterruptedException {
        clickOn("#duplicates");
        FxAssert.verifyThat("#duplicates_pane", NodeMatchers.isVisible());
        Thread.sleep(SMALL_DELAY);
        new FxRobot().interact(() -> {
            DuplicatesPane duplicatesPane = DuplicatesPane.getInstance();
            ObservableList<DuplicationInstance> items = duplicatesPane.getTable().getItems();
            assertEquals(items.size(), 2);
            DuplicatesPane.close();
        });
        Thread.sleep(SMALL_DELAY);
    }

    private void testUnitsPane() throws InterruptedException {
        clickOn("#units");
        FxAssert.verifyThat("#units_pane", NodeMatchers.isVisible());
        Thread.sleep(SMALL_DELAY);
        new FxRobot().interact(() -> {
            UnitsPane unitsPane = UnitsPane.getInstance();
            ObservableList<UnitInfo> items = unitsPane.getTable().getItems();
            assertEquals(items.size(), 14);
            UnitsPane.close();
        });
        Thread.sleep(SMALL_DELAY);
    }

    private void testDependenciesPane() throws InterruptedException {
        clickOn("#logical_components_tab");
        clickOn("#visualize");
        clickOn("#measured_dependencies");
        FxAssert.verifyThat("#dependencies_pane", NodeMatchers.isVisible());
        Thread.sleep(1000);
        new FxRobot().interact(() -> {
            DependenciesPane dependenciesPane = DependenciesPane.getInstance();
            ObservableList<Dependency> items = dependenciesPane.getTable().getItems();
            assertEquals(items.size(), 2);
            assertEquals(items.get(0).getDependencyString(), "a -> b");
            assertEquals(items.get(1).getDependencyString(), "b -> c");
            DependenciesPane.close();
        });
        Thread.sleep(1000);
    }

    private void testAspectsSelectionAndPreview() throws InterruptedException {
        Thread.sleep(SMALL_DELAY);
        new FxRobot().interact(() -> {
            AspectsTablePane scopeAspectsTablePane = codeBrowserPane.getScopeAspectsTablePane();
            scopeAspectsTablePane.getTable().getSelectionModel().select(0);
        });
        Thread.sleep(SMALL_DELAY);
        new FxRobot().interact(() -> {
            ObservableList<SourceCodeAspect> items = codeBrowserPane.getScopeAspectsTablePane().getTable().getItems();
            assertEquals(items.get(0).getName(), "main");
            assertEquals(items.get(0).getSourceFiles().size(), 8);
        });
        Thread.sleep(SMALL_DELAY);
        new FxRobot().interact(() -> {
            assertFalse(codeBrowserPane.getAspectFilesBrowserPane().getCodePreviewEditor().getCodeArea().getEditorValue().isEmpty());
        });
        Thread.sleep(SMALL_DELAY);
        new FxRobot().interact(() -> {
            AspectsTablePane scopeAspectsTablePane = codeBrowserPane.getScopeAspectsTablePane();
            scopeAspectsTablePane.getTable().getSelectionModel().select(5);
        });
        Thread.sleep(SMALL_DELAY);
        new FxRobot().interact(() -> {
            assertEquals(codeBrowserPane.getAspectFilesBrowserPane().getCodePreviewEditor().getCodeArea().getEditorValue(), GENERATED_JAVA_CLASS_EXAMPLE);
        });
        Thread.sleep(SMALL_DELAY);
        new FxRobot().interact(() -> {
            AspectsTablePane scopeAspectsTablePane = codeBrowserPane.getScopeAspectsTablePane();
            scopeAspectsTablePane.getTable().getSelectionModel().select(0);
        });
        Thread.sleep(SMALL_DELAY);
    }

    private File createTempJavaFile(Path folder, String prefix, String packageHeader, String imports) throws IOException {
        File file = Files.createTempFile(folder, prefix, ".java").toFile();
        FileUtils.writeStringToFile(file, JAVA_CLASS_EXAMPLE
                .replace("${package}", packageHeader)
                .replace("${imports}", imports), StandardCharsets.UTF_8);

        return file;
    }

    private File createTempGeneratedJavaFile(Path folder, String prefix) throws IOException {
        File file = Files.createTempFile(folder, prefix, ".java").toFile();
        FileUtils.writeStringToFile(file, GENERATED_JAVA_CLASS_EXAMPLE, StandardCharsets.UTF_8);

        return file;
    }

    private File createTempJavaScriptFile(Path folder, String prefix) throws IOException {
        File file = Files.createTempFile(folder, prefix, ".js").toFile();
        FileUtils.writeStringToFile(file, JAVASCRIPT_EXAMPLE, StandardCharsets.UTF_8);

        return file;
    }

    private File createTestProject() throws IOException {
        Path root = Files.createTempDirectory("test");
        Path component1 = Files.createTempDirectory(root, "component1");
        Path component2 = Files.createTempDirectory(root, "component2");
        Path component3 = Files.createTempDirectory(root, "component3");

        createTempJavaFile(component1, "A", "a", "import b;");
        createTempJavaFile(component1, "A", "a", "import b;");
        createTempJavaFile(component1, "A", "a", "import b;");
        createTempJavaFile(component1, "Test", "a", "import b;");
        createTempJavaFile(component1, "Test", "a", "import b;");
        createTempJavaFile(component1, "Test", "a", "import b;");

        createTempJavaFile(component2, "B", "b", "import c;");
        createTempJavaFile(component2, "B", "b", "import c;");
        createTempGeneratedJavaFile(component2, "import b;");
        createTempGeneratedJavaFile(component2, "import b;");
        createTempGeneratedJavaFile(component2, "import b;");
        createTempGeneratedJavaFile(component2, "import b;");
        createTempGeneratedJavaFile(component2, "import b;");
        createTempJavaFile(component2, "Test", "import b;", "import c;");
        createTempJavaFile(component2, "Test", "import b;", "import c;");

        createTempJavaFile(component3, "C", "c", "");
        createTempJavaScriptFile(component3, "C");
        createTempJavaScriptFile(component3, "C");

        return root.toFile();
    }

    private File getTestConfigurationFile(CodeConfiguration codeConfiguration) throws IOException {
        File file = Files.createTempFile("configuration", ".json").toFile();
        FileUtils.writeStringToFile(file, new JsonGenerator().generate(codeConfiguration), StandardCharsets.UTF_8);

        return file;
    }

    private CodeConfiguration getTestCodeConfiguration() throws IOException {
        CodeConfiguration codeConfiguration = CodeConfiguration.getDefaultConfiguration();
        SourceCodeAspect test = new SourceCodeAspect("test");
        test.setSourceFileFilters(Arrays.asList(new SourceFileFilter(".*Test.*", "")));
        codeConfiguration.setTest(test);
        SourceCodeAspect generated = new SourceCodeAspect("generated");
        generated.setSourceFileFilters(Arrays.asList(new SourceFileFilter("", ".*auto[-]generated.*")));
        codeConfiguration.setGenerated(generated);

        codeConfiguration.setSrcRoot(createTestProject().getPath());
        codeConfiguration.setExtensions(Arrays.asList("java", "js"));

        codeConfiguration.getCrossCuttingConcerns().add(new CrossCuttingConcernsGroup("general"));

        return codeConfiguration;
    }
}