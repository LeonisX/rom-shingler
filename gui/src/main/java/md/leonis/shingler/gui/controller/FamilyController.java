package md.leonis.shingler.gui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import md.leonis.shingler.ListFilesa;
import md.leonis.shingler.gui.controls.ListViewDialog;
import md.leonis.shingler.gui.controls.SmartChoiceDialog;
import md.leonis.shingler.gui.dto.NameView;
import md.leonis.shingler.gui.dto.NodeStatus;
import md.leonis.shingler.gui.view.FxmlView;
import md.leonis.shingler.gui.view.StageManager;
import md.leonis.shingler.model.CollectionType;
import md.leonis.shingler.model.Family;
import md.leonis.shingler.model.GID;
import md.leonis.shingler.model.Name;
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

import static md.leonis.shingler.gui.view.StageManager.runInBackground;
import static md.leonis.shingler.model.ConfigHolder.*;

@Controller
public class FamilyController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FamilyController.class);

    private static final Pattern PATTERN = Pattern.compile("\\d*|\\d+\\.\\d*");

    private final StageManager stageManager;

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
    public Button openDirButton;
    public Button runButton;
    public TreeView<NameView> familyRelationsTreeView;
    public Button expandAllButton2;
    public Button collapseAllButton2;
    public TextField jakkardTextField2;
    public Button findRelativesButton;
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

    public ContextMenu familiesContextMenu;
    public MenuItem renameFamilyMenuItem;
    public MenuItem deleteFamilyMenuItem;
    public MenuItem openFamilyDirItem;
    public MenuItem runFamilyItem;

    public MenuItem kickAwayMenuItem;

    public ContextMenu orphanFamiliesContextMenu;
    public MenuItem addToThisFamilyMenuItem;
    public MenuItem openOrphanFamilyDirItem;
    public MenuItem runOrphanFamilyItem;

    public MenuItem newFamilyMenuItem;
    public MenuItem addToFamilyMenuItem;
    public MenuItem findFamilyMenuItem;
    public MenuItem findFamiliesAutoMenuItem;
    public ContextMenu familyRelationsContextMenu;
    public MenuItem mergeRelativesIntoMenuItem;
    public MenuItem openFamilyRelationsDirItem;
    public MenuItem runFamilyRelationsItem;
    public Button toCollectionsButton;
    public Button saveFamiliesButton;
    public Button saveRelationsButton;


    private TreeItem<NameView> familyRootItem = new TreeItem<>(NameView.EMPTY);
    private TreeItem<NameView> familyRelationsRootItem = new TreeItem<>(NameView.EMPTY);
    private TreeItem<NameView> orphanRootItem = new TreeItem<>(NameView.EMPTY);

    public TreeView<NameView> familyTreeView;
    public TreeView<NameView> orphanTreeView;
    public ComboBox<Integer> precisionCheckBox;
    public TextField jakkardTextField;
    public Button expandAllButton;
    public Button collapseAllButton;

    private TreeItem<NameView> lastNameView = null;

    @Lazy
    public FamilyController(StageManager stageManager) {
        this.stageManager = stageManager;
    }

    @FXML
    @SuppressWarnings("all")
    private void initialize() {

        jakkardTextField.setText("" + jakkard);
        jakkardTextField2.setText("" + jakkard);
        TextFormatter formatter = new TextFormatter((UnaryOperator<TextFormatter.Change>) change -> PATTERN.matcher(change.getControlNewText()).matches() ? change : null);
        jakkardTextField.setTextFormatter(formatter);
        TextFormatter formatter2 = new TextFormatter((UnaryOperator<TextFormatter.Change>) change -> PATTERN.matcher(change.getControlNewText()).matches() ? change : null);
        jakkardTextField2.setTextFormatter(formatter2);

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

        familyTreeView.setOnMouseClicked(mouseEvent -> onMouseClick(familyTreeView));
        familyTreeView.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, this::consume);

        orphanTreeView.setOnMouseClicked(mouseEvent -> onMouseClick(orphanTreeView));
        orphanTreeView.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, this::consume);

        familyRelationsTreeView.setOnMouseClicked(mouseEvent -> onMouseClick(familyRelationsTreeView));
        familyRelationsTreeView.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, this::consume);

        saveFamiliesButton.visibleProperty().bind(familiesModified);
        saveRelationsButton.visibleProperty().bind(familyRelationsModified);

        loadFamilies();
        tryToLoadFamilyRelations();
    }

    private void onMouseClick(TreeView<NameView> treeView) {
        if (treeView.getSelectionModel().getSelectedItems().isEmpty() || treeView.getSelectionModel().getSelectedItems().get(0) == null) {
            lastNameView = null;
        } else {
            lastNameView = treeView.getSelectionModel().getSelectedItem();
        }
    }

    private void consume(ContextMenuEvent event) {
        if (lastNameView == null) {
            event.consume();
        }
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
    private Map<NameView, List<NameView>> orphanView = new LinkedHashMap<>();
    private Map<NameView, List<NameView>> familyRelationsView = new LinkedHashMap<>();

    // TODO expand all previously expanded after operations (group, kick)
    private void showFamilies() {
        int total = romsCollection.getGids().size();
        int inFamily = families.values().stream().map(Family::size).mapToInt(Integer::intValue).sum();

        totalFamiliesLabel.setText("" + families.size());
        totalGamesLabel.setText("" + romsCollection.getGids().size());
        groupedGamesLabel.setText("" + inFamily);
        orphanedGamesLabel.setText("" + (total - inFamily));

        if (tabPane.getSelectionModel().getSelectedIndex() == 0) { // First tab

            familiesView = new LinkedHashMap<>();

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

            familyRootItem.getChildren().clear();
            familyTreeView.setRoot(familyRootItem);
            familyTreeView.setShowRoot(false);
            familyTreeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            familyTreeView.setCellFactory(treeViewCellFactory());

            familiesView.forEach((key, value) -> {
                TreeItem<NameView> node = new TreeItem<>(key);
                value.forEach(c -> node.getChildren().add(new TreeItem<>(c)));
                familyRootItem.getChildren().add(node);
            });

            orphanView = new LinkedHashMap<>();

            Set<String> familyNames = families.values().stream().flatMap(f -> f.getMembers().stream().map(Name::getName)).collect(Collectors.toSet());
            Set<String> orphanedNames = romsCollection.getGids().values().stream().map(GID::getTitle).collect(Collectors.toSet());
            orphanedNames.removeAll(familyNames);

            orphanedNames.stream().filter(this::filter).forEach(name -> orphanView.put(new NameView(name), new ArrayList<>()));

            orphanRootItem.getChildren().clear();
            orphanTreeView.setRoot(orphanRootItem);
            orphanTreeView.setShowRoot(false);
            orphanTreeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            orphanTreeView.setCellFactory(treeViewCellFactory());

            orphanView.forEach((key, value) -> {
                TreeItem<NameView> node = new TreeItem<>(key);
                value.forEach(c -> node.getChildren().add(new TreeItem<>(c)));
                orphanRootItem.getChildren().add(node);
            });

        } else { // Family relations tab

            if (familyRelations != null) {
                familyRelationsView = new LinkedHashMap<>();

                familyRelations.forEach((key1, value1) -> familyRelationsView.put(new NameView(key1, (value1.isEmpty() ? 0 : value1.entrySet().iterator().next().getValue())),
                        value1.entrySet().stream().map(en -> new NameView(en.getKey(), en.getValue())).collect(Collectors.toList())));

                familyRelationsRootItem.getChildren().clear();
                familyRelationsTreeView.setRoot(familyRelationsRootItem);
                familyRelationsTreeView.setShowRoot(false);
                familyRelationsTreeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
                familyRelationsTreeView.setCellFactory(treeViewCellFactory());

                familyRelationsView.forEach((key, value) -> {
                    TreeItem<NameView> node = new TreeItem<>(key);
                    value.forEach(c -> node.getChildren().add(new TreeItem<>(c)));
                    familyRelationsRootItem.getChildren().add(node);
                });
            }
        }

        updateTrees();
    }

    private void updateTrees() {

        if (tabPane.getSelectionModel().getSelectedIndex() == 0) { // First tab
            familyRootItem.getChildren().sort(orderByTitleButton.isSelected() ? byTitle : byJakkard);
        } else { // Family relations tab
            if (familyRelations != null) {
                familyRelationsRootItem.getChildren().sort(orderByTitleButton2.isSelected() ? byTitle : byJakkard);
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

    public void jakkardTextFieldKeyReleased() {
        try {
            jakkard = Double.parseDouble(jakkardTextField.getText());
            familyTreeView.refresh();
        } catch (NumberFormatException ignore) {
            jakkard = 0;
        }
    }

    public void expandAllButtonClick() {
        familyRootItem.getChildren().forEach(item -> {
            if (item != null && !item.isLeaf()) {
                item.setExpanded(true);
            }
        });
    }

    public void collapseAllButtonClick() {
        familyRootItem.getChildren().forEach(item -> {
            if (item != null && !item.isLeaf()) {
                item.setExpanded(false);
            }
        });
    }

    public void precisionCheckBoxAction() {
        setDenominatorId(precisionCheckBox.getSelectionModel().getSelectedIndex());
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

    public void generateFamiliesButtonClick() {
        if (romsCollection.getType() == CollectionType.PLAIN) {
            ListFilesa.generateFamilies();
        } else {
            ListFilesa.archiveToFamilies();
        }
        showFamilies();
    }

    public void calculateRelationsButtonClick() {

        runInBackground(() -> {
            try {
                registerRunningTask("calculateRelations");
                ListFilesa.calculateRelations();
                unRegisterRunningTask("calculateRelations");
                familiesModified.setValue(true);
                showFamilies();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void selectButtonClick() {
        TextInputDialog dialog = stageManager.getTextInputDialog("Text Input Dialog", "Look, a Text Input Dialog", "Enter search phrase:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String phrase = result.get().toLowerCase();
            familyRootItem.getChildren().forEach(c -> {
                if ((c.getValue().getStatus() != NodeStatus.FAMILY) && c.getValue().getName().toLowerCase().contains(phrase)) {
                    familyTreeView.getSelectionModel().select(c);
                }
            });
        }
    }

    public void addToFamilyButtonClick() {

        List<Name> selectedNames = orphanTreeView.getSelectionModel().getSelectedItems().stream()
                .filter(t -> NodeStatus.isMember(t.getValue().getStatus()))
                .filter(t -> t.getParent().equals(orphanRootItem))
                .map(t -> t.getValue().toName())
                .collect(Collectors.toList());

        List<String> choices = families.keySet().stream().sorted().collect(Collectors.toList());
        SmartChoiceDialog<String> dialog = stageManager.getChoiceDialog("Choice Dialog", "Look, a Choice Dialog", "Select family:", choices.get(0), choices);

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            Family family = families.get(result.get());
            family.getMembers().addAll(selectedNames);
            ListFilesa.calculateRelations(family);

            familiesModified.setValue(true);

            showFamilies();
        }
    }

    public void newFamilyButtonClick() {

        List<Name> selectedNames = orphanTreeView.getSelectionModel().getSelectedItems().stream()
                .filter(t -> t.getValue().getStatus() != NodeStatus.FAMILY)
                .filter(t -> t.getParent().equals(orphanRootItem))
                .map(t -> t.getValue().toName())
                .collect(Collectors.toList());

        if (selectedNames.isEmpty()) {
            LOGGER.warn("Games are not selected");
            return;
        }

        TextInputDialog dialog = stageManager.getTextInputDialog("Text Input Dialog", "Look, a Text Input Dialog", "Please enter new family name:", selectedNames.get(0).getCleanName());

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String familyName = result.get();
            Family family = new Family(familyName, selectedNames);
            families.put(familyName, family);
            ListFilesa.calculateRelations(family);

            familiesModified.setValue(true);

            showFamilies();
        }
    }

    public void kickAwayButtonClick() {

        Set<Family> modifiedFamilies = new HashSet<>();

        familyTreeView.getSelectionModel().getSelectedItems().stream()
                .filter(t -> NodeStatus.isMember(t.getValue().getStatus()))
                .filter(t -> !t.getParent().equals(familyRootItem))
                .forEach(t -> {
                    String name = t.getValue().getName();
                    String familyName = t.getValue().getFamilyName();
                    Family family = families.get(familyName);
                    List<Name> filteredMembers = family.getMembers().stream().filter(n -> !n.getName().equals(name)).collect(Collectors.toList());
                    family.setMembers(filteredMembers);
                    modifiedFamilies.add(family);
                });

        modifiedFamilies.forEach(ListFilesa::calculateRelations);

        familiesModified.setValue(true);

        families = families.entrySet().stream()
                .filter(f -> !f.getValue().getMembers().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        showFamilies();
    }

    public void openDirButtonClick() {
        try {
            Desktop.getDesktop().open(romsCollection.getRomsPath().toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public void runListButtonClick() {

        try {
            NameView nameView = lastNameView.getValue();

            if (nameView != null) {
                if (NodeStatus.isFamily(nameView.getStatus())) {
                    Family family = families.get(nameView.getFamilyName());
                    if (!family.getMembers().isEmpty()) {
                        Name bestCandidate = family.getMembers().stream().max(Comparator.comparing(Name::getIndex)).orElse(family.getMembers().get(0));

                        ListViewDialog<String> dialog = stageManager.getListViewDialog("Choice Dialog", "Look, a Choice Dialog", "Select a specific game:",
                                bestCandidate.getName(), family.getMembers().stream().map(Name::getName).collect(Collectors.toList()));

                        Optional<String> result = dialog.showAndWait();
                        if (result.isPresent()) {
                            bestCandidate = family.getMembers().stream().filter(n -> n.getName().equals(result.get())).findFirst().orElse(bestCandidate);
                        }

                        nameView = new NameView(bestCandidate, bestCandidate.getName(), -1);
                    }
                }

                if (romsCollection.getType() == CollectionType.PLAIN) {
                    Desktop.getDesktop().open(romsCollection.getRomsPath().resolve(nameView.getName()).toFile());
                } else {
                    Path path = IOUtils.extractFromArchive(romsCollection.getRomsPath().resolve(nameView.getFamilyName()), nameView.getName());
                    Desktop.getDesktop().open(path.toFile());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void findFamilyButtonClick() {

        orphanTreeView.getSelectionModel().getSelectedItems().forEach(selectedItem -> {
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

    public void addToThisFamilyClick() {

        long modified = orphanTreeView.getSelectionModel().getSelectedItems().stream().filter(i -> NodeStatus.isFamily(i.getValue().getStatus())).peek(selectedItem -> {
            NameView nameView = selectedItem.getValue();

            Name parent = selectedItem.getParent().getValue().toName();
            Family family = families.get(nameView.getFamilyName());
            family.getMembers().add(parent);

            ListFilesa.calculateRelations(family);

        }).count();

        familiesModified.setValue(modified > 0);

        showFamilies();
    }

    public void findFamiliesAutoButtonClick() {

        long modified = orphanTreeView.getSelectionModel().getSelectedItems().stream().map(selectedItem -> {
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

        familiesModified.setValue(modified > 0);

        orphanTreeView.getSelectionModel().clearSelection();

        showFamilies();
    }

    public void checkBoxAction() {
        showFamilies();
    }

    public void expandAllButtonClick2() {
        familyRelationsRootItem.getChildren().forEach(item -> {
            if (item != null && !item.isLeaf()) {
                item.setExpanded(true);
            }
        });
    }

    public void collapseAllButtonClick2() {
        familyRelationsRootItem.getChildren().forEach(item -> {
            if (item != null && !item.isLeaf()) {
                item.setExpanded(false);
            }
        });
    }

    //TODO unify
    public void jakkardTextFieldKeyReleased2() {
        try {
            jakkard = Double.parseDouble(jakkardTextField2.getText());
            familyRelationsTreeView.refresh();
        } catch (NumberFormatException ignore) {
            jakkard = 0;
        }
    }

    public void findRelativesButtonClick() {

        LOGGER.info("Find related families...");

        if (tryToLoadFamilyRelations() == null) {
            findAgainRelativesButtonClick();
        } else {
            showFamilies();
        }
    }

    private Map<Family, Map<Family, Double>> tryToLoadFamilyRelations() {
        File familyRelationsFile = fullFamilyRelationsPath().toFile();

        if (familyRelationsFile.exists()) {
            LOGGER.info(String.format("\nReading family relations from file %s...", familyRelationsFile));
            familyRelations = IOUtils.loadFamilyRelations(familyRelationsFile);
        }
        return familyRelations;
    }

    public void findAgainRelativesButtonClick() {

        LOGGER.info("Find related families...");

        LOGGER.info("\nGenerating family relations from scratch...");
        familyRelations = families.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getValue, e -> ListFilesa.calculateRelations(e.getValue().getMother().getName(), e.getKey()), (first, second) -> first, LinkedHashMap::new));

        familyRelationsModified.setValue(true);

        showFamilies();
    }

    public void mergeRelativesIntoButtonClick() {

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

            familiesModified.setValue(true);
            familyRelationsModified.setValue(true);

            showFamilies();
        }
    }

    public void orderButtonClick() {
        updateTrees();
    }

    public void renameFamilyMenuItemClick() {

        ObservableList<TreeItem<NameView>> selectedItems = familyTreeView.getSelectionModel().getSelectedItems();

        if (selectedItems.size() == 1 && selectedItems.get(0).getValue().getStatus() == NodeStatus.FAMILY) {

            String familyName = selectedItems.get(0).getValue().getFamilyName();

            stageManager.getTextInputDialog("title", "headerText", "contextText", familyName)
                    .showAndWait().ifPresent(newFamilyName -> {

                if (!familyName.equals(newFamilyName)) {
                    Family family = families.remove(familyName);
                    family.setName(newFamilyName);
                    families.put(family.getName(), family);

                    familyRelations.forEach((key, value) -> {
                        if (key.getName().equals(familyName)) {
                            key.setName(newFamilyName);
                        }
                        value.forEach((key1, value1) -> {
                            if (key1.getName().equals(familyName)) {
                                key1.setName(newFamilyName);
                            }
                        });
                    });

                    familiesModified.setValue(true);
                    familyRelationsModified.setValue(true);

                    showFamilies();
                }
            });
        }
    }

    public void deleteFamilyMenuItemClick() {

        ObservableList<TreeItem<NameView>> selectedItems = familyTreeView.getSelectionModel().getSelectedItems();

        List<String> toDelete = selectedItems.stream().filter(i -> i.getValue().getStatus() == NodeStatus.FAMILY).map(i -> i.getValue().getFamilyName()).collect(Collectors.toList());

        if (!toDelete.isEmpty()) {
            String text = toDelete.size() == 1 ? toDelete.get(0) : toDelete.size() + " families";

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation Dialog");
            alert.setHeaderText("Look, a Confirmation Dialog");
            alert.setContentText(String.format("Delete %s?", text));

            alert.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    toDelete.forEach(familyName -> {
                        families.remove(familyName);
                        familyRelations.entrySet().removeIf(e -> toDelete.contains(e.getKey().getName()));
                        familyRelations.forEach((key, value) -> value.entrySet().removeIf(e -> toDelete.contains(e.getKey().getName())));
                    });

                    familiesModified.setValue(true);
                    familyRelationsModified.setValue(true);

                    showFamilies();
                }
            });
        }
    }


    public void familyTreeViewContextMenuRequest(ContextMenuEvent event) {
        showPopupMenu(familiesContextMenu, familyTreeView, familyRootItem, event);
    }

    public void orphanTreeViewContextMenuRequest(ContextMenuEvent event) {
        showPopupMenu(orphanFamiliesContextMenu, orphanTreeView, orphanRootItem, event);
    }

    private void showPopupMenu(ContextMenu contextMenu, TreeView<NameView> treeView, TreeItem<NameView> rootItem, ContextMenuEvent event) {

        if (lastNameView != null && findRoot(lastNameView) == rootItem) {

            String hideId = (NodeStatus.isFamily(lastNameView.getValue().getStatus())) ? "m" : "f";

            contextMenu.getItems().forEach(item -> {
                if (item.getUserData() != null && item.getUserData().equals(hideId)) {
                    item.setVisible(false);
                } else {
                    item.setVisible(true);
                }
            });

            contextMenu.show(treeView, event.getScreenX(), event.getScreenY());
        }
    }

    public void familyRelationsContextMenuRequest(ContextMenuEvent event) {

        if (lastNameView != null && findRoot(lastNameView) == familyRelationsRootItem) {

            String hideId = lastNameView.getParent().equals(familyRelationsRootItem) ? "m" : "f";

            familyRelationsContextMenu.getItems().forEach(item -> {
                if (item.getUserData() != null && item.getUserData().equals(hideId)) {
                    item.setVisible(false);
                } else {
                    item.setVisible(true);
                }
            });

            familyRelationsContextMenu.show(familyRelationsTreeView, event.getScreenX(), event.getScreenY());
        }
    }

    private TreeItem<NameView> findRoot(TreeItem<NameView> item) {
        if (item == null || item.getParent() == null) {
            return item;
        } else {
            return findRoot(item.getParent());
        }
    }

    public void toCollectionsButtonClick() {
        if (familiesModified.getValue()) {
            saveFamiliesButtonClick();
        }
        if (familyRelationsModified.getValue()) {
            saveRelationsButtonClick();
        }
        stageManager.showPane(FxmlView.COLLECTION);
    }

    public void saveFamiliesButtonClick() {
        LOGGER.info("Saving families...");
        IOUtils.createDirectories(workFamiliesPath());
        stageManager.showWaitAlertAndRun("Saving families", () -> IOUtils.serialize(fullFamiliesPath().toFile(), families));
        familiesModified.setValue(false);
    }

    public void saveRelationsButtonClick() {
        LOGGER.info("Saving family relations...");
        IOUtils.createDirectories(workFamiliesPath());
        stageManager.showWaitAlertAndRun("Saving family relations", () -> IOUtils.serialize(fullFamilyRelationsPath().toFile(), familyRelations));
        familyRelationsModified.setValue(false);
    }
}
