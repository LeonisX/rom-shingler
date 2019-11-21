package md.leonis.shingler.gui.controls;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.css.PseudoClass;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Duration;
import md.leonis.shingler.log.InternalLogger;
import md.leonis.shingler.log.Level;
import md.leonis.shingler.log.LogRecord;

import java.text.SimpleDateFormat;

public class LogView extends ListView<LogRecord> {

    private static final int MAX_ENTRIES = 10_000;

    private final static PseudoClass trace = PseudoClass.getPseudoClass("trace");
    private final static PseudoClass debug = PseudoClass.getPseudoClass("debug");
    private final static PseudoClass info = PseudoClass.getPseudoClass("info");
    private final static PseudoClass warn = PseudoClass.getPseudoClass("warn");
    private final static PseudoClass error = PseudoClass.getPseudoClass("error");

    private final static SimpleDateFormat timestampFormatter = new SimpleDateFormat("HH:mm:ss.SSS");

    private final BooleanProperty showTimestamp = new SimpleBooleanProperty(false);
    private final BooleanProperty showSource = new SimpleBooleanProperty(false);
    private final ObjectProperty<Level> filterLevel = new SimpleObjectProperty<>(null);
    private final BooleanProperty tail = new SimpleBooleanProperty(true);
    private final BooleanProperty paused = new SimpleBooleanProperty(false);
    private final DoubleProperty refreshRate = new SimpleDoubleProperty(60);
    private final DoubleProperty progress = new SimpleDoubleProperty(0);

    private final ObservableList<LogRecord> logItems = FXCollections.observableArrayList();

    public BooleanProperty showTimeStampProperty() {
        return showTimestamp;
    }

    public BooleanProperty showSourceProperty() {
        return showSource;
    }

    public ObjectProperty<Level> filterLevelProperty() {
        return filterLevel;
    }

    public BooleanProperty tailProperty() {
        return tail;
    }

    public BooleanProperty pausedProperty() {
        return paused;
    }

    public DoubleProperty refreshRateProperty() {
        return refreshRate;
    }

    public DoubleProperty progressProperty() {
        return progress;
    }

    public LogView() {
        getStyleClass().add("log-view");

        Timeline logTransfer = new Timeline(
                new KeyFrame(
                        Duration.seconds(1),
                        event -> {
                            InternalLogger.log.drainTo(logItems);

                            if (logItems.size() > MAX_ENTRIES) {
                                logItems.remove(0, logItems.size() - MAX_ENTRIES);
                            }

                            if (tail.get()) {
                                scrollTo(logItems.size());
                            }
                            Double value = logItems.get(logItems.size() - 1).getProgress();
                            if (null == value) {
                                progress.setValue(progress.getValue() / 1.1);
                            } else {
                                progress.setValue(value);
                            }
                        }
                )
        );
        logTransfer.setCycleCount(Timeline.INDEFINITE);
        logTransfer.rateProperty().bind(refreshRateProperty());

        this.pausedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue && logTransfer.getStatus() == Animation.Status.RUNNING) {
                logTransfer.pause();
            }

            if (!newValue && logTransfer.getStatus() == Animation.Status.PAUSED && getParent() != null) {
                logTransfer.play();
            }
        });

        this.parentProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                logTransfer.pause();
            } else if (!paused.get()) {
                logTransfer.play();
            }
        });

        filterLevel.addListener((observable, oldValue, newValue) -> setItems(
                new FilteredList<>(logItems, logRecord -> logRecord.getLevel().ordinal() >= filterLevel.get().ordinal())
        ));
        filterLevel.set(Level.DEBUG);

        setCellFactory(param -> new ListCell<LogRecord>() {
            {
                showTimestamp.addListener(observable -> updateItem(this.getItem(), this.isEmpty()));
            }

            @Override
            protected void updateItem(LogRecord item, boolean empty) {
                super.updateItem(item, empty);

                pseudoClassStateChanged(trace, false);
                pseudoClassStateChanged(debug, false);
                pseudoClassStateChanged(info, false);
                pseudoClassStateChanged(warn, false);
                pseudoClassStateChanged(error, false);

                if (item == null || empty) {
                    setText(null);
                    return;
                }

                String timestamp = "";
                if (showTimestamp.get()) {
                    timestamp = (item.getTimestamp() == null) ? "" : timestampFormatter.format(item.getTimestamp()) + " ";
                }

                String source = "";
                if (showSource.get()) {
                    source = (item.getContext() == null) ? "" : item.getContext() + " ";
                }

                setText(timestamp + source + item.getMessage());

                switch (item.getLevel()) {
                    case TRACE:
                        pseudoClassStateChanged(trace, true);
                        break;

                    case DEBUG:
                        pseudoClassStateChanged(debug, true);
                        break;

                    case INFO:
                        pseudoClassStateChanged(info, true);
                        break;

                    case WARN:
                        pseudoClassStateChanged(warn, true);
                        break;

                    case ERROR:
                        pseudoClassStateChanged(error, true);
                        break;
                }
            }
        });
    }
}
