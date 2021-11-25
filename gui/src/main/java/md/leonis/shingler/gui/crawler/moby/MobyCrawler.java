package md.leonis.shingler.gui.crawler.moby;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import md.leonis.shingler.gui.crawler.moby.model.*;
import md.leonis.shingler.utils.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

//TODO читать историю
// https://www.mobygames.com/stats/recent_entries
// https://www.mobygames.com/stats/recent_entries/offset,0/so,2d/
// https://www.mobygames.com/stats/recent_modifications
// https://www.mobygames.com/stats/recent_reviews

//TODO нужен UI где можно видеть прогресс, очереди.

//TODO распараллелить обработку игр и сохранение картинок.
//Пока код тестируется, есть смысл параллельно сохранять картинки.
public class MobyCrawler {

    private static final String HOST = "https://www.mobygames.com";

    public static final String GAME_MAIN_REFERRER = "https://www.mobygames.com/search/quick?q=%s";

    public static final String PLATFORMS = "https://www.mobygames.com/browse/games/full,1/";
    public static final String GAMES_PAGES = "https://www.mobygames.com/browse/games/%s/offset,%s/so,0a/list-games/";
    public static final String GAME_MAIN = "https://www.mobygames.com/game/%s/%s";
    public static final String GAME_CREDITS = "https://www.mobygames.com/game/%s/%s/credits";
    public static final String GAME_SCREENSHOTS = "https://www.mobygames.com/game/%s/%s/screenshots";
    public static final String GAME_SCREENSHOTS_IMAGE = "https://www.mobygames.com/game/%s/%s/screenshots/gameShotId,%s";
    // About mobyrank: https://www.mobygames.com/info/mobyrank?nof=1
    //TODO тут ссылки на некоторые источники сдохли.
    //Некоторые совсем, некоторые переехали.
    //Нужно уметь мэппить
    public static final String GAME_REVIEWS = "https://www.mobygames.com/game/%s/%s/mobyrank";
    public static final String GAME_COVER_ART = "https://www.mobygames.com/game/%s/%s/cover-art";
    public static final String GAME_COVER_ART_IMAGE = "https://www.mobygames.com/game/%s/%s/cover-art/gameCoverId,%s";
    public static final String GAME_PROMO_ART = "https://www.mobygames.com/game/%s/%s/promo";
    public static final String GAME_PROMO_ART_IMAGE = "https://www.mobygames.com/game/%s/%s/promo/promoImageId,%s";
    public static final String GAME_RELEASES = "https://www.mobygames.com/game/%s/%s/release-info";
    public static final String GAME_TRIVIA = "https://www.mobygames.com/game/%s/%s/trivia";
    public static final String GAME_HINTS = "https://www.mobygames.com/game/%s/%s/hints";
    public static final String GAME_HINTS_PAGE = "https://www.mobygames.com/game/%s/%s/hints/hintId,%s";
    public static final String GAME_SPECS = "https://www.mobygames.com/game/%s/%s/techinfo";
    public static final String GAME_ADS = "https://www.mobygames.com/game/%s/%s/adblurbs";
    public static final String GAME_RATING_SYSTEMS = "https://www.mobygames.com/game/%s/%s/rating-systems";

    //TODO sources (журналы, сайты, откуда брались обзоры и ревью.
    // ><a href="https://www.mobygames.com/mobyrank/source/sourceId,998/">Tilt </a> (Dec, 1987)</div>
    // там список игр, которые оценивал источник с оценкой и датой


    //TODO proxies list
    public static final HttpExecutor executor = HttpExecutor.directInstance();

    private static final Queue<Map.Entry<String, String>> httpQueue = new ConcurrentLinkedQueue<>();

    public static Map<String, String> platforms;
    public static Map<String, String> games;
    public static Map<String, String> companies;
    public static Map<String, String> sheets;
    public static Map<String, String> gameGroups;
    public static Map<String, String> developers;
    public static Map<String, String> sources;
    public static Map<String, String> users;
    public static Map<String, String> attributes;
    public static List<String> brokenImages;

    private static class HttpProcessor implements Runnable {

        boolean cancelled = false;
        boolean canStop = false;
        boolean inWork = false;
        volatile Exception exception = null;
        volatile boolean finished = false;

        @Override
        public void run() {
            while (!cancelled) {
                try {
                    if (httpQueue.isEmpty()) {
                        if (canStop && !inWork) {
                            stop();
                        }
                        Thread.sleep(50);
                    } else {
                        // TODO process
                        inWork = true;
                        Map.Entry<String, String> file = httpQueue.poll();
                        if (file == null) {
                            System.out.println("===============File is null");
                            Thread.sleep(50);
                        } else {
                            try {
                                executor.saveFile(file.getKey(), file.getValue());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                        inWork = false;
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    exception = e;
                    cancelled = true;
                }
            }
            finished = true;
        }

        public void stop() {
            cancelled = true;
        }
    }

    public static void main(String[] args) throws Exception {

        /*//TODO get list of systems
        Map<String, String> platforms = parseSystemsList();
        System.out.println(platforms);

        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new File("platforms.json"), platforms);
        */

        preload();


        //два контрибьютора + не закрытые теги.
        MobyEntry entry = new MobyEntry("nes", "10-yard-fight_");
        parseGameMain(entry);
        parseGameCredits(entry);
        /*parseGameScreenshots(entry);
        parseGameReviews(entry);
        parseGameCoverArt(entry);
        parseGamePromoArt(entry);
        parseGameReleases(entry);
        parseGameTrivia(entry);
        parseGameHints(entry);
        parseGameSpecs(entry);
        parseGameAds(entry);
        parseGameRatings(entry);*/

        System.out.println(entry);

        //Thread.sleep(300000);



        List<HttpProcessor> processors = new ArrayList<>();
        processors.add(new HttpProcessor());
        processors.add(new HttpProcessor());
        processors.add(new HttpProcessor());
        processors.add(new HttpProcessor());

        ExecutorService service = Executors.newCachedThreadPool();
        processors.forEach(service::execute);

        HttpExecutor.validate = false;

        //String[] platforms = new String[]{"nes", "sg-1000", "sega-master-system", "game-gear", "sega-32x"};

        String[] platforms = new String[]{"genesis", "snes", "gameboy", "gameboy-color", "gameboy-advance", "game-com"};

        for (String platformId : platforms) {

            Map<String, String> games = parseGamesListToc(platformId);
            save(String.format("games-%s.json", platformId), games);

            List<MobyEntry> mobyEntries = new ArrayList<>();

            int i = 0;
            for (String gameId : games.keySet()) {
                MobyEntry mobyEntry = new MobyEntry(platformId, gameId);

                try {
                    parseGameMain(mobyEntry);
                    // если ошибочная игра - не парсить.
                    if (mobyEntry.gameId().isEmpty()) {
                        System.out.println("Ignore this game");
                    } else {
                        parseGameCredits(mobyEntry);
                        parseGameScreenshots(mobyEntry);
                        parseGameReviews(mobyEntry);
                        parseGameCoverArt(mobyEntry);
                        parseGamePromoArt(mobyEntry);
                        parseGameReleases(mobyEntry);
                        parseGameTrivia(mobyEntry);
                        parseGameHints(mobyEntry);
                        parseGameSpecs(mobyEntry);
                        parseGameAds(mobyEntry);
                        parseGameRatings(mobyEntry);

                        mobyEntries.add(mobyEntry);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                stopAllProcessorsOnError(service, processors);

                if (++i == 50) {
                    save();
                    save(String.format("moby-entry-%s.json", platformId), mobyEntries);
                    // TODO save position, return to position later
                    i = 0;
                }
            }

            save();
            save(String.format("moby-entry-%s.json", platformId), mobyEntries);
        }

        processors.forEach(p -> p.canStop = true);

        while (processors.stream().filter(p -> p.finished).count() < processors.size()) {
            stopAllProcessorsOnError(service, processors);
            Thread.sleep(200);
        }
    }

    private static void stopAllProcessorsOnError(ExecutorService service, List<HttpProcessor> processors) throws InterruptedException {
        Exception exception = processors.stream().filter(p -> p.exception != null).findFirst().map(p -> p.exception).orElse(null);
        if (exception != null) {
            processors.forEach(p -> p.cancelled = true);
            service.awaitTermination(2, TimeUnit.SECONDS);
            service.shutdown();
            throw new RuntimeException(exception);
        }
    }

    private static void save() throws IOException {
        System.out.println("Save in progress...");
        save("companies.json", companies);
        save("sheets.json", sheets);
        save("gameGroups.json", gameGroups);
        save("developers.json", developers);
        save("sources.json", sources);
        save("users.json", users);
        save("attributes.json", attributes);
        save("brokenImages.json", brokenImages);
        System.out.println("Saved.");
    }

    private static void save(String fileName, Object object) throws IOException {

        System.out.printf("Save %s in progress...%n", fileName);
        IOUtils.backupFile(Paths.get(fileName));

        String result = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(object);
        result = result.replace("&nbsp;", " ");

        Files.write(Paths.get(fileName), result.getBytes());
        System.out.println("Saved.");
    }

    private static void parseGameMain(MobyEntry entry) throws Exception {

        HttpExecutor.HttpResponse response = executor.getPage(getGameLink(entry), getGameMainReferrer(entry.gameId()));

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

        assert gameName.equals(games.get(entry.gameId()));
        assert platformName.equals(games.get(entry.platformId()));

        // Main  Credits  Screenshots  Reviews  Cover Art  Promo Art  Releases  Trivia  Hints  Specs  Ad Blurb  Rating Systems  Buy/Trade
        Elements lis = selectUl(rightPanelHeader, "ul.nav-tabs");
        assert lis.size() == 14;

        // Иногда по ошибке игра отображается для системы, но фактически она ей не принадлежит
        // Тогда моби показывает групповую инфу, которая нам не нужна.
        if (!getA(lis.get(0)).href().equals(getGameLink(entry))) {
            System.out.println("Ignore this game and delete: " + games.get(entry.getGameId()));
            entry.setGameId("");
            return;
        }

        assert getA(lis.get(0)).text().equals("Main");      // https://www.mobygames.com/game/%s/%s
        assert getA(lis.get(1)).text().equals("Credits");   //https://www.mobygames.com/game/%s/%s/credits
        assert getA(lis.get(2)).text().equals("Screenshots"); //https://www.mobygames.com/game/%s/%s/screenshots
        assert getA(lis.get(3)).text().equals("Reviews");   //https://www.mobygames.com/game/%s/%s/mobyrank
        assert getA(lis.get(4)).text().equals("Cover Art"); //https://www.mobygames.com/game/%s/%s/cover-art
        assert getA(lis.get(5)).text().equals("Promo Art"); //https://www.mobygames.com/game/%s/%s/promo
        assert getA(lis.get(6)).text().equals("Releases");  //https://www.mobygames.com/game/%s/%s/release-info
        assert getA(lis.get(7)).text().equals("Trivia");    //https://www.mobygames.com/game/%s/%s/trivia
        assert getA(lis.get(8)).text().equals("Hints");     //https://www.mobygames.com/game/%s/%s/hints
        assert getA(lis.get(9)).text().equals("Specs");     //https://www.mobygames.com/game/%s/%s/techinfo
        assert getA(lis.get(10)).text().equals("Ad Blurb"); //https://www.mobygames.com/game/%s/%s/adblurbs
        assert getA(lis.get(11)).text().equals("Rating Systems");//https://www.mobygames.com/game/%s/%s/rating-systems
        assert lis.get(12).text().isEmpty();
        assert getA(lis.get(13)).text().equals("Buy/Trade");//https://www.mobygames.com/game/%s/%s/buy-trade

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
                    // <a href="https://www.mobygames.com/company/capcom-usa-inc">Capcom&nbsp;U.S.A.,&nbsp;Inc.</a>
                    entry.setPublishers(getSheets(divs.get(i + 1)));
                    break;
                case "Developed by":
                    // <a href="https://www.mobygames.com/company/capcom-co-ltd">Capcom&nbsp;Co.,&nbsp;Ltd.</a>
                    entry.setDevelopers(getSheets(divs.get(i + 1)));
                    break;
                case "Released":
                    // <a href="https://www.mobygames.com/game/%s/%s/release-info">Nov, 1986</a>
                    getAs(divs.get(i + 1)).forEach(a -> entry.getDates().add(a.text()));
                    break;
                case "Official Site":
                    // <a href="https://www.mobygames.com/company/capcom-co-ltd">Capcom&nbsp;Co.,&nbsp;Ltd.</a>
                    entry.setOfficialSites(getSheets(divs.get(i + 1)));
                    break;
                case "Also For":
                    // <a href="https://www.mobygames.com/game/cpc/1942arcade">Amstrad CPC</a>
                    // ,
                    // <a href="https://www.mobygames.com/game/android/1942arcade">Android</a>
                    // ,
                    // ...
                    // |
                    // <a href="https://www.mobygames.com/game/1942arcade">Combined&nbsp;View</a>
                    List<A> alsoFor = getAs(divs.get(i + 1));
                    alsoFor.subList(0, alsoFor.size() - 1).forEach(a -> entry.getAlsoFor().add(getPreLastChunk(a.href())));
                    break;
                case "Platform":
                case "Platforms":
                    // <a href="https://www.mobygames.com/game/cpc/1942arcade">Amstrad CPC</a>
                    // ,
                    // <a href="https://www.mobygames.com/game/android/1942arcade">Android</a>
                    // ,
                    // ...
                    // |
                    // <a href="https://www.mobygames.com/game/1942arcade">Combined&nbsp;View</a>
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
                    // <a href="https://www.mobygames.com/attribute/sheet/attributeId,91/">Kids to Adults</a>
                    entry.setEsbrRatings(getSheets(divs.get(i + 1)));
                    break;
                case "Genre":
                    // <a href="https://www.mobygames.com/genre/sheet/action/">Action</a>
                    entry.setGenres(getSheets(divs.get(i + 1)));
                    break;
                case "Perspective":
                    // <a href="https://www.mobygames.com/genre/sheet/top-down/">Top-down</a>
                    entry.setPerspectives(getSheets(divs.get(i + 1)));
                    break;
                case "Visual":
                    // <a href="https://www.mobygames.com/genre/sheet/2d-scrolling/">2D&nbsp;scrolling</a>
                    entry.setVisuals(getSheets(divs.get(i + 1)));
                    break;
                case "Pacing":
                    // <a href="https://www.mobygames.com/genre/sheet/turn-based/">Turn-based</a>
                    entry.setPacings(getSheets(divs.get(i + 1)));
                    break;
                case "Art":
                    // <a href="https://www.mobygames.com/genre/sheet/anime-manga/">Anime&nbsp;/&nbsp;manga</a>
                    entry.setArts(getSheets(divs.get(i + 1)));
                    break;
                case "Gameplay":
                    // <a href="https://www.mobygames.com/genre/sheet/arcade_/">Arcade</a>,
                    // <a href="https://www.mobygames.com/genre/sheet/shooter/">Shooter</a>
                    entry.setGameplays(getSheets(divs.get(i + 1)));
                    break;
                case "Educational":
                    // <a href="https://www.mobygames.com/genre/sheet/math-logic/">Math&nbsp;/&nbsp;logic</a>
                    entry.setEducationals(getSheets(divs.get(i + 1)));
                    break;
                case "Interface":
                    // <a href="https://www.mobygames.com/genre/sheet/direct_control/">Direct&nbsp;control</a>
                    entry.setInterfaces(getSheets(divs.get(i + 1)));
                    break;
                case "Vehicular":
                    // <a href="https://www.mobygames.com/genre/sheet/automobile/">Automobile</a>
                    entry.setVehiculars(getSheets(divs.get(i + 1)));
                    break;
                case "Setting":
                    // <a href="https://www.mobygames.com/genre/sheet/world-war-ii/">World&nbsp;War&nbsp;II</a>
                    entry.setSettings(getSheets(divs.get(i + 1)));
                    break;
                case "Sport":
                    // <a href="https://www.mobygames.com/genre/sheet/football-american/">Football&nbsp;(American)</a>
                    entry.setSports(getSheets(divs.get(i + 1)));
                    break;
                case "Narrative":
                    // <a href="https://www.mobygames.com/genre/sheet/football-american/">Football&nbsp;(American)</a>
                    entry.setNarratives(getSheets(divs.get(i + 1)));
                    break;
                case "Special Edition":
                    // <a href="https://www.mobygames.com/genre/sheet/physical-extras/">Physical&nbsp;extras</a>
                    entry.setSpecialEditions(getSheets(divs.get(i + 1)));
                    break;
                case "Add-on":
                    // <a href="https://www.mobygames.com/genre/sheet/map_level/">Map&nbsp;/&nbsp;level</a>
                    entry.setAddons(getSheets(divs.get(i + 1)));
                    break;
                case "Misc":
                    // <a href="https://www.mobygames.com/genre/sheet/regional-differences/">Regional&nbsp;differences</a>
                    entry.setMiscs(getSheets(divs.get(i + 1)));
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
            } else {
                Element element = (Element) node;
                assert element != null;
                if (element.className().equals("sideBarLinks")) {
                    break;
                }
                //если в описании не закрытые теги, то захватывается и код ниже,
                //не удаётся определить где конец. необходимо искать его в детях и
                //если есть, то фильтровать, сохранять и прерывать цикл.
                int index = -1;
                for (int i = 0; i < element.childNodes().size(); i++) {
                    if (element.childNode(i) instanceof Element) {
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
            // https://www.mobygames.com/game-group/mario-games
            gameGroups.put(getLastChunk(a), a.text());
            entry.getGameGroup().add(getLastChunk(a));
        });
    }

    private static List<String> getSheets(Element div) {
        return getAs(div).stream().map(a -> {
            String sheet = getLastChunk(a);
            sheets.put(sheet, a.text());
            return sheet;
        }).collect(Collectors.toList());
    }

    private static void parseGameCredits(MobyEntry entry) throws Exception {

        if (!entry.hasCredits()) {
            return;
        }

        HttpExecutor.HttpResponse response = executor.getPage(String.format(GAME_CREDITS, entry.platformId(), entry.gameId()), getGameLink(entry));
        Element container = getContainer(response);
        Element divColMd12 = getByClass(container, "col-md-12");
        // Все метаданные по игре + обложка
        Element coreGameInfo = getById(divColMd12, "floatholder coreGameInfo");
        //Title or BR if no credits
        Element h2Title = getNext(coreGameInfo);

        if (h2Title.tagName().equals("h2")) {
            assert h2Title.text().equals(games.get(entry.gameId()) + " Credits");
            //Div со всеми кредитами
            Element div = getNext(h2Title);
            // X people
            assert ((TextNode) getNextNode(div)).text().endsWith(" people");
            // Table
            Element table = getTable(div);
            assert table.attr("summary").equals("List of Credits");

            Map<String, List<Credits>> credits = new LinkedHashMap<>();
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
                        nextGroup = nextGroup + UUID.randomUUID().toString().toCharArray()[0];
                    }
                    if (!credits.isEmpty()) {
                        entry.getCredits().put(group, credits);
                    }
                    group = nextGroup;
                    credits = new LinkedHashMap<>();

                } else if (tds.size() == 2) {
                    assert tr.hasClass("crln");
                    String role = tds.get(0).text();
                    List<Credits> ids = parseCredits(tds.get(1));
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

    private static void parseGameScreenshots(MobyEntry entry) throws Exception {

        if (!entry.hasScreenshots()) {
            return;
        }

        String thisLink = String.format(GAME_SCREENSHOTS, entry.platformId(), entry.gameId());
        HttpExecutor.HttpResponse response = executor.getPage(thisLink, getGameLink(entry));
        Element container = getContainer(response);
        Element divColMd12 = getByClass(container, "col-md-12");

        Element h2 = select(divColMd12, "h2:contains(User Screenshots)");
        Element h3 = getNext(h2);
        assert h3.text().equals(games.get(entry.gameId()) + " version");
        Element divRow = getNext(h3);
        assert divRow.tagName().equals("div");

        // <div class="thumbnail-image-wrapper">
        // <a href="https://www.mobygames.com/game/nes/1942arcade/screenshots/gameShotId,34513/"
        // title="1942 NES Title screen" class="thumbnail-image"
        // style="background-image:url(/images/shots/s/34513-1942-nes-screenshot-title-screen.jpg);"></a>
        // </div>
        Elements divs = getAllByClass(divRow, "thumbnail");
        for (Element div : divs) {
            Element divI = select(div, "div.thumbnail-image-wrapper");
            A a = getA(divI);
            String id = getLastChunk(a).split(",")[1];
            // https://www.mobygames.com/game/nes/1942arcade/screenshots/gameShotId,34513/
            // https://www.mobygames.com/images/shots/l/34513-1942-nes-screenshot-title-screen.jpg
            //System.out.println(a.attr("href"));
            String style = a.attr("style");
            style = style.substring(0, style.length() - 2);
            style = style.replace("background-image:url(", "");

            Element divS = select(div, "div.thumbnail-caption");
            Element small = getByTag(divS, "small");
            String description = small.text();

            MobyImage mobyImage = new MobyImage(id, style, description);
            parseGameScreenshotsImage(entry, mobyImage);
            httpQueue.add(new AbstractMap.SimpleEntry<>(HOST + mobyImage.getSmall(), thisLink));
            httpQueue.add(new AbstractMap.SimpleEntry<>(HOST + mobyImage.getLarge(), thisLink));
            entry.getScreens().add(mobyImage);
        }
    }

    private static void parseGameScreenshotsImage(MobyEntry entry, MobyImage mobyImage) throws Exception {

        String thisLink = String.format(GAME_SCREENSHOTS_IMAGE, entry.platformId(), entry.gameId(), mobyImage.getId());
        HttpExecutor.HttpResponse response = executor.getPage(thisLink, getGameLink(entry));
        Element container = getContainer(response);

        // <div class="screenshot">
        // <img class="img-responsive" alt="3-D WorldRunner NES Worldrunning between enemies" src="/images/shots/l/54328-3-d-worldrunner-nes-screenshot-worldrunning-between-enemies.png" width="256" height="240" border="0">
        // <h3>Worldrunning between enemies</h3></div>
        Element div = select(container, "div.screenshot");
        Element img = div.child(0);
        assert (img.tagName().equals("img"));
        Element h3 = div.child(1);
        assert (h3.tagName().equals("h3"));

        mobyImage.setLarge(img.attr("src"));
        assert h3.text().equals(mobyImage.getDescription());
    }

    private static void parseGameReviews(MobyEntry entry) throws Exception {

        if (!entry.hasReviews()) {
            return;
        }

        HttpExecutor.HttpResponse response = executor.getPage(String.format(GAME_REVIEWS, entry.platformId(), entry.gameId()), getGameLink(entry));
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
                // https://www.mobygames.com/game/nes/1942arcade/reviews/reviewerId,1717/
                //String reviewLink = a.href();
                String summary = a.text();
                a = getA(tds.get(1));
                // https://www.mobygames.com/user/sheet/userSheetId,1717/
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
        //  <div class="source scoreBorderHi"><a href="https://www.mobygames.com/mobyrank/source/sourceId,998/">Tilt </a> (Dec, 1987)</div>
        //  <div class="citation">En conclusion, 1942 est, à tous points de vue, un très bon jeu sur console.</div>
        //  <div class="url"><a target="_blank" href="http://download.abandonware.org/magazines/Tilt/tilt_numero049/TILT%20-%20n%B049%20-%20decembre%201987%20-%20page100%20et%20page101.jpg">read review</a></div>
        //</div>
        for (Element divRank : divRanks) {
            Elements divs = getDivs(divRank);
            String stringScore = divs.get(1).text();
            Integer score = stringScore.isEmpty() ? null : Integer.valueOf(stringScore);
            // Такие следует сохранять
            // <div class="source scoreBorderHi"><a href="https://www.mobygames.com/mobyrank/source/sourceId,2153/">Sharkberg</a> (Sep 09, 2015)</div>
            TextNode textNode = (TextNode) divs.get(2).childNode(1);
            A a = getA(divs.get(2));
            String source = a.text().trim();
            // https://www.mobygames.com/mobyrank/source/sourceId,998/
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

    private static void parseGameCoverArt(MobyEntry entry) throws Exception {

        if (!entry.hasCoverArt()) {
            return;
        }

        String thisLink = String.format(GAME_COVER_ART, entry.platformId(), entry.gameId());
        HttpExecutor.HttpResponse response = executor.getPage(thisLink, getGameLink(entry));
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
            assert h2.text().equals(platforms.get(entry.platformId()));
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
                // <a href="https://www.mobygames.com/game/nes/3-d-worldrunner/cover-art/gameCoverId,32861/"
                // title="3-D WorldRunner NES Front Cover" class="thumbnail-cover"
                // style="background-image:url(/images/covers/s/32861-3-d-worldrunner-nes-front-cover.jpg);"></a>
                Element a = select(div, "a.thumbnail-cover");
                // https://www.mobygames.com/images/covers/s/17048-1942-nes-front-cover.jpg
                // https://www.mobygames.com/images/covers/l/17048-1942-nes-front-cover.jpg
                String id = getLastChunk(a.attr("href")).split(",")[1];
                String style = a.attr("style");
                style = style.substring(0, style.length() - 2); // ");:
                style = style.replace("background-image:url(", "");

                Element divS = select(div, "div.thumbnail-cover-caption");
                Element p = getByTag(divS, "p");
                //System.out.println(p.text());
                MobyArtImage mobyImage = new MobyArtImage(id, style, p.text());
                parseGameCoverArtImage(entry, mobyImage);
                httpQueue.add(new AbstractMap.SimpleEntry<>(HOST + mobyImage.getSmall(), thisLink));
                httpQueue.add(new AbstractMap.SimpleEntry<>(HOST + mobyImage.getLarge(), thisLink));
                covers.getImages().add(mobyImage);
            }

            entry.getCovers().add(covers);
            covers = new Covers();
            current = getNext(current);
            assert current.className().equals("sideBarLinks");
        } while (true);
    }

    private static void parseGameCoverArtImage(MobyEntry entry, MobyArtImage mobyImage) throws Exception {

        String thisLink = String.format(GAME_COVER_ART_IMAGE, entry.platformId(), entry.gameId(), mobyImage.getId());
        HttpExecutor.HttpResponse response = executor.getPage(thisLink, getGameLink(entry));
        Element container = getContainer(response);
        Element div = getByClass(container, "col-md-12 col-lg-12");
        // <center><img class="img-responsive" alt="Shenmue Dreamcast Front Cover"
        // src="/images/covers/l/35667-shenmue-dreamcast-front-cover.jpg" width="800" height="681" border="0"></center>
        Element center = select(div, "center");
        Element img = center.child(0);
        assert (img.tagName().equals("img"));
        mobyImage.setLarge(img.attr("src"));

        Element table = select(container, "table[summary*=Cover Descriptions]");
        getTrs(table).forEach(tr -> mobyImage.getSummary().put(tr.child(0).text(), tr.child(1).text().replace(": ", "")));
    }

    private static void parseGamePromoArt(MobyEntry entry) throws Exception {

        if (!entry.hasPromoArt()) {
            return;
        }

        String thisLink = String.format(GAME_PROMO_ART, entry.platformId(), entry.gameId());
        HttpExecutor.HttpResponse response = executor.getPage(thisLink, getGameLink(entry));
        Element container = getContainer(response);

        // в цикле читать дивы:
        // coverHeading
        // row
        // sideBarLinks
        Element current = getByClass(container, "col-md-12 col-lg-12");
        Elements sections = getAllByTag(current, "section");

        // грабить здесь всё, кроме нижнего описания, его брать отсюда:
        // https://www.mobygames.com/game/snes/contra-iii-the-alien-wars/promo/promoImageId,92195/
        //короче, операция получения доп инфо в обоих случаях это просто добор инфы.
        // подумать как это сделать грамотно.
        for (Element section : sections) {// <h2><a href="https://www.mobygames.com/game/snes/contra-iii-the-alien-wars/promo/groupId,52916/">Magazine Advertisements</a></h2>
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
                //      a - <a href="https://www.mobygames.com/game/snes/contra-iii-the-alien-wars/promo/promoImageId,92195/">
                //       img - screenshot <img alt="Contra III: The Alien Wars Screenshot" src="/images/promo/s/92195-contra-iii-the-alien-wars-screenshot.jpg">
                a = getA(figure);
                String promoImageId = getLastChunk(a).split(",")[1];
                String promoImage = a.child(0).attr("src");
                //      figcaption
                //        span - imageTypeName
                //        [br]
                //        текст - описание источника. Может быть линк со ссылкой на источник
                //        <a href="https://www.mobygames.com/mobyrank/source/sourceId,106/">VideoGame...</a>
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

                PromoImage pi = new PromoImage(promoImageId, promoImage, imageTypeName, sourceDescr);

                parseGamePromoArtImage(entry, pi);

                httpQueue.add(new AbstractMap.SimpleEntry<>(HOST + pi.getSmall(), thisLink));
                httpQueue.add(new AbstractMap.SimpleEntry<>(HOST + pi.getLarge(), thisLink));
                httpQueue.add(new AbstractMap.SimpleEntry<>(pi.getOriginal(), thisLink));

                promo.getImages().add(pi);
            }
            entry.getPromos().add(promo);
        }
    }

    private static void parseGamePromoArtImage(MobyEntry entry, PromoImage promoImage) throws Exception {

        String thisLink = String.format(GAME_PROMO_ART_IMAGE, entry.platformId(), entry.gameId(), promoImage.getId());
        HttpExecutor.HttpResponse response = executor.getPage(thisLink, getGameLink(entry));
        Element container = getContainer(response);

        Element figure = select(container, "figure.promoImage");
        //      a - <a href="https://www.mobygames.com/images/promo/original/1465483400-3500966055.jpg"><img alt="1942 Screenshot" src="/images/promo/l/4391-1942-screenshot.jpg" width="321" height="242" border="0"></a>
        //       img - screenshot <img alt="1942 Screenshot" src="/images/promo/l/4391-1942-screenshot.jpg" width="321" height="242" border="0">
        A a = getA(figure);
        Element img = a.child(0);
        assert img.tagName().equals("img");
        String large = img.attr("src");

        promoImage.setLarge(large);
        promoImage.setOriginal(a.href());
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

    private static void parseGameReleases(MobyEntry entry) throws Exception {

        if (!entry.hasReleases()) {
            return;
        }

        HttpExecutor.HttpResponse response = executor.getPage(String.format(GAME_RELEASES, entry.platformId(), entry.gameId()), getGameLink(entry));
        Element container = getContainer(response);
        Element divColMd12 = getByClass(container, "col-md-12");
        // Все метаданные по игре + обложка
        Element coreGameInfo = getById(divColMd12, "floatholder coreGameInfo");

        Element br = getNext(coreGameInfo);
        Element p = getNext(br);
        assert p.text().equals("Release dates, publisher and developer information for this game listed by platform:");

        Element h2Title = getNext(p);
        assert h2Title.text().equals(platforms.get(entry.platformId()));

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
                Element a = getNext(divFl); //<a href="https://www.mobygames.com/company/capcom-co-ltd">Capcom Co., Ltd.</a>
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
    private static void parseGameTrivia(MobyEntry entry) throws Exception {

        if (!entry.hasTrivia()) {
            return;
        }

        HttpExecutor.HttpResponse response = executor.getPage(String.format(GAME_TRIVIA, entry.platformId(), entry.gameId()), getGameLink(entry));
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
            if (el == null) { // https://www.mobygames.com/game/nes/ballblazer/trivia - contributed внутри ul
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
        // https://www.mobygames.com/game/nes/bible-buffet/trivia
        // содержит только текст без h3 :(
        if (key == null) {
            //TODO automatically parse these values, get key from them
            key = "unnamed";
        }
        entry.getTrivia().put(key, values);
    }

    private static void parseGameHints(MobyEntry entry) throws Exception {

        if (!entry.hasHints()) {
            return;
        }

        HttpExecutor.HttpResponse response = executor.getPage(String.format(GAME_HINTS, entry.platformId(), entry.gameId()), getGameLink(entry));
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
                // <a href="https://www.mobygames.com/game/snes/killer-instinct/hints/hintId,1409/">Continue</a>
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

    private static List<String> parseGameHintsPage(MobyEntry entry, String hintId) throws Exception {

        HttpExecutor.HttpResponse response = executor.getPage(String.format(GAME_HINTS_PAGE, entry.platformId(), entry.gameId(), hintId), getGameLink(entry));
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
    private static void parseGameAds(MobyEntry entry) throws Exception {

        if (!entry.hasAdBlurb()) {
            return;
        }

        HttpExecutor.HttpResponse response = executor.getPage(String.format(GAME_ADS, entry.platformId(), entry.gameId()), getGameLink(entry));
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

    private static void parseGameSpecs(MobyEntry entry) throws Exception {

        if (!entry.hasSpecs()) {
            return;
        }

        HttpExecutor.HttpResponse response = executor.getPage(String.format(GAME_SPECS, entry.platformId(), entry.gameId()), getGameLink(entry));
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

    private static void parseGameRatings(MobyEntry entry) throws Exception {

        if (!entry.hasSpecs()) {
            return;
        }

        HttpExecutor.HttpResponse response = executor.getPage(String.format(GAME_RATING_SYSTEMS, entry.platformId(), entry.gameId()), getGameLink(entry));
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
                // <a href="https://www.mobygames.com/attribute/sheet/attributeId,91/">Kids to Adults</a>
                // <td><img alt="Teen" src="/images/i/15/32/3375282.jpeg" width="15" height="20" border="0">&nbsp;<a href="https://www.mobygames.com/attribute/sheet/attributeId,92/">Teen</a> (Descriptors: <a href="https://www.mobygames.com/attribute/sheet/attributeId,692/">Animated Blood and Gore</a>, <a href="https://www.mobygames.com/attribute/sheet/attributeId,690/">Animated Violence</a>) </td>
                List<String> values = as.stream().map(a -> {
                    String id = getLastChunk(a).split(",")[1];
                    attributes.put(id, a.text());
                    return id;
                }).collect(Collectors.toList());
                entry.getRatingSystems().put(key, values);
            }
        }
    }

    private static Map<String, String> parseGamesListToc(String platformId) throws Exception {

        Map<String, String> games = Collections.synchronizedMap(new LinkedHashMap<>());

        HttpExecutor.HttpResponse response = executor.getPage(String.format(GAMES_PAGES, platformId, 0), "https://www.mobygames.com");
        Document doc = Jsoup.parse(response.getBody());

        // <td class="mobHeaderPage" width="33%">Viewing Page 1 of 56</td>
        String[] chunks = getByClass(doc, "mobHeaderPage").text().split(" ");
        int pagesCount = Integer.parseInt(chunks[chunks.length - 1]);
        assert pagesCount > 0;

        System.out.println("Pages count: " + pagesCount);

        // <td class="mobHeaderItems" width="34%">(items 1-25 of 1397)</td>
        chunks = getByClass(doc, "mobHeaderItems").text().split(" ");
        String last = chunks[chunks.length - 1];
        int gamesCount = Integer.parseInt(last.substring(0, last.length() - 1));
        assert gamesCount > 0;

        System.out.println("Games count: " + gamesCount);

        //parse #0 page
        parseGamesList(games, response.getBody());

        //parse in cycle
        // 1 - 0
        // 2 - 25
        // 3 - 50
        for (int i = 1; i < pagesCount; i++) {
            response = executor.getPage(String.format(GAMES_PAGES, platformId, i * 25), String.format(GAMES_PAGES, platformId, 0));
            parseGamesList(games, response.getBody());
        }

        assert games.size() == gamesCount;

        return games;
    }

    private static void parseGamesList(Map<String, String> games, String html) {

        Document doc = Jsoup.parse(html);

        // <td class="mobHeaderPage" width="33%">Viewing Page 1 of 56</td>
        Element table = getByTag(getById(doc, "mof_object_list"), "tbody");
        getTrs(table).forEach(tr -> {
            // Game Title	        Year	Publisher	        Genre
            // Bad Street Brawler	1989	Mattel Electronics	Action
            A a = getA(getTds(tr).get(0));
            games.put(getLastChunk(a.href()), a.text());
        });
    }

    private static Map<String, String> parseSystemsList() throws Exception {

        Map<String, String> systems = new LinkedHashMap<>();

        HttpExecutor.HttpResponse response = executor.getPage(PLATFORMS, "https://www.mobygames.com");
        Document doc = Jsoup.parse(response.getBody());
        getAs(getByClass(doc, "browseTable")).forEach(a -> systems.put(getLastChunk(a), a.text()));

        return systems;
    }

    private static void preload() {

        platforms = loadMap("platforms.json");
        games = loadMap("games-nes.json");
        companies = loadMap("companies.json");
        sheets = loadMap("sheets.json");
        gameGroups = loadMap("gameGroups.json");
        developers = loadMap("developers.json");
        sources = loadMap("sources.json");
        users = loadMap("users.json");
        attributes = loadMap("attributes.json");

        brokenImages = loadList("brokenImages.json");
    }

    private static Map<String, String> loadMap(String fileName) {
        try {
            return new ObjectMapper().readValue(new File(fileName), new TypeReference<Map<String, String>>() {
            });
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private static List<String> loadList(String fileName) {
        try {
            return new ObjectMapper().readValue(new File(fileName), new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static String getGameMainReferrer(String gameId) {
        String formattedGameName = gameId.replace("-", " ").replace("_", " ").trim().replace(" ", "+");
        return String.format(GAME_MAIN_REFERRER, formattedGameName);
    }

    private static String getGameLink(MobyEntry entry) {
        return String.format(GAME_MAIN, entry.platformId(), entry.gameId());
    }

    private static Element getContainer(HttpExecutor.HttpResponse response) {

        Document doc = Jsoup.parse(response.getBody());
        Element container = Objects.requireNonNull(doc.getElementById("wrapper")).getElementsByClass("container").first();
        assert container != null;
        return container;
    }

    private static Element getByClass(Element element, String className) {

        Element child = element.getElementsByClass(className).first();
        assert child != null;
        return child;
    }

    private static Elements getAllByClass(Element element, String className) {
        return element.getElementsByClass(className);
    }

    private static Element getById(Element element, String id) {
        Element result = element.getElementById(id);
        assert result != null;
        return result;
    }

    private static Element getByTag(Element element, String tag) {
        Element result = element.getElementsByTag(tag).first();
        assert result != null;
        return result;
    }

    private static Elements getAllByTag(Element element, String className) {
        return element.getElementsByTag(className);
    }

    private static class A extends Element {

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

    private static List<A> getAs(Element element) {
        return element.getElementsByTag("a").stream().map(A::new).collect(Collectors.toList());
    }

    private static A getA(Element element) {
        return new A(Objects.requireNonNull(element.getElementsByTag("a").first()));
    }

    private static Elements getDivs(Element element) {
        return element.getElementsByTag("div");
    }

    private static Element getH2(Element element) {
        return Objects.requireNonNull(element.getElementsByTag("h2").first());
    }

    private static Element getTable(Element element) {
        return Objects.requireNonNull(element.getElementsByTag("table").first());
    }

    private static Elements getTrs(Element element) {
        return element.getElementsByTag("tr");
    }

    private static Elements getTds(Element element) {
        return element.getElementsByTag("td");
    }

    private static Elements getThs(Element element) {
        return element.getElementsByTag("th");
    }

    private static Element getNext(Element element) {
        Element el = element.nextElementSibling();
        assert el != null;
        return el;
    }

    private static Node getNextNode(Node element) {
        Node node = element.nextSibling();
        assert node != null;
        return node;
    }

    private static Element select(Element element, String selector) {
        Element result = element.selectFirst(selector);
        assert result != null;
        return result;
    }

    private static Elements selectUl(Element element, String selector) {
        Element ul = select(element, selector);
        return ul.getElementsByTag("li");
    }

    private static Elements selectNextUl(Element element, String selector) {
        Element ul = select(element, selector).nextElementSibling();
        assert ul != null;
        return ul.getElementsByTag("li");
    }

    private static Elements selectNextUlOrEmpty(Element element, String selector) {
        Element el = select(element, selector);
        return el == null ? new Elements() : Objects.requireNonNull(el.nextElementSibling()).getElementsByTag("li");
    }


    private static String getLastChunk(A a) {
        return getLastChunk(a.href());
    }

    private static String getPreLastChunk(A a) {
        return getPreLastChunk(a.href());
    }

    private static String getLastChunk(String url) {
        String[] chunks = url.split("/");
        return chunks[chunks.length - 1];
    }

    private static String getPreLastChunk(String url) {
        String[] chunks = url.split("/");
        return chunks[chunks.length - 2];
    }

    static List<Credits> parseCredits(Element td) {

        Map<String, String> idNames = new LinkedHashMap<>();
        // <a href="https://www.mobygames.com/developer/sheet/view/developerId,45166/">Ayako Mori</a>
        String text = td.childNodes().stream().map(node -> {

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
                    String id = getLastChunk(a).split(",")[1];
                    idNames.put(id, a.text().trim());
                    return id;
                }
            }
        }).collect(Collectors.joining());

        developers.putAll(idNames);

        List<String> devs = Arrays.stream(text.split(",")).map(String::trim).collect(Collectors.toList());

        return devs.stream().map(idOrName -> {

            List<String> str = Arrays.stream(idOrName.replaceAll("[\\[\\]()]", "!|").split("!\\|"))
                    .map(String::trim).filter(StringUtils::isNotBlank).collect(Collectors.toList());

            switch (str.size()) {
                case 1:
                    return createCredit(idNames, idOrName, null, null, null);
                case 2:
                    return createCredit(idNames, str.get(0), str.get(1), null, null);
                case 3:
                    return createCredit(idNames, str.get(0), str.get(1), str.get(2), null);
                case 4:
                    return createCredit(idNames, str.get(0), str.get(1), str.get(2), str.get(3));
                default: {
                    throw new RuntimeException("Wrong credits: " + text);
                }
            }
        }).collect(Collectors.toList());
    }

    //TODO сложные объекты по-любому потребуют ручной обработки
    private static Credits createCredit(Map<String, String> idNames, String id, String origName, String group, String yup) {
        String name = idNames.get(id); // Kunio Aoi
        if (name == null) {
            return new Credits(null, id, origName, group, yup);
        } else {
            return new Credits(id, name, origName, group, yup);
        }
    }

    static String decodeCfEmail(String encodedString) {
        int n, i;
        StringBuilder email = new StringBuilder();
        int r = Integer.valueOf(encodedString.substring(0, 2), 16);
        for (n = 2; n < encodedString.length() - 1; n += 2) {
            i = Integer.valueOf(encodedString.substring(n, n + 2), 16) ^ r;
            email.append((char) i);
        }
        return email.toString();
    }
}
