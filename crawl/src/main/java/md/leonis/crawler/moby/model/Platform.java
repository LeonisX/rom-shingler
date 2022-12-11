package md.leonis.crawler.moby.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Platform {

    private String id;          // dos, snes, c64
    private String title;       // MS-DOS, Super Nintendo, Commodore 64
    private long total;         // Games count
    private long completed;     // Games completed count
    private LocalDateTime date; // Archivation date

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
