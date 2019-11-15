package md.leonis.shingler.gui.controls;

import com.sun.javafx.tk.Toolkit;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogEvent;
import javafx.stage.Window;

import java.io.File;

public class SmartDirectoryChooser extends Dialog<File> {

    private File file;

    /**
     * The initial directory for the displayed dialog.
     */
    private ObjectProperty<File> initialDirectory;

    public SmartDirectoryChooser(String title, File value) {
        setTitle(title);
        setInitialDirectory(value);
    }

    public final void setInitialDirectory(final File value) {
        initialDirectoryProperty().set(value);
    }

    public final File getInitialDirectory() {
        return (initialDirectory != null) ? initialDirectory.get() : null;
    }

    public final ObjectProperty<File> initialDirectoryProperty() {
        if (initialDirectory == null) {
            initialDirectory = new SimpleObjectProperty<>(this, "initialDirectory");
        }

        return initialDirectory;
    }

    /**
     * Shows a new directory selection dialog. The method doesn't return until
     * the displayed dialog is dismissed. The return value specifies the
     * directory chosen by the user or {@code null} if no selection has been
     * made. If the owner window for the directory selection dialog is set,
     * input to all windows in the dialog's owner chain is blocked while the
     * dialog is being shown.
     *
     * @param ownerWindow the owner window of the displayed dialog
     * @return the selected directory or {@code null} if no directory has been
     *      selected
     */
    public File showDialog(final Window ownerWindow) {
        Event.fireEvent(this, new DialogEvent(this, DialogEvent.DIALOG_SHOWING));
        Event.fireEvent(this, new DialogEvent(this, DialogEvent.DIALOG_SHOWN));

        file = Toolkit.getToolkit().showDirectoryChooser(
                (ownerWindow != null) ? ownerWindow.impl_getPeer() : null,
                getTitle(),
                getInitialDirectory());

        Event.fireEvent(this, new DialogEvent(this, DialogEvent.DIALOG_HIDING));
        Event.fireEvent(this, new DialogEvent(this, DialogEvent.DIALOG_CLOSE_REQUEST));
        Event.fireEvent(this, new DialogEvent(this, DialogEvent.DIALOG_HIDDEN));

        return file;
    }

    public File getFile() {
        return file;
    }
}
