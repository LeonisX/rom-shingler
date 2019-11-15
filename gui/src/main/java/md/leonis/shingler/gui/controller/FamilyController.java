package md.leonis.shingler.gui.controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import javafx.util.Pair;
import md.leonis.shingler.ListFilesa;
import md.leonis.shingler.gui.dto.NameView;
import md.leonis.shingler.gui.dto.NodeStatus;
import md.leonis.shingler.gui.view.StageManager;
import md.leonis.shingler.model.*;
import md.leonis.shingler.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static md.leonis.shingler.model.ConfigHolder.*;
import static md.leonis.shingler.utils.BinaryUtils.bytesToHex;

//TODO calculate relations button
@Controller
public class FamilyController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FamilyController.class);

    private final StageManager stageManager;
    private final ConfigHolder configHolder;

    public VBox vBox;

    public HBox controlsHBox;
    public Label waitLabel;

    public Button auditFamiliesButton;
    public Button generateFamiliesButton;
    public Button verifyFamiliesButton;
    public Button mergeFamiliesButton;

    public Label totalFamiliesLabel;
    public Label totalGamesLabel;
    public Label groupedGamesLabel;
    public Label orphanedGamesLabel;

    public CheckBox allGoodCheckBox;
    public CheckBox pdCheckBox;
    public CheckBox hackCheckBox;
    public Button calculateRelationsButton;
    public Button selectButton;
    public Button kickAwayButton;
    public Button newGroupButton;
    public Button addToGroupButton;

    private TreeItem<NameView> rootItem = new TreeItem<>(NameView.EMPTY);

    public TreeView<NameView> familyTreeView;
    public ComboBox<Integer> precisionCheckBox;
    public TextField jakkardTextField;
    public Button expandAllButton;
    public Button collapseAllButton;

    @Lazy
    public FamilyController(StageManager stageManager, ConfigHolder configHolder) {
        this.stageManager = stageManager;
        this.configHolder = configHolder;
    }

    @FXML
    private void initialize() {

        jakkardTextField.setText("" + jakkard);
        //TODO up
        Pattern pattern = Pattern.compile("\\d*|\\d+\\.\\d*");
        TextFormatter formatter = new TextFormatter((UnaryOperator<TextFormatter.Change>) change -> pattern.matcher(change.getControlNewText()).matches() ? change : null);
        jakkardTextField.setTextFormatter(formatter);

        precisionCheckBox.setItems(FXCollections.observableArrayList(DENOMINATORS));
        precisionCheckBox.setCellFactory(precisionCheckBoxCellFactory);
        precisionCheckBox.getSelectionModel().select(getDenominatorId());

        controlsHBox.managedProperty().bind(controlsHBox.visibleProperty());
        waitLabel.managedProperty().bind(waitLabel.visibleProperty());

        //TODO for future - try to use rowFactory again
        /*leftColumn.setCellFactory(leftColumnCellFactory());
        rightColumn.setCellFactory(rightColumnCellFactory());

        leftColumn.setCellValueFactory(new PairNameFactory());
        rightColumn.setCellValueFactory(new PairNameFactory());
        leftHashColumn.setCellValueFactory(new PairLeftHashFactory());
        rightHashColumn.setCellValueFactory(new PairRightHashFactory());*/

        loadFamilies();
    }

    private Callback<ListView<Integer>, ListCell<Integer>> precisionCheckBoxCellFactory = new Callback<ListView<Integer>, ListCell<Integer>>() {

        @Override
        public ListCell<Integer> call(ListView<Integer> l) {
            return new ListCell<Integer>() {

                @Override
                protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    Color color = Color.DARKGREEN;
                    if (item == null || empty) {
                        setGraphic(null);
                    } else {
                        setText("1/" + item);
                        if (item > 8 && item < 128) {
                            color = Color.BLACK;
                        } else if (item >= 128) {
                            color = Color.DARKRED;
                        }
                    }
                    setTextFill(color);
                }
            };
        }
    };

    private void loadFamilies() {
        File familyFile = fullFamiliesPath().toFile();

        if (familyFile.exists()) {
            System.out.println(String.format("%nReading families from file %s...", familyFile));
            families = IOUtils.loadFamilies(familyFile);
        } else {
            families = new HashMap<>();
        }

        showFamilies();
    }

    // TODO expand all expanded after operations (group, kick)
    private void showFamilies() {
        int total = romsCollection.getGids().size();
        int inFamily = families.values().stream().map(Family::size).mapToInt(Integer::intValue).sum();

        totalFamiliesLabel.setText("" + families.size());
        totalGamesLabel.setText("" + romsCollection.getGids().size());
        groupedGamesLabel.setText("" + inFamily);
        orphanedGamesLabel.setText("" + (total - inFamily));

        Map<NameView, List<NameView>> familiesView = new LinkedHashMap<>();

        families.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(String::toString)))
                .forEach(e -> {
                    List<NameView> views = new ArrayList<>();
                    for (int i = 0; i < e.getValue().getMembers().size(); i++) {
                        Name name = e.getValue().getMembers().get(i);
                        views.add(new NameView(name, e.getKey(), e.getValue().getJakkardStatus(i)));
                    }

                    views = views.stream().sorted(Comparator.comparing(NameView::getJakkardStatus).reversed()).collect(Collectors.toList());

                    NameView root = new NameView(e, views);
                    familiesView.put(root, views);
                });

        Set<String> familyNames = families.values().stream().flatMap(f -> f.getMembers().stream().map(Name::getName)).collect(Collectors.toSet());
        Set<String> orphanedNames = romsCollection.getGids().values().stream().map(GID::getTitle).collect(Collectors.toSet());
        orphanedNames.removeAll(familyNames);

        orphanedNames.stream().sorted().forEach(name -> familiesView.put(new NameView(name), new ArrayList<>()));

        rootItem.getChildren().clear();
        familyTreeView.setRoot(rootItem);
        familyTreeView.setShowRoot(false);
        familyTreeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        familyTreeView.setCellFactory(treeViewCellFactory());

        familiesView.forEach((key, value) -> {
            TreeItem<NameView> node = new TreeItem<>(key);
            value.forEach(c -> node.getChildren().add(new TreeItem<>(c)));
            rootItem.getChildren().add(node);
        });
    }

    public void mergeFamiliesButtonClick(ActionEvent actionEvent) {
    }

    public void jakkardTextFieldKeyReleased(KeyEvent keyEvent) {
        try {
            jakkard = Double.parseDouble(jakkardTextField.getText());
            System.out.println(jakkard);
            familyTreeView.refresh();
        } catch (NumberFormatException ignore) {
            jakkard = 0;
        }
    }

    public void expandAllButtonClick(ActionEvent actionEvent) {
        rootItem.getChildren().forEach(item -> {
            if (item != null && !item.isLeaf()) {
                item.setExpanded(true);
            }
        });
    }

    public void collapseAllButtonClick(ActionEvent actionEvent) {
        rootItem.getChildren().forEach(item -> {
            if (item != null && !item.isLeaf()) {
                item.setExpanded(false);
            }
        });
    }

    public void precisionCheckBoxAction(ActionEvent actionEvent) {
        setDenominatorId(precisionCheckBox.getSelectionModel().getSelectedItem());
        loadFamilies();
    }

    private Callback<TreeView<NameView>, TreeCell<NameView>> treeViewCellFactory() {

        return new Callback<TreeView<NameView>, TreeCell<NameView>>() {
            @Override
            public TreeCell<NameView> call(TreeView<NameView> param) {
                return new TreeCell<NameView>() {

                    @Override
                    public void updateItem(NameView item, boolean empty) {
                        super.updateItem(item, empty);
                        if (!isEmpty() && !(null == item)) {
                            Color color = Color.BLACK;

                            double minIndex = item.getJakkardStatus();

                            if (item.getStatus() == NodeStatus.FAMILY) {
                                if (item.getItems().size() >= 2) {
                                    minIndex = item.getItems().stream().min(Comparator.comparing(NameView::getJakkardStatus)).map(NameView::getJakkardStatus).orElse(0.0);
                                } else {
                                    minIndex = 100;
                                }
                            }

                            if (minIndex < jakkard) {
                                color = Color.RED;
                            }
                            setText(item.toString());
                            setTextFill(color);
                            familyTreeView.refresh();
                        } else {
                            setText(null);
                        }
                    }
                };
            }
        };
    }

    public void generateFamiliesButtonClick(ActionEvent actionEvent) {
        if (romsCollection.getType() == CollectionType.PLAIN) {
            ListFilesa.generateFamilies();
        } else {
            ListFilesa.archiveToFamilies();
        }
        showFamilies();
    }

    public void calculateRelationsButtonClick(ActionEvent actionEvent) {
        ListFilesa.calculateRelations();
        showFamilies();
    }

    public void auditFamiliesButtonClick(ActionEvent actionEvent) {
    }

    public void verifyFamiliesButtonClick(ActionEvent actionEvent) {
    }

    public void selectButtonClick(ActionEvent actionEvent) {
        TextInputDialog dialog = stageManager.getTextInputDialog("Text Input Dialog", "Look, a Text Input Dialog", "Enter search phrase:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String phrase = result.get().toLowerCase();
            rootItem.getChildren().forEach(c -> {
                if ((c.getValue().getStatus() != NodeStatus.FAMILY) && c.getValue().getName().toLowerCase().contains(phrase)) {
                    familyTreeView.getSelectionModel().select(c);
                }
            });
        }
    }

    //TODO in future allow to merge families too
    public void addToGroupButtonClick(ActionEvent actionEvent) {

        List<Name> selectedNames = familyTreeView.getSelectionModel().getSelectedItems().stream()
                .filter(t -> t.getValue().getStatus() != NodeStatus.FAMILY)
                .filter(t -> t.getParent().equals(rootItem))
                .map(t -> t.getValue().toName())
                .collect(Collectors.toList());

        List<String> choices = families.keySet().stream().sorted().collect(Collectors.toList());
        ChoiceDialog<String> dialog = stageManager.getChoiceDialog("Choice Dialog", "Look, a Choice Dialog", "Select group:", choices.get(0), choices);

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()){
            Family family = families.get(result.get());
            family.getMembers().addAll(selectedNames);
            ListFilesa.calculateRelations(family);

            showFamilies();
        }
    }

    //TODO in future allow to merge families too
    public void newGroupButtonClick(ActionEvent actionEvent) {

        List<Name> selectedNames = familyTreeView.getSelectionModel().getSelectedItems().stream()
                .filter(t -> t.getValue().getStatus() != NodeStatus.FAMILY)
                .filter(t -> t.getParent().equals(rootItem))
                .map(t -> t.getValue().toName())
                .collect(Collectors.toList());

        if (selectedNames.isEmpty()) {
            LOGGER.warn("Games are not selected");
            return;
        }

        TextInputDialog dialog = stageManager.getTextInputDialog("Text Input Dialog", "Look, a Text Input Dialog", "Please enter new group name:", selectedNames.get(0).getCleanName());

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()){
            String familyName = result.get();
            Family family = new Family(familyName, selectedNames);
            families.put(familyName, family);
            ListFilesa.calculateRelations(family);

            showFamilies();
        }
    }

    //TODO in future allow to delete families too
    public void kickAwayButtonClick(ActionEvent actionEvent) {

        Set<Family> modifiedFamilies = new HashSet<>();

        familyTreeView.getSelectionModel().getSelectedItems().stream()
                .filter(t -> t.getValue().getStatus() != NodeStatus.FAMILY)
                .filter(t -> !t.getParent().equals(rootItem))
                .forEach(t -> {
                    String name = t.getValue().getName();
                    String familyName = t.getValue().getFamilyName();
                    Family family = families.get(familyName);
                    List<Name> filteredMembers = family.getMembers().stream().filter(n -> !n.getName().equals(name)).collect(Collectors.toList());
                    family.setMembers(filteredMembers);
                    modifiedFamilies.add(family);
                });

        modifiedFamilies.forEach(ListFilesa::calculateRelations);

        families = families.entrySet().stream()
                .filter(f -> !f.getValue().getMembers().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        showFamilies();
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
