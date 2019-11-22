package md.leonis.shingler.gui.controller.template;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import md.leonis.shingler.gui.controls.LogView;
import md.leonis.shingler.gui.view.StageManager;
import md.leonis.shingler.log.Level;
import org.springframework.context.annotation.Lazy;

import static md.leonis.shingler.model.ConfigHolder.needToStop;
import static md.leonis.shingler.model.ConfigHolder.runningTasks;

public class LogController extends VBox {

    @FXML
    private VBox layout;
    @FXML
    private HBox controls;
    @FXML
    private ChoiceBox<Level> filterLevel;
    @FXML
    private ToggleButton showTimestamp;
    @FXML
    private ToggleButton showSource;
    @FXML
    private ToggleButton tail;
    @FXML
    private ToggleButton pause;
    @FXML
    private Slider rate;
    @FXML
    private Label rateLabel;
    @FXML
    private VBox rateLayout;
    @FXML
    private Label progressLabel;
    @FXML
    private ProgressBar progressBar;
    @FXML
    public Button saveStopButton;

    private final StageManager stageManager;

    @Lazy
    public LogController(StageManager stageManager) {

        this.stageManager = stageManager;
        stageManager.loadTemplate("log", this, () -> {
        });

        LogView logView = new LogView();

        filterLevel.setItems(FXCollections.observableArrayList(Level.values()));
        filterLevel.getSelectionModel().select(Level.DEBUG);
        logView.filterLevelProperty().bind(filterLevel.getSelectionModel().selectedItemProperty());
        logView.showTimeStampProperty().bind(showTimestamp.selectedProperty());
        logView.showSourceProperty().bind(showSource.selectedProperty());
        logView.tailProperty().bind(tail.selectedProperty());
        logView.pausedProperty().bind(pause.selectedProperty());
        logView.refreshRateProperty().bind(rate.valueProperty());
        rateLabel.textProperty().bind(Bindings.format("Update: %.2f fps", rate.valueProperty()));
        rateLabel.setStyle("-fx-font-family: monospace;");
        progressBar.progressProperty().bind(Bindings.divide(logView.progressProperty(), 100));
        progressLabel.textProperty().bind(Bindings.format("%.2f%%", logView.progressProperty()));
        controls.setMinHeight(HBox.USE_PREF_SIZE);

        layout.getChildren().add(logView);

        VBox.setVgrow(logView, Priority.ALWAYS);

        AnchorPane.setTopAnchor(layout, 0.0);
        AnchorPane.setBottomAnchor(layout, 0.0);
        AnchorPane.setLeftAnchor(layout, 0.0);
        AnchorPane.setRightAnchor(layout, 0.0);
    }

    public void saveStopButtonClick() {

        if (runningTasks.isEmpty()) {
            stageManager.showInformationAlert("No long-running tasks!", "Be patient.", "Long-running tasks are not running. Ongoing will end quickly.");
            return;
        }

        needToStop[0] = true;
        stageManager.showWaitAlertAndRun("Waiting for the completion of long-running tasks...", () -> {
            while (needToStop[0]) {
            }
        });
    }
}
