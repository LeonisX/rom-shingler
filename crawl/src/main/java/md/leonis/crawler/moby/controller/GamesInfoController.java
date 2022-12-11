package md.leonis.crawler.moby.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import md.leonis.crawler.moby.config.ConfigHolder;
import md.leonis.crawler.moby.view.StageManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import java.io.File;

import static md.leonis.crawler.moby.config.ConfigHolder.mobyStructure;
import static md.leonis.crawler.moby.config.ConfigHolder.tiviStructure;

@Controller
public class GamesInfoController {

    private final StageManager stageManager;
    private final ConfigHolder configHolder;

    public ListView<String> tiviListView;
    public FlowPane tiviFlowPane;

    public ListView<String> mobyListView;
    public FlowPane mobyFlowPane;
    public BorderPane rightBorderPane;

    @Lazy
    public GamesInfoController(StageManager stageManager, ConfigHolder configHolder) {
        this.stageManager = stageManager;
        this.configHolder = configHolder;
    }

    @FXML
    private void initialize() {
        if (tiviStructure == null) {
            fillListView(tiviListView, tiviFlowPane, mobyStructure);
            rightBorderPane.setVisible(false);
            rightBorderPane.setMaxWidth(0);
            ((BorderPane) rightBorderPane.getParent()).setMaxWidth(400);
        } else if (mobyStructure == null) {
            fillListView(tiviListView, tiviFlowPane, tiviStructure);
            rightBorderPane.setVisible(false);
            rightBorderPane.setMaxWidth(0);
            ((BorderPane) rightBorderPane.getParent()).setMaxWidth(400);
        } else {
            fillListView(tiviListView, tiviFlowPane, tiviStructure);
            fillListView(mobyListView, mobyFlowPane, mobyStructure);
        }
    }

    private void fillListView(ListView<String> listView, FlowPane flowPane, GamesBindingController.Structure structure) {
        listView.getItems().clear();
        if (structure != null) {
            listView.getItems().add(structure.getUnmodifiedTitle());
            listView.getItems().add("Platform ID: " + structure.getPlatformId());
            listView.getItems().add("Game ID: " + structure.getGameId());

            //listView.getItems().add("Family: " + structure.getFamily());
            listView.getItems().add("Year: " + structure.getYear());
            listView.getItems().add("");
            structure.getAlternativeTitles().forEach(a -> listView.getItems().add(a));

            structure.getImages().stream().limit(12).forEach(i -> {
                //System.out.println(i);
                Image image = new Image(new File(i).toURI().toString());
                double width = image.getWidth();
                double height = image.getHeight();
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(128);
                imageView.setFitHeight(height * 128 / width);
                flowPane.getChildren().add(imageView);
            });
        }
    }
}
