package md.leonis.shingler.gui.crawler.moby;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MobyCrawlerTest {

    private static final String CREDITS1 = "<a href=\"https://www.mobygames.com/developer/sheet/view/developerId,45166/\">Ayako Mori</a>";
    private static final String CREDITS2 = "<a href=\"https://www.mobygames.com/developer/sheet/view/developerId,55535/\">Hideo Kodzima</a>";
    private static final String CREDITS3 = "<a href=\"https://www.mobygames.com/developer/sheet/view/developerId,12345/\">Eric Chahi</a>";

    @BeforeEach
    void setUp() {
        MobyCrawler.developers = new LinkedHashMap<>();
    }

    @Test
    void testCreditsA() { // A
        Element td = getTd(CREDITS1);
        assertEquals("[{45166=Ayako Mori}]", MobyCrawler.parseCredits(td).toString());
        assertEquals("{45166=Ayako Mori}", MobyCrawler.developers.toString());
    }

    @Test
    void testCreditsAAA() { // A A A
        Element td = getTd(CREDITS1 + ", " + CREDITS2 + ", " + CREDITS3);
        assertEquals("[{45166=Ayako Mori}, {55535=Hideo Kodzima}, {12345=Eric Chahi}]", MobyCrawler.parseCredits(td).toString());
        assertEquals("{45166=Ayako Mori, 55535=Hideo Kodzima, 12345=Eric Chahi}", MobyCrawler.developers.toString());
    }

    @Test
    void testCreditsT() { // t
        Element td = getTd("Duke Nukem");
        assertEquals("[{null=Duke Nukem}]", MobyCrawler.parseCredits(td).toString());
        assertEquals("{}", MobyCrawler.developers.toString());
    }

    @Test
    void testCreditsAt() { // A (t)
        Element td = getTd(CREDITS1 + " (John Doe)");
        assertEquals("[{45166=Ayako Mori, origName=John Doe}]", MobyCrawler.parseCredits(td).toString());
        assertEquals("{45166=Ayako Mori}", MobyCrawler.developers.toString());
    }

    @Test
    void testCreditsTt() { // t, t
        Element td = getTd("name1, name2");
        assertEquals("[{null=name1}, {null=name2}]", MobyCrawler.parseCredits(td).toString());
        assertEquals("{}", MobyCrawler.developers.toString());
    }

    @Test
    void testCreditsTtAt() { // t, t, A (t)
        Element td = getTd("name1, name2, " + CREDITS1 + " (name3)");
        assertEquals("[{null=name1}, {null=name2}, {45166=Ayako Mori, origName=name3}]", MobyCrawler.parseCredits(td).toString());
        assertEquals("{45166=Ayako Mori}", MobyCrawler.developers.toString());
    }

    @Test
    void testCreditsTtt() { // t, t (t)
        Element td = getTd("name1, name2 (group2)");
        assertEquals("[{null=name1}, {null=name2, origName=group2}]", MobyCrawler.parseCredits(td).toString());
        assertEquals("{}", MobyCrawler.developers.toString());
    }

    @Test
    void testCreditsAtAt() { // A (t), A (t)
        Element td = getTd(CREDITS1 + " (name1), " + CREDITS2 + " (name2)");
        assertEquals("[{45166=Ayako Mori, origName=name1}, {55535=Hideo Kodzima, origName=name2}]", MobyCrawler.parseCredits(td).toString());
        assertEquals("{45166=Ayako Mori, 55535=Hideo Kodzima}", MobyCrawler.developers.toString());
    }

    @Test
    void testCreditsAttAtttt() { // A (t), t, A (t [t]), t [t]
        Element td = getTd(CREDITS1 + " (origName1), name2, " + CREDITS2 + " (origName3 [group3]), name4 [origName4]");
        assertEquals("[{45166=Ayako Mori, origName=origName1}, {null=name2}, {55535=Hideo Kodzima, origName=origName3, group=group3}, {null=name4, origName=origName4}]", MobyCrawler.parseCredits(td).toString());
        assertEquals("{45166=Ayako Mori, 55535=Hideo Kodzima}", MobyCrawler.developers.toString());
    }

    @Test
    void testCreditsAtt() { // A (t [t] )  Rieko Kodama (Phoenix Rie [フェニックスりえ])
        Element td = getTd(CREDITS1 + " (name [group] )");
        assertEquals("[{45166=Ayako Mori, origName=name, group=group}]", MobyCrawler.parseCredits(td).toString());
        assertEquals("{45166=Ayako Mori}", MobyCrawler.developers.toString());
    }

    @Test
    void testCreditsTT() { // t [t]
        Element td = getTd("name [origName]");
        assertEquals("[{null=name, origName=origName}]", MobyCrawler.parseCredits(td).toString());
        assertEquals("{}", MobyCrawler.developers.toString());
    }

    @Test
    void testCreditsTTAt() { // t [t], A (t)
        Element td = getTd("name [origName], " + CREDITS1 + " (origName2)");
        assertEquals("[{null=name, origName=origName}, {45166=Ayako Mori, origName=origName2}]", MobyCrawler.parseCredits(td).toString());
        assertEquals("{45166=Ayako Mori}", MobyCrawler.developers.toString());
    }

    @Test
    void testCreditsttATtATt() { // t, t, A (T (t) ), A (T (t) )
        Element td = getTd("name1, name2, " + CREDITS1 + " (origName3 (note3) ), " + CREDITS2 + " (origName4 (note4) )");
        assertEquals("[{null=name1}, {null=name2}, {45166=Ayako Mori, origName=origName3, group=note3}, {55535=Hideo Kodzima, origName=origName4, group=note4}]", MobyCrawler.parseCredits(td).toString());
        assertEquals("{45166=Ayako Mori, 55535=Hideo Kodzima}", MobyCrawler.developers.toString());
    }

    private Element getTd(String credits) {
        Document doc = Jsoup.parse("<td class=\"crln\">" + credits + "</td>");
        return doc.body();
    }
}