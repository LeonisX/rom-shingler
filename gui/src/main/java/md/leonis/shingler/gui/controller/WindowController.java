package md.leonis.shingler.gui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import md.leonis.shingler.gui.controller.template.TemplateController;
import md.leonis.shingler.gui.domain.ScriptWord;
import md.leonis.shingler.gui.view.StageManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

@Controller
public class WindowController {

    @FXML
    public VBox vBox;

    @FXML
    public TableView<ScriptWord> wordsTable;

    @FXML
    public TableColumn<ScriptWord, String> wordColumn;
    @FXML
    public TableColumn<ScriptWord, String> levelColumn;
    @FXML
    public TableColumn<ScriptWord, String> frequencyColumn;
    @FXML
    public TableColumn<ScriptWord, String> xColumn;

    private ObservableList<ScriptWord> wordData = FXCollections.observableArrayList();

    private StageManager stageManager;

    @Lazy
    public WindowController(StageManager stageManager) {
        this.stageManager = stageManager;
    }

    @FXML
    private void initialize() {

        TemplateController templateController = new TemplateController(stageManager);
        //templateController.getSelectAllButton().setOnAction(event -> selectAllClick());
        templateController.getSelectedLevelsListenerHandles().registerListener(event -> refreshWebView());
        vBox.getChildren().add(templateController);

        wordColumn.setCellValueFactory(new PropertyValueFactory<>("word"));
        //transcrColumn.setCellValueFactory(new PropertyValueFactory<>("level"));
        //levelColumn.setCellValueFactory(word -> new SimpleStringProperty(word.getValue().getLevel().getTitle()));
        /*levelColumn.setCellValueFactory(word -> new SimpleStringProperty(
                word.getValue().getLevel() == null ? "" : word.getValue().getLevel().name()));*/
        levelColumn.sortTypeProperty();
        frequencyColumn.setCellValueFactory(new PropertyValueFactory<>("frequency"));
        xColumn.setCellValueFactory(new PropertyValueFactory<>("x"));

        wordsTable.setItems(wordData);
    }

    private void selectAllClick() {
        //stageManager.showInformationAlert("Title: selected all", "Header: selected all", "some content");
    }

    private void refreshWebView() {
        //stageManager.showWarningAlert("Title: refresh", "Header: refresh", "some content");
    }
}
