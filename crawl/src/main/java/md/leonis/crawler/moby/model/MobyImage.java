package md.leonis.crawler.moby.model;

public class MobyImage {

    private String id;
    private String small;
    private String large;
    private String description;

    public MobyImage(String id, String small, String description) {
        this.id = id;
        this.small = small;
        this.description = description;
    }

    public String getSmall() {
        return small;
    }

    public void setSmall(String small) {
        this.small = small;
    }

    public String getLarge() {
        return large;
    }

    public void setLarge(String large) {
        this.large = large;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
