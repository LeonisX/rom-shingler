package md.leonis.crawler.moby.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import md.leonis.crawler.moby.MobyCrawler;
import md.leonis.crawler.moby.config.ConfigHolder;
import md.leonis.crawler.moby.model.Platform;
import md.leonis.crawler.moby.view.FxmlView;
import md.leonis.crawler.moby.view.StageManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class PlatformsController {

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final StageManager stageManager;
    private final ConfigHolder configHolder;
    public Button backButton;
    public TableView<Platform> platformsTableView;
    public TableColumn<Platform, String> platformTableColumn;
    public TableColumn<Platform, String> gamesTableColumn;
    public TableColumn<Platform, String> percentTableColumn;
    public TableColumn<Platform, String> versionTableColumn;
    public Button reloadPlatformsButton;

    @Lazy
    public PlatformsController(StageManager stageManager, ConfigHolder configHolder) {
        this.stageManager = stageManager;
        this.configHolder = configHolder;
    }

    @FXML
    private void initialize() {

        //TODO
        //ConfigHolder.platforms.add(new Platform("1", "title", 100, 55, LocalDateTime.now()));

        platformsTableView.setItems(FXCollections.observableArrayList(ConfigHolder.platforms));
        platformTableColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        gamesTableColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(String.format("%s", p.getValue().getCount() == 0 ? "?" : p.getValue().getIndex() * 100.0 / p.getValue().getCount())));
        percentTableColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(String.format("%.2f%%", p.getValue().getCount() == 0 ? 0 : p.getValue().getIndex() * 100.0 / p.getValue().getCount())));
        versionTableColumn.setCellValueFactory(p -> new ReadOnlyStringWrapper(p.getValue().getDate() == null ? "" : formatter.format(p.getValue().getDate())));
    }

    public void backButtonClick() {
        stageManager.showPane(FxmlView.SOURCES);
    }

    public void reloadPlatformsButtonClick() throws Exception {

        List<Platform> platforms = MobyCrawler.parsePlatformsList();
        platforms.forEach(p -> p.updateFrom(ConfigHolder.platformsById.get(p.getId())));
        //TODO diff
        List<String> titles = platforms.stream().map(Platform::getTitle).collect(Collectors.toList());
        titles.removeAll(ConfigHolder.platformsById.values().stream().map(Platform::getTitle).collect(Collectors.toList()));
        if (!titles.isEmpty()) {
            String text = String.join(", ", titles);
            stageManager.showInformationAlert("New platforms", titles.size() + " new platform(s) found:", text);
        }
        ConfigHolder.setPlatforms(platforms);
        platformsTableView.setItems(FXCollections.observableArrayList(ConfigHolder.platforms));
    }
}
