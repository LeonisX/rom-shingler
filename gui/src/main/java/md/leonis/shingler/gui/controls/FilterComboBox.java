package md.leonis.shingler.gui.controls;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.input.KeyEvent;

import java.util.Collection;
import java.util.List;

// https://stackoverflow.com/questions/13362607/combobox-jump-to-typed-char
class FilterComboBox<T> extends ComboBox<T> {

    private final FilterComboBox<T> comboBox = this;
    private ObservableList<T> items;
    private ObservableList<T> filteredItems;
    private String searchString;
    private T selection;

    /*private class KeyHandler implements EventHandler<KeyEvent> {

        private SingleSelectionModel<T> selectionModel;

        KeyHandler() {
            selectionModel = getSelectionModel();
            searchString = "";
        }

        @Override
        public void handle(KeyEvent event) {

            if (event.getCode() == KeyCode.BACK_SPACE && searchString.length() > 0) {
                searchString = searchString.substring(0, searchString.length() - 1);
            } else {
                searchString += event.getText().toUpperCase();
            }

            if (searchString.length() == 0) {
                fcbo.setItems(items);
                selectionModel.selectFirst();
                return;
            }

            if (event.getCode().isLetterKey() || event.getCode().isDigitKey()) {
                filteredItems.setAll(items.stream().filter(item -> item.toString().toUpperCase().startsWith(searchString)).collect(Collectors.toList()));
                fcbo.setItems(filteredItems);
                selectionModel.selectFirst();
            }
        }
    }*/

    private class KeyHandler implements EventHandler<KeyEvent> {

        private final FilterComboBox<T> comboBox;

        public KeyHandler(FilterComboBox<T> comboBox) {
            this.comboBox = comboBox;
        }

        @Override
        public void handle(KeyEvent event) {
            T s = jumpTo(event.getText(), comboBox.getValue(), comboBox.getItems());
            if (s != null) {
                comboBox.setValue(s);
            }
        }
    }

    private T jumpTo(String keyPressed, T currentlySelected, List<T> items) {
        String key = keyPressed.toUpperCase();
        if (key.matches("^[A-Z0-9]$")) {
            // Only act on letters so that navigating with cursor keys does not
            // try to jump somewhere.
            boolean letterFound = false;
            boolean foundCurrent = currentlySelected == null;
            for (T s : items) {
                if (s.toString().toUpperCase().startsWith(key)) {
                    letterFound = true;
                    if (foundCurrent) {
                        return s;
                    }
                    foundCurrent = s.equals(currentlySelected);
                    this.getSelectionModel().select(s);
                }
            }
            if (letterFound) {
                return jumpTo(keyPressed, null, items);
            }
        }
        return null;
    }

    FilterComboBox(Collection<T> choices) {
        this(FXCollections.observableArrayList(choices));
    }

    private FilterComboBox(final ObservableList<T> items) {
        super(items);
        this.items = items;
        this.filteredItems = FXCollections.observableArrayList();

        setOnKeyReleased(new KeyHandler(this));

        this.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == Boolean.FALSE) {
                searchString = "";
                comboBox.setItems(items);
                comboBox.getSelectionModel().select(selection);
            }

        });

        this.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                selection = newValue;
            }
        });
    }
}
