package md.leonis.shingler.gui.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import md.leonis.shingler.Main1024a;
import md.leonis.shingler.gui.config.ConfigHolder;
import md.leonis.shingler.gui.controller.template.LogController;
import md.leonis.shingler.gui.view.FxmlView;
import md.leonis.shingler.gui.view.StageManager;
import md.leonis.shingler.model.CollectionType;
import md.leonis.shingler.model.GID;
import md.leonis.shingler.model.RomsCollection;
import md.leonis.shingler.utils.IOUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static md.leonis.shingler.gui.config.ConfigHolder.*;

@Controller
public class DashboardController {

    //TODO delete or rename
    public Button newProjectButton;
    public Button openProjectButton;
    public Button gamesToFamilyButton;
    public Button goodMergedToFamilyButton;
    
    public Label workDirLabel;
    public Button changeWorkDirButton;
    
    public ListView<String> collectionsView;
    public TextArea textArea;
    
    public Button newCollectionButton;
    public Button openCollectionButton; //TODO
    public Button deleteCollectionButton;
    public Button renameCollectionButton;
    public Button typeButton;
    
    public Button selectCollectionFilesButton;
    public Button scanCollectionFilesButton;
    public Button scanCollectionHashesButton;
    public Button generateShinglesButton;
    public Button compareCollectionsButton;

    @FXML
    private AnchorPane anchorPane;

    private final StageManager stageManager;
    private final ConfigHolder configHolder;

    @Lazy
    public DashboardController(StageManager stageManager, ConfigHolder configHolder) {
        this.stageManager = stageManager;
        this.configHolder = configHolder;
    }

    private LinkedHashMap<String, String> platforms;

    private int level = 0;
    private RomsCollection romsCollection;

    @FXML
    private void initialize() {
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

        IOUtils.createDirectory(collectionsDir);
        platforms.values().forEach(p -> IOUtils.createDirectory(collectionsDir.resolve(p)));

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
        List<Path> collections = IOUtils.listFiles(collectionsDir.resolve(platform));
        textArea.setText("Collections: " + collections.size());
    }

    private void showCollections(String selectedItem) {
        level = 1;
        platform = selectedItem;
        collectionsView.setItems(FXCollections.observableArrayList(".."));

        Path workCollectionsDir = collectionsDir.resolve(platform);
        List<Path> collections = IOUtils.listFiles(workCollectionsDir);
        collectionsView.getItems().addAll(FXCollections.observableArrayList(collections.stream().map(c -> c.getFileName().toString()).collect(Collectors.toList())));
        collectionsView.getSelectionModel().selectFirst();
        textArea.setText("");
    }

    private void loadCollection(String selectedItem) {
        collection = selectedItem;

        Path workCollectionsDir = collectionsDir.resolve(platform);
        romsCollection = IOUtils.loadCollection(workCollectionsDir.resolve(collection).toFile());

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

    public void newProjectButtonClick() {
    }

    public void openProjectButtonClick() {
    }

    //TODO delete if not need
    public void gamesToFamilyButtonClick() {
    }
    
    public void goodMergedToFamilyButtonClick() {

    }

    public void changeWorkDirButtonClick() {
    }

    public void newCollectionClick() {
        RomsCollection collection = new RomsCollection();
        collection.setTitle("untitled");
        collection.setPlatform(platform);
        IOUtils.serialize(collectionsDir.resolve(platform).resolve(collection.getTitle()).toFile(), collection);

        showCollections(platform);
    }

    //TODO delete???
    public void openCollectionClick() {
    }

    public void deleteCollectionButtonClick() {
        String currentCollection = collectionsView.getSelectionModel().getSelectedItem();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation Dialog");
        alert.setHeaderText("Look, a Confirmation Dialog");
        alert.setContentText(String.format("Do you really want to delete %s collection?", currentCollection));

        alert.showAndWait().map(result -> {
            if (result == ButtonType.OK) {
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
            return null;
        });
    }

    public void renameCollectionButtonClick() {
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
            IOUtils.serialize(workCollectionsDir.resolve(collection).toFile(), romsCollection);

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

    public void typeButtonClick() {
        CollectionType type = CollectionType.valueOf(typeButton.getText());
        int id = type.ordinal();
        id = (id == CollectionType.values().length - 1) ? 0 : id + 1;
        type = CollectionType.values()[id];
        typeButton.setText(type.name());
        romsCollection.setType(type);

        Path workCollectionsDir = collectionsDir.resolve(platform);
        IOUtils.serialize(workCollectionsDir.resolve(collection).toFile(), romsCollection);
        showCollection();
    }

    public void selectCollectionFilesButtonClick() {
        Stage stage = (Stage) gamesToFamilyButton.getScene().getWindow();
        DirectoryChooser directoryChooser = configHolder.getDirectoryChooser("Select directory with unpacked games");
        File dir = directoryChooser.showDialog(stage);
        if (dir != null) {
            romsCollection.setRomsPath(dir.getAbsolutePath());

            scanCollectionFilesButtonClick();
        }
    }

    public void scanCollectionFilesButtonClick() {

        Thread thread = new Thread(() -> {
            switch (romsCollection.getType()) {
                case PLAIN:
                    List<Path> romsPaths = IOUtils.listFiles(Paths.get(romsCollection.getRomsPath()));
                    Map<String, GID> gids = Main1024a.GIDsFromFiles(romsPaths);
                    romsCollection.setGids(gids);
                    break;
                case MERGED:
                    List<Path> familiesPaths = IOUtils.listFiles(Paths.get(romsCollection.getRomsPath()));
                    Map<String, GID> mergedGids = Main1024a.GIDsFromMergedFile(familiesPaths.stream().filter(f -> f.getFileName().toString().endsWith(".7z")).collect(Collectors.toList()));
                    romsCollection.setGids(mergedGids);
                    break;
            }

            Path workCollectionsDir = collectionsDir.resolve(platform);
            IOUtils.serialize(workCollectionsDir.resolve(collection).toFile(), romsCollection);

            showCollection();
        });
        thread.setDaemon(true);
        thread.start();
    }

    public void scanCollectionHashesButtonClick() {

        Thread thread = new Thread(() -> {
            try {
                switch (romsCollection.getType()) {
                    case PLAIN:
                        List<Path> romsPaths = IOUtils.listFiles(Paths.get(romsCollection.getRomsPath()));
                        Map<String, GID> gids = Main1024a.calculateHashes(romsPaths);
                        romsCollection.setGids(gids);
                    case MERGED:
                        List<Path> familiesPaths = IOUtils.listFiles(Paths.get(romsCollection.getRomsPath()));
                        Map<String, GID> mergedGids = Main1024a.calculateMergedHashes(familiesPaths.stream().filter(f -> f.getFileName().toString().endsWith(".7z")).collect(Collectors.toList()));
                        romsCollection.setGids(mergedGids);
                        break;
                }

                Path workCollectionsDir = collectionsDir.resolve(platform);
                IOUtils.serialize(workCollectionsDir.resolve(collection).toFile(), romsCollection);

                showCollection();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public void generateShinglesButtonClick() {
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

    public void compareCollectionsButtonClick() {
        selectedCollections = collectionsView.getSelectionModel().getSelectedItems();
        stageManager.showNewWindow(FxmlView.COMPARE);
    }
}
