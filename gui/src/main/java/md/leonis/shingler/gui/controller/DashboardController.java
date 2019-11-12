package md.leonis.shingler.gui.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import md.leonis.shingler.GID;
import md.leonis.shingler.Main1024a;
import md.leonis.shingler.gui.config.ConfigHolder;
import md.leonis.shingler.gui.controller.template.LogController;
import md.leonis.shingler.gui.service.TestService;
import md.leonis.shingler.gui.view.FxmlView;
import md.leonis.shingler.gui.view.StageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static md.leonis.shingler.gui.config.ConfigHolder.*;

@Controller
public class DashboardController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DashboardController.class);

    @FXML
    public Button newProjectButton;
    @FXML
    public Button openProjectButton;
    @FXML
    public Button gamesToFamilyButton;
    @FXML
    public Button goodMergedToFamilyButton;
    public Label workDirLabel;
    public Button changeWorkDirButton;
    public Button newCollectionButton;
    public Button openCollectionButton;
    public TextArea textArea;
    public Button renameCollectionButton;
    public ListView<String> collectionsView;
    public Button deleteCollectionButton;
    public Button scanCollectionFilesButton;
    public Button scanCollectionHashesButton;
    public Button generateShinglesButton;
    public Button selectCollectionFilesButton;
    public Button compareCollectionsButton;
    public Button typeButton;

    @FXML
    private AnchorPane anchorPane;

    /*@FXML
    public TextArea infoTextArea;*/

    private final StageManager stageManager;

    private final ConfigHolder configHolder;

    private final TestService testService;

    @Lazy
    public DashboardController(StageManager stageManager, ConfigHolder configHolder, TestService testService) {
        this.stageManager = stageManager;
        this.configHolder = configHolder;
        this.testService = testService;
    }

    private LinkedHashMap<String, String> platforms;

    private int level = 0;
    private Main1024a.RomsCollection romsCollection;

    @FXML
    private void initialize() {
        int count = testService.getDictionaries().size();
        //infoTextArea.setText(String.format("Dictionaries in DB:\n%s\n\nWords to learn:\n%s", count, configHolder.getWordsToLearnCount()));
        LogController logController = new LogController(stageManager);
        anchorPane.getChildren().add(logController);
        //templateController.getSelectAllButton().setOnAction(event -> selectAllClick());
        //logController.getSelectedLevelsListenerHandles().registerListener(event -> refreshWebView());


        //TODO config
        userHome = Paths.get(System.getProperty("user.home"));
        rootWorkDir = userHome.resolve("shingler");
        shinglesDir = rootWorkDir.resolve("shingles");
        collectionsDir = rootWorkDir.resolve("collections");
        workDirLabel.setText(rootWorkDir.toString());

        //TODO read from disk
        platforms = new LinkedHashMap<>();
        platforms.put("NES", "nes");
        platforms.put("TEST", "test");

        Main1024a.createDirectory(collectionsDir);
        platforms.values().forEach(p -> Main1024a.createDirectory(collectionsDir.resolve(p)));

        showPlatforms();

        collectionsView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        collectionsView.setOnMouseClicked(click -> {

            int selectedCount = collectionsView.getSelectionModel().getSelectedItems().size();

            if (selectedCount > 1) {
                textArea.setText(selectedCount + " collections are selected");
                return;
            }

            boolean dblClick = click.getClickCount() == 2;
            String selectedItem = collectionsView.getSelectionModel().getSelectedItem();

            if (selectedItem.equals("..")) {
                if (dblClick) {
                    showPlatforms();
                } else {
                    textArea.setText("");
                }
            } else {
                if (level == 0) {
                    if (dblClick) {
                        showCollections(platforms.get(selectedItem));
                    } else {
                        showPlatform(platforms.get(selectedItem));
                    }
                } else {
                    loadCollection(selectedItem);
                }
            }
        });
    }


    private void showPlatforms() {
        level = 0;
        platform = null;
        collectionsView.setItems(FXCollections.observableArrayList(platforms.keySet()));

        if (!platforms.isEmpty()) {
            collectionsView.getSelectionModel().selectFirst();
            showPlatform(platforms.keySet().iterator().next());
        }
    }

    private void showPlatform(String selectedItem) {
        platform = selectedItem;
        List<Path> collections = Main1024a.listFilesForFolder(collectionsDir.resolve(platform));
        textArea.setText("Collections: " + collections.size());
    }

    private void showCollections(String selectedItem) {
        level = 1;
        platform = selectedItem;
        collectionsView.setItems(FXCollections.observableArrayList(".."));

        Path workCollectionsDir = collectionsDir.resolve(platform);
        List<Path> collections = Main1024a.listFilesForFolder(workCollectionsDir);
        collectionsView.getItems().addAll(FXCollections.observableArrayList(collections.stream().map(c -> c.getFileName().toString()).collect(Collectors.toList())));
        collectionsView.getSelectionModel().selectFirst();
        textArea.setText("");
    }

    private void loadCollection(String selectedItem) {
        collection = selectedItem;

        Path workCollectionsDir = collectionsDir.resolve(platform);
        romsCollection = Main1024a.readCollectionFromFile(workCollectionsDir.resolve(collection).toFile());

        showCollection();
    }

    private void showCollection() {
        textArea.setText(collection);
        textArea.appendText("\nType: " + romsCollection.getType());
        textArea.appendText("\nRoms path: " + (null == romsCollection.getRomsPath() ? "---" : romsCollection.getRomsPath()));
        boolean isEmpty = romsCollection.getGids().isEmpty();
        textArea.appendText("\nRoms count: " + (isEmpty ? "---" : romsCollection.getGids().size()));

        boolean hasShaHash = !isEmpty && !(null == romsCollection.getGids().values().iterator().next().getSha1());
        boolean hasCrcHash = !isEmpty && !(null == romsCollection.getGids().values().iterator().next().getCrc32());

        if (hasShaHash) {
            textArea.appendText("\nHashes: YES");
        } else if (hasCrcHash) {
            textArea.appendText("\nHashes: CRC only");
        } else {
            textArea.appendText("\nHashes: ---");
        }

        textArea.appendText("\nFamilies: " + (isEmpty || null == romsCollection.getGids().values().iterator().next().getFamily() ? "---" : "YES"));
        //TODO verify shingles count
        textArea.appendText("\nShingles: TODO");
    }

    public void windowShow() {
        stageManager.showNewWindow(FxmlView.WINDOW);
    }

    public void showNotification() {

        LOGGER.info("DashboardController|24.15123214");
        //stageManager.showInformationAlert("Title: selected all", "Header: selected all", "some content");
    }


    public void showNotification2() {

        LOGGER.info("DashboardController");
        //stageManager.showInformationAlert("Title: selected all", "Header: selected all", "some content");
    }

    public void newProjectButtonClick(ActionEvent actionEvent) {
    }

    public void openProjectButtonClick(ActionEvent actionEvent) {
    }

    public void goodMergedToFamilyButtonClick(ActionEvent actionEvent) {

    }

    public void changeWorkDirButtonClick(ActionEvent actionEvent) {
    }

    public void newCollectionClick(ActionEvent actionEvent) {
        Main1024a.RomsCollection collection = new Main1024a.RomsCollection();
        collection.setTitle("untitled");
        collection.setPlatform(platform);
        Main1024a.serialize(collectionsDir.resolve(platform).resolve(collection.getTitle()).toFile(), collection);

        showCollections(platform);
    }

    public void renameCollectionButtonClick(ActionEvent actionEvent) {
        String currentCollection = collectionsView.getSelectionModel().getSelectedItem();
        TextInputDialog dialog = new TextInputDialog(currentCollection);
        dialog.setTitle("Text Input Dialog");
        dialog.setHeaderText("Look, a Text Input Dialog");
        dialog.setContentText("Please enter collection name:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            collection = result.get();
            romsCollection.setTitle(collection);

            Path workCollectionsDir = collectionsDir.resolve(platform);
            Main1024a.serialize(workCollectionsDir.resolve(collection).toFile(), romsCollection);

            try {
                Files.delete(workCollectionsDir.resolve(currentCollection));
            } catch (IOException e) {
                e.printStackTrace();
            }

            showCollections(platform);
            collectionsView.getSelectionModel().select(collection);
            showCollection();
        }
    }

    public void typeButtonClick(ActionEvent actionEvent) {
        Main1024a.CollectionType type = Main1024a.CollectionType.valueOf(typeButton.getText());
        int id = type.ordinal();
        id = (id == Main1024a.CollectionType.values().length - 1) ? 0 : id + 1;
        type = Main1024a.CollectionType.values()[id];
        typeButton.setText(type.name());
        romsCollection.setType(type);

        Path workCollectionsDir = collectionsDir.resolve(platform);
        Main1024a.serialize(workCollectionsDir.resolve(collection).toFile(), romsCollection);
        showCollection();
    }

    //TODO delete???
    public void openCollectionClick(ActionEvent actionEvent) {
    }

    public void deleteCollectionButtonClick(ActionEvent actionEvent) {
        String currentCollection = collectionsView.getSelectionModel().getSelectedItem();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation Dialog");
        alert.setHeaderText("Look, a Confirmation Dialog");
        alert.setContentText(String.format("Do you really want to delete %s collection?", currentCollection));

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            Path workCollectionsDir = collectionsDir.resolve(platform);

            try {
                Files.delete(workCollectionsDir.resolve(currentCollection));
            } catch (IOException e) {
                e.printStackTrace();
            }

            showCollections(platform);
            collection = null;
            collectionsView.getSelectionModel().select(0);
            textArea.setText("");
        }
    }

    public void selectCollectionFilesButtonClick(ActionEvent actionEvent) {
        Stage stage = (Stage) gamesToFamilyButton.getScene().getWindow();
        DirectoryChooser directoryChooser = configHolder.getDirectoryChooser("Select directory with unpacked games");
        File dir = directoryChooser.showDialog(stage);
        if (dir != null) {
            romsCollection.setRomsPath(dir.getAbsolutePath());

            scanCollectionFilesButtonClick(null);
        }
    }

    public void scanCollectionFilesButtonClick(ActionEvent actionEvent) {

        Thread thread = new Thread(() -> {
            switch (romsCollection.getType()) {
                case PLAIN:
                    List<Path> romsPaths = Main1024a.listFilesForFolder(Paths.get(romsCollection.getRomsPath()));
                    Map<String, GID> gids = Main1024a.filesToGid(romsPaths);
                    romsCollection.setGids(gids);
                    break;
                case MERGED:
                    List<Path> familiesPaths = Main1024a.listFilesForFolder(Paths.get(romsCollection.getRomsPath()));
                    Map<String, GID> mergedGids = Main1024a.mergedFilesToGid(familiesPaths.stream().filter(f -> f.getFileName().toString().endsWith(".7z")).collect(Collectors.toList()));
                    romsCollection.setGids(mergedGids);
                    break;
            }

            Path workCollectionsDir = collectionsDir.resolve(platform);
            Main1024a.serialize(workCollectionsDir.resolve(collection).toFile(), romsCollection);

            showCollection();
        });
        thread.setDaemon(true);
        thread.start();
    }

    public void scanCollectionHashesButtonClick(ActionEvent actionEvent) {

        Thread thread = new Thread(() -> {
            try {
                switch (romsCollection.getType()) {
                    case PLAIN:
                        List<Path> romsPaths = Main1024a.listFilesForFolder(Paths.get(romsCollection.getRomsPath()));
                        Map<String, GID> gids = Main1024a.calculateHashes(romsPaths);
                        romsCollection.setGids(gids);
                    case MERGED:
                        List<Path> familiesPaths = Main1024a.listFilesForFolder(Paths.get(romsCollection.getRomsPath()));
                        Map<String, GID> mergedGids = Main1024a.calculateMergedHashes(familiesPaths.stream().filter(f -> f.getFileName().toString().endsWith(".7z")).collect(Collectors.toList()));
                        romsCollection.setGids(mergedGids);
                        break;
                }

                Path workCollectionsDir = collectionsDir.resolve(platform);
                Main1024a.serialize(workCollectionsDir.resolve(collection).toFile(), romsCollection);

                showCollection();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public void generateShinglesButtonClick(ActionEvent actionEvent) {
        Path workShinglesDir = shinglesDir.resolve(platform);
        Main1024a.createSampleDirs(workShinglesDir);

        Path romsFolder = Paths.get(romsCollection.getRomsPath());

        Thread thread = new Thread(() -> {
            try {
                Main1024a.generateShinglesNio(romsCollection, romsFolder, workShinglesDir);

                showCollection();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public void compareCollectionsButtonClick(ActionEvent actionEvent) {
        selectedCollections = collectionsView.getSelectionModel().getSelectedItems();
        stageManager.showNewWindow(FxmlView.COMPARE);
    }

    //TODO delete if not need
    public void gamesToFamilyButtonClick(ActionEvent actionEvent) {
    }
}
