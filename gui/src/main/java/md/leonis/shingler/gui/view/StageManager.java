package md.leonis.shingler.gui.view;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import md.leonis.shingler.gui.controls.ListViewDialog;
import md.leonis.shingler.gui.controls.SmartChoiceDialog;
import md.leonis.shingler.gui.controls.SmartDirectoryChooser;
import md.leonis.shingler.gui.dto.DialogTexts;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages switching Scenes on the Primary Stage
 */
public class StageManager {

    private static final Logger log = getLogger(StageManager.class);
    private final Stage primaryStage;
    private final SpringFXMLLoader springFXMLLoader;

    public StageManager(SpringFXMLLoader springFXMLLoader, Stage stage) {
        this.springFXMLLoader = springFXMLLoader;
        this.primaryStage = stage;
    }

    public void switchScene(final FxmlView view) {
        switchScene(view, StageStyle.DECORATED);
    }

    public void switchScene(final FxmlView view, StageStyle stageStyle) {
        Parent viewRootNodeHierarchy = loadViewNodeHierarchy(view.getFxmlFile());
        show(viewRootNodeHierarchy, view.getTitle(), stageStyle);
    }

    private void show(final Parent rootnode, String title, StageStyle stageStyle) {
        Scene scene = prepareScene(rootnode);
        //scene.getStylesheets().add("/styles/Styles.css");

        //primaryStage.initStyle(stageStyle);
        primaryStage.setTitle(title);
        primaryStage.setScene(scene);
        primaryStage.sizeToScene();
        primaryStage.centerOnScreen();

        try {
            primaryStage.show();
        } catch (Exception exception) {
            logAndExit ("Unable to show scene for title" + title,  exception);
        }
    }


    //TODO refactor!!!
    public void showNewWindow(final FxmlView view) {
        Parent viewRootNodeHierarchy = loadViewNodeHierarchy(view.getFxmlFile());

        Scene secondScene = new Scene(viewRootNodeHierarchy);
        //Scene secondScene = new Scene(viewRootNodeHierarchy, 230, 100);

        // New window (Stage)
        Stage newWindow = new Stage();
        newWindow.setTitle(view.getTitle());
        newWindow.setScene(secondScene);

        // Set position of second window, related to primary window.
        newWindow.setX(primaryStage.getX() + 200);
        newWindow.setY(primaryStage.getY() + 100);

        newWindow.show();
    }

    private Scene prepareScene(Parent rootNode) {
        Scene scene = primaryStage.getScene();

        if (scene == null) {
            scene = new Scene(rootNode);
            scene.getStylesheets().add("log-view.css");
            return scene;
        }

        scene.setRoot(rootNode);
        return scene;
    }

    /**
     * Loads the object hierarchy from a FXML document and returns to root node
     * of that hierarchy.
     *
     * @return Parent root node of the FXML document hierarchy
     */
    private Parent loadViewNodeHierarchy(String fxmlFilePath) {
        Parent rootNode = null;
        try {
            rootNode = springFXMLLoader.load(fxmlFilePath);
            Objects.requireNonNull(rootNode, "A Root FXML node must not be null");
        } catch (Exception exception) {
            logAndExit("Unable to load FXML view " + fxmlFilePath, exception);
        }
        return rootNode;
    }

    private void logAndExit(String errorMsg, Exception exception) {
        log.error(errorMsg, exception, exception.getCause());
        Platform.exit();
    }

    public void showInformationAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void showWarningAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void showErrorAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void loadTemplate(String templateName, Parent parent, Runnable runnable) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(String.format("/fxml/template/%sTemplate.fxml", templateName)));
        loader.setController(parent);
        loader.setRoot(parent);
        try {
            loader.load();
            runnable.run();
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }
    }

    public static void runInBackground(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        thread.start();
    }


    private final Map<String, File> initialDirs = new HashMap<>();

    public SmartDirectoryChooser getDirectoryChooser(String title) {
        return getDirectoryChooser(title, initialDirs.get(title));
    }

    public SmartDirectoryChooser getDirectoryChooser(String title, Path romsPath) {
        return getDirectoryChooser(title, romsPath == null ? null : romsPath.toFile());
    }

    public SmartDirectoryChooser getDirectoryChooser(String title, File initialDir) {
        SmartDirectoryChooser directoryChooser = new SmartDirectoryChooser(title, initialDir);
        directoryChooser.setOnCloseRequest(e ->
                initialDirs.put(title, ((SmartDirectoryChooser) e.getSource()).getFile())
        );
        return directoryChooser;
    }

    private final Map<DialogTexts, String> inputDialogTexts = new HashMap<>();

    public TextInputDialog getTextInputDialog(String title, String headerText, String contentText) {
        return getTextInputDialog(title, headerText, contentText, null);
    }

    public TextInputDialog getTextInputDialog(String title, String headerText, String contentText, String defaultValue) {
        DialogTexts dialogTexts = new DialogTexts(title, headerText, contentText);
        String value = (defaultValue == null) ? inputDialogTexts.get(dialogTexts) : defaultValue;
        TextInputDialog dialog = new TextInputDialog(value);
        dialog.setTitle(title);
        dialog.setHeaderText(title);
        dialog.setContentText(title);
        dialog.setOnCloseRequest(e ->
                inputDialogTexts.put(dialogTexts, ((TextInputDialog) e.getSource()).getEditor().getText())
        );
        return dialog;
    }

    private final Map<DialogTexts, String> choiceDialogTexts = new HashMap<>();

    public SmartChoiceDialog<String> getChoiceDialog(String title, String headerText, String contentText, List<String> choices) {
        return getChoiceDialog(title, headerText, contentText, null);
    }

    public SmartChoiceDialog<String> getChoiceDialog(String title, String headerText, String contentText, String defaultChoice, List<String> choices) {
        DialogTexts dialogTexts = new DialogTexts(title, headerText, contentText);
        String choice = (defaultChoice == null) ? choiceDialogTexts.get(dialogTexts) : defaultChoice;
        SmartChoiceDialog<String> dialog = new SmartChoiceDialog<>(choice, choices);
        dialog.setTitle(title);
        dialog.setHeaderText(title);
        dialog.setContentText(title);
        dialog.setOnCloseRequest(e ->
                choiceDialogTexts.put(dialogTexts, (String) ((SmartChoiceDialog) e.getSource()).getSelectedItem())
        );
        return dialog;
    }

    private final Map<DialogTexts, String> listViewDialogTexts = new HashMap<>();

    public ListViewDialog<String> getListViewDialog(String title, String headerText, String contentText, List<String> choices) {
        return getListViewDialog(title, headerText, contentText, null);
    }

    public ListViewDialog<String> getListViewDialog(String title, String headerText, String contentText, String defaultChoice, List<String> choices) {
        DialogTexts dialogTexts = new DialogTexts(title, headerText, contentText);
        String choice = (defaultChoice == null) ? choiceDialogTexts.get(dialogTexts) : defaultChoice;
        ListViewDialog<String> dialog = new ListViewDialog<>(choice, choices);
        dialog.setTitle(title);
        dialog.setHeaderText(title);
        dialog.setContentText(title);
        dialog.setOnCloseRequest(e ->
                listViewDialogTexts.put(dialogTexts, (String) ((ListViewDialog) e.getSource()).getSelectedItem())
        );
        return dialog;
    }
}
