package md.leonis.crawler.moby;

import md.leonis.crawler.moby.model.GameEntry;
import md.leonis.crawler.moby.model.Platform;

import java.util.List;

public interface Crawler {

    List<Platform> getPlatformsList() throws Exception;

    void savePlatformsList(List<Platform> platforms) throws Exception;

    List<GameEntry> getSavedGamesList(String platformId) throws Exception;

    List<GameEntry> getGamesList(String platformId) throws Exception;

    void saveGamesList(String platformId, List<GameEntry> games) throws Exception;
}
