package md.leonis.shingler.gui.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import md.leonis.shingler.gui.config.ConfigHolder;
import md.leonis.shingler.gui.controller.template.LogController;
import md.leonis.shingler.gui.service.TestService;
import md.leonis.shingler.gui.view.FxmlView;
import md.leonis.shingler.gui.view.StageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

@Controller
public class DashboardController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardController.class);

    @FXML
    private AnchorPane anchorPane;

    /*@FXML
    public TextArea infoTextArea;*/

    private final StageManager stageManager;

    private final ConfigHolder configHolder;

    private final TestService testService;

    @Lazy
    public DashboardController(StageManager stageManager, ConfigHolder configHolder, TestService testService) {
        this.stageManager = stageManager;
        this.configHolder = configHolder;
        this.testService = testService;
    }

    @FXML
    private void initialize() {
        int count = testService.getDictionaries().size();
        //infoTextArea.setText(String.format("Dictionaries in DB:\n%s\n\nWords to learn:\n%s", count, configHolder.getWordsToLearnCount()));
        LogController logController = new LogController(stageManager);
        anchorPane.getChildren().add(logController);
        //templateController.getSelectAllButton().setOnAction(event -> selectAllClick());
        //logController.getSelectedLevelsListenerHandles().registerListener(event -> refreshWebView());
    }

    public void windowShow() {
        stageManager.showNewWindow(FxmlView.WINDOW);
    }

    public void showNotification() {
        LOGGER.info("DashboardController");
        //stageManager.showInformationAlert("Title: selected all", "Header: selected all", "some content");
    }
}
