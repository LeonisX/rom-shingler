package md.leonis.shingler.gui.controls;

import com.sun.javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;

// https://tech.chitgoks.com/2013/07/19/how-to-go-to-item-in-combobox-on-keypress-in-java-fx-2/
class SearchComboBox<T> extends ComboBox<T> {

    private static final int IDLE_INTERVAL_MILLIS = 1000;

    private Instant instant = Instant.now();
    private StringBuilder sb = new StringBuilder();

    public SearchComboBox(Collection<T> choices) {
        this(FXCollections.observableArrayList(choices));
    }

    public SearchComboBox(final ObservableList<T> items) {
        this();
        setItems(items);
        getSelectionModel().selectFirst();
    }

    public SearchComboBox() {
        super();

        this.addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == KeyCode.ESCAPE && sb.length() > 0) {
                resetSearch();
            }
        });

        this.setOnKeyReleased(event -> {

                    if (Duration.between(instant, Instant.now()).toMillis() > IDLE_INTERVAL_MILLIS) {
                        resetSearch();
                    }

                    instant = Instant.now();

                    if (event.getCode() == KeyCode.DOWN || event.getCode() == KeyCode.UP || event.getCode() == KeyCode.TAB) {
                        return;
                    } else if (event.getCode() == KeyCode.BACK_SPACE && sb.length() > 0) {
                        sb.deleteCharAt(sb.length() - 1);
                    } else {
                        sb.append(event.getText().toLowerCase());
                    }

                    if (sb.length() == 0) {
                        return;
                    }

                    boolean found = false;
                    for (int i = 0; i < getItems().size(); i++) {
                        if (event.getCode() != KeyCode.BACK_SPACE && getItems().get(i).toString().toLowerCase().startsWith(sb.toString())) {
                            ListView listView = getListView();
                            listView.getSelectionModel().clearAndSelect(i);
                            scroll();
                            found = true;
                            break;
                        }
                    }

                    if (!found && sb.length() > 0)
                        sb.deleteCharAt(sb.length() - 1);
                }
        );

        // add a focus listener such that if not in focus, reset the search process
        this.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                resetSearch();
            } else {
                scroll();
            }
        });
    }

    private void resetSearch() {
        sb.setLength(0);
        instant = Instant.now();
    }

    private void scroll() {
        ListView listView = getListView();
        int selectedIndex = listView.getSelectionModel().getSelectedIndex();
        listView.scrollTo(selectedIndex == 0 ? selectedIndex : selectedIndex - 1);
    }

    private ListView getListView() {
        return ((ComboBoxListViewSkin) this.getSkin()).getListView();
    }
}
