package md.leonis.crawler.moby.controller;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Robot;
import com.sun.javafx.scene.control.skin.TableViewSkin;
import com.sun.javafx.scene.control.skin.VirtualFlow;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.stage.Stage;
import javafx.util.Pair;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import md.leonis.crawler.moby.config.ConfigHolder;
import md.leonis.crawler.moby.crawler.Crawler;
import md.leonis.crawler.moby.model.GameEntry;
import md.leonis.crawler.moby.model.jsdos.GameFileEntry;
import md.leonis.crawler.moby.view.FxmlView;
import md.leonis.crawler.moby.view.StageManager;
import md.leonis.shingler.model.dto.TiviStructure;
import md.leonis.shingler.utils.FileUtils;
import md.leonis.shingler.utils.StringUtils;
import md.leonis.shingler.utils.WebUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static md.leonis.crawler.moby.config.ConfigHolder.*;
import static md.leonis.shingler.utils.StringUtils.*;
import static md.leonis.shingler.utils.TiviApiUtils.loadTiviGames;

@Controller
public class GamesBindingController {

    final DataFormat dataFormat = new DataFormat("title");

    private final StageManager stageManager;
    private final ConfigHolder configHolder;

    public Button closeButton;
    public Button saveButton;
    public Button rollbackButton;
    public Button autoAssignButton;
    public CheckBox showGreenCheckBox;

    public Label infoLabel;

    public TableView<Cont> tableView;
    public TableColumn<Cont, String> idTableColumn;
    public TableColumn<Cont, String> familyTableColumn;
    public TableColumn<Cont, String> tiviTableColumn;
    public TableColumn<Cont, String> sourceTableColumn;
    public TableColumn<Cont, Cont> plusButtonTableColumn;
    public TableColumn<Cont, Cont> deleteButtonTableColumn;

    public ListView<Structure> listView;
    public TextField searchTextField;
    public Button serviceButton;

    List<Binding> gamesBinding;
    List<GameEntry> gameEntries = new ArrayList<>();

    Deque<Cont> rollbackList = new ArrayDeque<>();

    List<Structure> fullMoby = new ArrayList<>();

    ObservableList<Cont> observableList = FXCollections.observableArrayList();
    FilteredList<Cont> filteredList = new FilteredList<>(observableList);

    FilteredList<Structure> listViewData;

    Crawler crawler;

    private Cont activeCont = null;

    Robot robot;

    private Stage stage;

    @Lazy
    public GamesBindingController(StageManager stageManager, ConfigHolder configHolder) {
        this.stageManager = stageManager;
        this.configHolder = configHolder;
    }

    @FXML
    private void initialize() throws Exception {
        robot = Application.GetApplication().createRobot();

        idTableColumn.setCellValueFactory(v -> new ReadOnlyStringWrapper(org.apache.commons.lang3.StringUtils.right("000" + v.getValue().getId(), 4)));
        familyTableColumn.setCellValueFactory(v -> new ReadOnlyStringWrapper(v.getValue().getFamily()));
        tiviTableColumn.setCellValueFactory(v -> new ReadOnlyStringWrapper(v == null || v.getValue().getTivi() == null ? "" : v.getValue().getTivi().getUnmodifiedTitle()));
        sourceTableColumn.setCellValueFactory(v -> new ReadOnlyStringWrapper(v == null || v.getValue().getMoby() == null ? "" : v.getValue().getMoby().getUnmodifiedTitle()));

        plusButtonTableColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        plusButtonTableColumn.setCellFactory(param -> new TableCell<Cont, Cont>() {
            private final Button okButton = new Button("+");

            @Override
            protected void updateItem(Cont cont, boolean empty) {
                super.updateItem(cont, empty);

                if (cont == null) {
                    setGraphic(null);
                    return;
                }

                setGraphic(okButton);
                okButton.setOnAction(
                        event -> {
                            int index = observableList.indexOf(cont);
                            observableList.add(index, new Cont(cont.getId(), cont.getFamily(), cont.getTivi(), cont.getMoby(), cont.isGreen()));
                        }
                );
            }
        });

        deleteButtonTableColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        deleteButtonTableColumn.setCellFactory(param -> new TableCell<Cont, Cont>() {
            private final Button deleteButton = new Button("x");

            @Override
            protected void updateItem(Cont cont, boolean empty) {
                super.updateItem(cont, empty);

                if (cont == null) {
                    setGraphic(null);
                    return;
                }

                setGraphic(deleteButton);
                deleteButton.setOnAction(event -> rollbackOrDelete(cont));
            }
        });

        tableView.setRowFactory(tableView -> {
            final TableRow<Cont> row = new TableRow<Cont>() {

                @Override
                protected void updateItem(Cont item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null && item.isGreen()) {
                        setStyle("-fx-background-color: #ccffcc;");
                    } else {
                        setStyle("");
                    }
                }
            };

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Cont cont = row.getItem();
                    // открывать страницы параллельно
                    if (cont.getTivi() != null) {
                        new Thread(() -> WebUtils.openWebPage(String.format("%sgame/%s/%s.html", sitePath, cont.getTivi().platformId, cont.getTivi().gameId))).start();
                    }
                    if (cont.getMoby() != null) {
                        new Thread(() -> WebUtils.openWebPage(crawler.getGamePage(cont.getMoby().platformId, cont.getMoby().gameId))).start();
                    }
                }
            });

            row.setOnMouseMoved(event -> {
                if (event.isControlDown() && stage == null && activeCont != null) {
                    tiviStructure = activeCont.getTivi();
                    mobyStructure = activeCont.getMoby();

                    stage = stageManager.showFloatWindow(FxmlView.GAMES_INFO, robot.getMouseX(), robot.getMouseY());
                }
                if (!event.isControlDown() && stage != null) {
                    stage.close();
                    stage = null;
                }
            });

            row.setOnMouseExited(event -> {
                //controlIsDown = false;
            });

            row.hoverProperty().addListener((observable) -> {
                activeCont = row.getItem();
                if (row.isHover() && activeCont != null) {
                    String tivi = activeCont.getTivi() == null ? "" : activeCont.getTivi().platformId + "::" + activeCont.getTivi().getGameId() + "::" + activeCont.getTivi().getYear();
                    String moby = activeCont.getMoby() == null ? "" : activeCont.getMoby().platformId + "::" + activeCont.getMoby().getGameId() + "::" + activeCont.getMoby().getYear();
                    infoLabel.setText(tivi + "   " + moby);
                } else {
                    infoLabel.setText("");
                }
            });

            row.setOnDragOver(event -> {
                // data is dragged over the target
                if (event.getDragboard().hasContent(dataFormat)) {
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                }
                event.consume();
            });

            row.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (event.getDragboard().hasContent(dataFormat)) {

                    Cont dstCont = row.getItem();
                    Cont srcCont = (Cont) db.getContent(dataFormat);
                    // copy, delete
                    if (dstCont.getTivi() == null && srcCont.getTivi() != null) {
                        dstCont.setTivi(srcCont.getTivi());
                        dstCont.setGreen(true);
                        observableList.removeIf(c -> c.getTivi() == null && c.getMoby().getGameId().equals(srcCont.getMoby().getGameId()));
                        rollbackButton.setVisible(true);
                        rollbackList.add(dstCont);
                        updateFamilies(dstCont);
                    } else if (dstCont.getMoby() == null && srcCont.getMoby() != null) {
                        dstCont.setMoby(srcCont.getMoby());
                        dstCont.setGreen(true);
                        observableList.removeIf(c -> c.getTivi() == null && c.getMoby().getGameId().equals(srcCont.getMoby().getGameId()));
                        rollbackButton.setVisible(true);
                        rollbackList.add(dstCont);
                        updateFamilies(dstCont);
                    } else {
                        System.out.printf("Can't copy %s to %s!%n", srcCont, dstCont);
                    }
                    tableView.refresh();
                    success = true;
                }
                event.setDropCompleted(success);
                event.consume();
            });

            return row;
        });

        tableView.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
            if (event.isControlDown() && stage == null && activeCont != null) {
                tiviStructure = activeCont.getTivi();
                mobyStructure = activeCont.getMoby();

                stage = stageManager.showFloatWindow(FxmlView.GAMES_INFO, robot.getMouseX(), robot.getMouseY());
                event.consume();
            }
        });

        tableView.addEventFilter(KeyEvent.KEY_RELEASED, (KeyEvent event) -> {
            if (stage != null) {
                stage.close();
                stage = null;
            }
            if (!event.getText().isEmpty()) {
                for (int i = 0; i < tableView.getItems().size(); i++) {
                    if (tableView.getItems().get(i).getTivi() != null && tableView.getItems().get(i).getTivi().getUnmodifiedTitle().toLowerCase().startsWith(event.getText())) {
                        tableView.getSelectionModel().select(i);
                        ((VirtualFlow<?>) ((TableViewSkin<?>) tableView.getSkin()).getChildren().get(1)).show(i);
                        break;
                    }
                }
                event.consume();
            }
        });

        listView.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
            if (event.isControlDown() && stage == null && activeCont != null) {
                tiviStructure = activeCont.getTivi();
                mobyStructure = activeCont.getMoby();

                stage = stageManager.showFloatWindow(FxmlView.GAMES_INFO, robot.getMouseX(), robot.getMouseY());
            }
            event.consume();
        });

        listView.addEventFilter(KeyEvent.KEY_RELEASED, (KeyEvent event) -> {
            if (stage != null) {
                stage.close();
                stage = null;
            }
            event.consume();
        });

        //drag
        tableView.setOnDragDetected(event -> {
            // drag was detected, start drag-and-drop gesture
            Cont selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {

                Dragboard db = tableView.startDragAndDrop(TransferMode.ANY);
                ClipboardContent content = new ClipboardContent();
                content.put(dataFormat, selected);
                db.setContent(content);
                event.consume();
            }
        });

        //drag
        listView.setOnDragDetected(event -> {
            // drag was detected, start drag-and-drop gesture
            Structure selected = listView.getSelectionModel().getSelectedItem();

            if (selected != null) {
                Cont cont = new Cont(0, null, null, selected, false);
                Dragboard db = listView.startDragAndDrop(TransferMode.ANY);
                ClipboardContent content = new ClipboardContent();
                content.put(dataFormat, cont);
                db.setContent(content);
                event.consume();
            }
        });

        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldCont, newCont) -> {
            if (newCont != null) {
                if (newCont.getMoby() == null /*|| newCont.getTivi() == null*/) {
                    //searchTextField.setText("");
                    setListBoxItems(newCont);
                } else {
                    listView.setItems(FXCollections.emptyObservableList());
                    //searchTextField.setText("");
                }
            }
        });

        listView.setCellFactory(lv -> {
            final ListCell<Structure> cell = new ListCell<Structure>() {
                @Override
                public void updateItem(Structure item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(item.getUnmodifiedTitle());
                        setOnMouseClicked(mouseClickedEvent -> {
                            if (mouseClickedEvent.getButton().equals(MouseButton.PRIMARY) && mouseClickedEvent.getClickCount() == 2) {
                                //show page
                                Structure selected = listView.getSelectionModel().getSelectedItem();
                                if (selected != null) {
                                    new Thread(() -> WebUtils.openWebPage(crawler.getGamePage(selected.getPlatformId(), selected.getGameId()))).start();
                                }
                            }
                        });
                    }
                }
            };

            cell.setOnMouseEntered(event -> activeCont = new Cont(0, null, null, cell.getItem(), false));

            cell.setOnMouseExited(event -> activeCont = null);

            return cell;
        });

        searchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (listViewData != null) {
                listViewData.setPredicate(structure -> {
                    if (newValue == null || newValue.isEmpty()) {
                        return true;
                    }
                    return structure.getTitle().contains(newValue.toLowerCase());
                });
            }
        });

        // read moby, tivi
        crawler = getCrawler();

        List<TiviStructure> tiviList = platformsBindingMapEntries.stream().flatMap(e -> {
            List<TiviStructure> data = new ArrayList<>();
            try {
                data = loadTiviGames(e.getKey(), false);
            } catch (Exception ex) {
                stageManager.showErrorAlert("Error loading Tivi Games", ex.getClass().getSimpleName() + ": " + ex.getMessage(), ex);
            }
            return data.stream().filter(t -> !t.getSid().equals("hak")).sorted(Comparator.comparing(TiviStructure::getName));
        }).collect(Collectors.toList());

        // zamechanie-po-baze-dannyh
        // Замечание по базе данных

        for (String p : platformsBindingMapEntries.stream().flatMap(e -> e.getValue().stream()).collect(Collectors.toList())) {
            gameEntries.addAll(crawler.loadGamesList(p));
        }

        //List<Structure> tivi = tiviList.stream().map(Structure::new).collect(Collectors.toList());
        List<Structure> moby = gameEntries.stream().map(g -> new Structure(g, crawler)).collect(Collectors.toList());
        fullMoby = new ArrayList<>(moby);
        gameEntries.forEach(g -> g.getAlternateTitles().forEach(a -> fullMoby.add(new Structure(g, a, crawler))));

        //Map<String, Structure> tiviMap = new TreeMap<>(tivi.stream().collect(Collectors.toMap(Structure::getGameId, Function.identity())));
        Set<Structure> mobyMap = new HashSet<>(moby);

        //загрузить сопоставление если есть
        gamesBinding = new ArrayList<>();
        for (Map.Entry<String, List<String>> e : platformsBindingMapEntries) {
            for (String mobyPlatformId : e.getValue()) {
                Map<String, List<String>> map = crawler.loadGamesBindingMap(e.getKey(), mobyPlatformId);
                map.forEach((key, value) -> gamesBinding.add(new Binding(e.getKey(), key, mobyPlatformId, value)));
            }
        }
        Set<Pair<String, String>> deleteMoby = gamesBinding.stream()
                .flatMap(g -> g.getMobyGameIds().stream().map(m -> new Pair<>(g.getMobyPlatformId(), m))).collect(Collectors.toSet());

        // остальные
        Collection<Structure> all = tiviList.stream().map(Structure::new).collect(Collectors.toList());
        all.addAll(mobyMap);

        tableView.setItems(filteredList);

        all.stream().sorted(Comparator.comparing(Structure::getTitle)).forEach(s -> {
            if (s.isLeft()) {
                Binding binding = gamesBinding.stream().filter(g -> g.getPlatformId().equals(s.getPlatformId()) && g.getGameId().equals(s.getGameId())).findFirst().orElse(null);
                if (binding != null) {
                    for (String id : binding.getMobyGameIds()) {
                        observableList.add(new Cont(observableList.size(), s.getNeutralFamily(), s, mobyMap.stream().filter(g -> g.getPlatformId().equals(binding.mobyPlatformId) && g.getGameId().equals(id)).findFirst().get(), true));
                    }
                } else {
                    observableList.add(new Cont(observableList.size(), s.getNeutralFamily(), s, null, false));
                }
            } else {
                if (deleteMoby.stream().noneMatch(m -> m.equals(new Pair<>(s.platformId, s.gameId)))) {
                    observableList.add(new Cont(observableList.size(), "", null, s, false));
                }
            }
        });
    }

    // какие-то ручные запросы
    public void serviceButtonClick() throws Exception {
        List<TiviStructure> data = loadTiviGames("dos", false);
        Set<String> titles = data.stream().map(TiviStructure::getName).collect(Collectors.toSet());
        Set<String> cpus = data.stream().map(TiviStructure::getCpu).collect(Collectors.toSet());

        List<GameEntry> gameEntries = crawler.loadGamesList("dos");
        Map<String, GameEntry> map = gameEntries.stream().collect(Collectors.toMap(GameEntry::getGameId, Function.identity()));

        List<String> lines = new ArrayList<>();

        final int[] nextIndex = {data.stream().mapToInt(TiviStructure::getId).max().orElseThrow(() -> new RuntimeException("Igor to net!"))};

        String sys = "dos";
        long created = System.currentTimeMillis() / 1000;

        observableList.stream().filter(c -> c.getMoby() != null && !c.isGreen()).map(Cont::getMoby).forEach(structure -> {
            //System.out.println(structure.getTitle() + ": " + structure.getGameId());

            String title = structure.getUnmodifiedTitle();
            String year = structure.getYear().isEmpty() ? "Alt" : structure.getYear();
            //A, The
            if (title.startsWith("A ")) {
                title = title.substring(2) + ", A";
            }
            if (title.startsWith("The ")) {
                title = title.substring(4) + ", The";
            }
            title = getAddon(title, year, "%s (%s)", titles);

            String cpu = StringUtils.cpu(title);
            cpu = getAddon(cpu, year, "%s-%s", cpus).toLowerCase();

            String finalTitle = title.replace("'", "''");
            String finalCpu = cpu;
            optionalGameFileEntry(map.get(structure.getGameId())).ifPresent(file -> {
                String game = file.getHost() + file.getUrl();
                String query = String.format(insert, sys,
                        ++nextIndex[0], sys, created, created, StringUtils.sid(finalTitle), finalCpu, finalTitle, "", "", // n, sys, created, modified, sid, cpu, name, descript, keywords
                        "", "", "", "", "", "", "", "", // region, publisher, developer, god, god1, ngamers, type, genre
                        "", "", "", "", "", "", "", // image1-7
                        "", "", "", "", "", "", "", // image8-14
                        game, 0, "", 0, game, "yes", 0, "", "", // game, downloaded, music, music_downloaded, rom, playable, played, text1, text2
                        "", "", "", "", 0, 0, 0, 0, 0 // analog, drname, cros, serie, rating, userrating, totalrating, viewes, comments
                );
                System.out.println(query);
                lines.add(query);
            });
        });

        //add game, rom, playable
        observableList.stream().filter(Cont::isGreen).forEach(cont ->
                optionalGameFileEntry(map.get(cont.getMoby().getGameId())).ifPresent(file -> {
                    String game = file.getHost() + file.getUrl();
                    String query = String.format(update, sys, game, game, cont.getTivi().getGameId());
                    System.out.println(query);
                    lines.add(query);
                }));

        //cpus: _ -> -
        observableList.stream().map(Cont::getTivi).filter(Objects::nonNull).forEach(structure -> {
            if (structure.gameId.contains("_")) {
                String query = String.format(updateCpu, sys, StringUtils.cpu(structure.gameId), structure.gameId);
                System.out.println(query);
                lines.add(query);
            }
        });

        FileUtils.saveToFile(getSourceDir(getSource()).resolve("insert.sql"), lines);
        System.out.println("gata");
    }

    private Optional<GameFileEntry> optionalGameFileEntry(GameEntry gameEntry) {
        return gameEntry.getFiles().stream().filter(f -> f.getUrl().endsWith(".jsdos")).findFirst();
    }

    String updateCpu = "UPDATE `base_%s` SET cpu='%s' WHERE cpu='%s';";

    String update = "UPDATE `base_%s` SET game='%s', rom='%s', playable='yes' WHERE cpu='%s';";

    String insert = "INSERT INTO `base_%s` VALUES " +
            // n, sys, created, modified, sid, cpu, name, descript, keywords
            // 1, '3do', 0, 1640065258, '1', 'zamechanie-po-baze-dannyh', 'Замечание по базе данных', '', ''
            "(%s, '%s', %s, %s, '%s', '%s', '%s', '%s', '%s', " +
            // region, publisher, developer, god, god1, ngamers, type, genre
            // 'EU;', 'Leonis', '', '2012', '0303', NULL, '', ''
            "'%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', " +
            // image1 - image14
            "'%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', '%s', " +
            // game, downloaded, music, music_downloaded, rom, playable, played, text1, text2
            // '', 0, '', 0, '', 'no', 0, 'Стро...', ''
            "'%s', %s, '%s', %s, '%s', '%s', %s, '%s', '%s', " +
            // analog, drname, cros, serie, rating, userrating, totalrating, viewes, comments
            // '', '', '', '', 53, 93, 224, 15246, 1
            "'%s', '%s', '%s', '%s', %s, %s, %s, %s, %s);";

    String rep = " 23456789";

    private String getAddon(String string, String key, String format, Set<String> strings) {
        if (!strings.contains(string)) {
            return string;
        }
        for (int i = 0; i < rep.length(); i++) {
            String addon = (i == 0 && !key.isEmpty()) ? key : "Alt" + rep.charAt(i);
            String newString = String.format(format, string, addon);
            if (!strings.contains(newString)) {
                strings.add(newString);
                return newString;
            }
        }
        throw new RuntimeException("How???");
    }

    public void autoAssignButtonClick() {
        // одинаковые тоже в верх
        // если два кандидата, то не мэппить.
        for (Cont cont : new ArrayList<>(observableList)) {
            if (cont.getTivi() == null) {
                continue;
            }
            Structure tiviStructure = cont.getTivi();
            List<Cont> tiviMatches = observableList.stream()
                    .filter(c -> c.getTivi() != null).filter(s -> s.getTivi().getTitle().equals(tiviStructure.getTitle())).collect(Collectors.toList());
            List<Cont> mobyMatches = observableList.stream()
                    .filter(c -> c.getMoby() != null).filter(s -> s.getMoby().getTitle().equals(tiviStructure.getTitle())).collect(Collectors.toList());
            if (tiviMatches.size() == 1 && mobyMatches.size() == 1) {
                Cont mobyStructure = mobyMatches.get(0);
                cont.setMoby(mobyStructure.getMoby());
                cont.setGreen(true);
                // remove moby
                observableList.remove(mobyStructure);
            }
        }

        // семьи
        for (Cont cont : observableList) {
            if (cont.getTivi() != null && cont.getMoby() != null) {
                updateFamilies(cont);
            }
        }

        tableView.refresh();
    }

    private void updateFamilies(Cont cont) {

        String family = cont.getTivi().getFamily();

        if (family.trim().isEmpty()) {
            return;
        }

        // семья не должна начинаться "Multicarts, Public_Domain, в ней не должно быть более
        if (family.contains("Multicarts") || family.contains("Public_Domain")) {
            return;
        }

        String platformId = cont.getTivi().getPlatformId();

        long familySize = observableList.stream().filter(i -> i.getTivi() != null && i.getTivi().getFamily().equals(family) && platformId.equals(i.getTivi().getPlatformId())).count();
        if (familySize > 10) {
            return;
        }

        for (Cont nextCont : observableList) {
            if (cont.getTivi().equals(nextCont.getTivi())) {
                continue;
            }

            if (nextCont.getMoby() == null && nextCont.getTivi() != null && family.equals(nextCont.getTivi().getFamily()) && platformId.equals(nextCont.getTivi().getPlatformId())) {
                nextCont.setMoby(cont.getMoby());
                nextCont.setGreen(true);
                rollbackList.add(nextCont);
            }
        }
    }

    private void setListBoxItems(Cont cont) {

        Structure structure = cont.getTivi() == null ? cont.getMoby() : cont.getTivi();

        if (structure != null) {
            List<Structure> list = fullMoby.stream().filter(p -> p.getTitle().equals(structure.getTitle())).collect(Collectors.toList());
            list.add(new Structure("", "", "", "-------------", "", "", new ArrayList<>(), new ArrayList<>(), false));

            List<Structure> gamesList = fullMoby.stream().map(p -> new Pair<>(new LevenshteinDistance().apply(structure.getTitle(), p.getTitle()), p))
                    .sorted(Comparator.comparing(Pair::getKey)).filter(p -> p.getKey() <= 5).map(Pair::getValue).collect(Collectors.toList());

            list.addAll(gamesList);
            list.add(new Structure("", "", "", "-------------", "", "", new ArrayList<>(), new ArrayList<>(), false));
            list.addAll(fullMoby.stream().sorted(Comparator.comparing(Structure::getUnmodifiedTitle)).collect(Collectors.toList()));

            listViewData = new FilteredList<>(FXCollections.observableList(list));
            listView.setItems(FXCollections.emptyObservableList());
            listView.setItems(listViewData);
        }
    }

    public void closeButtonClick() {
        saveBindings();
        stageManager.showPane(FxmlView.PLATFORMS);
        //((Stage) closeButton.getScene().getWindow()).close();
    }

    public void saveButtonClick() {
        saveBindings();
    }

    private void saveBindings() {

        try {
            List<Cont> conts = observableList.stream().filter(Cont::isGreen).collect(Collectors.toList());
            gamesBinding = new ArrayList<>();
            conts.forEach(cont -> {
                String gameId = cont.getTivi().getGameId();
                String platformId = cont.getTivi().getPlatformId();
                String mobyGameId = cont.getMoby().getGameId();
                String mobyPlatformId = cont.getMoby().getPlatformId();
                gamesBinding.add(new Binding(platformId, gameId, mobyPlatformId, new ArrayList<>(Collections.singletonList(mobyGameId))));
            });

            Map<String, List<Binding>> bindingMap = gamesBinding.stream().distinct().collect(Collectors.groupingBy(Binding::getPlatformId)); // by platformId
            for (Map.Entry<String, List<Binding>> e : bindingMap.entrySet()) {
                String gameId = e.getKey();
                Map<String, List<Binding>> mobyBindingMap = e.getValue().stream().collect(Collectors.groupingBy(Binding::getMobyPlatformId)); // by mobyPlatformId
                for (Map.Entry<String, List<Binding>> entry : mobyBindingMap.entrySet()) {
                    String mobyGameId = entry.getKey();
                    Map<String, List<String>> result = entry.getValue().stream().collect(Collectors.groupingBy(Binding::getGameId))
                            .entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, el -> el.getValue().stream()
                                    .flatMap(b -> b.getMobyGameIds().stream()).distinct().collect(Collectors.toList())));

                    crawler.saveGamesBindingMap(gameId, mobyGameId, new TreeMap<>(result));
                }
            }

        } catch (Exception e) {
            stageManager.showErrorAlert("Error saving gamesBinding", e.getClass().getSimpleName() + e.getMessage(), e);
        }
    }

    public void rollbackButtonClick() {
        if (rollbackList.isEmpty()) {
            return;
        }
        rollbackOrDelete(rollbackList.pollLast());
        rollbackButton.setVisible(!rollbackList.isEmpty());
    }

    private void rollbackOrDelete(Cont cont) {
        int index = observableList.indexOf(cont);

        if (index >= 0) {
            if (cont.getMoby() != null) {
                Cont newCont = new Cont(tableView.getSelectionModel().getSelectedIndex() + 1, "", null, cont.getMoby(), false);
                observableList.add(index + 1, newCont);
                cont.setGreen(false);
                cont.setMoby(null);
            } else {
                observableList.remove(cont);
            }
        }
    }

    public void showGreenCheckBoxAction() {
        Cont cont = tableView.getSelectionModel().getSelectedItem();
        int index = tableView.getSelectionModel().getSelectedIndex();
        TableViewSkin<?> ts = (TableViewSkin<?>) tableView.getSkin();
        VirtualFlow<?> vf = (VirtualFlow<?>) ts.getChildren().get(1);

        if (cont == null) {
            int first = vf.getFirstVisibleCellWithinViewPort().getIndex();
            int last = vf.getLastVisibleCellWithinViewPort().getIndex();
            index = (last + first) / 2;
        }

        for (int i = index; i < tableView.getItems().size(); i++) {
            if (!tableView.getItems().get(i).isGreen()) {
                cont = tableView.getItems().get(i);
                break;
            }
        }

        filteredList.setPredicate(t -> !t.isGreen() || showGreenCheckBox.isSelected());
        tableView.refresh();
        if (cont != null) {
            tableView.getSelectionModel().select(cont);
            Platform.runLater(() -> vf.show(tableView.getSelectionModel().getSelectedIndex()));
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Binding implements Serializable {
        private String platformId;
        private String gameId;
        private String mobyPlatformId;
        private List<String> mobyGameIds;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Cont implements Serializable {
        private Integer id;
        private String family;
        private Structure tivi;
        private Structure moby;
        private boolean green;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Structure implements Serializable {

        private String platformId;
        private String gameId;
        private String unmodifiedTitle;
        private String title;
        private String family;
        private String year;
        private List<String> alternativeTitles = new ArrayList<>();
        private List<String> images = new ArrayList<>();

        private boolean left;

        public Structure(TiviStructure structure) {
            this.platformId = structure.getSys();
            this.gameId = structure.getCpu();
            this.unmodifiedTitle = unescapeChars(structure.getName());
            this.title = normalize(unmodifiedTitle);
            this.family = structure.getGame();
            this.left = true;
            this.year = structure.getGod1() + structure.getGod();
            this.alternativeTitles = structure.getDrname() == null ? new ArrayList<>() : Arrays.asList(structure.getDrname().split(";"));
            this.images = structure.getImages().stream().map(s -> localSitePath + "images/" + platformId + "/i/" + structure.getSid() + "/" + s).collect(Collectors.toList());
            //System.out.println(this.images);
        }

        public Structure(GameEntry gameEntry, Crawler crawler) {
            this.platformId = gameEntry.getPlatformId();
            this.gameId = gameEntry.getGameId();
            this.unmodifiedTitle = gameEntry.getTitle();
            this.title = normalize(unmodifiedTitle);
            this.family = gameId;
            this.left = false;
            this.year = String.join(", ", gameEntry.getDates());
            this.alternativeTitles = gameEntry.getAlternateTitles().stream().map(a -> a.split("\"")[1]).collect(Collectors.toList());
            this.images = gameEntry.getScreens().stream().map(s -> crawler.getFilePath(gameEntry.getPlatformId(), s.getHost(), s.getLarge()).toAbsolutePath().toString()).collect(Collectors.toList());
        }

        // "alternateTitles" : [ "\"Alien 3\" -- Alternative spelling" ],
        public Structure(GameEntry gameEntry, String altName, Crawler crawler) {
            this.platformId = gameEntry.getPlatformId();
            this.gameId = gameEntry.getGameId();
            this.unmodifiedTitle = altName.split("\"")[1];
            this.title = normalize(unmodifiedTitle);
            this.unmodifiedTitle = this.unmodifiedTitle + " *";
            this.family = gameId;
            this.left = false;
            this.year = String.join(", ", gameEntry.getDates());
            this.alternativeTitles = gameEntry.getAlternateTitles().stream().map(a -> a.split("\"")[1]).collect(Collectors.toList());
            this.images = gameEntry.getScreens().stream().map(s -> crawler.getFilePath(gameEntry.getPlatformId(), s.getHost(), s.getLarge()).toAbsolutePath().toString()).collect(Collectors.toList());
        }

        private String normalize(String s) {
            s = s.trim();
            if (s.isEmpty()) {
                return "??????????";
            }
            s = unescapeChars(s);
            s = replaceColon(s);
            s = removeThe(s);
            s = toChunks(s).get(0);
            s = removeFront(s, "Disney ");
            s = removeFront(s, "Disney's ");
            s = removeSpecialChars(s);
            s = s.replace("-", " ");
            s = s.replace("-", " ");
            s = s.replace("  ", " ").replace("  ", " ").replace("  ", " ").trim();
            s = s.replace("²", " 2").replace("³", " 3").replace("é", "e").replace("ü", "u").replace("ū", "uu")
                    .replace("ō", "ou").replace("ä", "a");

            return s.toLowerCase();
        }

        public String getNeutralFamily() {
            String[] chunks = family.split("/");
            //System.out.println(Arrays.asList(chunks));
            return StringUtils.stripArchiveExtension(chunks[chunks.length - 1]);
        }

        @Override
        public String toString() {
            return unmodifiedTitle;
        }
    }
}
