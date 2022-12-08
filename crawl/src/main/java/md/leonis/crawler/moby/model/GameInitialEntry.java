package md.leonis.crawler.moby.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GameInitialEntry {

    protected String platformId;
    protected String gameId;
    protected String title;

    public GameInitialEntry(GameEntry entry) {
        this.platformId = entry.getPlatformId();
        this.gameId = entry.getGameId();
        this.title = entry.getTitle();
    }
}
