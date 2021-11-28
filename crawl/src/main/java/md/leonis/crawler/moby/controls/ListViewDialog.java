package md.leonis.crawler.moby.controls;

import com.sun.javafx.scene.control.skin.resources.ControlResources;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class ListViewDialog<T> extends Dialog<T> {

    /**************************************************************************
     *
     * Fields
     *
     **************************************************************************/

    private final GridPane grid;
    private final Label label;
    private final ListView<T> list;
    private final T defaultChoice;

    public ListViewDialog() {
        this((T)null, (T[])null);
    }

    public ListViewDialog(T defaultChoice, T... choices) {
        this(defaultChoice,
                choices == null ? Collections.emptyList() : Arrays.asList(choices));
    }

    public ListViewDialog(T defaultChoice, Collection<T> choices) {
        final DialogPane dialogPane = getDialogPane();

        // -- grid
        this.grid = new GridPane();
        this.grid.setHgap(10);
        this.grid.setMaxWidth(Double.MAX_VALUE);
        this.grid.setAlignment(Pos.CENTER_LEFT);

        // -- label
        label = createContentLabel(dialogPane.getContentText());
        label.setPrefWidth(Region.USE_COMPUTED_SIZE);
        label.textProperty().bind(dialogPane.contentTextProperty());

        dialogPane.contentTextProperty().addListener(o -> updateGrid());

        setTitle(ControlResources.getString("Dialog.confirm.title"));
        dialogPane.setHeaderText(ControlResources.getString("Dialog.confirm.header"));
        dialogPane.getStyleClass().add("choice-dialog");
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        final double MIN_WIDTH = 150;

        list = new ListView<>(FXCollections.observableList(new ArrayList<>(choices)));
        list.setMinWidth(MIN_WIDTH);
        list.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(list, Priority.ALWAYS);
        GridPane.setFillWidth(list, true);

        this.defaultChoice = list.getItems().contains(defaultChoice) ? defaultChoice : null;

        if (defaultChoice == null) {
            list.getSelectionModel().selectFirst();
        } else {
            list.getSelectionModel().select(defaultChoice);
        }

        updateGrid();

        setResultConverter((dialogButton) -> {
            ButtonBar.ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
            return data == ButtonBar.ButtonData.OK_DONE ? getSelectedItem() : null;
        });
    }


    /**
     * Returns the currently selected item in the dialog.
     */
    public final T getSelectedItem() {
        return list.getSelectionModel().getSelectedItem();
    }

    /**
     * Returns the property representing the currently selected item in the dialog.
     */
    public final ReadOnlyObjectProperty<T> selectedItemProperty() {
        return list.getSelectionModel().selectedItemProperty();
    }

    /**
     * Sets the currently selected item in the dialog.
     * @param item The item to select in the dialog.
     */
    public final void setSelectedItem(T item) {
        list.getSelectionModel().select(item);
    }

    /**
     * Returns the list of all items that will be displayed to users. This list
     * can be modified by the developer to add, remove, or reorder the items
     * to present to the user.
     */
    public final ObservableList<T> getItems() {
        return list.getItems();
    }

    /**
     * Returns the default choice that was specified in the constructor.
     */
    public final T getDefaultChoice() {
        return defaultChoice;
    }



    /**************************************************************************
     *
     * Private Implementation
     *
     **************************************************************************/

    private void updateGrid() {
        grid.getChildren().clear();

        grid.add(label, 0, 0);
        grid.add(list, 1, 0);
        getDialogPane().setContent(grid);

        Platform.runLater(list::requestFocus);
    }

    // From DialogPane
    private static Label createContentLabel(String text) {
        Label label = new Label(text);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMaxHeight(Double.MAX_VALUE);
        label.getStyleClass().add("content");
        label.setWrapText(true);
        label.setPrefWidth(360);
        return label;
    }
}
