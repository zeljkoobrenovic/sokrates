package nl.obren.sokrates.integrationtests.uitests;

import javafx.scene.Scene;
import javafx.stage.Stage;
import nl.obren.sokrates.codeexplorer.codebrowser.CodeBrowserPane;
import nl.obren.sokrates.common.io.UserProperties;
import org.apache.log4j.BasicConfigurator;
import org.junit.Test;
import org.testfx.api.FxAssert;
import org.testfx.framework.junit.ApplicationTest;
import org.testfx.matcher.base.NodeMatchers;

import java.io.IOException;

public class BasicNoConfigurationFileTest extends ApplicationTest {

    @Override
    public void start(Stage stage) throws IOException {
        BasicConfigurator.configure();
        UserProperties.getInstance("sokrates").setInstance(new UserProperties("sokrates"));

        stage.setTitle("Code Explorer");
        stage.setScene(new Scene(new CodeBrowserPane(stage), 800, 600));

        stage.show();
    }

    @Test
    public void testOpeningWithoutExistingConfigurationFile() throws InterruptedException, IOException {
        Thread.sleep(200);
        FxAssert.verifyThat("#new_project_dialog", NodeMatchers.isVisible());
        Thread.sleep(100);
    }
}
