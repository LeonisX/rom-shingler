package md.leonis.crawler.moby.controller;

import javafx.fxml.FXML;
import md.leonis.crawler.moby.view.StageManager;
import md.leonis.shingler.model.ConfigHolder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

@Controller
public class PlatformsController {

    private final StageManager stageManager;
    private final ConfigHolder configHolder;

    @Lazy
    public PlatformsController(StageManager stageManager, ConfigHolder configHolder) {
        this.stageManager = stageManager;
        this.configHolder = configHolder;
    }

    @FXML
    private void initialize() {

    }
}
