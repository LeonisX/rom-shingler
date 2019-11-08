package md.leonis.shingler.gui.controller;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
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

import javax.swing.plaf.nimbus.State;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

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

    private Path userHome;
    private Path rootWorkDir;

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
        workDirLabel.setText(rootWorkDir.toString());
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

    public void gamesToFamilyButtonClick(ActionEvent actionEvent) {
        //TODO revert
        /*Stage stage = (Stage) gamesToFamilyButton.getScene().getWindow();
        DirectoryChooser directoryChooser = configHolder.getDirectoryChooser("Select directory with unpacked games");
        File dir = directoryChooser.showDialog(stage);*/
        File dir = new File("D:\\Downloads\\games");
        if (dir != null) {
            //stageManager.showInformationAlert(dir.getAbsolutePath(), dir.getAbsolutePath(), dir.getAbsolutePath());
            //TODO revert
            //configHolder.saveInitialDir(directoryChooser, dir);

            //TODO select platform
            //TextInputDialog dialog = new TextInputDialog("walter");
            /*TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Text Input Dialog");
            dialog.setHeaderText("Look, a Text Input Dialog");
            dialog.setContentText("Please enter your name:");*/


            // Traditional way to get the response value.
            /*Optional<String> result = dialog.showAndWait();
            if (result.isPresent()){
                System.out.println("Your name: " + result.get());
            }*/
            // The Java 8 way to get the response value (with lambda expression).
            //result.ifPresent(name -> System.out.println("Your name: " + name));

            rootWorkDir = userHome.resolve("shingler");
            workDirLabel.setText(rootWorkDir.toString());

            //TODO
            String platform = "nes";

            //TODO cool error processing
            try {
                Path workDir = rootWorkDir.resolve(platform);
                Main1024a.createSampleDirs(workDir);

                Path romsFolder = dir.toPath();
                List<Path> files = Main1024a.listFilesForFolder(romsFolder);

                Thread thread = new Thread(() -> {
                    new LinkedHashMap<>();
                    try {
                        final Map<String, Main1024a.GID> gidMap = Main1024a.filesToGid(workDir, files);

                        Main1024a.generateShinglesNio(gidMap, romsFolder, workDir);

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                thread.setDaemon(true);
                thread.start();

            } catch (Exception e) {
                stageManager.showErrorAlert("Exception!", e.getMessage(), Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.joining("\n")));
            }
        }
    }

    public void goodMergedToFamilyButtonClick(ActionEvent actionEvent) {

    }

    public void changeWorkDirButtonClick(ActionEvent actionEvent) {
    }
}
