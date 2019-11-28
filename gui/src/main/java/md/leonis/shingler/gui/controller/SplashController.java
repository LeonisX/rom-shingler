package md.leonis.shingler.gui.controller;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.util.Duration;
import md.leonis.shingler.gui.view.FxmlView;
import md.leonis.shingler.gui.view.StageManager;
import md.leonis.shingler.model.Platform;
import md.leonis.shingler.utils.IOUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import static md.leonis.shingler.model.ConfigHolder.*;

@Controller
public class SplashController {

    private final StageManager stageManager;

    @Lazy
    public SplashController(StageManager stageManager) {
        this.stageManager = stageManager;
    }

    @FXML
    private void initialize() {

        //TODO service, read from disk
        platforms.put("NES", new Platform("NES", "nes", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)", "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*"));
        platformsByCpu.put("nes", platforms.values().iterator().next());

        IOUtils.createDirectories(collectionsDir);
        platforms.values().forEach(p -> IOUtils.createDirectories(collectionsDir.resolve(p.getCpu())));

        //TODO switch on
        //PauseTransition delay = new PauseTransition(Duration.seconds(1));
        PauseTransition delay = new PauseTransition(Duration.millis(1));
        //TODO boolean - need to wait all processes
        delay.setOnFinished(event -> {
            stageManager.switchScene(FxmlView.DASHBOARD);
            stageManager.showPane(FxmlView.COLLECTION);
        });
        delay.play();
    }
}
