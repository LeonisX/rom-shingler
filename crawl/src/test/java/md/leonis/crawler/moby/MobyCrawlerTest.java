package md.leonis.crawler.moby;

import md.leonis.crawler.moby.model.CreditsNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MobyCrawlerTest {

    // Cloudflare email protection
    // Code from: https://www.ipvoid.com/cloudflare-email-decoder/

    @Test
    //   r o o t i . r p g . s e g a . c o
    // cbb9a4a4bfa28bb9bbace5b8aeacaae5a8a4
    void testCfDecodeEmail() { // A
        assertEquals("rooti@rpg.sega.co", MobyCrawler.decodeCfEmail("cbb9a4a4bfa28bb9bbace5b8aeacaae5a8a4"));
    }


    // A
    // A, A, A
    // t
    // A (t)
    // t, t
    // t, t, A (t)
    // t, t (t)
    // A (t), A (t)
    // A (t), t, A (t [t]), t [t]
    // A (t (t) )  Rieko Kodama (Phoenix Rie [フェニックスりえ])
    // t [t]
    // t [t], A (t)
    // t, t, A (T (t) ), A (T (t) )
    // Tetsuji Tanaka (Tanaka [タナカ]/Yuk)

    private static final String CREDITS1 = "<a href=\".../developer/sheet/view/developerId,45166/\">Ayako Mori</a>";
    private static final String CREDITS2 = "<a href=\".../developer/sheet/view/developerId,55535/\">Hideo Kodzima</a>";
    private static final String CREDITS3 = "<a href=\".../developer/sheet/view/developerId,12345/\">Eric Chahi</a>";

    @BeforeEach
    void setUp() {
        MobyCrawler.developers = new LinkedHashMap<>();
    }

    @Test
    void testCreditsA() { // A
        Element td = getTd(CREDITS1);
        Assertions.assertEquals("45166=Ayako Mori", toString(MobyCrawler.parseCredits(td)));
        assertEquals("{45166=Ayako Mori}", MobyCrawler.developers.toString());
    }
    
    private String toString(List<CreditsNode> nodes) {
        return nodes.stream().map(CreditsNode::toString).collect(Collectors.joining(", "));
    }

    @Test
    void testCreditsAAA() { // A A A
        Element td = getTd(CREDITS1 + ", " + CREDITS2 + ", " + CREDITS3);
        Assertions.assertEquals("45166=Ayako Mori, 55535=Hideo Kodzima, 12345=Eric Chahi", toString(MobyCrawler.parseCredits(td)));
        assertEquals("{45166=Ayako Mori, 55535=Hideo Kodzima, 12345=Eric Chahi}", MobyCrawler.developers.toString());
    }

    @Test
    void testCreditsT() { // t
        Element td = getTd("Duke Nukem");
        Assertions.assertEquals("Duke Nukem", toString(MobyCrawler.parseCredits(td)));
        assertEquals("{}", MobyCrawler.developers.toString());
    }

    @Test
    void testCreditsAt() { // A (t)
        Element td = getTd(CREDITS1 + " (John Doe)");
        Assertions.assertEquals("45166=Ayako Mori (John Doe)", toString(MobyCrawler.parseCredits(td)));
        assertEquals("{45166=Ayako Mori}", MobyCrawler.developers.toString());
    }

    @Test
    void testCreditsTt() { // t, t
        Element td = getTd("name1, name2");
        Assertions.assertEquals("name1, name2", toString(MobyCrawler.parseCredits(td)));
        assertEquals("{}", MobyCrawler.developers.toString());
    }

    @Test
    void testCreditsTtAt() { // t, t, A (t)
        Element td = getTd("name1, name2, " + CREDITS1 + " (name3)");
        Assertions.assertEquals("name1, name2, 45166=Ayako Mori (name3)", toString(MobyCrawler.parseCredits(td)));
        assertEquals("{45166=Ayako Mori}", MobyCrawler.developers.toString());
    }

    @Test
    void testCreditsTtt() { // t, t (t)
        Element td = getTd("name1, name2 (group2)");
        Assertions.assertEquals("name1, name2 (group2)", toString(MobyCrawler.parseCredits(td)));
        assertEquals("{}", MobyCrawler.developers.toString());
    }

    @Test
    void testCreditsAtAt() { // A (t), A (t)
        Element td = getTd(CREDITS1 + " (name1), " + CREDITS2 + " (name2)");
        Assertions.assertEquals("45166=Ayako Mori (name1), 55535=Hideo Kodzima (name2)", toString(MobyCrawler.parseCredits(td)));
        assertEquals("{45166=Ayako Mori, 55535=Hideo Kodzima}", MobyCrawler.developers.toString());
    }

    @Test
    void testCreditsAttAtttt() { // A (t), t, A (t [t]), t [t]
        Element td = getTd(CREDITS1 + " (origName1), name2, " + CREDITS2 + " (origName3 [group3]), name4 [origName4]");
        Assertions.assertEquals("45166=Ayako Mori (origName1), name2, 55535=Hideo Kodzima (origName3 [group3]), name4 [origName4]", toString(MobyCrawler.parseCredits(td)));
        assertEquals("{45166=Ayako Mori, 55535=Hideo Kodzima}", MobyCrawler.developers.toString());
    }

    @Test
    void testCreditsAtt() { // A (t [t] )  Rieko Kodama (Phoenix Rie [フェニックスりえ])
        Element td = getTd(CREDITS1 + " (name [group] )");
        Assertions.assertEquals("45166=Ayako Mori (name [group])", toString(MobyCrawler.parseCredits(td)));
        assertEquals("{45166=Ayako Mori}", MobyCrawler.developers.toString());
    }

    @Test
    void testCreditsTT() { // t [t]
        Element td = getTd("name [origName]");
        Assertions.assertEquals("name [origName]", toString(MobyCrawler.parseCredits(td)));
        assertEquals("{}", MobyCrawler.developers.toString());
    }

    @Test
    void testCreditsTTAt() { // t [t], A (t)
        Element td = getTd("name [origName], " + CREDITS1 + " (origName2)");
        Assertions.assertEquals("name [origName], 45166=Ayako Mori (origName2)", toString(MobyCrawler.parseCredits(td)));
        assertEquals("{45166=Ayako Mori}", MobyCrawler.developers.toString());
    }

    @Test
    void testCreditsttATtATt() { // t, t, A (T (t) ), A (T (t) )
        Element td = getTd("name1, name2, " + CREDITS1 + " (origName3 (note3) ), " + CREDITS2 + " (origName4 (note4) )");
        Assertions.assertEquals("name1, name2, 45166=Ayako Mori (origName3 (note3)), 55535=Hideo Kodzima (origName4 (note4))", toString(MobyCrawler.parseCredits(td)));
        assertEquals("{45166=Ayako Mori, 55535=Hideo Kodzima}", MobyCrawler.developers.toString());
    }

    @Test
    void testCreditsATty() { // A (T [t]/Y)
        Element td = getTd(CREDITS1 + " (origName3 [note3]/Yup)");
        Assertions.assertEquals("45166=Ayako Mori (origName3 [note3] /Yup)", toString(MobyCrawler.parseCredits(td)));
        assertEquals("{45166=Ayako Mori}", MobyCrawler.developers.toString());
    }

    @Test
    void testCreditsRiverTop() { // (River Top/Kawasaki Minoru [リバートップ/かわさき みのる])
        Element td = getTd(CREDITS1 + " (River Top/Kawasaki Minoru [リバートップ/かわさき みのる])");
        Assertions.assertEquals("45166=Ayako Mori (River Top/Kawasaki Minoru [リバートップ/かわさき みのる])", toString(MobyCrawler.parseCredits(td)));
        assertEquals("{45166=Ayako Mori}", MobyCrawler.developers.toString());
    }

    @Test
    void testCreditsEncryptedEmail() { // Bug-Bug, rooti@rpg.sega.co
        Element td = getTd("Bug-Bug, <a href=\"/cdn-cgi/l/email-protection\" class=\"__cf_email__\" data-cfemail=\"cbb9a4a4bfa28bb9bbace5b8aeacaae5a8a4\">[email&#160;protected]</a>");
        Assertions.assertEquals("Bug-Bug, rooti@rpg.sega.co", toString(MobyCrawler.parseCredits(td)));
        assertEquals("{}", MobyCrawler.developers.toString());
    }

    @Test
    void testCreditsOgawan() { // Ogawan [Soon to go 'puff-puff' in Hawaii [おがわん] [もうすぐ ハワイでパフパフ]]
        Element td = getTd("Ogawan [Soon to go 'puff-puff' in Hawaii [おがわん] [もうすぐ ハワイでパフパフ]]");
        Assertions.assertEquals("Ogawan [Soon to go 'puff-puff' in Hawaii [おがわん] [もうすぐ ハワイでパフパフ]]", toString(MobyCrawler.parseCredits(td)));
        assertEquals("{}", MobyCrawler.developers.toString());
    }

    @Test
    void testCreditsKaeru() { // Kaeru Taniguchi (Shacho Kwaeru [Feeling?] [くわえる　しゃちょー　[って　かんじ？])
        Element td = getTd(CREDITS1 + " (Shacho Kwaeru [Feeling?] [くわえる　しゃちょー　[って　かんじ？])");
        Assertions.assertEquals("45166=Ayako Mori (Shacho Kwaeru [Feeling?] [くわえる　しゃちょー　 [って　かんじ？]])", toString(MobyCrawler.parseCredits(td)));
        assertEquals("{45166=Ayako Mori}", MobyCrawler.developers.toString());
    }

    @Test
    void testCreditsKao() { // Kao [sucking in bike] [かお [バイクで　すってん]]
        Element td = getTd("Kao [sucking in bike] [かお [バイクで　すってん]]");
        Assertions.assertEquals("Kao [sucking in bike] [かお [バイクで　すってん]]", toString(MobyCrawler.parseCredits(td)));
        assertEquals("{}", MobyCrawler.developers.toString());
    }

    @Test
    void testCreditsNorihiko() { // Norihiko Togashi (Noririn♪ -and sound- [の　り　り　ん　♪ [や　お　と　が])
        Element td = getTd(CREDITS1 + " (Noririn♪ -and sound- [の　り　り　ん　♪ [や　お　と　が])");
        Assertions.assertEquals("45166=Ayako Mori (Noririn♪ -and sound- [の　り　り　ん　♪ [や　お　と　が]])", toString(MobyCrawler.parseCredits(td)));
        assertEquals("{45166=Ayako Mori}", MobyCrawler.developers.toString());
    }

    @Test
    // Тут ничего нельзя поделать, разве что полностью переписать всё ещё раз, но оно не стоит того на самом деле.
    void testCreditsFuncom() { // Erik Gløersen (of FUNCOM Productions [Oslo, Norway])
        Element td = getTd(CREDITS1 + " (of FUNCOM Productions [Oslo, Norway])");
        Assertions.assertEquals("45166=Ayako Mori (of FUNCOM Productions [Oslo]), Norway", toString(MobyCrawler.parseCredits(td)));
        assertEquals("{45166=Ayako Mori}", MobyCrawler.developers.toString());
    }

    @Test
    void testCredits4x() { // <a href="http://www.4xtechnologies.com">http://www.4xtechnologies.com</a>
        Element td = getTd("<a href=\"http://www.4xtechnologies.com\">http://www.4xtechnologies.com</a>");
        Assertions.assertEquals("http://www.4xtechnologies.com", toString(MobyCrawler.parseCredits(td)));
        assertEquals("{}", MobyCrawler.developers.toString());
    }


    private Element getTd(String credits) {
        Document doc = Jsoup.parse("<td class=\"crln\">" + credits + "</td>");
        return doc.body();
    }
}