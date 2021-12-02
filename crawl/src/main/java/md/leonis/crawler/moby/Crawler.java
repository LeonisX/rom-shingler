package md.leonis.crawler.moby;

import md.leonis.crawler.moby.model.Platform;

import java.util.List;
import java.util.Map;

public interface Crawler {

    List<Platform> getPlatformsList() throws Exception;

    void savePlatformsList(List<Platform> platforms) throws Exception;

    Map<String, String> getSavedGamesList(String platformId) throws Exception;

    Map<String, String> getGamesList(String platformId) throws Exception;

    void saveGamesList(String platformId, Map<String, String> games) throws Exception;
}
