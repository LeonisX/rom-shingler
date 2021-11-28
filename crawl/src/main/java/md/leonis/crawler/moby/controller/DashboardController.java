package md.leonis.crawler.moby.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import md.leonis.crawler.moby.view.StageManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

@Controller
public class DashboardController {

    @FXML
    public BorderPane container;
    @FXML
    private AnchorPane anchorPane;

    private final StageManager stageManager;

    @Lazy
    public DashboardController(StageManager stageManager) {
        this.stageManager = stageManager;
    }

    @FXML
    private void initialize() {

        stageManager.setRootLayout(container);

        //anchorPane.getChildren().add(new LogController(stageManager));
    }
}
