package md.leonis.shingler.gui.utils;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;

import java.util.HashSet;
import java.util.Set;

// Idea (not code) from LibFX
public class ListenerHandles {

    private Set<InvalidationListener> listeners;

    private Observable observable;

    public ListenerHandles(Observable observable) {
        this.observable = observable;
        this.listeners = new HashSet<>();
    }

    public void registerListener(InvalidationListener listener) {
        observable.addListener(listener);
        listeners.add(listener);
    }

    public void removeListener(InvalidationListener listener) {
        observable.removeListener(listener);
        listeners.remove(listener);
    }

    public void removeAllListeners() {
        disableListeners();
        listeners.clear();
    }

    public void disableListeners() {
        listeners.forEach(listener -> observable.removeListener(listener));
    }

    public void enableListeners() {
        listeners.forEach(listener -> observable.addListener(listener));
    }

    public void enableAndNotifyListeners() {
        listeners.forEach(listener -> {
            observable.addListener(listener);
            listener.invalidated(observable);
        });
    }
}
