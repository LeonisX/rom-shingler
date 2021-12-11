package md.leonis.crawler.moby.controller;

import com.sun.javafx.scene.control.skin.VirtualFlow;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import md.leonis.crawler.moby.HttpProcessor;
import md.leonis.crawler.moby.config.ConfigHolder;
import md.leonis.crawler.moby.crawler.Crawler;
import md.leonis.crawler.moby.dto.FileEntry;
import md.leonis.crawler.moby.model.GameEntry;
import md.leonis.crawler.moby.view.FxmlView;
import md.leonis.crawler.moby.view.StageManager;
import md.leonis.shingler.utils.FileUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static md.leonis.crawler.moby.config.ConfigHolder.*;

@Controller
public class ActivityController {

    private final StageManager stageManager;
    private final ConfigHolder configHolder;

    public Button suspendButton;
    public Button abortButton;

    public TableView<GameEntry> gamesTableView;
    public TableColumn<GameEntry, String> platformTableColumn;
    public TableColumn<GameEntry, String> gameTableColumn;

    public TableView<FileEntry> filesTableView;
    public TableColumn<FileEntry, String> platformFileTableColumn;
    public TableColumn<FileEntry, String> fileTableColumn;

    public TableView<HttpProcessor> processorsTableView;
    public TableColumn<HttpProcessor, String> processorTableColumn;
    public TableColumn<HttpProcessor, String> processorFileTableColumn;

    public TableView<String> logsTableView;
    public TableColumn<String, String> logsTableColumn;

    private Crawler crawler;

    @Lazy
    public ActivityController(StageManager stageManager, ConfigHolder configHolder) {
        this.stageManager = stageManager;
        this.configHolder = configHolder;
    }

    @FXML
    private void initialize() {

        crawler = getCrawler();

        platformTableColumn.setCellValueFactory(new PropertyValueFactory<>("platformId"));
        //gameTableColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        gameTableColumn.setCellValueFactory(g -> new SimpleStringProperty(g.getValue().getTitle() + "\t\t" + g.getValue().getErrorsCount()));
        gamesTableView.setItems(FXCollections.observableArrayList(new ArrayList<>()));

        platformFileTableColumn.setCellValueFactory(new PropertyValueFactory<>("platformId"));
        fileTableColumn.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getHost() + f.getValue().getUri() + "\t\t" + f.getValue().getErrorsCount()));
        filesTableView.setItems(FXCollections.observableArrayList(activity.getFileEntries()));

        processorTableColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        processorFileTableColumn.setCellValueFactory(new PropertyValueFactory<>("file"));
        processorsTableView.setItems(FXCollections.observableArrayList(crawler.getProcessor().getProcessors()));

        logsTableColumn.setCellValueFactory(l -> new ReadOnlyStringWrapper(l.getValue()));
        logsTableView.setItems(FXCollections.observableArrayList(new ArrayList<>()));

        //TODO use activity task

        //TODO show diff


        Task<Void> task = new Task<Void>() {
            @Override
            public Void call() {
                try {
                    List<GameEntry> gameEntries = crawler.getGamesList(activity.getPlatforms());
                    gamesTableView.getItems().addAll(gameEntries);
                    crawler.setConsumers(crawlerRefreshConsumer, crawlerSuccessConsumer, crawlerErrorConsumer);
                    crawler.getProcessor().setAddFileConsumer(addFileConsumer);
                    crawler.getProcessor().getProcessors().forEach(p -> p.setConsumers(httpProcessorRefreshConsumer, httpProcessorSuccessConsumer, httpProcessorErrorConsumer));

                    crawler.processGamesList(gameEntries);

                    if (crawler.isSuspended()) {
                        activity.setFileEntries(filesTableView.getItems().stream().filter(f -> !f.isCompleted()).collect(Collectors.toList()));
                        FileUtils.saveAsJson(getSourceDir(getSource()), "activity", activity);
                        Platform.runLater(() -> stageManager.showInformationAlert("Suspended title", "Suspended header", ""));

                    } else if (crawler.isAborted()) {
                        FileUtils.deleteJsonFile(getSourceDir(getSource()), "activity");
                        Platform.runLater(() -> stageManager.showInformationAlert("Aborted title", "Aborted header", ""));

                    } else {
                        crawler.getProcessor().finalizeProcessors();
                        FileUtils.deleteJsonFile(getSourceDir(getSource()), "activity");
                        Platform.runLater(() -> {
                            errorsMap = new TreeMap<>();
                            gamesTableView.getItems().forEach(g -> errorsMap.put(g.getTitle(), g.getExceptions()));
                            filesTableView.getItems().forEach(f -> errorsMap.put(f.getHost() + f.getUri(), f.getExceptions()));
                            if (errorsMap.isEmpty()) {
                                stageManager.showInformationAlert("Congratulations!", "You have successfully processed all the data!", "");
                            } else {
                                stageManager.showNewWindow(FxmlView.ACTIVITY_ERRORS);
                            }
                        });
                    }
                    updatePlatforms(gameEntries);
                    activity = null;
                    Platform.runLater(() -> stageManager.showPane(FxmlView.PLATFORMS));

                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> stageManager.showErrorAlert("Error", "Stack trace", e));
                }
                return null;
            }
        };

        new Thread(task).start();
    }

    private final Consumer<GameEntry> crawlerRefreshConsumer = new Consumer<GameEntry>() {
        @Override
        public void accept(GameEntry gameEntry) {
            try {
                Platform.runLater(() -> {
                    gamesTableView.getSelectionModel().select(gameEntry);
                    //refresh();
                });
            } catch (Exception ignored) {

            }
        }
    };

    private final Consumer<GameEntry> crawlerSuccessConsumer = new Consumer<GameEntry>() {
        @Override
        public void accept(GameEntry gameEntry) {
            try {
                Platform.runLater(() -> {
                    gamesTableView.getItems().remove(gameEntry);
                    //refresh();
                });
            } catch (Exception ignored) {

            }
        }
    };

    private final Consumer<GameEntry> crawlerErrorConsumer = new Consumer<GameEntry>() {
        @Override
        public void accept(GameEntry gameEntry) {
            Platform.runLater(() -> {
                Throwable throwable = gameEntry.getExceptions().get(gameEntry.getExceptions().size() - 1);
                addLog(String.format("%s:%s: %s", gameEntry.getPlatformId(), gameEntry.getTitle(), throwable.getMessage()));
                gamesTableView.getItems().remove(gameEntry);
                gamesTableView.getItems().add(gameEntry);
                //refresh();
            });
        }
    };

    private final Consumer<FileEntry> addFileConsumer = new Consumer<FileEntry>() {
        @Override
        public void accept(FileEntry file) {
            filesTableView.getItems().add(file);
        }
    };

    private final Consumer<FileEntry> httpProcessorRefreshConsumer = new Consumer<FileEntry>() {
        @Override
        public void accept(FileEntry file) {
            if (file != null) {
                Platform.runLater(() -> filesTableView.getSelectionModel().select(file));
            }
            refresh();
        }
    };

    private final Consumer<FileEntry> httpProcessorSuccessConsumer = new Consumer<FileEntry>() {
        @Override
        public void accept(FileEntry file) {
            if (file != null) {
                Platform.runLater(() -> filesTableView.getItems().remove(file));
            }
            refresh();
        }
    };

    private final Consumer<FileEntry> httpProcessorErrorConsumer = new Consumer<FileEntry>() {
        @Override
        public void accept(FileEntry file) {
            Platform.runLater(() -> {
                filesTableView.getItems().remove(file);
                filesTableView.getItems().add(file);
                addLog(String.format("%s:%s: %s", file.getPlatformId(), file.getHost() + file.getUri(),
                        file.getExceptions().get(file.getExceptions().size() - 1)));
            });
            refresh();
        }
    };

    private void refresh() {
        logsTableView.refresh();
        processorsTableView.refresh();
        gamesTableView.refresh();
        filesTableView.refresh();
        showSelected();
    }

    private void showSelected() {
        Platform.runLater(() -> {
            Skin<?> skin = gamesTableView.getSkin();
            if (skin instanceof SkinBase) {
                ((VirtualFlow<?>) ((SkinBase<?>) skin).getChildren().get(1)).show(gamesTableView.getSelectionModel().getSelectedIndex());
            }
            skin = filesTableView.getSkin();
            if (skin instanceof SkinBase) {
                ((VirtualFlow<?>) ((SkinBase<?>) skin).getChildren().get(1)).show(filesTableView.getSelectionModel().getSelectedIndex());
            }
        });
    }

    private void addLog(String message) {
        Platform.runLater(() -> {
            logsTableView.getItems().add(message);
            logsTableView.scrollTo(logsTableView.getItems().size() - 1);
        });
    }

    private void updatePlatforms(List<GameEntry> gameEntries) {

        activity.getPlatforms().forEach(p -> {
            List<GameEntry> entries = gameEntries.stream().filter(g -> g.getPlatformId().equals(p)).collect(Collectors.toList());
            long brokenImages = filesTableView.getItems().stream().filter(f -> f.getPlatformId().equals(p)).count();
            platformsById.get(p).setTotal(entries.size());
            platformsById.get(p).setCompleted(entries.stream().filter(GameEntry::isCompleted).count() - brokenImages);
            platformsById.get(p).setDate(LocalDateTime.now());
            try {
                crawler.savePlatformsList(platforms);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }

    public void suspendButtonClick() {
        crawler.setSuspended(true);
    }

    public void abortButtonClick() {
        crawler.setAborted(true);
    }
}
