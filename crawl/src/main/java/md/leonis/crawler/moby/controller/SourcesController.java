package md.leonis.crawler.moby.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import md.leonis.crawler.moby.config.ConfigHolder;
import md.leonis.crawler.moby.model.Activity;
import md.leonis.crawler.moby.model.Platform;
import md.leonis.crawler.moby.view.FxmlView;
import md.leonis.crawler.moby.view.StageManager;
import md.leonis.shingler.utils.FileUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import static md.leonis.crawler.moby.config.ConfigHolder.activity;
import static md.leonis.crawler.moby.config.ConfigHolder.sourceDir;

@Controller
public class SourcesController {

    private final StageManager stageManager;
    private final ConfigHolder configHolder;
    public Button mobyButton;
    public Button testButton;

    @Lazy
    public SourcesController(StageManager stageManager, ConfigHolder configHolder) {
        this.stageManager = stageManager;
        this.configHolder = configHolder;
    }

    @FXML
    private void initialize() {

    }

    public void mobyButtonClick() {
        loadActivityAndShowPane("moby");
    }

    public void testButtonClick() {
        loadActivityAndShowPane("test");
    }

    private void loadActivityAndShowPane(String source) {
        try {
            ConfigHolder.setSource(source);
            ConfigHolder.setPlatforms(FileUtils.loadJsonList(sourceDir, "platforms", Platform.class));

            activity = FileUtils.loadAsJson(sourceDir, "activity", Activity.class);
            if (null == activity) {
                stageManager.showPane(FxmlView.PLATFORMS);
            } else {
                stageManager.showPane(FxmlView.ACTIVITY);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
