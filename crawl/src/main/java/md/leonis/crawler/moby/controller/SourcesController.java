package md.leonis.crawler.moby.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import md.leonis.crawler.moby.config.ConfigHolder;
import md.leonis.crawler.moby.model.Platform;
import md.leonis.crawler.moby.view.FxmlView;
import md.leonis.crawler.moby.view.StageManager;
import md.leonis.shingler.utils.FileUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

@Controller
public class SourcesController {

    private final StageManager stageManager;
    private final ConfigHolder configHolder;
    public Button mobyButton;

    @Lazy
    public SourcesController(StageManager stageManager, ConfigHolder configHolder) {
        this.stageManager = stageManager;
        this.configHolder = configHolder;
    }

    @FXML
    private void initialize() {

    }

    public void mobyButtonClick() {
        ConfigHolder.setPlatforms(FileUtils.loadJsonList(ConfigHolder.sourceDir, "platforms", Platform.class));
        stageManager.showPane(FxmlView.PLATFORMS);
    }
}
