package md.leonis.shingler.gui.controller;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import javafx.util.Pair;
import md.leonis.shingler.ListFilesa;
import md.leonis.shingler.gui.controls.ListViewDialog;
import md.leonis.shingler.gui.controls.SmartChoiceDialog;
import md.leonis.shingler.gui.dto.NameView;
import md.leonis.shingler.gui.dto.NodeStatus;
import md.leonis.shingler.gui.view.StageManager;
import md.leonis.shingler.model.*;
import md.leonis.shingler.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static md.leonis.shingler.model.ConfigHolder.*;
import static md.leonis.shingler.utils.BinaryUtils.bytesToHex;

@Controller
public class FamilyController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FamilyController.class);

    private static final Pattern PATTERN = Pattern.compile("\\d*|\\d+\\.\\d*");

    private final StageManager stageManager;
    private final ConfigHolder configHolder;

    public VBox vBox;

    public HBox controlsHBox;
    public Label waitLabel;

    public Button generateFamiliesButton;

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
    public Button openDirButton;
    public Button runButton;
    public Button findFamilyButton;
    public Button addToFamilyButton;
    public Button findFamiliesAutoButton;
    public CheckBox familiesCheckBox;
    public TreeView<NameView> familyRelationsTreeView;
    public Button expandAllButton2;
    public Button collapseAllButton2;
    public TextField jakkardTextField2;
    public Button findRelativesButton;
    public Button mergeRelativesIntoButton;
    public TabPane tabPane;
    public ToggleButton orderByTitleButton;
    public ToggleGroup orderTG;
    public ToggleButton orderByJakkardButton;
    public ToggleButton orderByTitleButton2;
    public ToggleGroup orderTG2;
    public ToggleButton orderByJakkardButton2;
    public Button findAgainRelativesButton;
    public Button openDirButton2;
    public Button runButton2;
    public Button runListButton2;
    public Button runListButton;

    private TreeItem<NameView> rootItem = new TreeItem<>(NameView.EMPTY);
    private TreeItem<NameView> rootItem2 = new TreeItem<>(NameView.EMPTY);

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
        jakkardTextField2.setText("" + jakkard);
        TextFormatter formatter = new TextFormatter((UnaryOperator<TextFormatter.Change>) change -> PATTERN.matcher(change.getControlNewText()).matches() ? change : null);
        jakkardTextField.setTextFormatter(formatter);

        precisionCheckBox.setItems(FXCollections.observableArrayList(DENOMINATORS));
        precisionCheckBox.setCellFactory(precisionCheckBoxCellFactory);
        precisionCheckBox.getSelectionModel().select(getDenominatorId());

        controlsHBox.managedProperty().bind(controlsHBox.visibleProperty());
        waitLabel.managedProperty().bind(waitLabel.visibleProperty());

        tabPane.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
            jakkardTextField.setText("" + jakkard);
            jakkardTextField2.setText("" + jakkard);
            showFamilies();
        });

        //TODO load family relations here???
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

    private Map<NameView, List<NameView>> familiesView = new LinkedHashMap<>();
    private Map<NameView, List<NameView>> familyRelationsView = new LinkedHashMap<>();

    // TODO expand all expanded after operations (group, kick)
    private void showFamilies() {
        int total = romsCollection.getGids().size();
        int inFamily = families.values().stream().map(Family::size).mapToInt(Integer::intValue).sum();

        totalFamiliesLabel.setText("" + families.size());
        totalGamesLabel.setText("" + romsCollection.getGids().size());
        groupedGamesLabel.setText("" + inFamily);
        orphanedGamesLabel.setText("" + (total - inFamily));

        if (tabPane.getSelectionModel().getSelectedIndex() == 0) { // First tab

            familiesView = new LinkedHashMap<>();

            //TODO delete at all, use separate trees
            if (familiesCheckBox.isSelected()) {
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
            }

            Set<String> familyNames = families.values().stream().flatMap(f -> f.getMembers().stream().map(Name::getName)).collect(Collectors.toSet());
            Set<String> orphanedNames = romsCollection.getGids().values().stream().map(GID::getTitle).collect(Collectors.toSet());
            orphanedNames.removeAll(familyNames);

            orphanedNames.stream().filter(this::filter)/*.sorted()*/.forEach(name -> familiesView.put(new NameView(name), new ArrayList<>()));

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
        } else { // Family relations tab

            if (familyRelations != null) {
                familyRelationsView = new LinkedHashMap<>();

                familyRelations.forEach((key1, value1) -> familyRelationsView.put(new NameView(key1, (value1.isEmpty() ? 0 : value1.entrySet().iterator().next().getValue())),
                        value1.entrySet().stream().map(en -> new NameView(en.getKey(), en.getValue())).collect(Collectors.toList())));

                rootItem2.getChildren().clear();
                familyRelationsTreeView.setRoot(rootItem2);
                familyRelationsTreeView.setShowRoot(false);
                familyRelationsTreeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                familyRelationsTreeView.setCellFactory(treeViewCellFactory());

                familyRelationsView.forEach((key, value) -> {
                    TreeItem<NameView> node = new TreeItem<>(key);
                    value.forEach(c -> node.getChildren().add(new TreeItem<>(c)));
                    rootItem2.getChildren().add(node);
                });
            }
        }

        updateTrees();
    }

    private void updateTrees() {

        if (tabPane.getSelectionModel().getSelectedIndex() == 0) { // First tab
            rootItem.getChildren().sort(orderByTitleButton.isSelected() ? byTitle : byJakkard);
        } else { // Family relations tab
            if (familyRelations != null) {
                rootItem2.getChildren().sort(orderByTitleButton2.isSelected() ? byTitle : byJakkard);
            }
        }
    }

    private static Comparator<TreeItem<NameView>> byTitle = Comparator.comparing((TreeItem<NameView> n) -> n.getValue().getName());
    private static Comparator<TreeItem<NameView>> byJakkard = Comparator.comparing((TreeItem<NameView> n) -> n.getValue().getJakkardStatus()).reversed();

    private boolean filter(String name) {
        boolean p = name.contains("(PD)") || name.contains("(PD)");
        boolean h = name.contains("(Hack)") || name.contains("(Hack)")
                || name.contains("(Hack ") || name.contains("(Hack ")
                || name.contains(" Hack)") || name.contains(" Hack)");
        boolean g = !(p || h);

        return (p && pdCheckBox.isSelected()) || (h && hackCheckBox.isSelected()) || (g && allGoodCheckBox.isSelected());
    }

    public void jakkardTextFieldKeyReleased(KeyEvent keyEvent) {
        try {
            jakkard = Double.parseDouble(jakkardTextField.getText());
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

                            if (item.getStatus() == NodeStatus.FAMILY || item.getStatus() == NodeStatus.ORPHAN) {
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
        SmartChoiceDialog<String> dialog = stageManager.getChoiceDialog("Choice Dialog", "Look, a Choice Dialog", "Select group:", choices.get(0), choices);

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            Family family = families.get(result.get());
            family.getMembers().addAll(selectedNames);
            ListFilesa.calculateRelations(family);
            LOGGER.info("Saving family...");
            IOUtils.serialize(fullFamiliesPath().toFile(), families);

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
        if (result.isPresent()) {
            String familyName = result.get();
            Family family = new Family(familyName, selectedNames);
            families.put(familyName, family);
            ListFilesa.calculateRelations(family);
            LOGGER.info("Saving family...");
            IOUtils.serialize(fullFamiliesPath().toFile(), families);

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
        LOGGER.info("Saving family...");
        IOUtils.serialize(fullFamiliesPath().toFile(), families);

        families = families.entrySet().stream()
                .filter(f -> !f.getValue().getMembers().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        showFamilies();
    }

    public void openDirButtonClick(ActionEvent actionEvent) {

        try {
            Desktop.getDesktop().open(romsCollection.getRomsPath().toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void runButtonClick(ActionEvent actionEvent) {

        try {
            TreeItem<NameView> item = familyTreeView.getSelectionModel().getSelectedItem();
            NameView nameView = item.getValue();

            if (nameView.getStatus() == NodeStatus.FAMILY || nameView.getStatus() == NodeStatus.FAMILY_LIST) {
                Family family = families.get(nameView.getFamilyName());
                Name bestCandidate = family.getMembers().stream().max(Comparator.comparing(Name::getIndex)).orElse(null);
                nameView = new NameView(bestCandidate, bestCandidate.getName(), -1);
            }

            if (romsCollection.getType() == CollectionType.PLAIN) {
                Desktop.getDesktop().open(romsCollection.getRomsPath().resolve(nameView.getName()).toFile());
            } else {
                Path path = IOUtils.extractFromArchive(romsCollection.getRomsPath().resolve(nameView.getFamilyName()), nameView.getName());
                Desktop.getDesktop().open(path.toFile());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //TODO unify
    public void runButtonClick2(ActionEvent actionEvent) {

        try {
            TreeItem<NameView> item = familyRelationsTreeView.getSelectionModel().getSelectedItem();
            NameView nameView = item.getValue();

            if (nameView.getStatus() == NodeStatus.FAMILY || nameView.getStatus() == NodeStatus.FAMILY_LIST) {
                Family family = families.get(nameView.getFamilyName());
                Name bestCandidate = family.getMembers().stream().max(Comparator.comparing(Name::getIndex)).orElse(null);
                nameView = new NameView(bestCandidate, bestCandidate.getName(), -1);
            }

            if (romsCollection.getType() == CollectionType.PLAIN) {
                Desktop.getDesktop().open(romsCollection.getRomsPath().resolve(nameView.getName()).toFile());
            } else {
                Path path = IOUtils.extractFromArchive(romsCollection.getRomsPath().resolve(nameView.getFamilyName()), nameView.getName());
                Desktop.getDesktop().open(path.toFile());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public void runListButtonClick(ActionEvent actionEvent) {

        try {
            TreeItem<NameView> item = familyTreeView.getSelectionModel().getSelectedItem();
            NameView nameView = item.getValue();

            if (nameView.getStatus() == NodeStatus.FAMILY || nameView.getStatus() == NodeStatus.FAMILY_LIST) {
                Family family = families.get(nameView.getFamilyName());
                Name bestCandidate = family.getMembers().stream().max(Comparator.comparing(Name::getIndex)).orElse(null);

                ListViewDialog<String> dialog = stageManager.getListViewDialog("Choice Dialog", "Look, a Choice Dialog", "Select group:",
                        bestCandidate.getName(), family.getMembers().stream().map(Name::getName).collect(Collectors.toList()));

                Optional<String> result = dialog.showAndWait();
                if (result.isPresent()) {
                    bestCandidate = family.getMembers().stream().filter(n -> n.getName().equals(result.get())).findFirst().orElse(null);
                }

                nameView = new NameView(bestCandidate, bestCandidate.getName(), -1);
            }

            if (romsCollection.getType() == CollectionType.PLAIN) {
                Desktop.getDesktop().open(romsCollection.getRomsPath().resolve(nameView.getName()).toFile());
            } else {
                Path path = IOUtils.extractFromArchive(romsCollection.getRomsPath().resolve(nameView.getFamilyName()), nameView.getName());
                Desktop.getDesktop().open(path.toFile());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void runListButton2Click(ActionEvent actionEvent) {

        try {
            TreeItem<NameView> item = familyRelationsTreeView.getSelectionModel().getSelectedItem();
            NameView nameView = item.getValue();

            if (nameView.getStatus() == NodeStatus.FAMILY || nameView.getStatus() == NodeStatus.FAMILY_LIST) {
                Family family = families.get(nameView.getFamilyName());
                Name bestCandidate = family.getMembers().stream().max(Comparator.comparing(Name::getIndex)).orElse(null);

                ListViewDialog<String> dialog = stageManager.getListViewDialog("Choice Dialog", "Look, a Choice Dialog", "Select group:",
                        bestCandidate.getName(), family.getMembers().stream().map(Name::getName).collect(Collectors.toList()));

                Optional<String> result = dialog.showAndWait();
                if (result.isPresent()) {
                    bestCandidate = family.getMembers().stream().filter(n -> n.getName().equals(result.get())).findFirst().orElse(null);
                }

                nameView = new NameView(bestCandidate, bestCandidate.getName(), -1);
            }

            if (romsCollection.getType() == CollectionType.PLAIN) {
                Desktop.getDesktop().open(romsCollection.getRomsPath().resolve(nameView.getName()).toFile());
            } else {
                Path path = IOUtils.extractFromArchive(romsCollection.getRomsPath().resolve(nameView.getFamilyName()), nameView.getName());
                Desktop.getDesktop().open(path.toFile());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void findFamilyButtonClick(ActionEvent actionEvent) {

        familyTreeView.getSelectionModel().getSelectedItems().forEach(selectedItem -> {
            NameView nameView = selectedItem.getValue();

            List<NameView> items = ListFilesa.calculateRelations(nameView.getName()).entrySet().stream()
                    .map(e -> new NameView(e.getKey(), e.getValue())).collect(Collectors.toList());

            nameView.setItems(items);

            selectedItem.getChildren().setAll(
                    items.stream().map(TreeItem::new).collect(Collectors.toList())
            );
            nameView.setJakkardStatus(items.get(0).getJakkardStatus());
            //selectedItem.setExpanded(true);
        });
        updateTrees();
    }

    public void addToFamilyButtonClick(ActionEvent actionEvent) {

        long modified = familyTreeView.getSelectionModel().getSelectedItems().stream().filter(i -> i.getValue().getStatus() == NodeStatus.FAMILY_LIST).peek(selectedItem -> {
            NameView nameView = selectedItem.getValue();

            if (nameView.getStatus() == NodeStatus.FAMILY_LIST) {
                Name parent = selectedItem.getParent().getValue().toName();
                Family family = families.get(nameView.getFamilyName());
                family.getMembers().add(parent);

                ListFilesa.calculateRelations(family);
            }
        }).count();

        if (modified > 0) {
            showFamilies();
            IOUtils.serialize(fullFamiliesPath().toFile(), families);
        }
    }

    public void findFamiliesAutoButtonClick(ActionEvent actionEvent) {

        long modified = familyTreeView.getSelectionModel().getSelectedItems().stream().map(selectedItem -> {
            NameView nameView = selectedItem.getValue();
            Map.Entry<Family, Double> entry = ListFilesa.calculateRelations(nameView.getName()).entrySet().iterator().next();

            if (entry.getValue() >= jakkard) {
                Family family = entry.getKey();
                family.getMembers().add(nameView.toName());
                return family;
            } else {
                return null;
            }
        }).filter(Objects::nonNull).peek(ListFilesa::calculateRelations).count();

        if (modified > 0) {
            showFamilies();
            IOUtils.serialize(fullFamiliesPath().toFile(), families);
        }
        familyTreeView.getSelectionModel().clearSelection();
    }

    public void checkBoxAction(ActionEvent actionEvent) {
        showFamilies();
    }

    public void expandAllButtonClick2(ActionEvent actionEvent) {
        rootItem2.getChildren().forEach(item -> {
            if (item != null && !item.isLeaf()) {
                item.setExpanded(true);
            }
        });
    }

    public void collapseAllButtonClick2(ActionEvent actionEvent) {
        rootItem2.getChildren().forEach(item -> {
            if (item != null && !item.isLeaf()) {
                item.setExpanded(false);
            }
        });
    }

    //TODO unify
    public void jakkardTextFieldKeyReleased2(KeyEvent keyEvent) {
        try {
            jakkard = Double.parseDouble(jakkardTextField2.getText());
            familyRelationsTreeView.refresh();
        } catch (NumberFormatException ignore) {
            jakkard = 0;
        }
    }

    public void findRelativesButtonClick(ActionEvent actionEvent) {

        LOGGER.info("Find related families...");

        File familyRelationsFile = fullFamilyRelationsPath().toFile();

        if (familyRelationsFile.exists()) {
            LOGGER.info(String.format("\nReading family relations from file %s...", familyRelationsFile));
            familyRelations = IOUtils.loadFamilyRelations(familyRelationsFile);
            showFamilies();
        } else {
            findAgainRelativesButtonClick(null);
        }

    }

    public void findAgainRelativesButtonClick(ActionEvent actionEvent) {

        LOGGER.info("Find related families...");

        File familyRelationsFile = fullFamilyRelationsPath().toFile();

        LOGGER.info("\nGenerating family relations from scratch...");
        familyRelations = families.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, e -> ListFilesa.calculateRelations(e.getValue().getMother().getName(), e.getKey()), (first, second) -> first, LinkedHashMap::new));

        IOUtils.createDirectories(workFamiliesPath());
        IOUtils.serialize(familyRelationsFile, familyRelations);

        showFamilies();
    }

    public void mergeRelativesIntoButtonClick(ActionEvent actionEvent) {

        if (familyRelationsTreeView.getSelectionModel().getSelectedItems().size() == 1) {

            TreeItem<NameView> mainFamilyTreeItem = familyRelationsTreeView.getSelectionModel().getSelectedItem();
            TreeItem<NameView> newFamilyMembersTreeItem = mainFamilyTreeItem.getParent();

            // move all members
            Family mainFamily = families.get(mainFamilyTreeItem.getValue().getFamilyName());
            Family newFamilyMembers = families.get(newFamilyMembersTreeItem.getValue().getFamilyName());
            mainFamily.getMembers().addAll(newFamilyMembers.getMembers());

            // delete empty family
            families.remove(newFamilyMembersTreeItem.getValue().getFamilyName());
            familyRelations.remove(newFamilyMembers);

            // calculate relations (main family), select mother
            ListFilesa.calculateRelations(mainFamily);

            // recalculate relations for main family
            ListFilesa.calculateRelations(mainFamily.getMother().getName(), mainFamily.getName());

            showFamilies();

            // save, save
            IOUtils.createDirectories(workFamiliesPath());
            IOUtils.serialize(fullFamiliesPath().toFile(), families);
            IOUtils.serialize(fullFamilyRelationsPath().toFile(), familyRelations);
        }
    }

    public void orderButtonClick(ActionEvent actionEvent) {
        updateTrees();
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
