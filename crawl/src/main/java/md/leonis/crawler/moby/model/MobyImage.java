package md.leonis.crawler.moby.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
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
}
