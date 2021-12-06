package md.leonis.crawler.moby.view;

import java.util.ResourceBundle;

public enum FxmlView {

    SPLASH("splash.title", "splash"),
    DASHBOARD("dashboard.title", "dashboard"),
    SOURCES("sources.title", "sources"),
    PLATFORMS("platforms.title", "platforms"),
    ACTIVITY("activity.title", "activity"),
    ACTIVITY_ERRORS("activity.errors.title", "activityErrors");

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
