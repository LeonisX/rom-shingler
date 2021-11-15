package md.leonis.shingler.gui.crawler.moby;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class MobyCrawler {

    public static final String SYSTEMS = "https://www.mobygames.com/browse/games/full,1/";
    //public static final String GAMES_PAGES = "https://www.mobygames.com/browse/games/%s/list-games/";
    public static final String GAMES_PAGES = "https://www.mobygames.com/browse/games/%s/offset,%s/so,0a/list-games/";


    //TODO proxies list
    public static final HttpExecutor executor = HttpExecutor.directInstance();

    public static void main(String[] args) throws Exception {

        //TODO get list of systems
        /*Map<String, String> systems = parseSystemsList();
        System.out.println(systems);*/

        //TODO in cycle
        Map<String, String> games = parseGamesList("nes");
        System.out.println(games);


    }

    private static Map<String, String> parseGamesList(String systemId) throws Exception {

        Map<String, String> games = new LinkedHashMap<>();

        HttpExecutor.HttpResponse response = executor.getResponse(String.format(GAMES_PAGES, systemId, 0));

        //TODO process exceptions -> retry list

        Document doc = Jsoup.parse(response.getBody());

        // <td class="mobHeaderPage" width="33%">Viewing Page 1 of 56</td>
        String[] chunks = Objects.requireNonNull(doc.getElementsByClass("mobHeaderPage").first()).text().split(" ");
        int pagesCount = Integer.parseInt(chunks[chunks.length -1]);
        assert pagesCount > 0;

        System.out.println("Pages count: " + pagesCount);

        // <td class="mobHeaderItems" width="34%">(items 1-25 of 1397)</td>
        chunks = Objects.requireNonNull(doc.getElementsByClass("mobHeaderItems").first()).text().split(" ");
        String last = chunks[chunks.length -1];
        int gamesCount = Integer.parseInt(last.substring(0, last.length() - 1));
        assert gamesCount > 0;

        System.out.println("Games count: " + gamesCount);

        /*Objects.requireNonNull(doc.getElementsByClass("browseTable").first()).getElementsByTag("a").forEach(a -> {
            games.put(parseSystemUrl(a.attr("href")), a.text());
        });*/

        //TODO parse #0 page
        parseGamesList2(games, response.getBody());

        //TODO parse in cycle
        // 1 - 0
        // 2 - 25
        // 3 - 50
        for (int i = 1; i < pagesCount; i++) {
            response = executor.getResponse(String.format(GAMES_PAGES, systemId, i * 25));
            parseGamesList2(games, response.getBody());
        }

        assert games.size() == gamesCount;

        return games;
    }

    private static void parseGamesList2(Map<String, String> games, String html) {

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

        HttpExecutor.HttpResponse response = executor.getResponse(SYSTEMS);

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
