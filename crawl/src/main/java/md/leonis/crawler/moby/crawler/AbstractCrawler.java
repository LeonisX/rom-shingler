package md.leonis.crawler.moby.crawler;

import md.leonis.crawler.moby.FilesProcessor;
import md.leonis.crawler.moby.ImagesValidator;
import md.leonis.crawler.moby.dto.FileEntry;
import md.leonis.crawler.moby.executor.Executor;
import md.leonis.crawler.moby.executor.HttpExecutor;
import md.leonis.crawler.moby.model.GameEntry;
import md.leonis.crawler.moby.model.Platform;
import org.apache.hc.core5.http.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static md.leonis.crawler.moby.config.ConfigHolder.*;
import static md.leonis.shingler.utils.FileUtils.*;
import static md.leonis.shingler.utils.StringUtils.escapePathChars;
import static md.leonis.shingler.utils.StringUtils.unescapeUriChars;

public abstract class AbstractCrawler implements Crawler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCrawler.class);

    private Map<String, List<GameEntry>> platformGamesMap = new HashMap<>();
    private boolean suspended = false;
    private boolean aborted = false;
    private Consumer<GameEntry> refreshConsumer = (a) -> {};
    private Consumer<GameEntry> successConsumer = (a) -> {};
    private Consumer<GameEntry> errorConsumer = (a) -> {};

    protected FilesProcessor processor;

    private static boolean useCache = true;

    private boolean validate = false;

    public List<GameEntry> getGamesList(List<String> platforms) {

        List<GameEntry> gameEntries = new ArrayList<>();

        for (String p : platforms) {
            List<GameEntry> list = loadGamesList(p);
            platformGamesMap.put(p, list);
            gameEntries.addAll(list);
        }

        return gameEntries;
    }


    public void processGamesList(List<GameEntry> gameEntries, boolean dieNow) throws Exception {
        if (platformGamesMap.isEmpty()) {
            platformGamesMap = gameEntries.stream().collect(Collectors.groupingBy(GameEntry::getPlatformId));
        }
        cycle(gameEntries, dieNow);
    }

    private void cycle(List<GameEntry> gameEntries, boolean dieNow) throws Exception {
        Set<String> affectedPlatforms = new HashSet<>();

        boolean completed = true;
        int index = 0;
        for (GameEntry gameEntry : gameEntries) {
            if (suspended || aborted) {
                getProcessor().stopProcessors();
                break;
            }
            try {
                if (!gameEntry.isCompleted() && gameEntry.getErrorsCount() < 5) {
                    refreshConsumer.accept(gameEntry);
                    affectedPlatforms.add(gameEntry.getPlatformId());
                    parseGameEntry(gameEntry);
                    gameEntry.setCompleted(true);
                    gameEntry.setErrorsCount(0);
                    successConsumer.accept(gameEntry);
                } else {
                    continue;
                }
            } catch (Exception e) {
                if (dieNow) {
                    throw e;
                }
                if (gameEntry.getErrorsCount() < 5) {
                    completed = false;
                    gameEntry.setErrorsCount(gameEntry.getErrorsCount() + 1);
                    gameEntry.getExceptions().add(e);
                }
                errorConsumer.accept(gameEntry);
            }

            if (++index == 50) {
                saveSupportData();
                for (String platform : affectedPlatforms) {
                    saveGamesList(platform, platformGamesMap.get(platform), gameEntry);
                }
                affectedPlatforms.clear();

                index = 0;
            }
        }

        if (!aborted) {
            saveSupportData();
            for (String platform : affectedPlatforms) {
                saveGamesList(platform, platformGamesMap.get(platform), null);
            }
        }

        if (!completed && !aborted && !suspended) {
            getProcessor().resetProcessors();
            cycle(gameEntries, dieNow);
        }
    }

    public void setConsumers(Consumer<GameEntry> refreshConsumer, Consumer<GameEntry> successConsumer, Consumer<GameEntry> errorConsumer) {
        this.refreshConsumer = refreshConsumer;
        this.successConsumer = successConsumer;
        this.errorConsumer = errorConsumer;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    public boolean isAborted() {
        return aborted;
    }

    public void setAborted(boolean aborted) {
        this.aborted = aborted;
    }

    // Crawler

    @Override
    public List<Platform> loadPlatformsList() {
        return loadJsonList(getSourceDir(getSource()), "platforms", Platform.class);
    }

    @Override
    public void savePlatformsList(List<Platform> platforms) throws Exception {
        saveAsJson(getSourceDir(getSource()), "platforms", platforms);
    }

    @Override
    public Map<String, List<String>> loadPlatformsBindingMap() {
        return loadJsonMapWithList(getSourceDir(getSource()), "platformsBinding", String.class);
    }

    @Override
    public void savePlatformsBindingMap(Map<String, List<String>> map) throws Exception {
        saveAsJson(getSourceDir(getSource()), "platformsBinding", map);
    }

    @Override
    public Map<String, List<String>> loadGamesBindingMap(String platformId, String sourcePlatformId) {
        return loadJsonMapWithList(getGamesDir(getSource()), platformId + "-" + sourcePlatformId + "-binding", String.class);
    }

    @Override
    public void saveGamesBindingMap(String platformId, String sourcePlatformId, Map<String, List<String>> map) throws Exception {
        saveAsJson(getGamesDir(getSource()), platformId + "-" + sourcePlatformId + "-binding", map);
    }

    @Override
    public List<GameEntry> loadGamesList(String platformId) {
        return loadJsonList(getGamesDir(getSource()), platformId, GameEntry.class);
    }

    @Override
    public void saveGamesList(String platformId, List<GameEntry> games, GameEntry currentGame) throws Exception {
        System.out.println("Save at: " + ((currentGame == null) ? "" : currentGame.getTitle()));
        saveAsJson(getGamesDir(getSource()), platformId, games.stream().filter(g -> !g.getGameId().isEmpty()).collect(Collectors.toList()));
    }


    // Files

    public Path getPagePath(String uri) {
        if (uri.startsWith("/")) {
            uri = uri.substring(1);
        }
        uri = escapePathChars(unescapeUriChars(uri));
        return getPagesDir(getSource()).resolve(uri).normalize().toAbsolutePath();
    }

    protected HttpExecutor.HttpResponse getAndSavePage(String uri, String referrer) throws Exception {
        URL url = new URL(uri);

        Path path = getPagePath(url.getPath());

        if (Files.isDirectory(path)) {
            path = path.resolve("default.htm");
        }

        if (Files.exists(path)) {
            if (useCache) { // TODO if read from cache - beatify way
                //TODO уметь выключать, чтобы не спамило
                //LOGGER.info("Use cached page: " + path.toAbsolutePath());
                return new Executor.HttpResponse(new String(Files.readAllBytes(path)), 200, new Header[0]);
            }
        } else {
            try {
                Files.delete(path);
            } catch (Exception ignored) {
            }
        }

        HttpExecutor.HttpResponse response = getExecutor().getPage(uri, referrer);

        if (response.getCode() != 200 || response.getBody().isEmpty()) {
            System.out.println(response.getCode());
            System.out.println(response.getBody().substring(0, Math.min(response.getBody().length(), 128)));
            throw new RuntimeException(response.getCode() + ": " + response.getBody());
        }

        System.out.println("trying to save page: " + path);
        Path dir = path.getParent();
        //при записи проверять и если есть такой файл, то копировать в дефолт.
        if (Files.isRegularFile(dir)) {
            Files.move(dir, dir.getParent().resolve("defaultTmp.htm"));
            Files.createDirectories(dir);
            Files.move(dir.getParent().resolve("defaultTmp.htm"), dir.resolve("default.htm"));
        }

        Files.createDirectories(dir);
        Files.write(path, response.getBody().getBytes());
        System.out.println("saved page: " + path);

        return response;
    }

    @Override
    public Path getFilePath(FileEntry fileEntry) {
        return getFilePath(fileEntry.getPlatformId(), fileEntry.getUri());
    }

    @Override
    public Path getFilePath(String platformId, String uri) {
        if (uri.startsWith("/")) {
            uri = uri.substring(1);
        }
        uri = escapePathChars(unescapeUriChars(uri));
        return getCacheDir(getSource()).resolve(platformId).resolve(uri).normalize().toAbsolutePath();
    }

    @Override
    public void fileConsumer(FileEntry fileEntry) {
        Path path = getFilePath(fileEntry);

        try {
            if (Files.exists(path)) { // TODO if read from cache

                //TODO remove this validation, use in the specific validation task only
                if (validate) {
                    if (ImagesValidator.isBrokenImage(path)) {
                        LOGGER.info("BrokenImage: " + path.toAbsolutePath());
                        Files.delete(path);
                        //throw new RuntimeException("Invalid image: " + path);
                    } else {
                        LOGGER.info("Already cached: " + path.toAbsolutePath());
                        return;
                    }
                } else {
                    //TODO уметь выключать, чтобы не спамило
                    //LOGGER.info("Already cached: " + path.toAbsolutePath());
                    return;
                }
            }

            Executor.HttpResponse response = getExecutor().getFile(fileEntry);

            //TODO
            if (response.getCode() == 404) {
                //YbomCrawler.brokenImages.add(host + uri);
            } else if (response.getCode() != 200) {
                throw new RuntimeException(response.getCode() + ": " + fileEntry.getHost() + fileEntry.getUri());
            } else {
                //System.out.println("trying to save: " + path);
                Path dir = path.getParent();
                Files.createDirectories(dir);
                Files.write(path, response.getBytes());
            }

        } catch (Exception e) {
            throw new RuntimeException(fileEntry.getHost() + fileEntry.getUri(), e);
        }
    }

    //TODO abstract
    @Override
    public void setProcessor(FilesProcessor processor) {
        this.processor = processor;
    }

    //TODO abstract
    @Override
    public FilesProcessor getProcessor() {
        return processor;
    }

    abstract public Executor getExecutor();
}
