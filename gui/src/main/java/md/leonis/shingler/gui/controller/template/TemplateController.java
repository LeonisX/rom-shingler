package md.leonis.shingler.gui.controller.template;

import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.HBox;
import md.leonis.shingler.gui.domain.LanguageLevel;
import md.leonis.shingler.gui.utils.ListenerHandles;
import md.leonis.shingler.gui.view.StageManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class TemplateController extends HBox {

    @FXML
    private CheckBox unkCheckBox;
    @FXML
    private CheckBox a0CheckBox;
    @FXML
    private CheckBox a1CheckBox;
    @FXML
    private CheckBox a2CheckBox;
    @FXML
    private CheckBox a2pCheckBox;

    @FXML
    private HBox containerHBox;

    @FXML
    private Button selectAllButton;

    private Set<LanguageLevel> levels;

    private ObservableSet<LanguageLevel> selectedLevels;

    private Set<CheckBox> checkBoxes;

    private ListenerHandles selectedLevelsListenerHandles;

    public TemplateController(StageManager stageManager) {
        this.levels = new HashSet<>(Arrays.asList(LanguageLevel.values()));
        this.selectedLevels = FXCollections.observableSet(new HashSet<>(levels));
        this.selectedLevelsListenerHandles = new ListenerHandles(selectedLevels);

        stageManager.loadTemplate("template", this, () ->
                checkBoxes = containerHBox.getChildren().stream()
                .filter(n -> n instanceof CheckBox)
                .map(n -> (CheckBox) n)
                .peek(c -> {
                    LanguageLevel languageLevel = getLevel(c);
                    c.setSelected(selectedLevels.contains(languageLevel));
                    c.setVisible(levels.contains(languageLevel));
                    c.managedProperty().bind(c.visibleProperty());
                })
                .filter(c -> levels.contains(getLevel(c)))
                .peek(c -> c.setOnAction(this::filterCheckBoxClick))
                .collect(Collectors.toSet()));
    }

    public void selectAllButtonClick() {
        selectedLevelsListenerHandles.disableListeners();
        checkBoxes.forEach(c -> c.setSelected(true));
        selectedLevels.addAll(levels);
        selectedLevelsListenerHandles.enableAndNotifyListeners();
    }

    private void filterCheckBoxClick(ActionEvent actionEvent) {
        updateSelectedList((CheckBox) actionEvent.getSource());
    }

    private void updateSelectedList(CheckBox checkBox) {
        LanguageLevel languageLevel = getLevel(checkBox);
        if (selectedLevels.contains(languageLevel)) {
            selectedLevels.remove(languageLevel);
        } else {
            selectedLevels.add(languageLevel);
        }
    }

    private LanguageLevel getLevel(CheckBox checkBox) {
        return LanguageLevel.valueOf(checkBox.getId().replace("CheckBox", "").toUpperCase());
    }

    public ObservableSet<LanguageLevel> getSelectedLevels() {
        return selectedLevels;
    }

    public ListenerHandles getSelectedLevelsListenerHandles() {
        return selectedLevelsListenerHandles;
    }

    public Button getSelectAllButton() {
        return selectAllButton;
    }
}
