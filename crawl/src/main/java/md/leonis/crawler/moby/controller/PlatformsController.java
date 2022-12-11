package md.leonis.crawler.moby.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import javafx.util.Pair;
import md.leonis.crawler.moby.config.ConfigHolder;
import md.leonis.crawler.moby.crawler.Crawler;
import md.leonis.crawler.moby.model.Activity;
import md.leonis.crawler.moby.model.GameEntry;
import md.leonis.crawler.moby.model.Platform;
import md.leonis.crawler.moby.view.FxmlView;
import md.leonis.crawler.moby.view.StageManager;
import md.leonis.shingler.utils.FileUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static md.leonis.crawler.moby.config.ConfigHolder.*;

@Controller
public class PlatformsController {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final StageManager stageManager;
    private final ConfigHolder configHolder;
    public Button backButton;
    public TableView<Platform> platformsTableView;
    public TableColumn<Platform, String> platformTableColumn;
    public TableColumn<Platform, Integer> gamesTableColumn;
    public TableColumn<Platform, String> percentTableColumn;
    public TableColumn<Platform, String> versionTableColumn;
    public Button reloadPlatformsButton;
    public MenuItem loadGamesListMenuItem;
    public MenuItem auditMenuItem;
    public MenuItem addToQueueMenuItem;
    public ListView<Platform> platformsQueueListView;

    public Button reloadGamesListButton;

    public Button loadGamesButton;
    public Button validateImagesButton;
    public CheckBox useCacheCheckBox;
    public Button clearListButton;

    public CheckBox showReadyPlatformsCheckBox;
    public CheckBox showEmptyPlatformsCheckBox;
    public Button platformsBindings;
    public MenuItem bindGamesMenuItem;

    private Crawler crawler;

    @Lazy
    public PlatformsController(StageManager stageManager, ConfigHolder configHolder) {
        this.stageManager = stageManager;
        this.configHolder = configHolder;
    }

    private void updatePlatformsList() {
        platformsTableView.setItems(FXCollections.observableArrayList(platforms.stream().filter(p -> {
            if (showEmptyPlatformsCheckBox.isSelected() && p.getTotal() == 0) {
                return true;
            } else {
                return showReadyPlatformsCheckBox.isSelected() && p.getTotal() > 0;
            }
        }).collect(Collectors.toList())));
    }

    @FXML
    private void initialize() {
        crawler = getCrawler();

        updatePlatformsList();

        platformsTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        platformsTableView.getSortOrder().add(gamesTableColumn);
        platformsTableView.sort();

        platformTableColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        gamesTableColumn.setCellValueFactory(new PropertyValueFactory<>("total"));
        percentTableColumn.setCellValueFactory(p ->
                new ReadOnlyStringWrapper(String.format("%.2f%%", p.getValue().getTotal() == 0 ? 0 : p.getValue().getCompleted() * 100.0 / p.getValue().getTotal())));
        versionTableColumn.setCellValueFactory(p ->
                new ReadOnlyStringWrapper(p.getValue().getDate() == null ? "" : formatter.format(p.getValue().getDate())));

        platformsQueueListView.setCellFactory(new Callback<ListView<Platform>, ListCell<Platform>>() {

            @Override
            public ListCell<Platform> call(ListView<Platform> param) {
                return new ListCell<Platform>() {

                    @Override
                    protected void updateItem(Platform item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null) {
                            setText(item.getTitle());
                        } else {
                            setText("");
                        }
                    }
                };
            }
        });
    }

    public void backButtonClick() {
        stageManager.showPane(FxmlView.SOURCES);
    }

    public void reloadPlatformsButtonClick() {
        try {
            List<Platform> platforms = crawler.parsePlatformsList();
            platforms.forEach(p -> p.updateFrom(platformsById.get(p.getId())));
            crawler.savePlatformsList(platforms);
            //diff
            List<String> titles = platforms.stream().map(Platform::getTitle).collect(Collectors.toList());
            titles.removeAll(platformsById.values().stream().map(Platform::getTitle).collect(Collectors.toList()));
            if (!titles.isEmpty()) {
                String text = String.join(", ", titles);
                stageManager.showInformationAlert("New platforms", titles.size() + " new platform(s) found:", text);
            }
            ConfigHolder.setPlatforms(platforms);
            updatePlatformsList();
        } catch (Exception e) {
            stageManager.showErrorAlert("Error reading platforms list", e.getMessage(), e);
        }
    }

    public void showReadyPlatformsCheckBoxClick() {
        updatePlatformsList();
    }

    public void showEmptyPlatformsCheckBoxClick() {
        updatePlatformsList();
    }


    // buttons
    public void reloadGamesListButtonClick() {
        if (platformsQueueListView.getItems().isEmpty() && !platformsTableView.getSelectionModel().getSelectedItems().isEmpty()) {
            platformsQueueListView.getItems().addAll(platformsTableView.getSelectionModel().getSelectedItems());
        }

        try {
            List<Pair<String, String>> allTitles = new ArrayList<>();
            for (Platform platform : platformsQueueListView.getItems()) {
                List<GameEntry> prevGames = crawler.loadGamesList(platform.getId());
                List<GameEntry> newGames = crawler.parseGamesList(platform.getId());
                //diff
                Collection<Pair<String, String>> titles = newGames.stream().map(g -> new Pair<>(g.getPlatformId(), g.getTitle())).collect(Collectors.toList());
                titles.removeAll(prevGames.stream().map(g -> new Pair<>(g.getPlatformId(), g.getTitle())).collect(Collectors.toList()));
                allTitles.addAll(titles);
                //save
                Map<String, GameEntry> gamesMap = prevGames.stream().collect(Collectors.toMap(GameEntry::getGameId, Function.identity()));
                newGames.forEach(ng -> {
                    GameEntry current = gamesMap.get(ng.getGameId());
                    if (null == current) {
                        prevGames.add(ng);
                    }
                });
                platformsById.get(platform.getId()).setTotal(newGames.size());
                platformsById.get(platform.getId()).setCompleted(newGames.stream().filter(GameEntry::isCompleted).count());
                //crawler.savePlatformsList(platforms);
                crawler.saveGamesList(platform.getId(), prevGames.stream().sorted(Comparator.comparing(GameEntry::getTitle)).collect(Collectors.toList()), null);
            }
            crawler.savePlatformsList(platforms);
            updatePlatformsList();
            if (!allTitles.isEmpty()) {
                String text = allTitles.stream().map(t -> t.getKey() + "::" + t.getValue()).collect(Collectors.joining(", "));
                stageManager.showInformationAlert("New games", allTitles.size() + " new game(s) found:", text);
            }
        } catch (Exception e) {
            stageManager.showErrorAlert("Error reading games list", e.getMessage(), e);
        }
    }

    public void loadGamesButtonClick() throws IOException {
        if (platformsQueueListView.getItems().isEmpty() && !platformsTableView.getSelectionModel().getSelectedItems().isEmpty()) {
            platformsQueueListView.getItems().addAll(platformsTableView.getSelectionModel().getSelectedItems());
        }

        List<String> platforms = platformsQueueListView.getItems().stream().map(Platform::getId).collect(Collectors.toList());
        if (!platforms.isEmpty()) {
            Activity.Task task = useCacheCheckBox.isSelected() ? Activity.Task.LOAD : Activity.Task.RELOAD;
            activity = new Activity(platforms, task);
            FileUtils.saveAsJson(getSourceDir(getSource()), "activity", activity);
            stageManager.showPane(FxmlView.ACTIVITY);
        } else {
            stageManager.showErrorAlert("No platforms selected!", "Please, select at once one platform", "");
        }
    }

    public void validateImagesButtonClick() throws IOException {
        if (platformsQueueListView.getItems().isEmpty() && !platformsTableView.getSelectionModel().getSelectedItems().isEmpty()) {
            platformsQueueListView.getItems().addAll(platformsTableView.getSelectionModel().getSelectedItems());
        }

        if (!platforms.isEmpty()) {
            List<String> platforms = platformsQueueListView.getItems().stream().map(Platform::getId).collect(Collectors.toList());
            activity = new Activity(platforms, Activity.Task.VALIDATE);
            FileUtils.saveAsJson(getSourceDir(getSource()), "activity", activity);
            stageManager.showPane(FxmlView.ACTIVITY);
        } else {
            stageManager.showErrorAlert("No platforms selected!", "Please, select at once one platform", "");
        }
    }

    public void clearListButtonClick() {
        platformsQueueListView.getItems().clear();
    }


    // context menu
    public void loadGamesListMenuItemClick() {


        //TODO show games diff if found
    }

    public void auditMenuItemClick() {
        //TODO update table only (%)
    }

    public void addToQueueMenuItemClick() {
        platformsTableView.getSelectionModel().getSelectedItems().forEach(platform -> platformsQueueListView.getItems().add(platform));
    }

    public void platformsBindingsClick() {
        stageManager.showPane(FxmlView.PLATFORMS_BINDING);
    }

    public void bindGamesMenuItemClick() {
        try {
            platformsBindingMap = crawler.loadPlatformsBindingMap();
            platformsBindingMapEntries = platformsBindingMap.entrySet().stream().filter(e -> CollectionUtils.containsAny(e.getValue(),
                    platformsTableView.getSelectionModel().getSelectedItems().stream().map(Platform::getId).collect(Collectors.toList()))).collect(Collectors.toList());
            List<String> keys = platformsBindingMapEntries.stream().map(Map.Entry::getKey).collect(Collectors.toList());
            platformsBindingMapEntries.addAll(platformsBindingMap.entrySet().stream().filter(e -> keys.contains(e.getKey())).collect(Collectors.toList()));
            platformsBindingMapEntries = platformsBindingMapEntries.stream().distinct().collect(Collectors.toList());
            if (!platformsBindingMapEntries.isEmpty()) {
                stageManager.showPane(FxmlView.GAMES_BINDING);
            } else {
                stageManager.showWarningAlert("Not allowed!", "Please, bind this platform first", "");
            }
        } catch (Exception e) {
            stageManager.showErrorAlert("No platforms bindings!", "Please, bind platforms first", e);
        }
    }
}
