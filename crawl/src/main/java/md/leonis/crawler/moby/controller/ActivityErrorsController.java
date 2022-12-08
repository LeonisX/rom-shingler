package md.leonis.crawler.moby.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.stage.Stage;
import md.leonis.crawler.moby.config.ConfigHolder;
import md.leonis.crawler.moby.view.StageManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import java.util.Arrays;
import java.util.stream.Collectors;

import static md.leonis.crawler.moby.config.ConfigHolder.errorsMap;

@Controller
public class ActivityErrorsController {

    private final StageManager stageManager;
    private final ConfigHolder configHolder;
    public Button okButton;
    public TreeTableView<String> gameOrFileTreeTableView;
    public TreeTableColumn<String, String> gameOrFileColumn;

    private final TreeItem<String> rootItem = new TreeItem<>("");

    @Lazy
    public ActivityErrorsController(StageManager stageManager, ConfigHolder configHolder) {
        this.stageManager = stageManager;
        this.configHolder = configHolder;
    }

    @FXML
    private void initialize() {
        gameOrFileTreeTableView.setRoot(rootItem);
        gameOrFileTreeTableView.setShowRoot(false);
        errorsMap.forEach((key, value) -> {
            TreeItem<String> treeItem = new TreeItem<>(key);
            value.stream().map(err -> err.toString() + "\n" + Arrays.stream(err.getStackTrace()).map(StackTraceElement::toString)
                    .collect(Collectors.joining("\n"))).distinct().forEach(err -> treeItem.getChildren().add(new TreeItem<>(err)));
            rootItem.getChildren().add(treeItem);
        });
        gameOrFileColumn.setCellValueFactory((TreeTableColumn.CellDataFeatures<String, String> p) ->
                new ReadOnlyStringWrapper(p.getValue().getValue()));
    }

    public void okButtonClick() {
        ((Stage) okButton.getScene().getWindow()).close();
    }
}
