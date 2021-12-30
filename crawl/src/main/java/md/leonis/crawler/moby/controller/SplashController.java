package md.leonis.crawler.moby.controller;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.util.Duration;
import md.leonis.crawler.moby.view.FxmlView;
import md.leonis.crawler.moby.view.StageManager;
import md.leonis.shingler.utils.FileUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import java.io.IOException;

import static md.leonis.crawler.moby.config.ConfigHolder.*;

@Controller
public class SplashController {

    private final StageManager stageManager;

    @Lazy
    public SplashController(StageManager stageManager) {
        this.stageManager = stageManager;
    }

    @FXML
    private void initialize() throws IOException {

        //TODO
        FileUtils.createDirectories(getSourceDir(getSource()));

        loadProtectedProperties();
        md.leonis.shingler.model.ConfigHolder.loadProtectedProperties();

        //TODO switch on
        //PauseTransition delay = new PauseTransition(Duration.seconds(1));
        PauseTransition delay = new PauseTransition(Duration.millis(1));
        //TODO boolean - need to wait all processes
        delay.setOnFinished(event -> {
            stageManager.switchScene(FxmlView.DASHBOARD);
            stageManager.showPane(FxmlView.SOURCES);
        });
        delay.play();
    }
}
