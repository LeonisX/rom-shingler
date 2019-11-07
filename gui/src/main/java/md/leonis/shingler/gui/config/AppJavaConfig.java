package md.leonis.shingler.gui.config;

import javafx.stage.Stage;
import md.leonis.shingler.gui.view.SpringFXMLLoader;
import md.leonis.shingler.gui.view.StageManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.ResourceBundle;

@Configuration
public class AppJavaConfig {

    private final SpringFXMLLoader springFXMLLoader;

    @Lazy // SpringFXMLLoader must be initialized first
    public AppJavaConfig(SpringFXMLLoader springFXMLLoader) {
        this.springFXMLLoader = springFXMLLoader;
    }

    @Bean // Internalization support for window titles
    public ResourceBundle resourceBundle() {
        return ResourceBundle.getBundle("Bundle");
    }

    @Bean
    @Lazy // Stage only created after Spring context bootstap
    public StageManager stageManager(Stage stage) {
        return new StageManager(springFXMLLoader, stage);
    }

    @Bean // A shared object that stores general application settings
    public ConfigHolder configHolder() {
        return new ConfigHolder();
    }
}
