package md.leonis.crawler.moby.controller;

import com.sun.javafx.scene.control.skin.VirtualFlow;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import md.leonis.crawler.moby.HttpProcessor;
import md.leonis.crawler.moby.config.ConfigHolder;
import md.leonis.crawler.moby.crawler.Crawler;
import md.leonis.crawler.moby.dto.FileEntry;
import md.leonis.crawler.moby.model.GameEntry;
import md.leonis.crawler.moby.model.GameInitialEntry;
import md.leonis.crawler.moby.view.FxmlView;
import md.leonis.crawler.moby.view.StageManager;
import md.leonis.shingler.utils.FileUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    public Button closeButton;

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

    private List<GameEntry> gameEntries;
    private ObservableList<GameEntry> gameEntryOList;
    private ObservableList<FileEntry> fileEntryOList;
    private ObservableList<String> logsOList;

    private Crawler crawler;

    @Lazy
    public ActivityController(StageManager stageManager, ConfigHolder configHolder) {
        this.stageManager = stageManager;
        this.configHolder = configHolder;
    }

    @FXML
    private void initialize() throws Exception {

        suspendButton.setDisable(false);
        abortButton.setDisable(false);
        closeButton.setDisable(true);

        crawler = getCrawler();

        platformTableColumn.setCellValueFactory(new PropertyValueFactory<>("platformId"));
        //gameTableColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        gameTableColumn.setCellValueFactory(g -> new SimpleStringProperty(g.getValue().getTitle() + "\t\t" + g.getValue().getErrorsCount()));

        platformFileTableColumn.setCellValueFactory(new PropertyValueFactory<>("platformId"));
        fileTableColumn.setCellValueFactory(f -> new SimpleStringProperty(f.getValue().getHost() + f.getValue().getUri() + "\t\t" + f.getValue().getErrorsCount()));

        processorTableColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        processorFileTableColumn.setCellValueFactory(new PropertyValueFactory<>("file"));

        logsTableColumn.setCellValueFactory(l -> new ReadOnlyStringWrapper(l.getValue()));
        logsTableView.setItems(FXCollections.observableArrayList(new ArrayList<>()));

        //TODO use activity task

        //TODO show diff

        gameEntries = crawler.getGamesList(activity.getPlatforms());
        gameEntryOList = FXCollections.observableList(gameEntries);
        gamesTableView.setItems(gameEntryOList);

        fileEntryOList = FXCollections.observableList(activity.getFileEntries());
        filesTableView.setItems(fileEntryOList);

        ObservableList<HttpProcessor> processorOList = FXCollections.observableList(crawler.getProcessor().getProcessors());
        processorsTableView.setItems(processorOList);

        logsOList = FXCollections.observableArrayList();
        logsTableView.setItems(logsOList);

        Task<Void> task = new Task<Void>() {
            @Override
            public Void call() {
                try {
                    crawler.setConsumers(crawlerRefreshConsumer, crawlerSuccessConsumer, crawlerErrorConsumer);
                    crawler.getProcessor().setAddFileConsumer(addFileConsumer);
                    crawler.getProcessor().getProcessors().forEach(p -> p.setConsumers(httpProcessorRefreshConsumer, httpProcessorSuccessConsumer, httpProcessorErrorConsumer));

                    crawler.processGamesList(new ArrayList<>(gameEntries), false);

                    if (crawler.isSuspended()) {
                        activity.setFileEntries(fileEntryOList.stream().filter(f -> !f.isCompleted()).collect(Collectors.toList()));
                        FileUtils.saveAsJson(getSourceDir(getSource()), "activity", activity);
                        Platform.runLater(() -> stageManager.showInformationAlert("Suspended title", "Suspended header", ""));

                    } else if (crawler.isAborted()) {
                        FileUtils.deleteJsonFile(getSourceDir(getSource()), "activity");
                        Platform.runLater(() -> stageManager.showInformationAlert("Aborted title", "Aborted header", ""));

                    } else {
                        crawler.getProcessor().finalizeProcessors();
                        FileUtils.deleteJsonFile(getSourceDir(getSource()), "activity");
                        Platform.runLater(() -> {
                            refresh();
                            errorsMap = new TreeMap<>();
                            gameEntryOList.stream().filter(Objects::nonNull).forEach(g -> errorsMap.put(g.getTitle(), g.getExceptions()));
                            fileEntryOList.stream().filter(Objects::nonNull).forEach(f -> errorsMap.put(f.getHost() + f.getUri(), f.getExceptions()));
                            if (errorsMap.isEmpty()) {
                                stageManager.showInformationAlert("Congratulations!", "You have successfully processed all games!", "");
                            } else {
                                List<GameInitialEntry> entries = gameEntryOList.stream()
                                        .filter(g -> !g.getExceptions().isEmpty()).map(GameInitialEntry::new).collect(Collectors.toList());
                                try {
                                    FileUtils.saveAsJson(getSourceDir(getSource()), "brokenGames", entries);
                                    FileUtils.saveAsJson(getSourceDir(getSource()), "brokenImages",
                                            fileEntryOList.stream().filter(Objects::nonNull).map(f -> f.getHost() + f.getUri()).collect(Collectors.toList()));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                stageManager.showNewWindow(FxmlView.ACTIVITY_ERRORS);
                            }
                        });
                    }
                    updatePlatforms(gameEntryOList);
                    activity = null;
                    suspendButton.setDisable(true);
                    abortButton.setDisable(true);
                    closeButton.setDisable(false);

                } catch (Exception e) {
                    e.printStackTrace();
                    Platform.runLater(() -> stageManager.showErrorAlert("Error", e.getMessage(), e));
                }
                return null;
            }
        };

        new Thread(task).start();
    }

    private final Consumer<GameEntry> crawlerRefreshConsumer = gameEntry -> {
        /*try {
            Platform.runLater(() -> {
                    gamesTableView.getSelectionModel().select(gameEntry);
                //refresh();
            });
        } catch (Exception ignored) {

        }*/
    };

    private final Consumer<GameEntry> crawlerSuccessConsumer = gameEntry -> gameEntryOp(false, gameEntry);

    private final Consumer<GameEntry> crawlerErrorConsumer = gameEntry -> {
        Throwable throwable = gameEntry.getExceptions().get(gameEntry.getExceptions().size() - 1);
        addLog(String.format("%s:%s: %s", gameEntry.getPlatformId(), gameEntry.getTitle(), throwable.getMessage()));
        gameEntryOp(false, gameEntry);
        gameEntryOp(true, gameEntry);
        //refresh();
    };

    synchronized private void gameEntryOp(boolean add, GameEntry gameEntry) {
        Platform.runLater(() -> {
            if (add) {
                gameEntryOList.add(gameEntry);
            } else {
                gameEntryOList.remove(gameEntry);
            }
        });
    }

    private final Consumer<FileEntry> addFileConsumer = file -> fileEntryOp(true, file);

    private final Consumer<FileEntry> httpProcessorRefreshConsumer = file -> {
            /*if (file != null) {
                Platform.runLater(() -> {
                    //try {
                        filesTableView.getSelectionModel().select(file);
                    //} catch (Exception ignored) {
                    //}
                });
            }
            refresh();*/
    };

    private final Consumer<FileEntry> httpProcessorSuccessConsumer = file -> {
        fileEntryOp(false, file);
        refresh();
    };

    private final Consumer<FileEntry> httpProcessorErrorConsumer = file -> {
        fileEntryOp(false, file);
        fileEntryOp(true, file);
        addLog(String.format("%s:%s: %s", file.getPlatformId(), file.getHost() + file.getUri(),
                file.getExceptions().get(file.getExceptions().size() - 1)));
        refresh();
    };

    synchronized private void fileEntryOp(boolean add, FileEntry file) {
        Platform.runLater(() -> {
            if (add) {
                fileEntryOList.add(file);
            } else {
                fileEntryOList.remove(file);
            }
        });
    }

    private void refresh() {
        logsTableView.refresh();
        processorsTableView.refresh();
        gamesTableView.refresh();
        filesTableView.refresh();
        //showSelected();
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
            logsOList.add(message);
            logsTableView.scrollTo(logsTableView.getItems().size() - 1);
        });
    }

    private void updatePlatforms(List<GameEntry> gameEntries) {
        activity.getPlatforms().forEach(p -> {
            List<GameEntry> entries = gameEntries.stream().filter(g -> g.getPlatformId().equals(p)).collect(Collectors.toList());
            long brokenImages = fileEntryOList.stream().filter(f -> f != null && f.getPlatformId().equals(p)).count();
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

    public void closeButtonClick() {
        Platform.runLater(() -> stageManager.showPane(FxmlView.PLATFORMS));
    }
}
