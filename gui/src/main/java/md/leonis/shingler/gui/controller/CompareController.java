package md.leonis.shingler.gui.controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import javafx.util.Pair;
import md.leonis.shingler.CSV;
import md.leonis.shingler.ListFilesa;
import md.leonis.shingler.gui.view.StageManager;
import md.leonis.shingler.model.ConfigHolder;
import md.leonis.shingler.model.GID;
import md.leonis.shingler.model.Platform;
import md.leonis.shingler.model.RomsCollection;
import md.leonis.shingler.utils.FileUtils;
import md.leonis.shingler.utils.IOUtils;
import md.leonis.shingler.utils.TiviUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static md.leonis.shingler.gui.view.StageManager.runInBackground;
import static md.leonis.shingler.model.ConfigHolder.*;
import static md.leonis.shingler.utils.BinaryUtils.bytesToHex;

@Controller
public class CompareController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompareController.class);

    private static final Color BLUE_GRAY = Color.color(0.4019608f, 0.4019608f, 0.6f);

    public VBox vBox;

    public TableView<Pair<GID, GID>> tableView;
    public TableColumn<Pair<GID, GID>, Pair<GID, GID>> leftColumn;
    public TableColumn<Pair<GID, GID>, Pair<GID, GID>> rightColumn;
    public TableColumn<Pair<GID, GID>, String> leftHashColumn;
    public TableColumn<Pair<GID, GID>, String> rightHashColumn;

    public HBox controlsHBox;
    public Button sortButton;

    public ToggleGroup toggleGroup;
    public RadioButton fullHashRadioButton;
    public RadioButton headlessRadioButton;
    
    public CheckBox allGoodCheckBox;
    public CheckBox pdCheckBox;
    public CheckBox hackCheckBox;
    public CheckBox badCheckBox;

    public Label waitLabel;

    private final StageManager stageManager;
    private final ConfigHolder configHolder;
    public Button auditButton;

    private List<Pair<GID, GID>> deletedPairs;
    private List<Pair<GID, GID>> addedPairs;
    private List<Pair<GID, GID>> samePairs;

    @Lazy
    public CompareController(StageManager stageManager, ConfigHolder configHolder) {
        this.stageManager = stageManager;
        this.configHolder = configHolder;
    }

    @FXML
    private void initialize() {

        controlsHBox.managedProperty().bind(controlsHBox.visibleProperty());
        waitLabel.managedProperty().bind(waitLabel.visibleProperty());

        //TODO for future - try to use rowFactory again
        leftColumn.setCellFactory(leftColumnCellFactory());
        rightColumn.setCellFactory(rightColumnCellFactory());

        leftColumn.setCellValueFactory(new PairNameFactory());
        rightColumn.setCellValueFactory(new PairNameFactory());
        leftHashColumn.setCellValueFactory(new PairLeftHashFactory());
        rightHashColumn.setCellValueFactory(new PairRightHashFactory());

        preloadCollections();
    }

    private Callback<TableColumn<Pair<GID, GID>, Pair<GID, GID>>, TableCell<Pair<GID, GID>, Pair<GID, GID>>> rightColumnCellFactory() {

        return new Callback<TableColumn<Pair<GID, GID>, Pair<GID, GID>>, TableCell<Pair<GID, GID>, Pair<GID, GID>>>() {
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
        };
    }

    private Callback<TableColumn<Pair<GID, GID>, Pair<GID, GID>>, TableCell<Pair<GID, GID>, Pair<GID, GID>>> leftColumnCellFactory() {

        return new Callback<TableColumn<Pair<GID, GID>, Pair<GID, GID>>, TableCell<Pair<GID, GID>, Pair<GID, GID>>>() {
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
        };
    }

    private void preloadCollections() {

        runInBackground(() -> {
            LOGGER.info("Preloading collections...");
            String collection1 = selectedCollections.get(0);
            String collection2 = selectedCollections.get(1);

            LOGGER.info("Reading from disk...");
            RomsCollection romsCollection1 = IOUtils.loadCollectionAsJson(workCollectionsPath().resolve(collection1).toFile());
            RomsCollection romsCollection2 = IOUtils.loadCollectionAsJson(workCollectionsPath().resolve(collection2).toFile());

            LOGGER.info("Prepare hashes...");
            Set<String> hashesLeft = romsCollection1.getGidsMap().values().stream().map(h -> bytesToHex(h.getSha1())).collect(Collectors.toSet());
            Set<String> hashesRight = romsCollection2.getGidsMap().values().stream().map(h -> bytesToHex(h.getSha1())).collect(Collectors.toSet());

            LOGGER.info("Processing hashes...");
            Set<String> hashesLeftUnique = new HashSet<>(hashesLeft);
            hashesLeftUnique.removeAll(hashesRight);
            Set<String> hashesRightUnique = new HashSet<>(hashesRight);
            hashesRightUnique.removeAll(hashesLeft);
            Set<String> hashesSame = new HashSet<>(hashesLeft);
            hashesSame.retainAll(hashesRight);

            //TODO here freeze if we have duplicate files (for example, Excellent Dizzy Collection, The (Prototype) [S][!].gg/sms)
            Map<String, GID> byLeftHash = romsCollection1.getGidsMap().values().stream().collect(Collectors.toMap(h -> bytesToHex(h.getSha1()), Function.identity()));
            Map<String, GID> byRightHash = romsCollection2.getGidsMap().values().stream().collect(Collectors.toMap(h -> bytesToHex(h.getSha1()), Function.identity()));

            LOGGER.info("Preparing added/deleted/modified collections...");

            deletedPairs = hashesLeftUnique.stream().map(h -> new Pair<>(byLeftHash.get(h), GID.EMPTY)).collect(Collectors.toList());
            addedPairs = hashesRightUnique.stream().map(h -> new Pair<>(GID.EMPTY, byRightHash.get(h))).collect(Collectors.toList());
            samePairs = hashesSame.stream().map(h -> new Pair<>(byLeftHash.get(h), byRightHash.get(h))).collect(Collectors.toList());

            filterResult();
        }, () -> {
            waitLabel.setVisible(false);
            controlsHBox.setVisible(true);
        });
    }

    public void sortButtonClick() {
        filterResult();
    }

    private void filterResult() {
        tableView.setItems(FXCollections.observableArrayList(preparePairs()));
    }

    private List<Pair<GID, GID>> preparePairs() {

        boolean omitHeaders = headlessRadioButton.isSelected();

        LOGGER.info("Filtering...");
        List<Pair<GID, GID>> pairs = filterPairs();

        LOGGER.info("Sorting...");
        pairs = sortByHash(pairs, omitHeaders);

        LOGGER.info("Merging result...");
        pairs = mergePairs(pairs, omitHeaders);

        return pairs;
    }

    private List<Pair<GID, GID>> filterPairs() {
        List<Pair<GID, GID>> filteredDeletedPairs = filter(deletedPairs);
        List<Pair<GID, GID>> filteredAddedPairs = filter(addedPairs);
        List<Pair<GID, GID>> filteredSamePairs= filter(samePairs);

        List<Pair<GID, GID>> pairs = new ArrayList<>();
        pairs.addAll(filteredDeletedPairs);
        pairs.addAll(filteredAddedPairs);
        pairs.addAll(filteredSamePairs);
        return pairs;
    }

    //TODO can use filtered from tableView.getItems()
    private List<Pair<GID, GID>> filter(List<Pair<GID, GID>> pairs) {

        Platform platform = platformsByCpu.get(ConfigHolder.platform);

        return pairs.stream().filter(pair -> {
            boolean p = platform.isPD(pair.getKey().getTitle())  || platform.isPD(pair.getValue().getTitle());
            boolean h = platform.isHack(pair.getKey().getTitle())  || platform.isHack(pair.getValue().getTitle());
            boolean b = platform.isBad(pair.getKey().getTitle())  || platform.isBad(pair.getValue().getTitle());
            boolean g = !(p || h || b);

            boolean showPd = (p && pdCheckBox.isSelected());
            boolean showHack = (h && hackCheckBox.isSelected());
            boolean showBad = (b && badCheckBox.isSelected());

            return (showPd || showHack || showBad) || (g && allGoodCheckBox.isSelected());
        }).collect(Collectors.toList());
    }

    private List<Pair<GID, GID>> sortByHash(List<Pair<GID, GID>> pairs, boolean omitHeaders) {

        return pairs.stream().sorted((p1, p2) -> {
            GID c1 = p1.getKey().getTitle().isEmpty() ? p1.getValue() : p1.getKey();
            GID c2 = p2.getKey().getTitle().isEmpty() ? p2.getValue() : p2.getKey();
            if (omitHeaders) {
                return bytesToHex(c1.getSha1wh()).compareTo(bytesToHex(c2.getSha1wh()));
            } else {
                return bytesToHex(c1.getSha1()).compareTo(bytesToHex(c2.getSha1()));
            }
        }).collect(Collectors.toList());
    }

    private List<Pair<GID, GID>> mergePairs(List<Pair<GID, GID>> pairs, boolean omitHeaders) {

        List<Pair<GID, GID>> mergedPairs = new ArrayList<>();

        if (!pairs.isEmpty()) {
            int i = 0;
            do {
                Pair<GID, GID> pair1 = pairs.get(i);
                Pair<GID, GID> pair2 = pairs.get(i + 1);

                if (omitHeaders) {
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
        }
        return sortByTitle(mergedPairs);
    }

    private List<Pair<GID, GID>> sortByTitle(List<Pair<GID, GID>> pairs) {

        return pairs.stream().sorted((p1, p2) -> {
            GID c1 = p1.getKey().getTitle().isEmpty() ? p1.getValue() : p1.getKey();
            GID c2 = p2.getKey().getTitle().isEmpty() ? p2.getValue() : p2.getKey();
            return c1.getTitle().compareToIgnoreCase(c2.getTitle());
        }).collect(Collectors.toList());
    }

    public void auditButtonClick() {
        List<Pair<GID, GID>> pairs = preparePairs();

        List<Pair<String, String>> renamed = pairs.stream().filter(p -> !ListFilesa.getCleanName(p.getKey().getTitle()).equals(ListFilesa.getCleanName(p.getValue().getTitle())))
                .filter(p -> StringUtils.isNotBlank(p.getKey().getTitle()) && StringUtils.isNotBlank(p.getValue().getTitle()))
                .map(p -> new Pair<>(p.getValue().getTitle(), p.getKey().getTitle())).collect(Collectors.toList());

        Map<String, List<Pair<GID, GID>>> familyList = pairs.stream().collect(Collectors.groupingBy(pair -> ListFilesa.getCleanName(pair.getValue().getTitle())));

        Map<String, List<Pair<GID, GID>>> added = familyList.entrySet().stream()
                .filter(e -> e.getValue().size() == e.getValue().stream().filter(v -> StringUtils.isBlank(v.getKey().getTitle())).count())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, List<Pair<GID, GID>>> deleted = familyList.entrySet().stream()
                .filter(e -> e.getValue().size() == e.getValue().stream().filter(v -> StringUtils.isBlank(v.getValue().getTitle())).count())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        FileUtils.saveToFile(getInputPath().resolve(platform + "_renamed.csv"), renamed.stream().map(p -> new CSV.RenamedStructure(TiviUtils.getSid(p.getKey()), ListFilesa.getCleanName(p.getKey()), ListFilesa.getCleanName(p.getValue()))).sorted(Comparator.comparing(CSV.RenamedStructure::getNewName)).sorted(Comparator.comparing(r -> sidOrder(r.getSid()))).map(p -> '"' + p.getSid() + "\";\"" + p.getNewName() + "\";\"" + p.getOldName() + '"').distinct().collect(Collectors.toList()));
        FileUtils.saveToFile(getInputPath().resolve(platform + "_added.csv"), added.entrySet().stream().map(p -> new CSV.AddedStructure(TiviUtils.getSid(p.getValue().get(0).getValue().getTitle()), p.getKey())).sorted(Comparator.comparing(CSV.AddedStructure::getName)).sorted(Comparator.comparing(r -> sidOrder(r.getSid()))).map(p -> '"' + p.getSid() + "\";\"" + p.getName() + '"').collect(Collectors.toList()));
        FileUtils.saveToFile(getInputPath().resolve(platform + "_deleted.txt"), deleted.keySet().stream().sorted().collect(Collectors.toList()));
    }

    private String sidOrder(String sid) {
        int repeat = (sid.equals("num")) ? 1 : sid.length();
        return StringUtils.repeat("z", repeat) + sid;
    }

    static class PairNameFactory implements Callback<TableColumn.CellDataFeatures<Pair<GID, GID>, Pair<GID, GID>>, ObservableValue<Pair<GID, GID>>> {
        @Override
        public ObservableValue<Pair<GID, GID>> call(TableColumn.CellDataFeatures<Pair<GID, GID>, Pair<GID, GID>> data) {
            return new ReadOnlyObjectWrapper<>(data.getValue());
        }
    }

    static class PairLeftHashFactory implements Callback<TableColumn.CellDataFeatures<Pair<GID, GID>, String>, ObservableValue<String>> {
        @Override
        public ObservableValue<String> call(TableColumn.CellDataFeatures<Pair<GID, GID>, String> data) {
            return new ReadOnlyObjectWrapper<>(data.getValue().getKey().getSha1() == null ? "" : bytesToHex(data.getValue().getKey().getSha1()));
        }
    }

    static class PairRightHashFactory implements Callback<TableColumn.CellDataFeatures<Pair<GID, GID>, String>, ObservableValue<String>> {
        @Override
        public ObservableValue<String> call(TableColumn.CellDataFeatures<Pair<GID, GID>, String> data) {
            return new ReadOnlyObjectWrapper<>(data.getValue().getValue().getSha1() == null ? "" : bytesToHex(data.getValue().getValue().getSha1()));
        }
    }
}
