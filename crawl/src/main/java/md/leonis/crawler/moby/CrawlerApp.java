package md.leonis.crawler.moby;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import md.leonis.crawler.moby.view.FxmlView;
import md.leonis.crawler.moby.view.StageManager;
import md.leonis.shingler.utils.MeasureMethodTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class CrawlerApp extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlerApp.class);

    private ConfigurableApplicationContext springContext;
    private StageManager stageManager;

    public static void main(String[] args) {
        MeasureMethodTest.premain();
        Application.launch(args);
    }

    /* We need to initialize the Spring context and this can be done in two different places:
     * If we need to create instances of types Scene, Stage, open popup,
     * then we need to do this in the start() method, because It is called in the UI thread.
     * Otherwise, we can use the init() method (as in the example below),
     * which is not called in the UI thread before the start() method is called.
     */
    @Override
    public void init() {
        String[] args = this.getParameters().getRaw().toArray(new String[0]);
        // We run Spring context initialization at the time of JavaFX initialization:
        SpringApplicationBuilder builder = new SpringApplicationBuilder(CrawlerApp.class);
        builder.headless(false);
        springContext = builder.run(args);
        //springContext = SpringApplication.run(MainApp.class, args);
    }

    @Override
    public void start(Stage primaryStage) {
        LOGGER.info("Starting primary stage");
        stageManager = springContext.getBean(StageManager.class, primaryStage);
        displayInitialScene();
    }

    @Override
    public void stop() {

        //TODO save modified data
        /*if (familiesModified.getValue() || familyRelationsModified.getValue()) {

            IOUtils.createDirectories(workFamiliesPath());

            if (familiesModified.getValue()) {
                LOGGER.info("Saving families...");
                stageManager.showWaitAlertAndRun("Saving families", () -> IOUtils.serializeFamiliesAsJson(fullFamiliesPath().toFile(), families));
            }

            if (familyRelationsModified.getValue()) {
                LOGGER.info("Saving family relations...");
                stageManager.showWaitAlertAndRun("Saving family relations", () -> IOUtils.serializeFamilyRelationsAsJson(fullFamilyRelationsPath().toFile(), familyRelations));
            }
        }*/

        springContext.stop();
        Platform.exit();
        System.exit(0);
    }

    protected void displayInitialScene() {
        //TODO w/o borders https://stackoverflow.com/questions/14972199/how-to-create-splash-screen-with-transparent-background-in-javafx
        // Caused by: java.lang.IllegalStateException: Cannot set style once stage has been set visible
        stageManager.switchScene(FxmlView.SPLASH, StageStyle.UNDECORATED);
        //stageManager.switchScene(FxmlView.TEST);
    }
}
