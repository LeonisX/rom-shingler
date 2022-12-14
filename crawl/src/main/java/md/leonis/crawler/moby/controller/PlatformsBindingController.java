package md.leonis.crawler.moby.controller;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.Pair;
import md.leonis.shingler.utils.TiviApiUtils;
import md.leonis.crawler.moby.config.ConfigHolder;
import md.leonis.crawler.moby.controls.SearchComboBox;
import md.leonis.crawler.moby.crawler.Crawler;
import md.leonis.crawler.moby.model.Platform;
import md.leonis.crawler.moby.view.FxmlView;
import md.leonis.crawler.moby.view.StageManager;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class PlatformsBindingController {

    private final StageManager stageManager;
    private final ConfigHolder configHolder;

    public VBox diffVBox;
    public Button closeButton;
    public Button rollbackButton;

    Map<String, List<String>> platformsBinding;
    List<Platform> platforms;

    Platform rollbackPlatform;
    Pane rollbackHBox;
    int rollbackIndex;

    Crawler crawler;

    @Lazy
    public PlatformsBindingController(StageManager stageManager, ConfigHolder configHolder) {
        this.stageManager = stageManager;
        this.configHolder = configHolder;
    }

    //TODO color?

    @FXML
    private void initialize() throws Exception {
        // read moby, tivi
        List<String> tiviList = TiviApiUtils.readTables(ConfigHolder.apiPath, ConfigHolder.serverSecret);
        crawler = ConfigHolder.getCrawler();
        platforms = crawler.loadPlatformsList();

        //загрузить сопоставление если есть
        platformsBinding = crawler.loadPlatformsBindingMap();

        // build
        for (Map.Entry<String, List<String>> entry : platformsBinding.entrySet()) {
            for (String s : entry.getValue()) {
                diffVBox.getChildren().add(newHBox(entry.getKey(), s));
                tiviList.remove(entry.getKey());
            }
        }

        // одинаковые тоже в верх
        List<String> platformIds = platforms.stream().map(Platform::getId).collect(Collectors.toList());
        for (String cpu : new ArrayList<>(tiviList)) {
            if (platformIds.contains(cpu)) {
                diffVBox.getChildren().add(newHBox(cpu, cpu));
                tiviList.remove(cpu);
            }
        }

        if (!diffVBox.getChildren().isEmpty()) {
            Button approveButton = new Button("Approve top \u261D");
            // delete, clear
            approveButton.setOnAction(action -> {
                Pane buttonPane = (Pane) ((Button) action.getSource()).getParent();
                int index = diffVBox.getChildren().indexOf(buttonPane);
                for (int i = index - 1; i >= 0; i--) {
                    processPane((Pane) diffVBox.getChildren().get(i));
                }
                diffVBox.getChildren().remove(buttonPane);
                rollbackButton.setVisible(false);
            });
            HBox box = new HBox(approveButton);
            box.setMinHeight(30);
            box.setAlignment(Pos.CENTER_LEFT);
            diffVBox.getChildren().add(box);
        }

        for (String cpu : tiviList) {
            diffVBox.getChildren().add(newHBox(cpu, null));
        }
    }

    private HBox newHBox(String cpu, String solution) {
        Label label = new Label(cpu);
        label.setMinWidth(60);
        Button okButton = new Button("ok");
        okButton.setMinWidth(30);
        okButton.setOnAction(action -> processPane((Pane) ((Button) action.getSource()).getParent()));
        Button plusButton = new Button("+");
        plusButton.setMinWidth(30);
        // add one more cpu below (gb, ngp)
        // идея в том, то в моби платформы раздроблены, gb/gbc, ngp/ngpc
        //а chip-8 вообще отсутствует
        plusButton.setOnAction(action -> {
            Pane pane = (Pane) ((Button) action.getSource()).getParent();
            int index = diffVBox.getChildren().indexOf(pane);
            diffVBox.getChildren().add(index, newHBox(cpu, null));
        });

        Button minusButton = new Button("-");
        minusButton.setMinWidth(30);
        minusButton.setOnAction(action -> {
            diffVBox.getChildren().remove(((Button) action.getSource()).getParent());
        });

        SearchComboBox<Platform> searchComboBox = new SearchComboBox<>();
        searchComboBox.setUserData(solution);

        searchComboBox.setCellFactory(new Callback<ListView<Platform>, ListCell<Platform>>() {
            @Override
            public ListCell<Platform> call(ListView<Platform> l) {
                return new ListCell<Platform>() {

                    @Override
                    protected void updateItem(Platform item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item == null || empty) {
                            setGraphic(null);
                        } else {
                            setText(item.getId());
                        }
                    }
                };
            }
        });

        setComboboxItems(searchComboBox, cpu);

        searchComboBox.setEditable(true);

        searchComboBox.setMinWidth(110);
        searchComboBox.getSelectionModel().select(0);

        HBox hBox = new HBox(5, label, searchComboBox, okButton, plusButton);
        hBox.setPadding(new Insets(0, 0, 0, 5));
        hBox.setMinHeight(27);
        hBox.setAlignment(Pos.CENTER_LEFT);
        return hBox;
    }

    private void setComboboxItems(ComboBox<Platform> comboBox, String cpu) {
        String solution = (comboBox.getUserData() != null) ? (String) comboBox.getUserData() : null;

        if (null != solution) {
            comboBox.setUserData(solution);
            comboBox.getItems().addAll(platforms.stream().filter(p -> p.getId().equals(solution)).collect(Collectors.toList()));
            comboBox.getItems().add(new Platform("-------", ""));
        }

        List<Platform> platformList = platforms.stream().map(p -> new Pair<>(new LevenshteinDistance().apply(cpu, p.getId()), p))
                .sorted(Comparator.comparing(Pair::getKey)).filter(p -> p.getKey() < cpu.length()).map(Pair::getValue).collect(Collectors.toList());

        comboBox.getItems().addAll(platformList);
        comboBox.getItems().add(new Platform("-------", ""));
        comboBox.getItems().addAll(platforms.stream().sorted(Comparator.comparing(Platform::getId)).collect(Collectors.toList()));
    }

    private void processPane(Pane pane) {
        String currentCpu = ((Label) (pane).getChildren().get(0)).getText();
        List<String> list = (platformsBinding.containsKey(currentCpu)) ? platformsBinding.get(currentCpu) : new ArrayList<>();
        ComboBox<Platform> comboBox = (ComboBox<Platform>) pane.getChildren().get(1);
        rollbackPlatform = comboBox.getItems().get(comboBox.getSelectionModel().getSelectedIndex());
        rollbackHBox = pane;
        rollbackIndex = diffVBox.getChildren().indexOf(pane);
        String platformId = rollbackPlatform.getId();
        list.add(platformId);
        platformsBinding.put(currentCpu, list.stream().distinct().filter(Objects::nonNull).collect(Collectors.toList()));
        saveBindings();
        diffVBox.getChildren().remove(pane); //TODO may be color

        // recalculate other
        // учитывать, что может попасться кнопка
        for (int i = diffVBox.getChildren().size() - 1; i >= 0; i--) {
            Pane pan = (Pane) diffVBox.getChildren().get(i);
            Object child = pan.getChildren().get(0);
            if (!(child instanceof Label)) {
                continue;
            }
            ComboBox<Platform> combo = (ComboBox<Platform>) pan.getChildren().get(1);
            Object object = combo.getSelectionModel().getSelectedItem();
            Platform selected = object instanceof Platform ? (Platform) object : platforms.stream().filter(p -> p.getId().equals((String) object)).findFirst().get();
            combo.getItems().remove(rollbackPlatform);
            if (selected.equals(rollbackPlatform)) {
                combo.getSelectionModel().select(0);
            } else {
                combo.getSelectionModel().select(selected);
            }
        }
        platforms.removeIf((p -> p.getId().equals(platformId)));
        rollbackButton.setVisible(true);
    }

    public void closeButtonClick() {
        saveBindings();
        stageManager.showPane(FxmlView.PLATFORMS);
        //((Stage) closeButton.getScene().getWindow()).close();
    }

    private void saveBindings() {
        try {
            crawler.savePlatformsBindingMap(new TreeMap<>(platformsBinding));

        } catch (Exception e) {
            stageManager.showErrorAlert("Error saving platformsBinding", e.getMessage(), e);
        }
    }

    //TODO rollback
    public void rollbackButtonClick() {
        String cpu = ((Label) (rollbackHBox).getChildren().get(0)).getText();
        List<String> list = platformsBinding.get(cpu).stream().filter(s -> !s.equals(rollbackPlatform.getId())).collect(Collectors.toList());
        if (!list.isEmpty()) {
            platformsBinding.put(cpu, list);
        } else {
            platformsBinding.remove(cpu);
        }

        // вернуть платформу
        platforms.add(rollbackPlatform);

        // вернуть хбокс
        diffVBox.getChildren().add(rollbackIndex, rollbackHBox);

        // пересчитать комбы
        saveBindings();

        // recalculate other
        // учитывать, что может попасться кнопка
        for (int i = diffVBox.getChildren().size() - 1; i >= 0; i--) {
            Pane pan = (Pane) diffVBox.getChildren().get(i);
            Object child = pan.getChildren().get(0);
            if (!(child instanceof Label)) {
                continue;
            }
            ComboBox<Platform> combo = (ComboBox<Platform>) pan.getChildren().get(1);
            setComboboxItems(combo, cpu);
        }
        rollbackButton.setVisible(false);
    }
}
