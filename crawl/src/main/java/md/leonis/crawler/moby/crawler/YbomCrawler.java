package md.leonis.crawler.moby.crawler;

import lombok.Data;
import lombok.EqualsAndHashCode;
import md.leonis.crawler.moby.FilesProcessor;
import md.leonis.crawler.moby.config.ConfigHolder;
import md.leonis.crawler.moby.dto.FileEntry;
import md.leonis.crawler.moby.executor.Executor;
import md.leonis.crawler.moby.executor.HttpExecutor;
import md.leonis.crawler.moby.model.*;
import md.leonis.shingler.utils.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static md.leonis.crawler.moby.config.ConfigHolder.*;
import static md.leonis.shingler.utils.FileUtils.*;

@EqualsAndHashCode(callSuper = true)
@Data
public class YbomCrawler extends AbstractCrawler {

    private static final Logger LOGGER = LoggerFactory.getLogger(YbomCrawler.class);

    private static final String HOST = new StringBuilder("moc.semagybom.www").reverse().toString();
    private static final String ROOT = "https://" + HOST;

    public static final String GAME_MAIN_REFERRER = ROOT + "/search/quick?q=%s";

    public static final String PLATFORMS = ROOT + "/browse/games/full,1/";
    public static final String GAMES_PAGES = ROOT + "/browse/games/%s/offset,%s/so,0a/list-games/";
    public static final String GAME_MAIN = ROOT + "/game/%s/%s";
    public static final String GAME_CREDITS = ROOT + "/game/%s/%s/credits";
    public static final String GAME_SCREENSHOTS = ROOT + "/game/%s/%s/screenshots";
    public static final String GAME_SCREENSHOTS_IMAGE = ROOT + "/game/%s/%s/screenshots/gameShotId,%s";
    // About rank: /info/mobyrank?nof=1
    //TODO тут ссылки на некоторые источники сдохли.
    //Некоторые совсем, некоторые переехали.
    //Нужно уметь мэппить
    //TODO sources (журналы, сайты, откуда брались обзоры и ревью.
    // ><a href="/mobyrank/source/sourceId,998/">Tilt </a> (Dec, 1987)</div>
    // там список игр, которые оценивал источник с оценкой и датой
    public static final String GAME_REVIEWS = ROOT + "/game/%s/%s/mobyrank";
    public static final String GAME_COVER_ART = ROOT + "/game/%s/%s/cover-art";
    public static final String GAME_COVER_ART_IMAGE = ROOT + "/game/%s/%s/cover-art/gameCoverId,%s";
    public static final String GAME_PROMO_ART = ROOT + "/game/%s/%s/promo";
    public static final String GAME_PROMO_ART_IMAGE = ROOT + "/game/%s/%s/promo/promoImageId,%s";
    public static final String GAME_RELEASES = ROOT + "/game/%s/%s/release-info";
    public static final String GAME_TRIVIA = ROOT + "/game/%s/%s/trivia";
    public static final String GAME_HINTS = ROOT + "/game/%s/%s/hints";
    public static final String GAME_HINTS_PAGE = ROOT + "/game/%s/%s/hints/hintId,%s";
    public static final String GAME_SPECS = ROOT + "/game/%s/%s/techinfo";
    public static final String GAME_ADS = ROOT + "/game/%s/%s/adblurbs";
    public static final String GAME_RATING_SYSTEMS = ROOT + "/game/%s/%s/rating-systems";

    private final Executor executor = new HttpExecutor();

    private static final boolean prependPlatformId = true;

    public static void main(String[] args) throws Exception {

        String[] pl = new String[]{"nes", "sg-1000", "sega-master-system", "game-gear", "sega-32x", "genesis",
                "snes", "gameboy", "gameboy-color", "game-com", "sega-cd", "3do", "virtual-boy", "1292-advanced-programmable-video-system",
                "apf", "channel-f", "neo-geo-pocket-color", "neo-geo-pocket", "atari-5200", "atari-7800", "turbografx-cd", "turbo-grafx",
                "supervision", "supergrafx", "rca-studio-ii", "intellivision", "colecovision", "bally-astrocade", "zx-spectrum", "atari-2600",
                "atari-8-bit", "gameboy-advance", "arcade", "n64",
                "arcadia-2001", "odyssey", "odyssey-2", "mattel-aquarius", "lynx", "jaguar", "interton-video-2000",
                "cd-i", "fred-cosmac", "colecoadam", "supervision", "vectrex", "sega-saturn", "playstation",
                "neo-geo", "neo-geo-cd", "neo-geo-x", "dreamcast"};

        pl = new String[]{/*"odyssey", "fred-cosmac",*/ "pc-booter", "dos"};

        /*String[] pl2 = new String[]{"nintendo-dsi", "nintendo-ds", "pet", "vic-20", "c64", "c128", "commodore-16-plus4", "trs-80", "trs-80-coco", "trs-80-mc-10",
                "msx", "amiga", "amiga-cd32", "cpc", "amstrad-pcw", "apple-i", "apple2", "apple2g", "atari-st", "bbc-micro_", "electron", "enterprise", "dos"};*/

        List<Platform> platforms = Arrays.stream(pl).map(p -> new Platform(p, p, 0, 0, null)).collect(Collectors.toList());
        //ConfigHolder.setPlatforms(FileUtils.loadJsonList(ConfigHolder.sourceDir, "platforms", Platform.class));
        ConfigHolder.setPlatforms(platforms);
        ConfigHolder.setSource("moby");

        YbomCrawler crawler = new YbomCrawler();
        crawler.setProcessor(new FilesProcessor(1, crawler::fileConsumer));

        /*for (Platform p : platforms) {
            List<GameEntry> gameEntries = crawler.parseGamesList(p.getId());
            crawler.saveGamesList(p.getId(), gameEntries.stream().sorted(Comparator.comparing(GameEntry::getTitle)).collect(Collectors.toList()));
        }

        crawler.processGamesList(crawler.getGamesList(Arrays.asList(pl)), false);
        */

        //!!!!!!!!!!!!!!
        crawler.useCache = false;

        List<GameEntry> entries = FileUtils.loadJsonList(getSourceDir(getSource()), "brokenGames", GameEntry.class);

        crawler.processGamesList(entries, true);

        //crawler.go();
        System.out.println("Gata!!!!!!");
    }

    public Map<String, String> companies;
    public Map<String, String> sheets;
    public Map<String, String> gameGroups;
    public Map<String, String> developers;
    public Map<String, String> sources;
    public Map<String, String> users;
    public Map<String, String> attributes;
    public List<String> brokenImages;

    private boolean validate = false;

    public YbomCrawler() {
        preload();
    }

    public YbomCrawler(FilesProcessor processor) {
        this();
        this.processor = processor;
    }

    public YbomCrawler(int processors) {
        this();
        this.processor = new FilesProcessor(processors, this::fileConsumer);
    }

    @Override
    public void setProcessor(FilesProcessor processor) {
        this.processor = processor;
    }

    public void go() throws Exception {

        for (Platform platform : platforms) {
            List<GameEntry> games = parseGamesList(platform.getId());

            int i = 0;
            for (GameEntry gameEntry : games) {
                try {
                    parseGameEntry(gameEntry);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                processor.stopAllProcessorsIfError();

                if (++i == 50) {
                    saveSupportData();
                    saveGamesList(platform.getId(), games, gameEntry);
                    // TODO save position, return to position later
                    i = 0;
                }
            }

            saveSupportData();
            saveGamesList(platform.getId(), games, null);
        }

        processor.finalizeProcessors();
    }

    @Override
    public void parseGameEntry(GameEntry gameEntry) throws Exception {
        parseGameMain(gameEntry);
        // если ошибочная игра - не парсить.
        if (gameEntry.getGameId().isEmpty()) {
            //TODO log somehow, or set status
            System.out.println("Ignore this game");
        } else {
            parseGameCredits(gameEntry);
            parseGameScreenshots(gameEntry);
            parseGameReviews(gameEntry);
            parseGameCoverArt(gameEntry);
            parseGamePromoArt(gameEntry);
            parseGameReleases(gameEntry);
            parseGameTrivia(gameEntry);
            parseGameHints(gameEntry);
            parseGameSpecs(gameEntry);
            parseGameAds(gameEntry);
            parseGameRatings(gameEntry);
        }
    }

    private void preload() {
        companies = loadJsonMap(getSourceDir(getSource()), "companies");
        sheets = loadJsonMap(getSourceDir(getSource()), "sheets");
        gameGroups = loadJsonMap(getSourceDir(getSource()), "gameGroups");
        developers = loadJsonMap(getSourceDir(getSource()), "developers");
        sources = loadJsonMap(getSourceDir(getSource()), "sources");
        users = loadJsonMap(getSourceDir(getSource()), "users");
        attributes = loadJsonMap(getSourceDir(getSource()), "attributes");
        brokenImages = loadJsonList(getSourceDir(getSource()), "brokenImages");
    }

    @Override
    public void saveSupportData() throws IOException {
        //System.out.println("Save in progress...");
        saveAsJson(getSourceDir(getSource()), "companies", companies);
        saveAsJson(getSourceDir(getSource()), "sheets", sheets);
        saveAsJson(getSourceDir(getSource()), "gameGroups", gameGroups);
        saveAsJson(getSourceDir(getSource()), "developers", developers);
        saveAsJson(getSourceDir(getSource()), "sources", sources);
        saveAsJson(getSourceDir(getSource()), "users", users);
        saveAsJson(getSourceDir(getSource()), "attributes", attributes);
        saveAsJson(getSourceDir(getSource()), "brokenImages", brokenImages);
        //System.out.println("Saved.");
    }

    private void parseGameMain(GameEntry entry) throws Exception {

        HttpExecutor.HttpResponse response = getAndSavePage(getGameLink(entry), getGameMainReferrer(entry.getGameId()));

        Element container = getContainer(response);

        getAllByTag(container, "moby").forEach(Node::unwrap);

        // 1942 (NES)
        // Main  Credits  Screenshots  Reviews  Cover Art  Promo Art  Releases  Trivia  Hints  Specs  Ad Blurb  Rating Systems  Buy/Trade
        Element rightPanelHeader = getByClass(container, "rightPanelHeader");
        Element niceHeaderTitle = getByClass(rightPanelHeader, "niceHeaderTitle");
        List<A> niceHeaderTitleA = getAs(niceHeaderTitle);

        // Здесь так же можно получить полное название игры и платформы
        String gameName = getLastChunk(niceHeaderTitleA.get(0).text());
        String platformName = getLastChunk(niceHeaderTitleA.get(1).text());

        assert gameName.equals(entry.getTitle());
        assert platformName.equals(platformsById.get(entry.getPlatformId()).getTitle());

        // Main  Credits  Screenshots  Reviews  Cover Art  Promo Art  Releases  Trivia  Hints  Specs  Ad Blurb  Rating Systems  Buy/Trade
        Elements lis = selectUl(rightPanelHeader, "ul.nav-tabs");
        assert lis.size() == 14;

        // Иногда по ошибке игра отображается для системы, но фактически она ей не принадлежит
        // Тогда моби показывает групповую инфу, которая нам не нужна.
        if (!getA(lis.get(0)).href().equals(getGameLink(entry))) {
            System.out.println("Ignore this game and delete: " + entry.getTitle());
            entry.setGameId("");
            return;
        }

        assert getA(lis.get(0)).text().equals("Main");      // /game/%s/%s
        assert getA(lis.get(1)).text().equals("Credits");   ///game/%s/%s/credits
        assert getA(lis.get(2)).text().equals("Screenshots"); ///game/%s/%s/screenshots
        assert getA(lis.get(3)).text().equals("Reviews");   ///game/%s/%s/mobyrank
        assert getA(lis.get(4)).text().equals("Cover Art"); ///game/%s/%s/cover-art
        assert getA(lis.get(5)).text().equals("Promo Art"); ///game/%s/%s/promo
        assert getA(lis.get(6)).text().equals("Releases");  ///game/%s/%s/release-info
        assert getA(lis.get(7)).text().equals("Trivia");    ///game/%s/%s/trivia
        assert getA(lis.get(8)).text().equals("Hints");     ///game/%s/%s/hints
        assert getA(lis.get(9)).text().equals("Specs");     ///game/%s/%s/techinfo
        assert getA(lis.get(10)).text().equals("Ad Blurb"); ///game/%s/%s/adblurbs
        assert getA(lis.get(11)).text().equals("Rating Systems");///game/%s/%s/rating-systems
        assert lis.get(12).text().isEmpty();
        assert getA(lis.get(13)).text().equals("Buy/Trade");///game/%s/%s/buy-trade

        entry.setHasCredits(!lis.get(1).hasClass("disabled"));
        entry.setHasScreenshots(!lis.get(2).hasClass("disabled"));
        entry.setHasReviews(!lis.get(3).hasClass("disabled"));
        entry.setHasCoverArt(!lis.get(4).hasClass("disabled"));
        entry.setHasPromoArt(!lis.get(5).hasClass("disabled"));
        entry.setHasReleases(!lis.get(6).hasClass("disabled"));
        entry.setHasTrivia(!lis.get(7).hasClass("disabled"));
        entry.setHasHints(!lis.get(8).hasClass("disabled"));
        entry.setHasSpecs(!lis.get(9).hasClass("disabled"));
        entry.setHasAdBlurb(!lis.get(10).hasClass("disabled"));
        entry.setHasRatings(!lis.get(11).hasClass("disabled"));

        Element divColMd8 = getByClass(container, "col-md-8 col-lg-8");
        // Все метаданные по игре + обложка
        Element coreGameInfo = getById(divColMd8, "floatholder coreGameInfo");

        // Second column
        Element coreGameRelease = getById(coreGameInfo, "coreGameRelease");

        Elements divs = getDivs(coreGameRelease);
        for (int i = 1; i < divs.size(); i += 2) {
            Element title = divs.get(i);
            switch (title.text()) {
                case "Published by":
                    // <a href=".../company/capcom-usa-inc">Capcom&nbsp;U.S.A.,&nbsp;Inc.</a>
                    entry.setPublishers(getSheets(divs.get(i + 1)));
                    break;
                case "Developed by":
                    // <a href=".../company/capcom-co-ltd">Capcom&nbsp;Co.,&nbsp;Ltd.</a>
                    entry.setDevelopers(getSheets(divs.get(i + 1)));
                    break;
                case "Released":
                    // <a href=".../game/%s/%s/release-info">Nov, 1986</a>
                    getAs(divs.get(i + 1)).forEach(a -> entry.getDates().add(a.text()));
                    break;
                case "Official Site":
                    // <a href=".../company/capcom-co-ltd">Capcom&nbsp;Co.,&nbsp;Ltd.</a>
                    entry.setOfficialSites(getSheets(divs.get(i + 1)));
                    break;
                case "Also For":
                    // <a href=".../game/cpc/1942arcade">Amstrad CPC</a>
                    // ,
                    // <a href=".../game/android/1942arcade">Android</a>
                    // ,
                    // ...
                    // |
                    // <a href=".../game/1942arcade">Combined&nbsp;View</a>
                    List<A> alsoFor = getAs(divs.get(i + 1));
                    alsoFor.subList(0, alsoFor.size() - 1).forEach(a -> entry.getAlsoFor().add(getPreLastChunk(a.href())));
                    break;
                case "Platform":
                case "Platforms":
                    // <a href=".../game/cpc/1942arcade">Amstrad CPC</a>
                    // ,
                    // <a href=".../game/android/1942arcade">Android</a>
                    // ,
                    // ...
                    // |
                    // <a href=".../game/1942arcade">Combined&nbsp;View</a>
                    List<A> platforms = getAs(divs.get(i + 1));
                    platforms.forEach(a -> entry.getAlsoFor().add(getPreLastChunk(a.href())));
                    break;
                default:
                    throw new RuntimeException(title.text());
            }
        }

        // Third column
        Element coreGameGenre = getById(coreGameInfo, "coreGameGenre");

        divs = getDivs(coreGameGenre);
        int start = 2;
        Element divv = divs.get(1);
        if (divv.attr("style").equals("float: right;")) {
            start++;
        }
        for (int i = start; i < divs.size(); i += 2) {
            Element title = divs.get(i);
            switch (title.text()) {
                case "ESRB Rating":
                    // <a href=".../attribute/sheet/attributeId,91/">Kids to Adults</a>
                    entry.setEsbrRatings(getSheets(divs.get(i + 1)));
                    break;
                case "Genre":
                    // <a href=".../genre/sheet/action/">Action</a>
                    entry.setGenres(getSheets(divs.get(i + 1)));
                    break;
                case "Perspective":
                    // <a href=".../genre/sheet/top-down/">Top-down</a>
                    entry.setPerspectives(getSheets(divs.get(i + 1)));
                    break;
                case "Visual":
                    // <a href=".../genre/sheet/2d-scrolling/">2D&nbsp;scrolling</a>
                    entry.setVisuals(getSheets(divs.get(i + 1)));
                    break;
                case "Pacing":
                    // <a href=".../genre/sheet/turn-based/">Turn-based</a>
                    entry.setPacings(getSheets(divs.get(i + 1)));
                    break;
                case "Art":
                    // <a href=".../genre/sheet/anime-manga/">Anime&nbsp;/&nbsp;manga</a>
                    entry.setArts(getSheets(divs.get(i + 1)));
                    break;
                case "Gameplay":
                    // <a href=".../genre/sheet/arcade_/">Arcade</a>,
                    // <a href=".../genre/sheet/shooter/">Shooter</a>
                    entry.setGameplays(getSheets(divs.get(i + 1)));
                    break;
                case "Educational":
                    // <a href=".../genre/sheet/math-logic/">Math&nbsp;/&nbsp;logic</a>
                    entry.setEducationals(getSheets(divs.get(i + 1)));
                    break;
                case "Interface":
                    // <a href=".../genre/sheet/direct_control/">Direct&nbsp;control</a>
                    entry.setInterfaces(getSheets(divs.get(i + 1)));
                    break;
                case "Vehicular":
                    // <a href=".../genre/sheet/automobile/">Automobile</a>
                    entry.setVehiculars(getSheets(divs.get(i + 1)));
                    break;
                case "Setting":
                    // <a href=".../genre/sheet/world-war-ii/">World&nbsp;War&nbsp;II</a>
                    entry.setSettings(getSheets(divs.get(i + 1)));
                    break;
                case "Sport":
                    // <a href=".../genre/sheet/football-american/">Football&nbsp;(American)</a>
                    entry.setSports(getSheets(divs.get(i + 1)));
                    break;
                case "Narrative":
                    // <a href=".../genre/sheet/football-american/">Football&nbsp;(American)</a>
                    entry.setNarratives(getSheets(divs.get(i + 1)));
                    break;
                case "Special Edition":
                    // <a href=".../genre/sheet/physical-extras/">Physical&nbsp;extras</a>
                    entry.setSpecialEditions(getSheets(divs.get(i + 1)));
                    break;
                case "Add-on":
                    // <a href=".../genre/sheet/map_level/">Map&nbsp;/&nbsp;level</a>
                    entry.setAddons(getSheets(divs.get(i + 1)));
                    break;
                case "Misc":
                    // <a href=".../genre/sheet/regional-differences/">Regional&nbsp;differences</a>
                    entry.setMiscs(getSheets(divs.get(i + 1)));
                    break;
                case "Amazon Rating":
                    // <a href=".../attribute/sheet/attributeId,2618/">Guidance Suggested</a>
                    entry.setAmazonRatings(getSheets(divs.get(i + 1)));
                    break;
                default:
                    throw new RuntimeException(title.text());
            }
        }

        // Description
        Element h2description = select(divColMd8, "h2:contains(Description)");

        // Читать ноды, пока не встретится класс sideBarLinks
        List<String> description = new ArrayList<>();
        Node node = h2description;

        do {
            node = getNextNode(node);
            if (node instanceof TextNode) {
                description.add(((TextNode) node).text());
                //System.out.println("TextNode: " + ((TextNode) node).text());
            } else {
                //System.out.println("Node: " + node);
                Element element = (Element) node;
                // Этот тот случай, когда два вложенных незакрытых тега. Сейчас будет долго реализовывать обработку детей
                // ради 1-2 игр лучше сделать такой хак.
                if (element == null) {
                    break;
                }
                if (element.className().equals("sideBarLinks")) {
                    break;
                }
                //если в описании не закрытые теги, то захватывается и код ниже,
                //не удаётся определить где конец. необходимо искать его в детях и
                //если есть, то фильтровать, сохранять и прерывать цикл.
                int index = -1;
                for (int i = 0; i < element.childNodes().size(); i++) {
                    if (element.childNode(i) instanceof Element) {
                        //System.out.println("element.childNode(i): " + element.childNode(i));
                        Element el = (Element) element.childNode(i);
                        if (el.tagName().equals("div") && el.className().equals("sideBarLinks")) {
                            index = i;
                            break;
                        }
                    }
                }
                if (index >= 0) {
                    List<Node> nodes = new ArrayList<>();
                    for (int i = index; i < element.childNodes().size(); i++) {
                        nodes.add(element.childNodes().get(i));
                    }
                    nodes.forEach(Node::remove);
                    description.add(element.outerHtml());
                    break;
                }

                description.add(element.outerHtml());
            }
        } while (true);

        entry.setDescription(description);

        // Skip screenshots, promo images, trailer

        // Alternate Titles
        lis = selectNextUlOrEmpty(divColMd8, "h2:contains(Alternate Titles)");
        lis.forEach(li -> entry.getAlternateTitles().add(((TextNode) li.childNode(0)).text() + ((Element) li.childNode(1)).text()));


        // Part of the Following Groups
        lis = selectNextUlOrEmpty(divColMd8, "h2:contains(Part of the Following Group)");
        lis.forEach(li -> {
            A a = getA(li.child(0));
            // .../game-group/mario-games
            gameGroups.put(getLastChunk(a), a.text());
            entry.getGameGroup().add(getLastChunk(a));
        });
    }

    private void parseGameCredits(GameEntry entry) throws Exception {

        if (!entry.isHasCredits()) {
            return;
        }

        HttpExecutor.HttpResponse response = getAndSavePage(String.format(GAME_CREDITS, entry.getPlatformId(), entry.getGameId()), getGameLink(entry));
        Element container = getContainer(response);
        Element divColMd12 = getByClass(container, "col-md-12");
        // Все метаданные по игре + обложка
        Element coreGameInfo = getById(divColMd12, "floatholder coreGameInfo");
        //Title or BR if no credits
        Element h2Title = getNext(coreGameInfo);

        if (h2Title.tagName().equals("h2")) {
            assert h2Title.text().equals(entry.getTitle() + " Credits");
            //Div со всеми кредитами
            Element div = getNext(h2Title);
            // X people
            assert ((TextNode) getNextNode(div)).text().endsWith(" people");
            // Table
            Element table = getTable(div);
            assert table.attr("summary").equals("List of Credits");

            Map<String, List<CreditsNode>> credits = new LinkedHashMap<>();
            String group = null;

            Elements trs = getTrs(table);
            for (Element tr : trs) {
                Elements tds = getTds(tr);
                if (tds.size() == 1) {
                    String nextGroup = tds.get(0).text();
                    if (nextGroup.equals("Other Games")) {
                        break;
                    }
                    if (entry.getCredits().containsKey(nextGroup)) {
                        nextGroup = nextGroup + "_" + UUID.randomUUID().toString().toCharArray()[0];
                    }
                    if (!credits.isEmpty()) {
                        entry.getCredits().put(group, credits);
                    }
                    group = nextGroup;
                    credits = new LinkedHashMap<>();

                } else if (tds.size() == 2) {
                    assert tr.hasClass("crln");
                    String role = tds.get(0).text();
                    List<CreditsNode> ids = parseCredits(tds.get(1));
                    credits.put(role, ids);
                } else {
                    throw new RuntimeException();
                }
            }
            if (!credits.isEmpty()) {
                entry.getCredits().put(group, credits);
            }
        } else if (h2Title.tagName().equals("br")) {
            Element p = getNext(h2Title);
            assert p.tagName().equals("p");
            assert p.text().startsWith("here are no credits for the");
            //System.out.println(p.text());
        } else {
            throw new RuntimeException(h2Title.outerHtml());
        }
    }

    private void parseGameScreenshots(GameEntry entry) throws Exception {
        if (!entry.isHasScreenshots()) {
            return;
        }

        String thisLink = String.format(GAME_SCREENSHOTS, entry.getPlatformId(), entry.getGameId());
        HttpExecutor.HttpResponse response = getAndSavePage(thisLink, getGameLink(entry));
        Element container = getContainer(response);
        Element divColMd12 = getByClass(container, "col-md-12");

        Element h2 = select(divColMd12, "h2:contains(User Screenshots)");
        Element h3 = getNext(h2);
        assert h3.text().equals(entry.getTitle() + " version");
        Element divRow = getNext(h3);
        assert divRow.tagName().equals("div");

        // <div class="thumbnail-image-wrapper">
        // <a href=".../game/nes/1942arcade/screenshots/gameShotId,34513/"
        // title="1942 NES Title screen" class="thumbnail-image"
        // style="background-image:url(/images/shots/s/34513-1942-nes-screenshot-title-screen.jpg);"></a>
        // </div>
        Elements divs = getAllByClass(divRow, "thumbnail");
        for (Element div : divs) {
            Element divI = select(div, "div.thumbnail-image-wrapper");
            A a = getA(divI);
            String id = getLastChunk(a).split(",")[1];
            // .../game/nes/1942arcade/screenshots/gameShotId,34513/
            // .../images/shots/l/34513-1942-nes-screenshot-title-screen.jpg
            //System.out.println(a.attr("href"));
            String small = a.attr("style");
            small = small.substring(0, small.length() - 2);
            small = removeHost(small.replace("background-image:url(", ""));

            Element divS = select(div, "div.thumbnail-caption");
            Element divSmall = getByTag(divS, "small");
            String description = divSmall.text();

            MobyImage mobyImage = new MobyImage(id, ROOT, small, description);
            parseGameScreenshotsImage(entry, mobyImage);
            processor.add(new FileEntry(entry.getPlatformId(), ROOT, mobyImage.getSmall(), thisLink));
            processor.add(new FileEntry(entry.getPlatformId(), ROOT, mobyImage.getLarge(), thisLink));
            entry.getScreens().add(mobyImage);
        }
    }

    private void parseGameScreenshotsImage(GameEntry entry, MobyImage mobyImage) throws Exception {

        String thisLink = String.format(GAME_SCREENSHOTS_IMAGE, entry.getPlatformId(), entry.getGameId(), mobyImage.getId());
        HttpExecutor.HttpResponse response = getAndSavePage(thisLink, getGameLink(entry));
        Element container = getContainer(response);

        // <div class="screenshot">
        // <img class="img-responsive" alt="3-D WorldRunner NES Worldrunning between enemies" src="/images/shots/l/54328-3-d-worldrunner-nes-screenshot-worldrunning-between-enemies.png" width="256" height="240" border="0">
        // <h3>Worldrunning between enemies</h3></div>
        Element div = select(container, "div.screenshot");
        Element img = div.child(0);
        assert (img.tagName().equals("img"));
        Element h3 = div.child(1);
        assert (h3.tagName().equals("h3"));

        String large = removeHost(img.attr("src"));
        mobyImage.setLarge(large);
        assert h3.text().equals(mobyImage.getDescription());
    }

    private void parseGameReviews(GameEntry entry) throws Exception {

        if (!entry.isHasReviews()) {
            return;
        }

        HttpExecutor.HttpResponse response = getAndSavePage(String.format(GAME_REVIEWS, entry.getPlatformId(), entry.getGameId()), getGameLink(entry));
        Element container = getContainer(response);
        Element divColMd12 = getByClass(container, "col-md-12");
        // Все метаданные по игре + обложка
        Element coreGameInfo = getById(divColMd12, "floatholder coreGameInfo");
        //Title or BR if no credits
        Element h2Title = getNext(coreGameInfo);
        assert h2Title.text().equals("User Reviews");
        // Table либо нет обзоров
        Element table = getNext(h2Title);
        if (table.tagName().equals("table")) {
            Elements trs = getTrs(table);
            trs.forEach(tr -> {
                Elements tds = getTds(tr);
                A a = getA(tds.get(0));
                // .../game/nes/1942arcade/reviews/reviewerId,1717/
                //String reviewLink = a.href();
                String summary = a.text();
                a = getA(tds.get(1));
                // .../user/sheet/userSheetId,1717/
                String reviewerLink = a.href();
                String reviewerId = getLastChunk(reviewerLink).split(",")[1];
                String reviewer = a.text();
                Element spanOrEm = tds.get(2).child(0);
                String note;
                if (spanOrEm.tagName().equals("em")) {
                    note = spanOrEm.text(); // unrated
                } else {
                    note = getByTag(spanOrEm, "img").attr("alt").replace(" Stars", "");
                }

                users.put(reviewerId, reviewer);
                entry.getUserReviews().add(new UserReview(note, reviewerId, summary));
            });
        } else {
            assert table.text().startsWith("There are no reviews for the ");
        }

        Element next = getNext(table);
        assert next.className().equals("sideBarLinks");
        next = getNext(next);
        assert next.tagName().equals("br");

        h2Title = getNext(next);
        assert h2Title.text().equals("Our Users Say");

        // Table либо нет ревью
        Element divFloatholder = getNext(h2Title);
        if (!divFloatholder.children().isEmpty()) {
            Element table2 = divFloatholder.child(0);
            Elements trs = getTrs(table2);
            trs.subList(1, trs.size() - 1).forEach(tr -> {
                Elements tds = getTds(tr);
                String category = tds.get(0).text();
                //String description = tds.get(1).text(); // How well the game mechanics work and the game plays., ...
                String userScore = tds.get(2).text();
                entry.getNotes().put(category, userScore);
            });
            Elements ths = getThs(table2);
            String overallUserVotes = ths.get(3).text(); // Overall User Score (52 votes)
            String overallUserScore = ths.get(4).text();
            entry.getNotes().put(overallUserVotes, overallUserScore);

        } else {
            assert table.text().startsWith("There are no reviews for the ");
        }

        next = getNext(table);
        assert next.className().equals("sideBarLinks");

        //critic reviews
        // Table либо нет ревью
        Elements divRanks = getAllByClass(divColMd12, "floatholder mobyrank scoresource");
        // <div class="floatholder mobyrank scoresource">
        //  <div class="fl scoreBoxMed scoreHi">80</div>
        //  <div class="source scoreBorderHi"><a href=".../mobyrank/source/sourceId,998/">Tilt </a> (Dec, 1987)</div>
        //  <div class="citation">En conclusion, 1942 est, à tous points de vue, un très bon jeu sur console.</div>
        //  <div class="url"><a target="_blank" href="http://download.abandonware.org/magazines/Tilt/tilt_numero049/TILT%20-%20n%B049%20-%20decembre%201987%20-%20page100%20et%20page101.jpg">read review</a></div>
        //</div>
        for (Element divRank : divRanks) {
            Elements divs = getDivs(divRank);
            String stringScore = divs.get(1).text();
            Integer score = stringScore.isEmpty() ? null : Integer.valueOf(stringScore);
            // Такие следует сохранять
            // <div class="source scoreBorderHi"><a href=".../mobyrank/source/sourceId,2153/">Sharkberg</a> (Sep 09, 2015)</div>
            TextNode textNode = (TextNode) divs.get(2).childNode(1);
            A a = getA(divs.get(2));
            String source = a.text().trim();
            // .../mobyrank/source/sourceId,998/
            String sourceId = getLastChunk(a).split(",")[1];
            String date = textNode.text().trim(); // todo parse
            String citation = divs.get(3).text();

            sources.put(sourceId, source);
            Review review = new Review(score, sourceId, date.substring(1, date.length() - 1), citation);
            entry.getReviews().add(review);

            if (divs.size() == 5) {
                review.setSourceUrl(getA(divs.get(4)).href()); //TODO download/parse/map broken links or read from archive
            }
        }
    }

    private void parseGameCoverArt(GameEntry entry) throws Exception {

        if (!entry.isHasCoverArt()) {
            return;
        }

        String thisLink = String.format(GAME_COVER_ART, entry.getPlatformId(), entry.getGameId());
        HttpExecutor.HttpResponse response = getAndSavePage(thisLink, getGameLink(entry));
        Element container = getContainer(response);

        Element current = select(container, "div.coverHeading");
        if (current == null) {
            // <p>There are no covers for the selected platform.</p>
            return;
        }
        current = current.previousElementSibling();
        // в цикле читать дивы:
        // coverHeading
        // row
        // sideBarLinks
        Covers covers = new Covers();

        do {
            assert current != null;
            current = getNext(current);
            if (current == null || !current.className().equals("coverHeading")) {
                break;
            }
            Element h2 = current.child(0);
            assert h2.tagName().equals("h2");
            assert h2.text().equals(platformsById.get(entry.getPlatformId()).getTitle());
            Elements trs = getTrs(current);
            // Эти параметры на самом деле разные, надо собрать список и мэппить соответственно.
            for (Element tr : trs) {
                Elements tds = getTds(tr);
                assert tds.size() == 3;
                String property = tds.get(0).text();
                assert tds.get(1).text().equals(" : ");
                Elements spans = getAllByTag(tds.get(2), "span");
                List<String> values = spans.stream()
                        .map(span -> ((TextNode) span.childNode(0)).text()).collect(Collectors.toList());
                //System.out.println(property + ": " + String.join(", ", values));
                covers.getProps().put(property, values);
            }

            current = getNext(current);
            assert current.className().equals("row");
            Elements divs = getAllByClass(current, "col-xs-6 col-sm-3 col-md-2");
            for (Element div : divs) {
                // <a href=".../game/nes/3-d-worldrunner/cover-art/gameCoverId,32861/"
                // title="3-D WorldRunner NES Front Cover" class="thumbnail-cover"
                // style="background-image:url(/images/covers/s/32861-3-d-worldrunner-nes-front-cover.jpg);"></a>
                Element a = select(div, "a.thumbnail-cover");
                // .../images/covers/s/17048-1942-nes-front-cover.jpg
                // .../images/covers/l/17048-1942-nes-front-cover.jpg
                String id = getLastChunk(a.attr("href")).split(",")[1];
                String small = a.attr("style");
                small = small.substring(0, small.length() - 2); // ");:
                small = removeHost(small.replace("background-image:url(", ""));

                Element divS = select(div, "div.thumbnail-cover-caption");
                Element p = getByTag(divS, "p");
                //System.out.println(p.text());
                MobyArtImage mobyImage = new MobyArtImage(id, ROOT, small, p.text());
                parseGameCoverArtImage(entry, mobyImage);
                processor.add(new FileEntry(entry.getPlatformId(), ROOT, mobyImage.getSmall(), thisLink));
                processor.add(new FileEntry(entry.getPlatformId(), ROOT, mobyImage.getLarge(), thisLink));
                covers.getImages().add(mobyImage);
            }

            entry.getCovers().add(covers);
            covers = new Covers();
            current = getNext(current);
            assert current.className().equals("sideBarLinks");
        } while (true);
    }

    private void parseGameCoverArtImage(GameEntry entry, MobyArtImage mobyImage) throws Exception {

        String thisLink = String.format(GAME_COVER_ART_IMAGE, entry.getPlatformId(), entry.getGameId(), mobyImage.getId());
        HttpExecutor.HttpResponse response = getAndSavePage(thisLink, getGameLink(entry));
        Element container = getContainer(response);
        Element div = getByClass(container, "col-md-12 col-lg-12");
        // <center><img class="img-responsive" alt="Shenmue Dreamcast Front Cover"
        // src="/images/covers/l/35667-shenmue-dreamcast-front-cover.jpg" width="800" height="681" border="0"></center>
        Element center = select(div, "center");
        Element img = center.child(0);
        assert (img.tagName().equals("img"));
        mobyImage.setLarge(removeHost(img.attr("src")));

        Element table = select(container, "table[summary*=Cover Descriptions]");
        getTrs(table).forEach(tr -> mobyImage.getSummary().put(tr.child(0).text(), tr.child(1).text().replace(": ", "")));
    }

    private void parseGamePromoArt(GameEntry entry) throws Exception {

        if (!entry.isHasPromoArt()) {
            return;
        }

        String thisLink = String.format(GAME_PROMO_ART, entry.getPlatformId(), entry.getGameId());
        HttpExecutor.HttpResponse response = getAndSavePage(thisLink, getGameLink(entry));
        Element container = getContainer(response);

        // в цикле читать дивы:
        // coverHeading
        // row
        // sideBarLinks
        Element current = getByClass(container, "col-md-12 col-lg-12");
        Elements sections = getAllByTag(current, "section");

        // грабить здесь всё, кроме нижнего описания, его брать отсюда:
        // .../game/snes/contra-iii-the-alien-wars/promo/promoImageId,92195/
        //короче, операция получения доп инфо в обоих случаях это просто добор инфы.
        // подумать как это сделать грамотно.
        for (Element section : sections) {// <h2><a href=".../game/snes/contra-iii-the-alien-wars/promo/groupId,52916/">Magazine Advertisements</a></h2>
            Element h2 = getH2(section);
            A a = getA(h2);
            String promoId = getLastChunk(a).split(",")[1];
            String promoGroup = a.text();
            // todo need to parse text, html in future
            String promoSource = (getNext(h2).tagName().equals("p")) ? getNext(h2).html() : "";  // Promo source

            Promo promo = new Promo(promoId, promoGroup, promoSource);

            // ul.thumbnailGallery
            Elements lis = selectUl(section, "ul.thumbnailGallery");

            //  li - эти в цикле
            //    figure
            for (Element li : lis) {
                Element figure = getByTag(li, "figure");
                if (figure != null) { // wrong li from figcaption
                    //      a - <a href=".../game/snes/contra-iii-the-alien-wars/promo/promoImageId,92195/">
                    //       img - screenshot <img alt="Contra III: The Alien Wars Screenshot" src="/images/promo/s/92195-contra-iii-the-alien-wars-screenshot.jpg">
                    a = getA(figure);
                    String promoImageId = getLastChunk(a).split(",")[1];
                    String promoImage = removeHost(a.child(0).attr("src"));
                    //      figcaption
                    //        span - imageTypeName
                    //        [br]
                    //        текст - описание источника. Может быть линк со ссылкой на источник
                    //        <a href=".../mobyrank/source/sourceId,106/">VideoGame...</a>
                    Element figcaption = getByTag(figure, "figcaption");
                    Element span = figcaption.child(0);
                    String imageTypeName = span.text();
                    List<String> sourceDescr = new ArrayList<>();
                    Node node = span.nextSibling();
                    while (node != null) {
                        if (node instanceof TextNode) {
                            sourceDescr.add(((TextNode) node).text());
                        } else {
                            sourceDescr.add(node.outerHtml());
                        }
                        node = getNextNode(node);
                    }

                    PromoImage pi = new PromoImage(promoImageId, ROOT, promoImage, imageTypeName, sourceDescr);

                    parseGamePromoArtImage(entry, pi);

                    processor.add(new FileEntry(entry.getPlatformId(), ROOT, pi.getSmall(), thisLink));
                    processor.add(new FileEntry(entry.getPlatformId(), ROOT, pi.getLarge(), thisLink));
                    processor.add(new FileEntry(entry.getPlatformId(), ROOT, pi.getOriginal(), thisLink));

                    promo.getImages().add(pi);
                }
            }
            entry.getPromos().add(promo);
        }
    }

    private void parseGamePromoArtImage(GameEntry entry, PromoImage promoImage) throws Exception {

        String thisLink = String.format(GAME_PROMO_ART_IMAGE, entry.getPlatformId(), entry.getGameId(), promoImage.getId());
        HttpExecutor.HttpResponse response = getAndSavePage(thisLink, getGameLink(entry));
        Element container = getContainer(response);

        Element figure = select(container, "figure.promoImage");
        //      a - <a href=".../images/promo/original/1465483400-3500966055.jpg"><img alt="1942 Screenshot" src="/images/promo/l/4391-1942-screenshot.jpg" width="321" height="242" border="0"></a>
        //       img - screenshot <img alt="1942 Screenshot" src="/images/promo/l/4391-1942-screenshot.jpg" width="321" height="242" border="0">
        A a = getA(figure);
        Element img = a.child(0);
        assert img.tagName().equals("img");
        String large = removeHost(img.attr("src"));

        promoImage.setLarge(large);
        promoImage.setOriginal(removeHost(a.href()));
        //      figcaption
        //        <p>
        //        <p>
        Element figcaption = getByTag(figure, "figcaption");
        for (int i = 1; i < figcaption.childNodeSize(); i++) {
            Node node = figcaption.childNode(i);
            if (node instanceof TextNode) {
                String text = ((TextNode) node).text();
                if (!promoImage.getSourceDescr().contains(text)) {
                    promoImage.getSourceDescr().add(text);
                }
            } else {
                String text1 = node.outerHtml();
                String text2 = ((Element) node).html();
                if (!promoImage.getSourceDescr().contains(text1) && !promoImage.getSourceDescr().contains(text2)) {
                    promoImage.getSourceDescr().add(text1);
                }
            }
        }
    }

    private void parseGameReleases(GameEntry entry) throws Exception {

        if (!entry.isHasReleases()) {
            return;
        }

        HttpExecutor.HttpResponse response = getAndSavePage(String.format(GAME_RELEASES, entry.getPlatformId(), entry.getGameId()), getGameLink(entry));
        Element container = getContainer(response);
        Element divColMd12 = getByClass(container, "col-md-12");
        // Все метаданные по игре + обложка
        Element coreGameInfo = getById(divColMd12, "floatholder coreGameInfo");

        Element br = getNext(coreGameInfo);
        Element p = getNext(br);
        assert p.text().equals("Release dates, publisher and developer information for this game listed by platform:");

        Element h2Title = getNext(p);
        assert h2Title.text().equals(platformsById.get(entry.getPlatformId()).getTitle());

        // читать по очереди, пока див.
        List<Map<String, String>> releases = new ArrayList<>();
        Element current = getNext(h2Title);
        boolean first = true;
        Map<String, String> release = new LinkedHashMap<>();
        while (current.tagName().equals("div")) {
            if (current.className().equals("floatholder")) {
                if (!first) {
                    releases.add(release);
                    release = new LinkedHashMap<>();
                    first = true;
                }
                Element divFl = current.child(0); // <div style="width: 10em;" class="fl">Published by</div>
                String key = divFl.text();
                Element a = getNext(divFl); //<a href=".../company/capcom-co-ltd">Capcom Co., Ltd.</a>
                String companyId = getLastChunk(a.attr("href"));
                String companyName = a.text();
                companies.put(companyId, companyName);
                release.put(key, companyId);
            } else {
                first = false;
                for (Element child : current.children()) {// <div class="floatholder relInfo">
                    //<div class="relInfoTitle">Country</div><div class="relInfoDetails"><span><img alt="Japan flag" src="/images/i/36/42/21792.gif" width="18" height="12" border="0"> Japan</span></div></div>
                    Element relInfoTitle = child.child(0);
                    String key = relInfoTitle.text();
                    Element relInfoDetails = child.child(1);
                    String value = relInfoDetails.html();
                    if (key.equals("Country") || key.equals("Countries")) {
                        value = relInfoDetails.text();
                    }
                    release.put(key, value);
                }
            }
            current = getNext(current);
        }
        releases.add(release);
        entry.setReleases(releases);
    }

    // Одинаковое значение для всех платформ!!!
    private void parseGameTrivia(GameEntry entry) throws Exception {

        if (!entry.isHasTrivia()) {
            return;
        }

        HttpExecutor.HttpResponse response = getAndSavePage(String.format(GAME_TRIVIA, entry.getPlatformId(), entry.getGameId()), getGameLink(entry));
        Element container = getContainer(response);
        Element divColMd12 = getByClass(container, "col-md-12 col-lg-12");

        // Trivia
        Node el = select(divColMd12, "h2:contains(Trivia)");
        if (el == null) {
            return;
        }
        String key = null;
        List<String> values = new ArrayList<>();
        while (true) {
            el = getNextNode(el);
            if (el == null) { // .../game/nes/ballblazer/trivia - contributed внутри ul
                break;
            } else if (el instanceof TextNode) {
                if (!((TextNode) el).text().trim().isEmpty()) {
                    values.add(((TextNode) el).text());
                }
            } else {
                Element ell = (Element) el;
                if (ell.tagName().equals("h3")) {
                    if (key != null) {
                        entry.getTrivia().put(key, values);
                    }
                    key = ell.text();
                    values = new ArrayList<>();
                } else if (ell.tagName().equals("div") && ell.className().equals("right")) {
                    break;
                } else if (ell.tagName().equals("small")) {
                    break;
                } else if (ell.className().equals("sideBarLinks")) {
                    break;
                } else {
                    values.add(ell.outerHtml());
                }
            }
        }
        // .../game/nes/bible-buffet/trivia
        // содержит только текст без h3 :(
        if (key == null) {
            //TODO automatically parse these values, get key from them
            key = "unnamed";
        }
        entry.getTrivia().put(key, values);
    }

    private void parseGameHints(GameEntry entry) throws Exception {

        if (!entry.isHasHints()) {
            return;
        }

        HttpExecutor.HttpResponse response = getAndSavePage(String.format(GAME_HINTS, entry.getPlatformId(), entry.getGameId()), getGameLink(entry));
        Element table = select(getContainer(response), "table[summary]");

        if (table == null) {
            return;
        }

        Elements trs = getTrs(table);

        Map<String, Map<String, List<String>>> allHints = new LinkedHashMap<>();
        Map<String, List<String>> hint = new LinkedHashMap<>();
        String key = "";
        boolean isGroup = true;

        for (Element tr : trs) {
            if (tr.className().equals("color3")) {
                if (!isGroup) {
                    allHints.put(key, hint);
                    hint = new LinkedHashMap<>();
                }
                isGroup = true;
                key = tr.child(0).child(0).text().split(" - ")[0]; // <td colspan="5"><b>General Hints/Tips - SNES</b></td>
            } else {
                isGroup = false;
                // <a href=".../game/snes/killer-instinct/hints/hintId,1409/">Continue</a>
                A a = getA(tr.child(1));
                String hintId = getLastChunk(a.href()).split(",")[1];
                String title = a.text();
                List<String> lines = parseGameHintsPage(entry, hintId);
                hint.put(title, lines);
            }
        }
        allHints.put(key, hint);
        entry.setHints(allHints);
    }

    private List<String> parseGameHintsPage(GameEntry entry, String hintId) throws Exception {

        HttpExecutor.HttpResponse response = getAndSavePage(String.format(GAME_HINTS_PAGE, entry.getPlatformId(), entry.getGameId(), hintId), getGameLink(entry));
        Element table = select(getContainer(response), "table[summary]");

        if (table == null) {
            throw new RuntimeException("No hints???");
        }

        Elements trs = getTrs(table);

        List<String> lines = new ArrayList<>();

        // <tr><td colspan="2" class="sbR sbL sbB"><p>Just when you...</p></td></tr>
        for (int i = 0; i < trs.get(1).childNodeSize(); i++) {

            Node node = trs.get(1).childNode(i);
            if (node instanceof TextNode) {
                lines.add(((TextNode) node).text());
            } else {
                lines.add(((Element) node).html());
            }
        }

        return lines;
    }

    // Одинаковое значение для всех платформ!!!
    private void parseGameAds(GameEntry entry) throws Exception {

        if (!entry.isHasAdBlurb()) {
            return;
        }

        HttpExecutor.HttpResponse response = getAndSavePage(String.format(GAME_ADS, entry.getPlatformId(), entry.getGameId()), getGameLink(entry));
        Element container = getContainer(response);
        Element divColMd12 = getByClass(container, "col-md-12 col-lg-12");

        // Advertising Blurbs
        Element current = select(divColMd12, "h2:contains(Advertising Blurbs)");
        while (true) {
            Element next = getNext(current);

            if (next == null) {
                break;
            }

            // бывает что юзеры вручную пытаются указать тег B, надо убирать его
            if (next.childNodeSize() != 1) {
                //System.out.println("!!!!!!!!!!!!!!!" + next);
                next.unwrap();
                next = getNext(current);
                if (next == null) {
                    break;
                }
            }

            current = next;

            // b, must have only one text node
            // Back of Plastic Case - C64 (AU):
            // www.nintendo.com - Wii U (US):
            // Back of Box (Canada):
            // Back of Box - SNES (UK):
            String title = current.html();

            current = getNext(current);

            List<String> list = (current == null) ? new ArrayList<>() : current.childNodes().stream().map(node -> {
                if (node instanceof TextNode) {
                    return ((TextNode) node).text().trim();
                } else {
                    Element cont = (Element) node;
                    Elements els = cont.select("p:contains(Contributed by)");
                    if (els.size() > 0) {
                        els.forEach(Node::remove);
                    }
                    if (cont.text().contains("Contributed by")) {
                        return "";
                    }
                    return cont.outerHtml();
                }
            }).filter(s -> s.length() >= 2).collect(Collectors.toList());
            //remove trail <br>s
            while (list.size() > 0 && list.get(list.size() - 1).trim().equals("<br>")) {
                list.remove(list.size() - 1);
            }
            String html = String.join("\n", list);
            if (html.isEmpty()) { // в случае ошибок на странице нижние данные слепляются
                entry.getAdBlurbs().put("", title);
            } else {
                entry.getAdBlurbs().put(title, html);
            }
            if (current == null) {
                break;
            }
        }
    }

    private void parseGameSpecs(GameEntry entry) throws Exception {

        if (!entry.isHasSpecs()) {
            return;
        }

        HttpExecutor.HttpResponse response = getAndSavePage(String.format(GAME_SPECS, entry.getPlatformId(), entry.getGameId()), getGameLink(entry));
        Element table = select(getContainer(response), "table.techInfo");

        Elements trs = getTrs(table);

        if (!trs.get(1).hasAttr("valign")) {
            return;
        }

        for (int i = 1; i < trs.size(); i++) {
            Element tr = trs.get(i);
            String key = tr.child(0).text();
            List<String> values = tr.child(1).childNodes().stream().map(node -> {
                if (node instanceof TextNode) {
                    return ((TextNode) node).text();
                } else if (((Element) node).tagName().equals("a")) {
                    return ((Element) node).text();
                } else {
                    return "";
                }
            }).filter(s -> s.length() > 2).collect(Collectors.toList());
            entry.getSpecs().put(key, values);
        }
    }

    private void parseGameRatings(GameEntry entry) throws Exception {

        if (!entry.isHasSpecs()) {
            return;
        }

        HttpExecutor.HttpResponse response = getAndSavePage(String.format(GAME_RATING_SYSTEMS, entry.getPlatformId(), entry.getGameId()), getGameLink(entry));
        Element container = getContainer(response);
        Element divColMd12 = getByClass(container, "col-md-12 col-lg-12");

        Element table = select(divColMd12, "table[summary*=Descriptor]");

        Elements trs = getTrs(table);

        for (Element tr : trs) {
            String key = tr.child(0).text();
            List<A> as = getAs(tr.child(2));
            if (as.isEmpty()) {
                entry.getRatingSystems().put(key, Collections.singletonList(tr.child(2).text()));
            } else {
                // <a href=".../attribute/sheet/attributeId,91/">Kids to Adults</a>
                // <td><img alt="Teen" src="/images/i/15/32/3375282.jpeg" width="15" height="20" border="0">&nbsp;<a href=".../attribute/sheet/attributeId,92/">Teen</a> (Descriptors: <a href=".../attribute/sheet/attributeId,692/">Animated Blood and Gore</a>, <a href=".../attribute/sheet/attributeId,690/">Animated Violence</a>) </td>
                List<String> values = as.stream().map(a -> {
                    String id = getLastChunk(a).split(",")[1];
                    attributes.put(id, a.text());
                    return id;
                }).collect(Collectors.toList());
                entry.getRatingSystems().put(key, values);
            }
        }
    }

    @Override
    public List<GameEntry> parseGamesList(String platformId) throws Exception {

        List<GameEntry> games = new ArrayList<>();

        HttpExecutor.HttpResponse response = getAndSavePage(String.format(GAMES_PAGES, platformId, 0), ROOT);
        Document doc = Jsoup.parse(response.getBody());

        // <td class="mobHeaderPage" width="33%">Viewing Page 1 of 56</td>
        // Может и не быть если игр меньше 25
        int pagesCount = 1;
        Element td = doc.getElementsByClass("mobHeaderPage").first();
        if (td != null) {
            String[] chunks = Objects.requireNonNull(td).text().split(" ");
            pagesCount = Integer.parseInt(chunks[chunks.length - 1]);
            assert pagesCount > 0;
        }

        System.out.println("Pages count: " + pagesCount);

        // <td class="mobHeaderItems" width="34%">(items 1-25 of 1397)</td>
        td = doc.getElementsByClass("mobHeaderItems").first();
        if (td != null) {
            String[] chunks = td.text().split(" ");
            String last = chunks[chunks.length - 1];
            int gamesCount = Integer.parseInt(last.substring(0, last.length() - 1));
            assert gamesCount > 0;

            System.out.println("Games count: " + gamesCount);
        }

        //parse #0 page
        parseGamesList(platformId, games, response.getBody());

        //parse in cycle
        // 1 - 0
        // 2 - 25
        // 3 - 50
        for (int i = 1; i < pagesCount; i++) {
            response = getAndSavePage(String.format(GAMES_PAGES, platformId, i * 25), String.format(GAMES_PAGES, platformId, 0));
            parseGamesList(platformId, games, response.getBody());
        }

        return games;
    }

    private void parseGamesList(String platformId, List<GameEntry> games, String html) {

        Document doc = Jsoup.parse(html);

        // <td class="mobHeaderPage" width="33%">Viewing Page 1 of 56</td>
        Element table = getByTag(getById(doc, "mof_object_list"), "tbody");
        getTrs(table).forEach(tr -> {
            // Game Title	        Year	Publisher	        Genre
            // Bad Street Brawler	1989	Mattel Electronics	Action
            A a = getA(getTds(tr).get(0));
            games.add(new GameEntry(platformId, getLastChunk(a.href()), a.text()));
        });
    }

    @Override
    public List<Platform> parsePlatformsList() throws Exception {
        List<Platform> platforms = new ArrayList<>();

        HttpExecutor.HttpResponse response = getAndSavePage(PLATFORMS, ROOT);
        Document doc = Jsoup.parse(response.getBody());
        getAs(getByClass(doc, "browseTable")).forEach(a -> platforms.add(new Platform(getLastChunk(a), a.text(), 0, 0, null)));

        return platforms;
    }

    private String getGameMainReferrer(String gameId) {
        String formattedGameName = gameId.replace("-", " ").replace("_", " ").trim().replace(" ", "+");
        return String.format(GAME_MAIN_REFERRER, formattedGameName);
    }

    private String getGameLink(GameEntry entry) {
        return String.format(GAME_MAIN, entry.getPlatformId(), entry.getGameId());
    }

    private Element getContainer(HttpExecutor.HttpResponse response) {
        Document doc = Jsoup.parse(response.getBody());
        Element container = Objects.requireNonNull(doc.getElementById("wrapper")).getElementsByClass("container").first();
        assert container != null;
        return container;
    }

    private List<String> getSheets(Element div) {
        return getAs(div).stream().map(a -> {
            String sheet = getLastChunk(a);
            sheets.put(sheet, a.text());
            return sheet;
        }).collect(Collectors.toList());
    }

    private String removeHost(String uri) {
        if (uri.startsWith(ROOT)) {
            return uri.replace(ROOT, "");
        } else {
            return uri;
        }
    }

    public List<CreditsNode> parseCredits(Element td) {
        Map<String, String> idNames = new LinkedHashMap<>();
        // <a href=".../developer/sheet/view/developerId,45166/">Ayako Mori</a>
        String text = td.childNodes().stream().map(node -> {

            //System.out.println("NNode: " + node.outerHtml());

            if (node instanceof TextNode) {
                return ((TextNode) node).text();
            } else {
                A a = new A(node);

                // Cloudflare email protection
                // <a href="/cdn-cgi/l/email-protection" class="__cf_email__" data-cfemail="cbb9a4a4bfa28bb9bbace5b8aeacaae5a8a4">[email&#160;protected]</a>
                String email = a.attr("data-cfemail");
                if (!email.isEmpty()) {
                    return decodeCfEmail(email);
                } else {
                    String chunk = getLastChunk(a);
                    if (!chunk.contains(",")) {
                        return a.text();
                    } else {
                        String id = getLastChunk(a).split(",")[1];
                        idNames.put(id, a.text().trim());
                        return id;
                    }
                }
            }
        }).collect(Collectors.joining());

        developers.putAll(idNames);

        List<String> devs = Arrays.stream(text.split(",")).map(String::trim).collect(Collectors.toList());

        return devs.stream().map(idOrName -> {

            // Предполагаю, что после ссылки всегда идёт скобка, текста быть не может.
            List<String> str = Arrays.stream(idOrName.replace("(", "|(|").replace(")", "|)|")
                    .replace("[", "|[|").replace("]", "|]|").replace("||", "|")/*.replace("/", "")*/.split("\\|"))
                    .map(String::trim).filter(StringUtils::isNotBlank).collect(Collectors.toList());

            //System.out.println("Array: " + str);
            if (str.isEmpty()) {
                str.add("");
            }

            String name = idNames.get(str.get(0)); // Kunio Aoi
            CreditsNode node = (name == null) ? new CreditsNode("text", str.get(0)) : new CreditsNode("link", str.get(0), name);
            parseNode(node, str, 1);
            return node;

        }).collect(Collectors.toList());
    }

    private void parseNode(CreditsNode node, List<String> str, int index) {

        //System.out.println("Node : " + node);
        //System.out.println(index == str.size() ? index : index + ": " + str.get(index));

        if (index == str.size()) {
            return;
        }

        switch (str.get(index)) {
            case "(":
                parseNode(new CreditsNode("round", node, str.get(++index)), str, ++index);
                break;
            case "[":
                parseNode(new CreditsNode("square", node, str.get(++index)), str, ++index);
                break;
            case ")":
            case "]":
                parseNode(node.getParent() == null ? node : node.getParent(), str, ++index);
                break;
            default:
                parseNode(new CreditsNode("text", node, str.get(index)), str, ++index);
        }
    }

    public String decodeCfEmail(String encodedString) {
        int n, i;
        StringBuilder email = new StringBuilder();
        int r = Integer.valueOf(encodedString.substring(0, 2), 16);
        for (n = 2; n < encodedString.length() - 1; n += 2) {
            i = Integer.valueOf(encodedString.substring(n, n + 2), 16) ^ r;
            email.append((char) i);
        }
        return email.toString();
    }

    @Override
    public FilesProcessor getProcessor() {
        return processor;
    }

    @Override
    public String getGamePage(String platformId, String gameId) {
        return String.format(GAME_MAIN, platformId, gameId);
    }

    @Override
    public Executor getExecutor() {
        return executor;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public boolean isPrependPlatformId() {
        return prependPlatformId;
    }
}
