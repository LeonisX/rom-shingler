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
    private long total;
    private long completed;
    private LocalDateTime date;

    public Platform(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public void updateFrom(Platform platform) {

        if (platform != null) {
            this.total = platform.total;
            this.completed = platform.completed;
            this.date = platform.date;
        }
    }

    @Override
    public String toString() {
        return id;
    }
}
