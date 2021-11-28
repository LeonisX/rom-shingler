package md.leonis.crawler.moby.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Platform {

    private String id;
    private String title;
    private int count;
    private int index;
    private LocalDateTime date;

    public void updateFrom(Platform platform) {

        if (platform != null) {
            this.count = platform.count;
            this.index = platform.index;
            this.date = platform.date;
        }
    }
}
