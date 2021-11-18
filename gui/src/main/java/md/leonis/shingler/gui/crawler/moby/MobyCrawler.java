package md.leonis.shingler.gui.crawler.moby;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class MobyCrawler {

    public static final String PLATFORMS = "https://www.mobygames.com/browse/games/full,1/";
    public static final String GAMES_PAGES = "https://www.mobygames.com/browse/games/%s/offset,%s/so,0a/list-games/";
    public static final String GAME_MAIN = "https://www.mobygames.com/game/%s/%s";


    //TODO proxies list
    public static final HttpExecutor executor = HttpExecutor.directInstance();

    public static void main(String[] args) throws Exception {

        /*//TODO get list of systems
        Map<String, String> platforms = parseSystemsList();
        System.out.println(platforms);

        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new File("platforms.json"), platforms);

        //TODO in cycle
        Map<String, String> games = parseGamesList("nes");
        System.out.println(games);

        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new File("games-nes.json"), games);*/

        Map<String, String> platforms = new ObjectMapper().readValue(new File("platforms.json"), new TypeReference<Map<String, String>>() {
        });
        Map<String, String> games = new ObjectMapper().readValue(new File("games-nes.json"), new TypeReference<Map<String, String>>() {
        });

        //parseGameMain("nes", "1942arcade");
        parseGameMain("nes", "super-mario-bros-2");
    }

    private static Map<String, String> parseGameMain(String platform, String gameId) throws Exception {

        HttpExecutor.HttpResponse response = executor.getResponse(String.format(GAME_MAIN, platform, gameId));

        //TODO process exceptions -> retry list

        Document doc = Jsoup.parse(response.getBody());

        //TODO save raw html + date, next time read offline

        Element container = Objects.requireNonNull(doc.getElementById("wrapper")).getElementsByClass("container").first();

        assert container != null;
        // 1942 (NES)
        // Main  Credits  Screenshots  Reviews  Cover Art  Promo Art  Releases  Trivia  Hints  Specs  Ad Blurb  Rating Systems  Buy/Trade
        Element rightPanelHeader = container.getElementsByClass("rightPanelHeader").first();

        assert rightPanelHeader != null;
        Element niceHeaderTitle = rightPanelHeader.getElementsByClass("niceHeaderTitle").first();
        assert niceHeaderTitle != null;
        Elements niceHeaderTitleA = niceHeaderTitle.getElementsByTag("a");

        // Здесь так же можно получить полное название игры и платформы
        assert getLastChunk(niceHeaderTitleA.get(0).attr("href")).equals(gameId);
        assert getLastChunk(niceHeaderTitleA.get(1).attr("href")).equals(platform);

        String gameName = getLastChunk(niceHeaderTitleA.get(0).text());
        String platformName = getLastChunk(niceHeaderTitleA.get(1).text());

        // Main  Credits  Screenshots  Reviews  Cover Art  Promo Art  Releases  Trivia  Hints  Specs  Ad Blurb  Rating Systems  Buy/Trade
        Element ul = rightPanelHeader.getElementsByTag("ul").first();
        assert ul != null;
        Elements lis = ul.getElementsByTag("li");
        assert lis.size() == 14;

        assert lis.get(0).getElementsByTag("a").text().equals("Main");      //https://www.mobygames.com/game/nes/1942arcade/credits
        assert lis.get(1).getElementsByTag("a").text().equals("Credits");   //https://www.mobygames.com/game/nes/1942arcade/screenshots
        assert lis.get(2).getElementsByTag("a").text().equals("Screenshots"); //https://www.mobygames.com/game/nes/1942arcade/mobyrank
        assert lis.get(3).getElementsByTag("a").text().equals("Reviews");   //https://www.mobygames.com/game/nes/1942arcade/mobyrank
        assert lis.get(4).getElementsByTag("a").text().equals("Cover Art"); //https://www.mobygames.com/game/nes/1942arcade/cover-art
        assert lis.get(5).getElementsByTag("a").text().equals("Promo Art"); //https://www.mobygames.com/game/nes/1942arcade/promo
        assert lis.get(6).getElementsByTag("a").text().equals("Releases");  //https://www.mobygames.com/game/nes/1942arcade/release-info
        assert lis.get(7).getElementsByTag("a").text().equals("Trivia");    //https://www.mobygames.com/game/nes/1942arcade/trivia
        assert lis.get(8).getElementsByTag("a").text().equals("Hints");     //https://www.mobygames.com/game/nes/1942arcade/hints
        assert lis.get(9).getElementsByTag("a").text().equals("Specs");     //https://www.mobygames.com/game/nes/1942arcade/techinfo
        assert lis.get(10).getElementsByTag("a").text().equals("Ad Blurb"); //https://www.mobygames.com/game/nes/1942arcade/adblurbs
        assert lis.get(11).getElementsByTag("a").text().equals("Rating Systems");//https://www.mobygames.com/game/nes/1942arcade/rating-systems
        assert lis.get(12).text().equals("");
        assert lis.get(13).getElementsByTag("a").text().equals("Buy/Trade");//https://www.mobygames.com/game/nes/1942arcade/buy-trade

        if (lis.get(0).hasClass("disabled")) {
            System.out.println("Main is disabled");
        }
        if (lis.get(1).hasClass("disabled")) {
            System.out.println("Credits is disabled");
        }
        if (lis.get(2).hasClass("disabled")) {
            System.out.println("Screenshots is disabled");
        }
        if (lis.get(3).hasClass("disabled")) {
            System.out.println("Reviews is disabled");
        }
        if (lis.get(4).hasClass("disabled")) {
            System.out.println("Cover Art is disabled");
        }
        if (lis.get(5).hasClass("disabled")) {
            System.out.println("Promo Art is disabled");
        }
        if (lis.get(6).hasClass("disabled")) {
            System.out.println("Releases is disabled");
        }
        if (lis.get(7).hasClass("disabled")) {
            System.out.println("Trivia is disabled");
        }
        if (lis.get(8).hasClass("disabled")) {
            System.out.println("Hints is disabled");
        }
        if (lis.get(9).hasClass("disabled")) {
            System.out.println("Specs is disabled");
        }
        if (lis.get(10).hasClass("disabled")) {
            System.out.println("Ad Blurb is disabled");
        }
        if (lis.get(11).hasClass("disabled")) {
            System.out.println("Rating Systems is disabled");
        }
        if (lis.get(13).hasClass("disabled")) {
            System.out.println("Buy/Trade is disabled");
        }

        Element divColMd8 = container.getElementsByClass("col-md-8").first();
        assert divColMd8 != null;
        // Все метаданные по игре + обложка
        Element coreGameInfo = divColMd8.getElementById("floatholder coreGameInfo");
        // Обложка + кол-во картинок
        Element coreGameCover = divColMd8.getElementById("coreGameCover");
        // Не хочу это парсить, поскольку всё то же самое есть на странице Cover Art.

        // Second column
        assert coreGameInfo != null;
        Element coreGameRelease = coreGameInfo.getElementById("coreGameRelease");

        assert coreGameRelease != null;
        Elements divs = coreGameRelease.getElementsByTag("div");
        assert divs.size() == 8;
        assert divs.get(0).text().equals("Published by");
        assert divs.get(2).text().equals("Developed by");
        assert divs.get(4).text().equals("Released");
        assert divs.get(6).text().equals("Also For");

        //TODO read all a's, parse
        // <a href="https://www.mobygames.com/company/capcom-usa-inc">Capcom&nbsp;U.S.A.,&nbsp;Inc.</a>
        divs.get(1).getElementsByTag("a");
        // <a href="https://www.mobygames.com/company/capcom-co-ltd">Capcom&nbsp;Co.,&nbsp;Ltd.</a>
        divs.get(3).getElementsByTag("a");
        // <a href="https://www.mobygames.com/game/nes/1942arcade/release-info">Nov, 1986</a>
        divs.get(5).getElementsByTag("a");
        // <a href="https://www.mobygames.com/game/cpc/1942arcade">Amstrad CPC</a>
        // ,
        // <a href="https://www.mobygames.com/game/android/1942arcade">Android</a>
        // ,
        // ...
        // |
        // <a href="https://www.mobygames.com/game/1942arcade">Combined&nbsp;View</a>
        divs.get(7).getElementsByTag("a");

        // Third column
        Element coreGameGenre = coreGameInfo.getElementById("coreGameGenre");
        assert coreGameGenre != null;
        divs = coreGameGenre.getElementsByTag("div");
        assert divs.size() == 10;
        assert divs.get(0).text().equals("Genre");
        assert divs.get(2).text().equals("Perspective");
        assert divs.get(4).text().equals("Visual");
        assert divs.get(6).text().equals("Gameplay");
        assert divs.get(8).text().equals("Setting");

        //TODO read all a's, parse
        // <a href="https://www.mobygames.com/genre/sheet/action/">Action</a>
        divs.get(1).getElementsByTag("a");
        // <a href="https://www.mobygames.com/genre/sheet/top-down/">Top-down</a>
        divs.get(3).getElementsByTag("a");
        // <a href="https://www.mobygames.com/genre/sheet/2d-scrolling/">2D&nbsp;scrolling</a>
        divs.get(5).getElementsByTag("a");
        // <a href="https://www.mobygames.com/genre/sheet/arcade_/">Arcade</a>,
        // <a href="https://www.mobygames.com/genre/sheet/shooter/">Shooter</a>
        divs.get(7).getElementsByTag("a");
        // <a href="https://www.mobygames.com/genre/sheet/world-war-ii/">World&nbsp;War&nbsp;II</a>
        divs.get(9).getElementsByTag("a");

        // Description
        Element h2description = divColMd8.child(4);
        assert h2description.text().equals("Description");

        Element iDescription = divColMd8.child(5);
        assert iDescription.tag().equals(Tag.valueOf("i"));
        String textStart = iDescription.text();

        String description = divColMd8.textNodes().get(1).text();
        assert description.length() > 8;

        // Skip screenshots, promo images, trailer


        // Alternate Titles
        Element at = divColMd8.select("h2:contains(Alternate Titles)").get(0);
        ul = at.nextElementSibling();
        assert ul != null;
        lis = ul.getElementsByTag("li");

        lis.forEach(li -> {
            System.out.println(((TextNode) li.childNode(0)).text() + ((Element) li.childNode(1)).text());
        });


        // Part of the Following Groups
        Element potfg = divColMd8.select("h2:contains(Part of the Following Groups)").get(0);
        ul = potfg.nextElementSibling();
        assert ul != null;
        lis = ul.getElementsByTag("li");

        lis.forEach(li -> {
            Element element = li.child(0);
            System.out.println(element.text() + element.attr("href"));
        });


        // Trivia
        Node el = divColMd8.select("h2:contains(Trivia)").get(0);
        while (true) {
            el = el.nextSibling();
            assert el != null;
            if (el instanceof TextNode) {
                System.out.print(el);
            } else {
                Element ell = (Element) el;
                if (ell.tagName().equals("h3")) {
                    System.out.println();
                    System.out.println("!!!" + ell.text());
                } else if (ell.tagName().equals("small")) {
                    break;
                } else if (ell.className().equals("sideBarLinks")) {
                    break;
                } else {
                    System.out.print(ell);
                }
            }
        }


        return null; //TODO
    }

    private static Map<String, String> parseGamesToc(String systemId) throws Exception {

        Map<String, String> games = new LinkedHashMap<>();

        HttpExecutor.HttpResponse response = executor.getResponse(String.format(GAMES_PAGES, systemId, 0));

        //TODO process exceptions -> retry list

        Document doc = Jsoup.parse(response.getBody());

        // <td class="mobHeaderPage" width="33%">Viewing Page 1 of 56</td>
        String[] chunks = Objects.requireNonNull(doc.getElementsByClass("mobHeaderPage").first()).text().split(" ");
        int pagesCount = Integer.parseInt(chunks[chunks.length - 1]);
        assert pagesCount > 0;

        System.out.println("Pages count: " + pagesCount);

        // <td class="mobHeaderItems" width="34%">(items 1-25 of 1397)</td>
        chunks = Objects.requireNonNull(doc.getElementsByClass("mobHeaderItems").first()).text().split(" ");
        String last = chunks[chunks.length - 1];
        int gamesCount = Integer.parseInt(last.substring(0, last.length() - 1));
        assert gamesCount > 0;

        System.out.println("Games count: " + gamesCount);

        /*Objects.requireNonNull(doc.getElementsByClass("browseTable").first()).getElementsByTag("a").forEach(a -> {
            games.put(parseSystemUrl(a.attr("href")), a.text());
        });*/

        //TODO parse #0 page
        parseGamesList(games, response.getBody());

        //TODO parse in cycle
        // 1 - 0
        // 2 - 25
        // 3 - 50
        for (int i = 1; i < pagesCount; i++) {
            response = executor.getResponse(String.format(GAMES_PAGES, systemId, i * 25));
            parseGamesList(games, response.getBody());
        }

        assert games.size() == gamesCount;

        return games;
    }

    private static void parseGamesList(Map<String, String> games, String html) {

        Document doc = Jsoup.parse(html);

        // <td class="mobHeaderPage" width="33%">Viewing Page 1 of 56</td>
        Element table = Objects.requireNonNull(doc.getElementById("mof_object_list")).getElementsByTag("tbody").first();
        assert table != null;
        table.getElementsByTag("tr").forEach(e -> {
            // Game Title	        Year	Publisher	        Genre
            // Bad Street Brawler	1989	Mattel Electronics	Action
            List<Element> a = e.getElementsByTag("td").stream().map(t -> t.getElementsByTag("a").first()).collect(Collectors.toList());
            /*System.out.println("Game Title:" + getLastChunk(a.get(0).attr("href")));
            System.out.println("Game Title:" + a.get(0).text());*/
            games.put(getLastChunk(a.get(0).attr("href")), a.get(0).text());
            /*System.out.println("Game Title:" + a.get(0));
            System.out.println("Year:" + a.get(1));
            System.out.println("Publisher:" + a.get(2));
            System.out.println("Genre:" + a.get(3));*/
        });
    }

    private static Map<String, String> parseSystemsList() throws Exception {

        Map<String, String> systems = new LinkedHashMap<>();

        HttpExecutor.HttpResponse response = executor.getResponse(PLATFORMS);

        //TODO process exceptions -> retry list

        Document doc = Jsoup.parse(response.getBody());
        Objects.requireNonNull(doc.getElementsByClass("browseTable").first()).getElementsByTag("a").forEach(a -> {
            systems.put(getLastChunk(a.attr("href")), a.text());
        });

        return systems;
    }

    private static String getLastChunk(String url) {
        String[] chunks = url.split("/");
        return chunks[chunks.length - 1];
    }
}
