package md.leonis.crawler.moby.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MobyImage {

    private String id;
    private String host;
    private String small;       // Starts with /
    private String large;       // Starts with /
    private String description;

    public MobyImage(String id, String host, String small, String description) {
        this.id = id;
        this.host = host;
        this.small = small;
        this.description = description;
    }
}
