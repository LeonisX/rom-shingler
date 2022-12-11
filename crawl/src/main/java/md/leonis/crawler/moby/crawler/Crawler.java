package md.leonis.crawler.moby.crawler;

import md.leonis.crawler.moby.FilesProcessor;
import md.leonis.crawler.moby.dto.FileEntry;
import md.leonis.crawler.moby.model.GameEntry;
import md.leonis.crawler.moby.model.Platform;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public interface Crawler {

    /*
    * Platforms
    */

    // Load platforms list from site
    List<Platform> parsePlatformsList() throws Exception;

    // Abstract
    // Load platforms list from disk
    List<Platform> loadPlatformsList() throws Exception;

    // Abstract
    // Save platforms list to disk
    void savePlatformsList(List<Platform> platforms) throws Exception;

    // Abstract
    // Load platforms binding map from disk
    Map<String, List<String>> loadPlatformsBindingMap() throws Exception;

    // Abstract
    // Save platforms binding map to disk
    void savePlatformsBindingMap(Map<String, List<String>> map) throws Exception;

    // Abstract
    // Load games binding map from disk
    Map<String, List<String>> loadGamesBindingMap(String platformId, String sourcePlatformId) throws Exception;

    // Abstract
    // Save games binding map to disk
    void saveGamesBindingMap(String platformId, String sourcePlatformId, Map<String, List<String>> map) throws Exception;

    /*
     * Games
     */

    // Parse game entry (html -> GameEntry)
    void parseGameEntry(GameEntry gameEntry) throws Exception;

    List<GameEntry> parseGamesList(String platformId) throws Exception;

    // Abstract
    List<GameEntry> loadGamesList(String platformId) throws Exception;

    // Abstract
    void saveGamesList(String platformId, List<GameEntry> games, GameEntry currentGame) throws Exception;

    List<GameEntry> getGamesList(List<String> platforms) throws Exception;

    void processGamesList(List<GameEntry> gameEntries, boolean dieNow) throws Exception;

    void saveSupportData() throws IOException;

    // Processor
    void setProcessor(FilesProcessor processor);

    FilesProcessor getProcessor();

    boolean isSuspended();

    boolean isAborted();

    void setSuspended(boolean b);

    void setAborted(boolean b);

    void setConsumers(Consumer<GameEntry> crawlerRefreshConsumer, Consumer<GameEntry> crawlerSuccessConsumer, Consumer<GameEntry> crawlerErrorConsumer);

    String getGamePage(String platformId, String gameId);

    // Files

    String getHost();

    // Abstract
    void fileConsumer(FileEntry fileEntry) throws Exception;

    // Abstract
    Path getFilePath(FileEntry fileEntry);

    // Abstract
    Path getFilePath(String platformId, String uri);

    // JSoup
    class A extends Element {

        private final String href;

        public A(Element a) {
            super(a.tag(), a.baseUri(), a.attributes());
            this.href = a.attr("href");
            this.addChildren(a.childNodes().toArray(new Node[0]));
            this.setParentNode(Objects.requireNonNull(a.parentNode()));
        }

        public A(Node a) {
            this((Element) a);
        }

        public String href() {
            return href;
        }
    }

    default Element getByClass(Element element, String className) {

        Element child = element.getElementsByClass(className).first();
        assert child != null;
        return child;
    }

    default Elements getAllByClass(Element element, String className) {
        return element.getElementsByClass(className);
    }

    default Element getById(Element element, String id) {
        Element result = element.getElementById(id);
        assert result != null;
        return result;
    }

    default Element getByTag(Element element, String tag) {
        Element result = element.getElementsByTag(tag).first();
        assert result != null;
        return result;
    }

    default Elements getAllByTag(Element element, String className) {
        return element.getElementsByTag(className);
    }

    default List<A> getAs(Element element) {
        return element.getElementsByTag("a").stream().map(A::new).collect(Collectors.toList());
    }

    default A getA(Element element) {
        return new A(Objects.requireNonNull(element.getElementsByTag("a").first()));
    }

    default Elements getDivs(Element element) {
        return element.getElementsByTag("div");
    }

    default Element getH2(Element element) {
        return Objects.requireNonNull(element.getElementsByTag("h2").first());
    }

    default Element getTable(Element element) {
        return Objects.requireNonNull(element.getElementsByTag("table").first());
    }

    default Elements getTrs(Element element) {
        return element.getElementsByTag("tr");
    }

    default Elements getTds(Element element) {
        return element.getElementsByTag("td");
    }

    default Elements getThs(Element element) {
        return element.getElementsByTag("th");
    }

    default Element getNext(Element element) {
        Element el = element.nextElementSibling();
        assert el != null;
        return el;
    }

    default Node getNextNode(Node element) {
        Node node = element.nextSibling();
        assert node != null;
        return node;
    }

    default Element select(Element element, String selector) {
        Element result = element.selectFirst(selector);
        assert result != null;
        return result;
    }

    default Elements selectUl(Element element, String selector) {
        Element ul = select(element, selector);
        return ul.getElementsByTag("li");
    }

    default Elements selectNextUl(Element element, String selector) {
        Element ul = select(element, selector).nextElementSibling();
        assert ul != null;
        return ul.getElementsByTag("li");
    }

    default Elements selectNextUlOrEmpty(Element element, String selector) {
        Element el = select(element, selector);
        return el == null ? new Elements() : Objects.requireNonNull(el.nextElementSibling()).getElementsByTag("li");
    }

    default String getLastChunk(A a) {
        return getLastChunk(a.href());
    }

    default String getPreLastChunk(A a) {
        return getPreLastChunk(a.href());
    }

    default String getLastChunk(String url) {
        String[] chunks = url.split("/");
        return chunks[chunks.length - 1];
    }

    default String getPreLastChunk(String url) {
        String[] chunks = url.split("/");
        return chunks[chunks.length - 2];
    }
}
