package md.leonis.crawler.moby.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import md.leonis.crawler.moby.config.ConfigHolder;
import md.leonis.crawler.moby.model.GameEntry;
import md.leonis.crawler.moby.model.Platform;
import md.leonis.crawler.moby.view.FxmlView;
import md.leonis.crawler.moby.view.StageManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

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
    public Button reloadGamesButton;
    public Button clearListButton;
    public CheckBox showReadyPlatformsCheckBox;
    public CheckBox showEmptyPlatformsCheckBox;

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

        //TODO
        //ConfigHolder.platforms.add(new Platform("1", "title", 100, 55, LocalDateTime.now()));

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
            List<Platform> platforms = crawler.getPlatformsList();
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
            stageManager.showErrorAlert("Error reading platforms list", "Stack trace", e);
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

        try {
            List<String> allTitles = new ArrayList<>();
            for (Platform platform : platformsQueueListView.getItems()) {
                List<GameEntry> prevGames = crawler.getSavedGamesList(platform.getId());
                List<GameEntry> newGames = crawler.getGamesList(platform.getId());
                //diff
                Collection<String> titles = newGames.stream().map(GameEntry::getTitle).collect(Collectors.toList());
                titles.removeAll(prevGames.stream().map(GameEntry::getTitle).collect(Collectors.toList()));
                allTitles.addAll(titles);
                //save
                Map<String, GameEntry> gamesMap = prevGames.stream().collect(Collectors.toMap(GameEntry::getGameId, Function.identity()));
                newGames.forEach(ng -> {
                    GameEntry current = gamesMap.get(ng.getGameId());
                    if (null != current) {
                        prevGames.add(ng);
                    }
                });
                platformsById.get(platform.getId()).setTotal(newGames.size());
                platformsById.get(platform.getId()).setCompleted(newGames.stream().filter(GameEntry::isCompleted).count());
                crawler.savePlatformsList(platforms);
                crawler.saveGamesList(platform.getId(), prevGames.stream().sorted(Comparator.comparing(GameEntry::getTitle)).collect(Collectors.toList()));
            }
            crawler.savePlatformsList(platforms);
            updatePlatformsList();
            //TODO show platform
            if (!allTitles.isEmpty()) {
                String text = String.join(", ", allTitles);
                stageManager.showInformationAlert("New games", allTitles.size() + " new game(s) found:", text);
            }
        } catch (Exception e) {
            stageManager.showErrorAlert("Error reading games list", "Stack trace", e);
        }
    }

    public void reloadGamesButtonClick() {

        gamesTableColumn.setSortable(true);
        gamesTableColumn.setSortType(TableColumn.SortType.DESCENDING);
        platformsTableView.getSortOrder().add(gamesTableColumn);
        platformsTableView.sort();

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
}
