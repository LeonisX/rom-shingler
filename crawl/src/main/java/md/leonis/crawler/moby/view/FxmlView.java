package md.leonis.crawler.moby.view;

import java.util.ResourceBundle;

public enum FxmlView {

    SPLASH("splash.title", "splash"),
    DASHBOARD("dashboard.title", "dashboard"),
    COLLECTION("collection.title", "collection"),
    COMPARE("compare.title", "compare"),
    FAMILY("family.title", "family");

    private static final String fxmlPath = "/fxml/%s.fxml";

    private final String title;
    private final String fxmlFileName;

    FxmlView(String title, String fxmlFileName) {
        this.title = title;
        this.fxmlFileName = fxmlFileName;
    }

    //TODO this bundle is cached?
    public String getTitle() {
        return ResourceBundle.getBundle("Bundle").getString(title);
    }

    public String getFxmlFile() {
        return String.format(fxmlPath, fxmlFileName);
    }
}
