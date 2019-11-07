package md.leonis.shingler.gui;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import md.leonis.shingler.InternalLogger;
import md.leonis.shingler.Level;
import md.leonis.shingler.Log;
import md.leonis.shingler.LogRecord;

import java.util.Random;

class Lorem {
    private static final String[] IPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque hendrerit imperdiet mi quis convallis. Pellentesque fringilla imperdiet libero, quis hendrerit lacus mollis et. Maecenas porttitor id urna id mollis. Suspendisse potenti. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Cras lacus tellus, semper hendrerit arcu quis, auctor suscipit ipsum. Vestibulum venenatis ante et nulla commodo, ac ultricies purus fringilla. Aliquam lectus urna, commodo eu quam a, dapibus bibendum nisl. Aliquam blandit a nibh tincidunt aliquam. In tellus lorem, rhoncus eu magna id, ullamcorper dictum tellus. Curabitur luctus, justo a sodales gravida, purus sem iaculis est, eu ornare turpis urna vitae dolor. Nulla facilisi. Proin mattis dignissim diam, id pellentesque sem bibendum sed. Donec venenatis dolor neque, ut luctus odio elementum eget. Nunc sed orci ligula. Aliquam erat volutpat.".split(" ");
    private static final int MSG_WORDS = 8;
    private int idx = 0;

    private Random random = new Random(42);

    synchronized String nextString() {
        int end = Math.min(idx + MSG_WORDS, IPSUM.length);

        StringBuilder result = new StringBuilder();
        for (int i = idx; i < end; i++) {
            result.append(IPSUM[i]).append(" ");
        }

        idx += MSG_WORDS;
        idx = idx % IPSUM.length;

        return result.toString();
    }

    synchronized Level nextLevel() {
        double v = random.nextDouble();

        if (v < 0.7) {
            return Level.TRACE;
        }

        if (v < 0.9) {
            return Level.DEBUG;
        }

        if (v < 0.95) {
            return Level.INFO;
        }

        if (v < 0.985) {
            return Level.WARN;
        }

        return Level.ERROR;
    }

}

public class LogViewer extends Application {
    private final Random random = new Random(42);

    @Override
    public void start(Stage stage) {
        Lorem lorem = new Lorem();
        InternalLogger logger = new InternalLogger("main");

        logger.info("Hello");
        logger.warn("Don't pick up alien hitchhickers");

        for (int x = 0; x < 20; x++) {
            Thread generatorThread = new Thread(
                    () -> {
                        for (; ; ) {
                            logger.log(
                                    new LogRecord(
                                            lorem.nextLevel(),
                                            Thread.currentThread().getName(),
                                            lorem.nextString()
                                    )
                            );

                            try {
                                Thread.sleep(random.nextInt(1_000));
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    },
                    "log-gen-" + x
            );
            generatorThread.setDaemon(true);
            generatorThread.start();
        }

        InternalLogger.log = new Log(1_000_000);
        LogView logView = new LogView();
        logView.setPrefWidth(400);

        ChoiceBox<Level> filterLevel = new ChoiceBox<>(
                FXCollections.observableArrayList(Level.values())
        );
        filterLevel.getSelectionModel().select(Level.DEBUG);
        logView.filterLevelProperty().bind(filterLevel.getSelectionModel().selectedItemProperty());

        ToggleButton showTimestamp = new ToggleButton("Show Timestamp");
        logView.showTimeStampProperty().bind(showTimestamp.selectedProperty());

        ToggleButton tail = new ToggleButton("Tail");
        logView.tailProperty().bind(tail.selectedProperty());

        ToggleButton pause = new ToggleButton("Pause");
        logView.pausedProperty().bind(pause.selectedProperty());

        Slider rate = new Slider(0.1, 60, 60);
        logView.refreshRateProperty().bind(rate.valueProperty());
        Label rateLabel = new Label();
        rateLabel.textProperty().bind(Bindings.format("Update: %.2f fps", rate.valueProperty()));
        rateLabel.setStyle("-fx-font-family: monospace;");
        VBox rateLayout = new VBox(rate, rateLabel);
        rateLayout.setAlignment(Pos.CENTER);

        HBox controls = new HBox(
                10,
                filterLevel,
                showTimestamp,
                tail,
                pause,
                rateLayout
        );
        controls.setMinHeight(HBox.USE_PREF_SIZE);

        VBox layout = new VBox(
                10,
                controls,
                logView
        );
        VBox.setVgrow(logView, Priority.ALWAYS);

        Scene scene = new Scene(layout);
        scene.getStylesheets().add("log-view.css");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
