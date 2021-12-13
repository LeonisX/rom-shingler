package md.leonis.crawler.moby;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import md.leonis.crawler.moby.view.FxmlView;
import md.leonis.crawler.moby.view.StageManager;
import md.leonis.shingler.utils.MeasureMethodTest;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(scanBasePackages = "md.leonis.crawler.moby", exclude={DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class TestApp extends Application {

    private ConfigurableApplicationContext springContext;

    public static void main(String[] args) {
        MeasureMethodTest.premain();
        Application.launch(args);
    }

    @Override
    public void init() {
        String[] args = this.getParameters().getRaw().toArray(new String[0]);
        SpringApplicationBuilder builder = new SpringApplicationBuilder(TestApp.class);
        builder.headless(false);
        springContext = builder.run(args);
    }

    @Override
    public void start(Stage primaryStage) {
        springContext.getBean(StageManager.class, primaryStage).switchScene(FxmlView.GAMES_BINDING);
    }

    @Override
    public void stop() {
        springContext.stop();
        Platform.exit();
        System.exit(0);
    }
}
