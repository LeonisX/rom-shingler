package md.leonis.shingler.gui.controller;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Callback;
import javafx.util.Duration;
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
    public MenuItem newSeparateFamiliesMenuItem;
    public MenuItem copyFamilyNameItem;
    public MenuItem copyOrphanNameItem;
    public TextField filterOrphanesTextField;
    public CheckBox redCheckBox;
    public CheckBox blackCheckBox;
    public CheckBox redFamilyCheckBox;
    public CheckBox blackFamilyCheckBox;
    /*public Button saveFamiliesButtonS;
    public Button saveFamiliesButtonJ;*/


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

    private Map<TreeView, String> searchMap = new HashMap<>();

    private Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(2), ev -> searchMap.entrySet().forEach(e -> e.setValue(""))));

    private Map<String, List<TreeItem<NameView>>> orphanChildren = new HashMap<>();

    private String orphanFilter = "";

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

        searchMap.put(familyTreeView, "");
        searchMap.put(orphanTreeView, "");
        searchMap.put(familyRelationsTreeView, "");

        searchMap.entrySet().forEach(e -> {
            e.getKey().setOnMouseClicked(mouseEvent -> onMouseClick(e.getKey()));
            e.getKey().addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, this::consume);
            e.getKey().setOnKeyPressed(getKeyEventEventHandler(e.getKey()));
        });

        saveFamiliesButton.visibleProperty().bind(familiesModified);
        saveRelationsButton.visibleProperty().bind(familyRelationsModified);

        familiesContextMenu.setUserData(familyTreeView);
        orphanFamiliesContextMenu.setUserData(orphanTreeView);

        familyTreeView.setRoot(familyRootItem);
        familyTreeView.setShowRoot(false);
        familyTreeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        familyTreeView.setCellFactory(treeViewCellFactory());

        orphanTreeView.setRoot(orphanRootItem);
        orphanTreeView.setShowRoot(false);
        orphanTreeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        orphanTreeView.setCellFactory(treeViewCellFactory());

        familyRelationsTreeView.setRoot(familyRelationsRootItem);
        familyRelationsTreeView.setShowRoot(false);
        familyRelationsTreeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        familyRelationsTreeView.setCellFactory(treeViewCellFactory());

        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();

        loadFamilies();
        tryToLoadFamilyRelations();
    }

    private EventHandler<KeyEvent> getKeyEventEventHandler(TreeView<NameView> treeView) {

        Set<KeyCode> codes = new HashSet<>(Arrays.asList(KeyCode.ESCAPE, KeyCode.UP, KeyCode.DOWN, KeyCode.LEFT, KeyCode.RIGHT,
                KeyCode.ENTER, KeyCode.PAGE_DOWN, KeyCode.PAGE_UP, KeyCode.HOME, KeyCode.END, KeyCode.INSERT, KeyCode.DELETE,
                KeyCode.SHIFT, KeyCode.CONTROL, KeyCode.ALT));

        return e -> {

            timeline.stop();
            final String searchString = searchMap.get(treeView);

            if (codes.contains(e.getCode())) {
                searchMap.replace(treeView, "");
                return;

            } else if (e.getCode() == KeyCode.BACK_SPACE && !searchString.isEmpty()) {
                searchMap.replace(treeView, searchString.substring(0, searchString.length() - 1));

            } else {
                searchMap.replace(treeView, searchString + e.getText().toLowerCase());
            }

            Optional<TreeItem<NameView>> result = treeView.getRoot().getChildren().stream()
                    .filter(c -> c.getValue().getName().toLowerCase().startsWith(searchMap.get(treeView))).findFirst();

            if (result.isPresent()) {
                int index = treeView.getRoot().getChildren().indexOf(result.get());
                treeView.getSelectionModel().clearAndSelect(index);
                treeView.scrollTo(index - 1);
            } else {
                searchMap.replace(treeView, "");
            }
            timeline.play();
        };
    }

    private void onMouseClick(TreeView<NameView> treeView) {
        searchMap.forEach((key, value) -> searchMap.replace(treeView, ""));
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

    private Map<NameView, List<NameView>> familiesView = new LinkedHashMap<>();
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

                        //views = views.stream().sorted(Comparator.comparing(NameView::getJakkardStatus).reversed()).collect(Collectors.toList());

                        NameView root = new NameView(e, views);
                        familiesView.put(root, views);
                    });

            familyRootItem.getChildren().clear();

            familiesView.forEach((key, value) -> {
                TreeItem<NameView> node = new TreeItem<>(key);
                value.forEach(c -> node.getChildren().add(new TreeItem<>(c)));
                if (isAllowedColor(redFamilyCheckBox, blackFamilyCheckBox, node)) {
                    familyRootItem.getChildren().add(node);
                }
            });

            List<NameView> orphanList = new ArrayList<>();

            Set<String> familyNames = families.values().stream().flatMap(f -> f.getMembers().stream().map(Name::getName)).collect(Collectors.toSet());
            Set<String> orphanedNames = romsCollection.getGids().values().stream().map(GID::getTitle).collect(Collectors.toSet());
            orphanedNames.removeAll(familyNames);

            orphanedNames.stream().filter(this::filter).forEach(name -> orphanList.add(new NameView(name)));

            orphanRootItem.getChildren().forEach(c -> {
                if (!c.getChildren().isEmpty()) {
                    orphanChildren.put(c.getValue().getName(), c.getChildren());
                }
            });

            orphanRootItem.getChildren().clear();

            orphanList.forEach(o -> {
                TreeItem<NameView> node = new TreeItem<>(o);
                List<TreeItem<NameView>> ch = orphanChildren.get(o.getName());
                if (ch != null && !ch.isEmpty()) {
                    o.setJakkardStatus(ch.get(0).getValue().getJakkardStatus());
                    o.setItems(ch.stream().map(TreeItem::getValue).collect(Collectors.toList()));
                    node.getChildren().addAll(ch);
                }
                if (isAllowedColor(redCheckBox, blackCheckBox, node)) {
                    orphanRootItem.getChildren().add(node);
                }
            });

        } else { // Family relations tab

            if (familyRelations != null) {
                familyRelationsView = new LinkedHashMap<>();

                familyRelations.forEach((key1, value1) -> familyRelationsView.put(new NameView(key1, (value1.isEmpty() ? 0 : value1.entrySet().iterator().next().getValue())),
                        value1.entrySet().stream().map(en -> new NameView(en.getKey(), en.getValue())).collect(Collectors.toList())));

                familyRelationsRootItem.getChildren().clear();

                familyRelationsView.forEach((key, value) -> {
                    TreeItem<NameView> node = new TreeItem<>(key);
                    value.forEach(c -> node.getChildren().add(new TreeItem<>(c)));
                    familyRelationsRootItem.getChildren().add(node);
                });
            }
        }

        updateTrees();
    }

    private boolean isAllowedColor(CheckBox redCheckBox, CheckBox blackCheckBox, TreeItem<NameView> node) {
        boolean red = isRed(node.getValue());
        return (red && redCheckBox.isSelected()) || (!red && blackCheckBox.isSelected());
    }

    private void updateTrees() {

        if (tabPane.getSelectionModel().getSelectedIndex() == 0) { // First tab
            familyRootItem.getChildren().forEach(c -> c.getChildren().sort(orderByTitleButton.isSelected() ? byTitle : byJakkard));
            orphanRootItem.getChildren().sort(orderByTitleButton.isSelected() ? byTitle : byJakkard);

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
        boolean nameFilter = orphanFilter.isEmpty() || name.toLowerCase().contains(orphanFilter);
        boolean g = !(p || h);

        return ((p && pdCheckBox.isSelected()) || (h && hackCheckBox.isSelected()) || (g && allGoodCheckBox.isSelected())) && nameFilter;
    }

    public void jakkardTextFieldKeyReleased(KeyEvent event) {
        try {
            TextField source = (TextField) event.getSource();
            jakkard = Double.parseDouble(source.getText());
            familyRelationsTreeView.refresh();
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
                            setText(item.toString());
                            setTextFill(isRed(item) ? Color.RED : Color.BLACK);
                            familyTreeView.refresh();
                        } else {
                            setText(null);
                        }
                    }
                };
            }
        };
    }

    private boolean isRed(NameView item) {
        double minIndex = item.getJakkardStatus();

        if (item.getStatus() == NodeStatus.FAMILY) {
            if (item.getItems().size() >= 2) {
                minIndex = item.getItems().stream().min(Comparator.comparing(NameView::getJakkardStatus)).map(NameView::getJakkardStatus).orElse(0.0);
            } else {
                minIndex = 100;
            }
        } else if (item.getStatus() == NodeStatus.ORPHAN) {
            if (item.getItems().size() >= 1) {
                minIndex = item.getItems().stream().max(Comparator.comparing(NameView::getJakkardStatus)).map(NameView::getJakkardStatus).orElse(0.0);
            } else {
                minIndex = 100;
            }
        }

        return minIndex < jakkard;
    }

    public void generateFamiliesButtonClick() {
        if (romsCollection.getType() == CollectionType.PLAIN) {
            ListFilesa.generateFamilies();
        } else {
            ListFilesa.archiveToFamilies();
        }
        orphanChildren.clear();
        showFamilies();
    }

    public void calculateRelationsButtonClick() {

        runInBackground(() -> {
            registerRunningTask("calculateRelations");
            ListFilesa.calculateRelations();
            unRegisterRunningTask("calculateRelations");
            familiesModified.setValue(true);
            orphanChildren.clear();
        }, this::showFamilies);
    }

    public void selectButtonClick() {
        TextInputDialog dialog = stageManager.getTextInputDialog("Text Input Dialog", "Look, a Text Input Dialog", "Enter search phrase:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            orphanTreeView.getSelectionModel().clearSelection();
            String phrase = result.get().toLowerCase();
            orphanRootItem.getChildren().forEach(c -> {
                if ((c.getValue().getStatus() != NodeStatus.FAMILY) && c.getValue().getName().toLowerCase().contains(phrase)) {
                    orphanTreeView.getSelectionModel().select(c);
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
        result.ifPresent(s -> runInBackground(() -> {
            Family family = families.get(s);
            family.getMembers().addAll(selectedNames);
            ListFilesa.calculateRelations(family);

            familiesModified.setValue(true);
        }, this::showFamilies));
    }

    public void newFamilyButtonClick() {

        List<Name> selectedNames = getSelectedOrphanes();

        if (selectedNames.isEmpty()) {
            LOGGER.warn("Games are not selected");
            return;
        }

        TextInputDialog dialog = stageManager.getTextInputDialog("Text Input Dialog", "Look, a Text Input Dialog", "Please enter new family name:", selectedNames.get(0).getCleanName());

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(s -> runInBackground(() -> {
            Family family = new Family(s, selectedNames);
            families.put(s, family);
            registerRunningTask("calculateRelations");
            ListFilesa.calculateRelations(family, true);
            unRegisterRunningTask("calculateRelations");
            familiesModified.setValue(true);
        }, this::showFamilies));
    }

    private List<Name> getSelectedOrphanes() {

        return orphanTreeView.getSelectionModel().getSelectedItems().stream()
                .filter(t -> t.getValue().getStatus() != NodeStatus.FAMILY)
                .filter(t -> t.getParent().equals(orphanRootItem))
                .map(t -> t.getValue().toName())
                .collect(Collectors.toList());
    }

    public void newSeparateFamiliesButtonClick() {

        List<Name> selectedNames = getSelectedOrphanes();

        if (selectedNames.isEmpty()) {
            LOGGER.warn("Games are not selected");
            return;
        }

        runInBackground(() -> {
            for (Name selectedName : selectedNames) {
                String familyName = selectedName.getCleanName();
                if (families.containsKey(familyName)) {
                    familyName = selectedName.getName();
                }
                if (families.containsKey(familyName)) {
                    familyName = selectedName.getName() + " !";
                }
                ArrayList<Name> names = new ArrayList<>();
                names.add(selectedName);
                Family family = new Family(familyName, names);
                families.put(familyName, family);
                registerRunningTask("calculateRelations");
                ListFilesa.calculateRelations(family);
                unRegisterRunningTask("calculateRelations");
            }
            familiesModified.setValue(true);
        }, this::showFamilies);
    }

    public void kickAwayButtonClick() {

        Set<Family> modifiedFamilies = new HashSet<>();
        List<TreeItem<NameView>> selectedItems = new ArrayList<>(familyTreeView.getSelectionModel().getSelectedItems());

        selectedItems.stream()
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

        runInBackground(() -> {
            modifiedFamilies.forEach(ListFilesa::calculateRelations);

            familiesModified.setValue(true);

            families = families.entrySet().stream()
                    .filter(f -> !f.getValue().getMembers().isEmpty())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        }, this::showFamilies);
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

    public void findFamilyCandidatesButtonClick() {

        runInBackground(() -> {
        List<TreeItem<NameView>> selectedItems = new ArrayList<>(orphanTreeView.getSelectionModel().getSelectedItems());

        registerRunningTask("findFamilyCandidates");

        for (int i = 0; i < selectedItems.size(); i++) {

            if (needToStop[0]) {
                LOGGER.info("Execution was interrupted!");
                needToStop[0] = false;
                break;
            }

            NameView nameView = selectedItems.get(i).getValue();
            LOGGER.info("Finding family candidates for {}|{}", nameView.getName(), (i + 1) * 100.0 / selectedItems.size());

            List<NameView> items = ListFilesa.calculateRelations(nameView.getName()).entrySet().stream()
                    .map(e -> new NameView(e.getKey(), e.getValue())).collect(Collectors.toList());

            nameView.setItems(items);
            nameView.setJakkardStatus(items.get(0).getJakkardStatus());

            selectedItems.get(i).setValue(nameView);

            selectedItems.get(i).getChildren().setAll(items.stream().map(TreeItem::new).collect(Collectors.toList()));

        }

        unRegisterRunningTask("findFamilyCandidates");
        }, this::updateTrees);
    }

    public void addToThisFamilyClick() {

        runInBackground(() -> {
            List<TreeItem<NameView>> items = orphanTreeView.getSelectionModel().getSelectedItems().stream().filter(i -> NodeStatus.isFamily(i.getValue().getStatus())).collect(Collectors.toList());

            for (TreeItem<NameView> item : items) {

                if (needToStop[0]) {
                    LOGGER.info("Execution was interrupted!");
                    needToStop[0] = false;
                    break;
                }

                NameView nameView = item.getValue();

                Name parent = item.getParent().getValue().toName();
                Family family = families.get(nameView.getFamilyName());
                family.getMembers().add(parent);

                //TODO not optimal. Need calculation at the end
                ListFilesa.calculateRelations(family, true);
            }

            familiesModified.setValue(items.size() > 0);
        }, this::showFamilies);
    }

    public void findFamiliesAutoButtonClick() {

        runInBackground(() -> {
            registerRunningTask("findBestFamily");

            Set<Family> modified = new HashSet<>();
            Map<Name, Family> mapping = new LinkedHashMap<>();
            List<TreeItem<NameView>> selectedItems = new ArrayList<>(orphanTreeView.getSelectionModel().getSelectedItems());
            int size = selectedItems.size();
            boolean interrupted = false;

            for (int i = 0; i < size; i++) {

                if (needToStop[0]) {
                    LOGGER.info("Execution was interrupted!");
                    needToStop[0] = false;
                    interrupted = true;
                    break;
                }

                NameView nameView = selectedItems.get(i).getValue();
                LOGGER.info("Finding best family for {}|{}", nameView.getName(), (i + 1) * 100.0 / size);
                Map.Entry<Family, Double> entry = ListFilesa.calculateRelations(nameView.getName()).entrySet().iterator().next();

                if (entry.getValue() >= jakkard) {
                    mapping.put(nameView.toName(), entry.getKey());
                }
            }

            unRegisterRunningTask("findBestFamily");

            if (!interrupted) {

                LOGGER.info("Assigning to families...");
                mapping.entrySet().stream().sorted(Comparator.comparing(e -> e.getValue().getName())).forEach(e -> {
                    e.getValue().getMembers().add(e.getKey());
                    modified.add(e.getValue());
                });

                registerRunningTask("calculateRelations");

                int i = 0;
                List<Family> modifiedFamilies = modified.stream().sorted(Comparator.comparing(Family::getName)).collect(Collectors.toList());
                for (Family family : modifiedFamilies) {

                    if (needToStop[0]) {
                        LOGGER.info("Execution was interrupted!");
                        needToStop[0] = false;
                        break;
                    }

                    LOGGER.info("Calculating relations for {}|{}", family.getName(), (i + 1) * 100.0 / modified.size());
                    ListFilesa.calculateRelations(family, false);
                    i++;
                }
            }

            unRegisterRunningTask("calculateRelations");
            familiesModified.setValue(modified.size() > 0);
        }, () -> {
            orphanTreeView.getSelectionModel().clearSelection();
            showFamilies();
        });
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

        runInBackground(() -> {

            LOGGER.info("Generating family relations from scratch...");

            familyRelations = new LinkedHashMap<>();

            int i = 0;
            for (Map.Entry<String, Family> family : families.entrySet()) {

                if (needToStop[0]) {
                    LOGGER.info("Execution was interrupted!");
                    needToStop[0] = false;
                    break;
                }

                LOGGER.info("Calculating relations for {}|{}", family.getValue().getName(), (i + 1) * 100.0 / families.entrySet().size());

                familyRelations.put(family.getValue(), ListFilesa.calculateRelations(family.getValue().getMother().getName(), family.getKey(), false));
                i++;
            }

            familyRelationsModified.setValue(true);
        }, this::showFamilies);
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
            ListFilesa.calculateRelations(mainFamily, true);

            // recalculate relations for main family
            ListFilesa.calculateRelations(mainFamily.getMother().getName(), mainFamily.getName(), true);

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

                    if (familyRelations != null) {
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
                        familyRelationsModified.setValue(true);
                    }

                    familiesModified.setValue(true);

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

    private void loadFamilies() {
        //File familyFile = fullFamiliesPath().toFile();
        File familyFile = fullFamiliesJsonPath().toFile();

        if (familyFile.exists()) {
            //System.out.println(String.format("%nReading families from file %s...", familyFile));
            System.out.println(String.format("%nReading families from JSON file %s...", familyFile));
            //families = IOUtils.loadFamilies(familyFile);
            families = IOUtils.loadFamiliesAsJson(familyFile);
        } else {
            families = new HashMap<>();
        }

        orphanChildren.clear();

        showFamilies();
    }

    /*public void saveFamiliesButtonClick() {
        LOGGER.info("Saving families...");
        IOUtils.createDirectories(workFamiliesPath());
        stageManager.showWaitAlertAndRun("Saving families", () -> IOUtils.serialize(fullFamiliesPath().toFile(), families));
        familiesModified.setValue(false);
    }*/

    public void saveFamiliesButtonClick() {
        LOGGER.info("Saving families as JSON...");
        IOUtils.createDirectories(workFamiliesPath());
        stageManager.showWaitAlertAndRun("Saving families", () -> IOUtils.serializeFamiliesAsJson(fullFamiliesJsonPath().toFile(), families));
        familiesModified.setValue(false);
    }

    public void saveRelationsButtonClick() {
        LOGGER.info("Saving family relations...");
        IOUtils.createDirectories(workFamiliesPath());
        stageManager.showWaitAlertAndRun("Saving family relations", () -> IOUtils.serialize(fullFamilyRelationsPath().toFile(), familyRelations));
        familyRelationsModified.setValue(false);
    }

    @SuppressWarnings("unchecked")
    public void copyNameButtonClick(ActionEvent event) {

        TreeView<NameView> source = (TreeView<NameView>) ((MenuItem) event.getSource()).getParentPopup().getUserData();
        String name = source.getSelectionModel().getSelectedItem().getValue().getName();

        ClipboardContent content = new ClipboardContent();
        content.putString(name);
        Clipboard.getSystemClipboard().setContent(content);

        LOGGER.info("Saved {} to clipboard", name);
    }

    public void filterOrphanesTextFieldAction() {
        String newText = filterOrphanesTextField.getText().toLowerCase();
        if (!orphanFilter.equals(newText)) {
            orphanFilter = newText;
            showFamilies();
        }
    }
}
