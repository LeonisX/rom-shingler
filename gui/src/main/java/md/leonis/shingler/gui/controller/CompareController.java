package md.leonis.shingler.gui.controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import javafx.util.Pair;
import md.leonis.shingler.GID;
import md.leonis.shingler.Main1024a;
import md.leonis.shingler.gui.config.ConfigHolder;
import md.leonis.shingler.gui.view.StageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static md.leonis.shingler.gui.config.ConfigHolder.*;

@Controller
public class CompareController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompareController.class);
    @FXML
    public VBox vBox;

    public TableView<Pair<GID, GID>> compareTable;
    public TableColumn<Pair<GID, GID>, Pair<GID, GID>> leftColumn;
    public TableColumn<Pair<GID, GID>, Pair<GID, GID>> rightColumn;
    public TableColumn<Pair<GID, GID>, String> leftColumn1;
    public TableColumn<Pair<GID, GID>, String> rightColumn1;
    public Button sortButton;
    public RadioButton fullHash;
    public ToggleGroup tg1;
    public RadioButton woHeader;
    public CheckBox good;
    public CheckBox pd;
    public CheckBox hack;
    public Label waitLabel;
    public HBox hBox;

    private final StageManager stageManager;
    private final ConfigHolder configHolder;

    private List<Pair<GID, GID>> pairs1;
    private List<Pair<GID, GID>> pairs2;
    private List<Pair<GID, GID>> samePairs;

    private static final Color BLUE_GRAY = Color.color(0.4019608f, 0.4019608f, 0.6f);

    @Lazy
    public CompareController(StageManager stageManager, ConfigHolder configHolder) {
        this.stageManager = stageManager;
        this.configHolder = configHolder;
    }

    @FXML
    private void initialize() {

        hBox.managedProperty().bind(hBox.visibleProperty());
        waitLabel.managedProperty().bind(waitLabel.visibleProperty());

        leftColumn.setCellFactory(new Callback<TableColumn<Pair<GID, GID>, Pair<GID, GID>>, TableCell<Pair<GID, GID>, Pair<GID, GID>>>() {
            @Override
            public TableCell<Pair<GID, GID>, Pair<GID, GID>> call(TableColumn<Pair<GID, GID>, Pair<GID, GID>> param) {
                return new TableCell<Pair<GID, GID>, Pair<GID, GID>>() {

                    @Override
                    public void updateItem(Pair<GID, GID> item, boolean empty) {
                        super.updateItem(item, empty);
                        if (!isEmpty() && !(null == item)) {
                            Color color = Color.GRAY;
                            if (!item.getKey().getTitle().isEmpty() && item.getValue().getTitle().isEmpty()) { // deleted
                                color = Color.RED;
                            } else if (!item.getKey().getTitle().equals(item.getValue().getTitle())) { // renamed
                                color = Color.BLUE;
                            } else if (!Arrays.equals(item.getKey().getSha1(), item.getValue().getSha1())) {
                                color = BLUE_GRAY;
                            }
                            setText(item.getKey().getTitle());
                            setTextFill(color);
                        }
                    }
                };
            }
        });

        rightColumn.setCellFactory(new Callback<TableColumn<Pair<GID, GID>, Pair<GID, GID>>, TableCell<Pair<GID, GID>, Pair<GID, GID>>>() {
            @Override
            public TableCell<Pair<GID, GID>, Pair<GID, GID>> call(TableColumn<Pair<GID, GID>, Pair<GID, GID>> param) {
                return new TableCell<Pair<GID, GID>, Pair<GID, GID>>() {

                    @Override
                    public void updateItem(Pair<GID, GID> item, boolean empty) {
                        super.updateItem(item, empty);
                        if (!isEmpty() && !(null == item)) {
                            Color color = Color.GRAY;
                            if (item.getKey().getTitle().isEmpty() && !item.getValue().getTitle().isEmpty()) { // new
                                color = Color.GREEN;
                            } else if (!item.getKey().getTitle().equals(item.getValue().getTitle())) { // renamed
                                color = Color.BLUE;
                            } else if (!Arrays.equals(item.getKey().getSha1(), item.getValue().getSha1())) {
                                color = BLUE_GRAY;
                            }
                            setText(item.getValue().getTitle());
                            setTextFill(color);
                        }
                    }
                };
            }
        });

        leftColumn.setCellValueFactory(new PairKeyFactory());
        rightColumn.setCellValueFactory(new PairKeyFactory());
        leftColumn1.setCellValueFactory(new PairKeyFactory2());
        rightColumn1.setCellValueFactory(new PairValueFactory2());

        Thread thread = new Thread(() -> {
            LOGGER.info("Comparing collections...");
            String collection1 = selectedCollections.get(0);
            String collection2 = selectedCollections.get(1);

            LOGGER.info("Reading from disk...");
            Path workCollectionsDir = collectionsDir.resolve(platform);
            Main1024a.RomsCollection romsCollection1 = Main1024a.readCollectionFromFile(workCollectionsDir.resolve(collection1).toFile());
            Main1024a.RomsCollection romsCollection2 = Main1024a.readCollectionFromFile(workCollectionsDir.resolve(collection2).toFile());

            LOGGER.info("Prepare hashes...");
            Set<String> hashes1 = romsCollection1.getGids().values().stream().map(h -> Main1024a.bytesToHex(h.getSha1())).collect(Collectors.toSet());
            Set<String> hashes2 = romsCollection2.getGids().values().stream().map(h -> Main1024a.bytesToHex(h.getSha1())).collect(Collectors.toSet());

            LOGGER.info("Processing hashes...");
            Set<String> hashes1new = new HashSet<>(hashes1);
            hashes1new.removeAll(hashes2);
            Set<String> hashes2new = new HashSet<>(hashes2);
            hashes2new.removeAll(hashes1);
            Set<String> hashesSame = new HashSet<>(hashes1);
            hashesSame.retainAll(hashes2);

            Map<String, GID> byHash1 = romsCollection1.getGids().values().stream().collect(Collectors.toMap(h -> Main1024a.bytesToHex(h.getSha1()), Function.identity()));
            Map<String, GID> byHash2 = romsCollection2.getGids().values().stream().collect(Collectors.toMap(h -> Main1024a.bytesToHex(h.getSha1()), Function.identity()));

            LOGGER.info("Preparing added/deleted/modified collections...");

            pairs1 = hashes1new.stream().map(h -> new Pair<>(byHash1.get(h), GID.EMPTY)).collect(Collectors.toList());
            pairs2 = hashes2new.stream().map(h -> new Pair<>(GID.EMPTY, byHash2.get(h))).collect(Collectors.toList());
            samePairs = hashesSame.stream().map(h -> {
                GID s1 = byHash1.get(h);
                GID s2 = byHash2.get(h);
                return new Pair<>(s1, s2);
            }).collect(Collectors.toList());

            sortButtonClick(null);
            waitLabel.setVisible(false);
            hBox.setVisible(true);
            compareTable.refresh();
        });
        thread.setDaemon(true);
        thread.start();
    }

    public void sortButtonClick(ActionEvent actionEvent) {

        LOGGER.info("Filtering...");
        pairs1= filter(pairs1);
        pairs2= filter(pairs2);
        samePairs= filter(samePairs);


        List<Pair<GID, GID>> pairs = new ArrayList<>();
        pairs.addAll(pairs1);
        pairs.addAll(pairs2);
        pairs.addAll(samePairs);

        LOGGER.info("Sorting...");
        boolean woHeaders = woHeader.isSelected();

        pairs = pairs.stream().sorted((p1, p2) -> {
            GID c1 = p1.getKey().getTitle().isEmpty() ? p1.getValue() : p1.getKey();
            GID c2 = p2.getKey().getTitle().isEmpty() ? p2.getValue() : p2.getKey();
            if (woHeaders) {
                String s1 = Main1024a.bytesToHex(c1.getSha1wh());
                String s2 = Main1024a.bytesToHex(c2.getSha1wh());
                return s1.compareTo(s2);
            } else {
                String s1 = Main1024a.bytesToHex(c1.getSha1());
                String s2 = Main1024a.bytesToHex(c2.getSha1());
                return s1.compareTo(s2);
            }
        }).collect(Collectors.toList());

        LOGGER.info("Merging result...");
        List<Pair<GID, GID>> mergedPairs = new ArrayList<>();

        int i = 0;
        do {
            Pair<GID, GID> pair1 = pairs.get(i);
            Pair<GID, GID> pair2 = pairs.get(i + 1);
            if (woHeaders) {
                if (pair1.getKey().getSha1wh() != null && Arrays.equals(pair1.getKey().getSha1wh(), pair2.getValue().getSha1wh())) {
                    mergedPairs.add(new Pair<>(pair1.getKey(), pair2.getValue()));
                    i++;
                } else if (pair1.getValue().getSha1wh() != null && Arrays.equals(pair1.getValue().getSha1wh(), pair2.getKey().getSha1wh())) {
                    mergedPairs.add(new Pair<>(pair1.getValue(), pair2.getKey()));
                    i++;
                } else {
                    mergedPairs.add(pair1);
                }
            } else {
                if (pair1.getKey().getSha1() != null && Arrays.equals(pair1.getKey().getSha1(), pair2.getValue().getSha1())) {
                    mergedPairs.add(new Pair<>(pair1.getKey(), pair2.getValue()));
                    i++;
                } else if (pair1.getValue().getSha1() != null && Arrays.equals(pair1.getValue().getSha1(), pair2.getKey().getSha1())) {
                    mergedPairs.add(new Pair<>(pair1.getValue(), pair2.getKey()));
                    i++;
                } else {
                    mergedPairs.add(pair1);
                }
            }
            i++;
        } while (i < pairs.size() - 1);
        if (mergedPairs.get(mergedPairs.size() - 1).equals(pairs.get(pairs.size() - 2))) {
            mergedPairs.add(pairs.get(pairs.size() - 1));
        }

        mergedPairs = mergedPairs.stream().sorted((p1, p2) -> {
            GID c1 = p1.getKey().getTitle().isEmpty() ? p1.getValue() : p1.getKey();
            GID c2 = p2.getKey().getTitle().isEmpty() ? p2.getValue() : p2.getKey();
            return c1.getTitle().compareToIgnoreCase(c2.getTitle());
        }).collect(Collectors.toList());

        compareTable.setItems(FXCollections.observableArrayList(mergedPairs));
    }

    //TODO can use filtered from compareTable.getItems()
    private List<Pair<GID, GID>> filter(List<Pair<GID, GID>> pairs) {
        return pairs.stream().filter(pair -> {
            boolean p = pair.getKey().getTitle().contains("(PD)")  || pair.getValue().getTitle().contains("(PD)");
            boolean h = pair.getKey().getTitle().contains("(Hack)")  || pair.getValue().getTitle().contains("(Hack)")
                    || pair.getKey().getTitle().contains("(Hack ") || pair.getValue().getTitle().contains("(Hack ")
                    ||pair.getKey().getTitle().contains(" Hack)") || pair.getValue().getTitle().contains(" Hack)");
            boolean g = !(p || h);

            return (p && pd.isSelected()) || (h && hack.isSelected()) || (g && good.isSelected());
        }).collect(Collectors.toList());
    }

    static class PairKeyFactory implements Callback<TableColumn.CellDataFeatures<Pair<GID, GID>, Pair<GID, GID>>, ObservableValue<Pair<GID, GID>>> {
        @Override
        public ObservableValue<Pair<GID, GID>> call(TableColumn.CellDataFeatures<Pair<GID, GID>, Pair<GID, GID>> data) {
            return new ReadOnlyObjectWrapper<>(data.getValue());
        }
    }

    static class PairKeyFactory2 implements Callback<TableColumn.CellDataFeatures<Pair<GID, GID>, String>, ObservableValue<String>> {
        @Override
        public ObservableValue<String> call(TableColumn.CellDataFeatures<Pair<GID, GID>, String> data) {
            return new ReadOnlyObjectWrapper<>(data.getValue().getKey().getSha1() == null ? "" : Main1024a.bytesToHex(data.getValue().getKey().getSha1()));
        }
    }

    static class PairValueFactory2 implements Callback<TableColumn.CellDataFeatures<Pair<GID, GID>, String>, ObservableValue<String>> {
        @Override
        public ObservableValue<String> call(TableColumn.CellDataFeatures<Pair<GID, GID>, String> data) {
            return new ReadOnlyObjectWrapper<>(data.getValue().getValue().getSha1() == null ? "" : Main1024a.bytesToHex(data.getValue().getValue().getSha1()));
        }
    }
}
