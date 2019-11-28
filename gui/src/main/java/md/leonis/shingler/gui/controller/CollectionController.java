package md.leonis.shingler.gui.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import md.leonis.shingler.Main1024a;
import md.leonis.shingler.gui.controls.SmartDirectoryChooser;
import md.leonis.shingler.gui.view.FxmlView;
import md.leonis.shingler.gui.view.StageManager;
import md.leonis.shingler.model.CollectionType;
import md.leonis.shingler.model.ConfigHolder;
import md.leonis.shingler.model.GID;
import md.leonis.shingler.model.RomsCollection;
import md.leonis.shingler.utils.IOUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static md.leonis.shingler.gui.view.StageManager.runInBackground;
import static md.leonis.shingler.model.ConfigHolder.*;

@Controller
public class CollectionController {

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
    public Button deleteCollectionButton;
    public Button renameCollectionButton;
    public Button typeButton;

    public Button selectCollectionFilesButton;
    public Button scanCollectionFilesButton;
    public Button scanCollectionHashesButton;
    public Button generateShinglesButton;
    public Button compareCollectionsButton;
    public Button manageFamiliesButton;

    private final StageManager stageManager;
    private final ConfigHolder configHolder;

    private boolean showPlatforms = true;

    @Lazy
    public CollectionController(StageManager stageManager, ConfigHolder configHolder) {
        this.stageManager = stageManager;
        this.configHolder = configHolder;
    }

    @FXML
    private void initialize() {

        //templateController.getSelectAllButton().setOnAction(event -> selectAllClick());
        //logController.getSelectedLevelsListenerHandles().registerListener(event -> refreshWebView());

        workDirLabel.setText(rootWorkDir.toString());

        collectionsView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        collectionsView.setOnMouseClicked(this::handleCollectionsViewMouseClicked);

        showPlatforms();
    }

    private void handleCollectionsViewMouseClicked(MouseEvent click) {

        int selectedCount = collectionsView.getSelectionModel().getSelectedItems().size();

        if (selectedCount > 1) {
            textArea.setText(selectedCount + " items are selected");
            return;
        }

        boolean dblClick = (click.getClickCount() == 2);
        String selectedItem = collectionsView.getSelectionModel().getSelectedItem();

        if (selectedItem != null) {
            if (selectedItem.equals("..")) {
                handleLevelUpClick(dblClick);
            } else {
                handleItemClick(dblClick, selectedItem);
            }
        }
    }

    private void handleLevelUpClick(boolean dblClick) {

        if (dblClick) {
            showPlatforms();
        } else {
            textArea.setText("");
        }
    }

    private void handleItemClick(boolean dblClick, String selectedItem) {

        if (showPlatforms) {
            if (dblClick) {
                showPlatformCollections(platforms.get(selectedItem).getCpu());
            } else {
                showPlatformStatus(platforms.get(selectedItem).getCpu());
            }
        } else {
            selectCollection(selectedItem);
        }
    }

    private void showPlatforms() {

        showPlatforms = true;
        platform = null;
        collectionsView.setItems(FXCollections.observableArrayList(platforms.keySet()));

        if (!platforms.isEmpty()) {
            collectionsView.getSelectionModel().selectFirst();
            showPlatformStatus(platforms.keySet().iterator().next());
        }
    }

    private void showPlatformStatus(String selectedItem) {

        platform = selectedItem;
        List<Path> collections = IOUtils.listFiles(workCollectionsPath()).stream().filter(f -> !f.getFileName().toString().endsWith(".bak")).collect(Collectors.toList());
        textArea.setText("Collections: " + collections.size());
    }

    private void showPlatformCollections(String selectedItem) {

        showPlatforms = false;
        platform = selectedItem;
        collectionsView.setItems(FXCollections.observableArrayList(".."));

        List<Path> collections = IOUtils.listFiles(workCollectionsPath()).stream().filter(f -> !f.getFileName().toString().endsWith(".bak")).collect(Collectors.toList());
        collectionsView.getItems().addAll(FXCollections.observableArrayList(collections.stream().map(c -> c.getFileName().toString()).collect(Collectors.toList())));
        //collectionsView.getSelectionModel().selectFirst();
        textArea.setText("");
    }

    private void selectCollection(String selectedItem) {

        collection = selectedItem;
        loadCollection();
        showCollection();
    }

    private void showCollection() {

        textArea.setText(collection);
        textArea.appendText("\nType: " + romsCollection.getType());
        textArea.appendText("\nRoms path: " + (null == romsCollection.getRomsPathString() ? "---" : romsCollection.getRomsPathString()));
        boolean isEmpty = romsCollection.getGidsMap().isEmpty();
        textArea.appendText("\nRoms count: " + (isEmpty ? "---" : romsCollection.getGidsMap().size()));

        typeButton.setText(romsCollection.getType().toString());

        boolean hasShaHash = !isEmpty && !(null == romsCollection.getGidsMap().values().iterator().next().getSha1());
        boolean hasCrcHash = !isEmpty && !(null == romsCollection.getGidsMap().values().iterator().next().getCrc32());

        if (hasShaHash) {
            textArea.appendText("\nHashes: YES");
        } else if (hasCrcHash) {
            textArea.appendText("\nHashes: CRC only");
        } else {
            textArea.appendText("\nHashes: ---");
        }

        textArea.appendText("\nFamilies: " + (isEmpty || null == romsCollection.getGidsMap().values().iterator().next().getFamily() ? "---" : "YES"));
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

        collection = "untitled";
        romsCollection = new RomsCollection();
        romsCollection.setTitle(collection);
        romsCollection.setPlatform(platform);
        saveCollection();

        showPlatformCollections(platform);
    }

    public void deleteCollectionButtonClick() {

        String currentCollection = collectionsView.getSelectionModel().getSelectedItem();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation Dialog");
        alert.setHeaderText("Look, a Confirmation Dialog");
        alert.setContentText(String.format("Do you really want to delete %s collection?", currentCollection));

        alert.showAndWait().map(result -> {
            if (result == ButtonType.OK) {
                collection = null;
                deleteCollection(currentCollection);
                showPlatformCollections(platform);
            }
            return null;
        });
    }

    //TODO rename, delete, type implement in separate management window
    public void renameCollectionButtonClick() {

        String currentCollection = collectionsView.getSelectionModel().getSelectedItem();
        TextInputDialog dialog = stageManager.getTextInputDialog("Text Input Dialog", "Look, a Text Input Dialog", "Please enter collection name:", currentCollection);

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            collection = result.get();
            romsCollection.setTitle(collection);

            saveCollection();
            deleteCollection(currentCollection);

            showPlatformCollections(platform);
            collectionsView.getSelectionModel().select(collection);
            showCollection();
        }
    }

    private void loadCollection() {
        romsCollection = IOUtils.loadCollectionAsJson(fullCollectionsPath().toFile());
    }

    private void saveCollection() {
        IOUtils.serializeAsJson(fullCollectionsPath().toFile(), romsCollection);
    }

    private void deleteCollection(String currentCollection) {
        IOUtils.deleteFile(workCollectionsPath().resolve(currentCollection));
    }

    public void typeButtonClick() {

        CollectionType type = CollectionType.valueOf(typeButton.getText());
        int id = type.ordinal();
        id = (id == CollectionType.values().length - 1) ? 0 : id + 1;
        type = CollectionType.values()[id];
        typeButton.setText(type.name());
        romsCollection.setType(type);

        saveCollection();
        showCollection();
    }

    public void selectCollectionFilesButtonClick() {

        Stage stage = (Stage) gamesToFamilyButton.getScene().getWindow();
        SmartDirectoryChooser directoryChooser = stageManager.getDirectoryChooser("Select directory with unpacked games", romsCollection.getRomsPath());
        File dir = directoryChooser.showDialog(stage);

        if (dir != null) {
            romsCollection.setRomsPathString(dir.getAbsolutePath());
            scanCollectionFilesButtonClick();
        }
    }

    public void scanCollectionFilesButtonClick() {

        runInBackground(() -> {
            switch (romsCollection.getType()) {
                case PLAIN:
                    List<Path> romsPaths = IOUtils.listFiles(Paths.get(romsCollection.getRomsPathString()));
                    Map<String, GID> gids = Main1024a.GIDsFromFiles(romsPaths);
                    romsCollection.setGidsMap(gids);
                    break;
                case MERGED:
                    List<Path> familiesPaths = IOUtils.listFiles(Paths.get(romsCollection.getRomsPathString()));
                    Map<String, GID> mergedGids = Main1024a.GIDsFromMergedFile(familiesPaths.stream().filter(f -> f.getFileName().toString().endsWith(".7z")).collect(Collectors.toList()));
                    romsCollection.setGidsMap(mergedGids);
                    break;
            }
            saveCollection();

        }, this::showCollection);
    }

    public void scanCollectionHashesButtonClick() {

        runInBackground(() -> {
            try {
                switch (romsCollection.getType()) {
                    case PLAIN:
                        //TODO may be use saved GIDs
                        List<Path> romsPaths = IOUtils.listFiles(Paths.get(romsCollection.getRomsPathString()));
                        Map<String, GID> gids = Main1024a.calculateHashes(romsPaths);
                        romsCollection.setGidsMap(gids);
                        break;
                    case MERGED:
                        List<Path> familiesPaths = IOUtils.listFiles(Paths.get(romsCollection.getRomsPathString()));
                        Map<String, GID> mergedGids = Main1024a.calculateMergedHashes(familiesPaths.stream().filter(f -> f.getFileName().toString().endsWith(".7z")).collect(Collectors.toList()));
                        romsCollection.setGidsMap(mergedGids);
                        break;
                }

                saveCollection();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, this::showCollection);
    }

    public void generateShinglesButtonClick() {

        Main1024a.createSampleDirs(workShinglesPath());

        Path romsFolder = Paths.get(romsCollection.getRomsPathString());

        runInBackground(() -> {
            try {
                Main1024a.generateShinglesNio(romsCollection, romsFolder, workShinglesPath());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, this::showCollection);
        //TODO show something?
    }

    public void compareCollectionsButtonClick() {
        //TODO initialize COMPARE controller directly
        selectedCollections = collectionsView.getSelectionModel().getSelectedItems();
        stageManager.showNewWindow(FxmlView.COMPARE);
    }

    public void manageFamiliesButtonClick() {
        stageManager.showPane(FxmlView.FAMILY);
        //stageManager.showNewWindow(FxmlView.FAMILY);
    }
}
