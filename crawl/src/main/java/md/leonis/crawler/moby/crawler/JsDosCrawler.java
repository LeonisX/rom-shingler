package md.leonis.crawler.moby.crawler;

import lombok.Data;
import md.leonis.crawler.moby.FilesProcessor;
import md.leonis.crawler.moby.dto.FileEntry;
import md.leonis.crawler.moby.executor.Executor;
import md.leonis.crawler.moby.executor.HttpExecutor;
import md.leonis.crawler.moby.model.GameEntry;
import md.leonis.crawler.moby.model.GameFileEntry;
import md.leonis.crawler.moby.model.MobyImage;
import md.leonis.crawler.moby.model.Platform;
import md.leonis.shingler.utils.ArchiveUtils;
import md.leonis.shingler.utils.FileUtils;
import md.leonis.shingler.utils.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static md.leonis.crawler.moby.config.ConfigHolder.*;

public class JsDosCrawler extends AbstractCrawler {

    private static final String PLATFORM_ID = "dos";
    private static final String HOST = "github.com";
    private static final String ROOT = "https://" + HOST;
    private static final String SOURCE_CODE = "/js-dos/repository/archive/refs/heads/main.zip";
    private static final String REFERRER = "https://github.com/js-dos/repository/";

    public static final String GAME_MAIN = "https://raw.githubusercontent.com/js-dos/repository/main/_pages/%s.njk";

    private final Executor executor = new HttpExecutor().withoutHostHeader();

    public JsDosCrawler() {
    }

    public JsDosCrawler(int processors) {
        this();
        this.processor = new FilesProcessor(processors, this::fileConsumer);
    }

    @Override
    public List<Platform> parsePlatformsList() {
        return Collections.singletonList(new Platform(PLATFORM_ID, "DOS", 0, 0, null));
    }

    @Override
    public void parseGameEntry(GameEntry entry) throws Exception {
        // https://cdn.dos.zone/original/2X/f/f9ddfcf94f9412ca549ff26b48f2c000d8ff7d24.png
        for (MobyImage screen : entry.getScreens()) {
            URI uri = new URI(screen.getLarge());
            processor.add(new FileEntry(entry.getPlatformId(), uri.getScheme() + "://" + uri.getHost(), uri.getPath(), "https://dos.zone"));
        }
        // custom/dos/quien-es-cualo.jsdos
        entry.getFiles().forEach(f -> processor.add(new FileEntry(entry.getPlatformId(), "https://cdn.dos.zone", "/" + f.getUrl(), "https://dos.zone")));
    }

    @Override
    public List<GameEntry> parseGamesList(String platformId) throws Exception {
        org.apache.commons.io.FileUtils.deleteDirectory(getPagesDir(getSource()).toFile());
        org.apache.commons.io.FileUtils.deleteDirectory(getCacheDir(getSource()).toFile());
        //load from github, delete files, save new
        FileEntry fileEntry = new FileEntry(PLATFORM_ID, ROOT, SOURCE_CODE, REFERRER);
        System.out.println("Downloading: " + ROOT + SOURCE_CODE);
        Executor.HttpResponse response = executor.getFile(fileEntry);

        if (response.getCode() != 200) {
            throw new RuntimeException(response.getCode() + ": " + SOURCE_CODE);
        } else {
            Path path = getFilePath(fileEntry);
            System.out.println("Save to: " + path);
            Path dir = path.getParent();
            Files.createDirectories(dir);
            Files.write(path, response.getBytes());
            System.out.println("Extracting...");
            ArchiveUtils.unzip(getFilePath(fileEntry), getPagesDir(getSource()));
        }

        return parseGames(platformId);
    }

    private List<GameEntry> parseGames(String platformId) throws IOException {
        List<Game> games = FileUtils.listFiles(getPagesDir(getSource()).resolve("repository-main").resolve("_pages")).stream().map(file -> {
            List<String> lines = IOUtils.loadTextFile(file);
            Game game = new Game();
            game.setFiles(loadFiles(lines));
            game.setLayout(loadGameField(lines, "layout:"));
            game.setOgDescription(loadGameField(lines, "ogDescription:"));
            game.setPermalink(loadGameField(lines, "permalink:"));
            game.setScreenshots(loadScreens(lines));
            game.setShortTitle(loadGameField(lines, "shortTitle:"));
            game.setTitle(loadGameField(lines, "title:"));
            game.setYoutube(loadGameField(lines, "youtube:"));
            String mp = loadGameField(lines, "multiplayer:");
            game.setMultiplayer(Boolean.parseBoolean(mp));
            return game;
        }).collect(Collectors.toList());
        FileUtils.saveAsJson(getGamesDir(getSource()), platformId + "-bundles", games);
        return games.stream().map(game -> {
            GameEntry gameEntry = new GameEntry(platformId, game.getPermalink().replace("/", ""), game.shortTitle);
            gameEntry.setDescription(Collections.singletonList(game.getOgDescription()));
            gameEntry.setHasScreenshots(!game.getScreenshots().isEmpty());
            gameEntry.setScreens(game.getScreenshots().stream().map(s -> new MobyImage("0", null, s, null)).collect(Collectors.toList()));
            if (StringUtils.isNotBlank(game.getYoutube())) {
                gameEntry.getVideos().add(game.getYoutube());
            }
            gameEntry.setFiles(game.getFiles());
            gameEntry.setMultiplayer(game.isMultiplayer());
            return gameEntry;
        }).collect(Collectors.toList());
    }

    private List<String> loadScreens(List<String> lines) {
        List<String> screens = new ArrayList<>();
        l1:
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("screenshots:")) {
                for (int j = i + 1; j < lines.size(); j++) {
                    if (lines.get(j).startsWith("- ")) {
                        screens.add(lines.get(j).trim().substring(2));
                    } else {
                        break l1;
                    }
                }
            }
        }
        return screens;
    }

    private String loadGameField(List<String> lines, String field) {
        StringBuilder sb = new StringBuilder();
        l1:
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(field)) {
                sb.append(lines.get(i).replace(field, "").trim());
                for (int j = i + 1; j < lines.size(); j++) {
                    if (lines.get(j).startsWith("  ")) {
                        sb.append(" ").append(lines.get(j).trim().substring(2));
                    } else {
                        break l1;
                    }
                }
            }
        }
        return sb.toString();
    }

    private List<GameFileEntry> loadFiles(List<String> lines) {
        int start = lines.indexOf("bundles:") + 1;
        int end = start + 1;
        for (int i = end; i < lines.size(); i++) {
            if (!lines.get(i).startsWith("- ") && !lines.get(i).startsWith("  ")) {
                break;
            } else {
                end++;
            }
        }
        List<String> list = lines.subList(start, end);
        List<List<String>> blocks = new ArrayList<>();

        list.forEach(l -> {
            if (l.startsWith("- ")) {
                blocks.add(new ArrayList<>());
            }
            blocks.get(blocks.size() - 1).add(l);
        });

        return blocks.stream().map(b -> {
            GameFileEntry fileEntry = new GameFileEntry();
            b.forEach(str -> {
                String s = str.substring(2);
                String[] chunks = s.split(":");
                switch (chunks[0]) {
                    case "lang":
                        fileEntry.setLang(chunks[1].trim());
                        break;
                    case "title":
                        fileEntry.setTitle(chunks[1].trim());
                        break;
                    case "url":
                        fileEntry.setUrl(chunks[1].trim());
                        break;
                    default:
                        fileEntry.getOther().put(chunks[0].trim(), chunks[1].trim());
                        break;
                }
            });
            return fileEntry;
        }).collect(Collectors.toList());
    }

    @Data
    static class Game {
        private List<GameFileEntry> files;
        private String layout;              // page.njk
        private String ogDescription;       // 4K Adventure is a famous....
        private String permalink;           // /4k-adventure-oct-31-1998/
        private List<String> screenshots;   // - https://cdn.dos.zon
        private String shortTitle;          // 4K Adventure
        private String title;               // 4K Adventure | 🕹️Play 4K Adventure Online | DOS game in browser
        private String youtube;             // aJk1PF7VoEA
        private boolean multiplayer;         // true
    }

    @Override
    public void saveSupportData() {
        // unused
    }

    @Override
    public String getGamePage(String platformId, String gameId) {
        return String.format(GAME_MAIN, gameId);
    }

    @Override
    public Executor getExecutor() {
        return executor;
    }

    @Override
    public String getHost() {
        return HOST;
    }
}
