package md.leonis.shingler.utils;

import md.leonis.shingler.model.ConfigHolder;
import md.leonis.shingler.model.Platform;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static md.leonis.shingler.model.ConfigHolder.platforms;
import static md.leonis.shingler.model.ConfigHolder.platformsByCpu;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StringUtilsTest {

    static {
        ConfigHolder.platform = "gg";
        platforms.put("Sega Game Gear", new Platform("Sega Game Gear", "gg",
                "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)",
                "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*",
                Arrays.asList(".gg", ".sms", ".7z", ".zip"), Collections.emptyList(), Collections.singletonList(0), 8));
        platforms.values().forEach(p -> platformsByCpu.put(p.getCpu(), p));
    }

    String ascii1 = " \u0001\u0002\u0003\u0004\u0005\u0006\t\n" +
            "\u000E\u000F\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019\u001A\u001B\u001C\u001D\u001E\u001F !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u007F";

    String ascii2 = "\u0080\u0081\u0082\u0083\u0084\u0085\u0086\u0087\u0088\u0089\u008A\u008B\u008C\u008D\u008E\u008F\u0090\u0091\u0092\u0093\u0094\u0095\u0096\u0097\u0098\u0099\u009A\u009B\u009C\u009D\u009E\u009F ¡¢£¤¥¦§¨©ª«¬\u00AD®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ";

    String cyrillic = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя";

    @Test
    void generateBooksCpu() {
        assertEquals(" 0123456789 ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklmnopqrstuvwxyz ", ascii1.replaceAll("[^\\p{Alnum}]+", " ")); //  !"#$%&'()*+,-./_:;<=>?@_[\]^_`_{|}~
        assertEquals(" ", ascii2.replaceAll("[^\\p{Alnum}]+", " ")); //  ¡¢£¤¥¦§¨©ª«¬­®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ
        assertEquals(" ", cyrillic.replaceAll("[^\\p{Alnum}]+", " ")); // АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя

        assertEquals("0123456789-abcdefghijklmnopqrstuvwxyz-abcdefghijklmnopqrstuvwxyz", StringUtils.cpu(ascii1));
        assertEquals("aaaaaa-ceeeeiiii-nooooo-uuuuy-aaaaaa-ceeeeiiii-nooooo-uuuuy-y", StringUtils.cpu(ascii2));
        assertEquals("abvgdeejziiklmnoprstufhcchshscyeiuiaabvgdeejziiklmnoprstufhcchshscyeiuia", StringUtils.cpu(cyrillic));

        assertEquals("aaeeiioooouuuu-aaeeiioooouuuu", StringUtils.cpu("aáeéiíoóöőuúüű AÁEÉIÍOÓÖŐUÚÜŰ"));
        assertEquals("", StringUtils.cpu("'\"().,&!?$@#%^*=/\\[];:|<>{}"));
        assertEquals("", StringUtils.cpu("-"));
        assertEquals("", StringUtils.cpu("-"));
        assertEquals("an", StringUtils.cpu("-AN"));
        assertEquals("an", StringUtils.cpu("-an"));
        assertEquals("normal-text-123", StringUtils.cpu("Normal  Text   123 "));
        assertEquals("russkii-tekst", StringUtils.cpu(" Русский     текст"));
        assertEquals("bugs-life", StringUtils.cpu("A Bugs Life, The"));
        assertEquals("never-ending-story-end", StringUtils.cpu("The Never Ending A Story The End"));
    }

    @Test
    void generateCpu() {
        assertEquals(" 0123456789 ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklmnopqrstuvwxyz ", ascii1.replaceAll("[^\\p{Alnum}]+", " ")); //  !"#$%&'()*+,-./_:;<=>?@_[\]^_`_{|}~
        assertEquals(" ", ascii2.replaceAll("[^\\p{Alnum}]+", " ")); //  ¡¢£¤¥¦§¨©ª«¬­®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ
        assertEquals(" ", cyrillic.replaceAll("[^\\p{Alnum}]+", " ")); // АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя

        assertEquals("0123456789 abcdefghijklmnopqrstuvwxyz abcdefghijklmnopqrstuvwxyz", StringUtils.generateCpu(ascii1));
        assertEquals("aaaaaa ceeeeiiii nooooo uuuuy aaaaaa ceeeeiiii nooooo uuuuy y", StringUtils.generateCpu(ascii2));
        assertEquals("abvgdeejziiklmnoprstufhcchshscyeiuiaabvgdeejziiklmnoprstufhcchshscyeiuia", StringUtils.generateCpu(cyrillic));

        assertEquals("aaeeiioooouuuu aaeeiioooouuuu", StringUtils.generateCpu("aáeéiíoóöőuúüű AÁEÉIÍOÓÖŐUÚÜŰ"));
        assertEquals("", StringUtils.generateCpu("'\"().,&!?$@#%^*=/\\[];:|<>{}"));
        assertEquals("", StringUtils.generateCpu("-"));
        assertEquals("", StringUtils.generateCpu("_"));
        assertEquals("a", StringUtils.generateCpu("-A"));
        assertEquals("a", StringUtils.generateCpu("_a"));
        assertEquals("normal text 123", StringUtils.generateCpu("Normal  Text   123 "));
        assertEquals("russkii tekst", StringUtils.generateCpu(" Русский   текст"));
    }

    @Test
    void cleanString() {
        assertEquals(" 0123456789 ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklmnopqrstuvwxyz ", ascii1.replaceAll("[^\\p{Alnum}]+", " ")); //  !"#$%&'()*+,-./_:;<=>?@_[\]^_`_{|}~
        assertEquals(" ", ascii2.replaceAll("[^\\p{Alnum}]+", " ")); //  ¡¢£¤¥¦§¨©ª«¬­®¯°±²³´µ¶·¸¹º»¼½¾¿ÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖ×ØÙÚÛÜÝÞßàáâãäåæçèéêëìíîïðñòóôõö÷øùúûüýþÿ
        assertEquals(" ", cyrillic.replaceAll("[^\\p{Alnum}]+", " ")); // АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя

        assertEquals("0123456789 ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklmnopqrstuvwxyz", StringUtils.cleanString(ascii1));
        assertEquals("AAAAAA CEEEEIIII NOOOOO UUUUY aaaaaa ceeeeiiii nooooo uuuuy y", StringUtils.cleanString(ascii2));
        assertEquals("ABVGDEEJZIIKLMNOPRSTUFHCCHSHSCYEIUIAabvgdeejziiklmnoprstufhcchshscyeiuia", StringUtils.cleanString(cyrillic));

        assertEquals("aaeeiioooouuuu AAEEIIOOOOUUUU", StringUtils.cleanString("aáeéiíoóöőuúüű AÁEÉIÍOÓÖŐUÚÜŰ"));
        assertEquals("", StringUtils.cleanString("'\"().,&!?$@#%^*=/\\[];:|<>{}"));
        assertEquals("", StringUtils.cleanString("-"));
        assertEquals("", StringUtils.cleanString("_"));
        assertEquals("A", StringUtils.cleanString("-A"));
        assertEquals("a", StringUtils.cleanString("_a"));
        assertEquals("Normal Text 123", StringUtils.cleanString("Normal Text 123 "));
        assertEquals("Russkii tekst", StringUtils.cleanString(" Русский текст"));
    }
    
    @Test
    void force63() {
        assertEquals("", StringUtils.force63(""));
        assertEquals("123456789 123456789 123456789 123456789 123456789 123456789 123", StringUtils.force63("123456789 123456789 123456789 123456789 123456789 123456789 123"));
        assertEquals("123456789 123456789 123456789 123456789 123456789 123456789 1234", StringUtils.force63("123456789 123456789 123456789 123456789 123456789 123456789 1234"));
        assertEquals("123456789 123456789 123456789 123456789 123456789 12345678912345", StringUtils.force63("123456789 123456789 123456789 123456789 123456789 123456789 12345"));
        assertEquals("123456789 123456789 123456789 123456789 123456789123456789123456", StringUtils.force63("123456789 123456789 123456789 123456789 123456789 123456789 123456"));
        assertEquals("123456789 123456789 123456789 1234567891234567891234567891234567", StringUtils.force63("123456789 123456789 123456789 123456789 123456789 123456789 1234567"));
        assertEquals("123456789 123456789 12345678912345678912345678912345678912345678", StringUtils.force63("123456789 123456789 123456789 123456789 123456789 123456789 12345678"));
        assertEquals("123456789 123456789 1-456789123456789123456789123456789123456789", StringUtils.force63("123456789 123456789 123456789 123456789 123456789 123456789 123456789"));
        assertEquals("123456789 123456789 1-456789123456789123456789123456789123456789", StringUtils.force63("123456789 123456789 123456789 123456789 123456789 123456789 123456789 "));
        assertEquals("123456789 123456789 1-6789-1234567891234567891234567891234567891", StringUtils.force63("123456789 123456789 123456789-123456789 123456789 123456789 123456789 1"));
        assertEquals("123456789 123456789 1-678912345678912345678912345678912345678912", StringUtils.force63("123456789 123456789 123456789 123456789 123456789 123456789 123456789 12"));
        assertEquals("123456789123456789 12-9 123456789123456789=123456789123456789123", StringUtils.force63("123456789'123456789 123456789 123456789 123456789=123456789 123456789 123"));
        assertEquals("123456789 123456789 1-912345678912345678912345678912345678912345", StringUtils.force63("123456789 123456789 123456789 123456789 123456789 123456789 123456789 12345"));
    }

    @Test
    void deleteSubstrTest() {
        assertEquals("Adventures of Captain Comic.7z", StringUtils.deleteSubstr(", The", "Adventures of Captain Comic, The.7z", 0));
    }

    @Test
    void toChunksTest1() {
        List<String> chunks = StringUtils.toChunks("test");
        assertArrayEquals(new String[]{"test"}, chunks.toArray(new String[0]));
    }

    @Test
    void toChunksTest2() {
        List<String> chunks = StringUtils.toChunks("test 2");
        assertArrayEquals(new String[]{"test 2"}, chunks.toArray(new String[0]));
    }

    @Test
    void toChunksTest3() {
        List<String> chunks = StringUtils.toChunks("test (U)");
        assertArrayEquals(new String[]{"test", "(U)"}, chunks.toArray(new String[0]));
    }

    @Test
    void toChunksTest4() {
        List<String> chunks = StringUtils.toChunks("test [p1]");
        assertArrayEquals(new String[]{"test", "[p1]"}, chunks.toArray(new String[0]));
    }

    @Test
    void toChunksTest5() {
        List<String> chunks = StringUtils.toChunks("test (A) [p1] (Unl)");
        assertArrayEquals(new String[]{"test", "(A)", "[p1]", "(Unl)"}, chunks.toArray(new String[0]));
    }

    @Test
    void toChunksTest6() {
        List<String> chunks = StringUtils.toChunks("test [T+Rus Leonis (test)]");
        assertArrayEquals(new String[]{"test", "[T+Rus Leonis (test)]"}, chunks.toArray(new String[0]));
    }

    @Test
    void toChunksTest7() {
        List<String> chunks = StringUtils.toChunks("test [T+Rus Leonis (test)] (Unl)");
        assertArrayEquals(new String[]{"test", "[T+Rus Leonis (test)]", "(Unl)"}, chunks.toArray(new String[0]));
    }

    @Test
    void toChunksTest8() {
        List<String> chunks = StringUtils.toChunks("test [T+Rus Leonis (test[1])] (Unl [a[s]])");
        assertArrayEquals(new String[]{"test", "[T+Rus Leonis (test[1])]", "(Unl [a[s]])"}, chunks.toArray(new String[0]));
    }

    @Test
    void formatTitle() {
        assertEquals("Adventures of Captain Comic", StringUtils.formatTitle("Adventures of Captain Comic"));
        assertEquals("Adventures of Captain Comic, The", StringUtils.formatTitle("The Adventures of Captain Comic"));
        assertEquals("Adventures of Captain Comic, A", StringUtils.formatTitle("A Adventures of Captain Comic"));

        assertEquals("Adventures of Captain Comic: The Game", StringUtils.formatTitle("Adventures of Captain Comic: The Game"));
        assertEquals("Adventures of Captain Comic, The: The Game", StringUtils.formatTitle("The Adventures of Captain Comic: The Game"));
        assertEquals("Adventures of Captain Comic, A: The Game", StringUtils.formatTitle("A Adventures of Captain Comic: The Game"));
        
        assertEquals("Adventures of Captain Comic - The Game", StringUtils.formatTitle("Adventures of Captain Comic - The Game"));
        assertEquals("Adventures of Captain Comic, The - The Game", StringUtils.formatTitle("The Adventures of Captain Comic - The Game"));
        assertEquals("Adventures of Captain Comic, A - The Game", StringUtils.formatTitle("A Adventures of Captain Comic - The Game"));
    }

    @Test
    void toTranslit() {
        assertEquals("Zamechanie po baze dannyh", StringUtils.toTranslit("Замечание по базе данных"));
    }

    @Test
    void cpu() {
        assertEquals("89-dennou-kyuusei-uranai", StringUtils.cpu("&rsquo;89 Dennou Kyuusei Uranai"));
        assertEquals("2-in-1-family-kid-aladdin-4", StringUtils.cpu("2-in-1 - Family Kid &amp; Aladdin 4"));
        assertEquals("adventures-of-rocky-and-bullwinkle-and-friends", StringUtils.cpu("Adventures of Rocky and Bullwinkle and Friends, The"));
    }

    @Test
    void normalize() {
        assertEquals("Ariel-Little_Mermaid", StringUtils.normalize("Ariel - The Little Mermaid"));

        assertEquals("Famicom_Doubutsu_Seitai_Zukan-KatteniShirokuma-MoriwoSukuenoMaki", StringUtils.normalize("Famicom Doubutsu Seitai Zukan! - Katte ni Shirokuma - Mori wo Sukue no Maki!"));
        assertEquals("Family_Trainer_10-Rai_Rai_Kyonshis-Baby_Kyonshi_noAmidaDaibouken", StringUtils.normalize("Family Trainer 10 - Rai Rai! Kyonshis - Baby Kyonshi no Amida Daibouken"));
        assertEquals("Hokuto_no_Ken_4-Shichisei_Haken_Den-Hokuto_Shinken_no_Kanata_e", StringUtils.normalize("Hokuto no Ken 4 - Shichisei Haken Den - Hokuto Shinken no Kanata e"));
        assertEquals("Kuai_Le_Bi_Qi_III-Di_Qiu_Zhan_Shi_Happy_Biqi_III-World_Fighter", StringUtils.normalize("Kuai Le Bi Qi III - Di Qiu Zhan Shi (Happy Biqi III - World Fighter)"));

        assertEquals("Famicom_Doubutsu_Seit-ukan-KatteniShirokuma-MoriwoSukuenoMaki.7z", StringUtils.normalize("Famicom Doubutsu Seitai Zukan! - Katte ni Shirokuma - Mori wo Sukue no Maki!", "7z"));
        assertEquals("Family_Trainer_10-Rai_RaiKyonshis-BabyKyonshinoAmidaDaibouken.7z", StringUtils.normalize("Family Trainer 10 - Rai Rai! Kyonshis - Baby Kyonshi no Amida Daibouken", "7z"));
        assertEquals("Hokuto_no_Ken_4-Shichisei_Haken_Den-Hokuto_Shinken_no_Kanatae.7z", StringUtils.normalize("Hokuto no Ken 4 - Shichisei Haken Den - Hokuto Shinken no Kanata e", "7z"));
        assertEquals("Kuai_Le_Bi_Qi_III-Di_Qiu_Zhan_Shi_Happy_Biqi_III-WorldFighter.7z", StringUtils.normalize("Kuai Le Bi Qi III - Di Qiu Zhan Shi (Happy Biqi III - World Fighter)", "7z"));

        assertEquals("Famicom_Doubutsu_Seit-kan-KatteniShirokuma-MoriwoSukuenoMaki.zip", StringUtils.normalize("Famicom Doubutsu Seitai Zukan! - Katte ni Shirokuma - Mori wo Sukue no Maki!", "zip"));
        assertEquals("Family_Trainer_10-RaiRaiKyonshis-BabyKyonshinoAmidaDaibouken.zip", StringUtils.normalize("Family Trainer 10 - Rai Rai! Kyonshis - Baby Kyonshi no Amida Daibouken", "zip"));
        assertEquals("Hokuto_no_Ken_4-Shichisei_Haken_Den-Hokuto_Shinken_noKanatae.zip", StringUtils.normalize("Hokuto no Ken 4 - Shichisei Haken Den - Hokuto Shinken no Kanata e", "zip"));
        assertEquals("Kuai_Le_Bi_Qi_III-Di_Qiu_Zhan_Shi_Happy_BiqiIII-WorldFighter.zip", StringUtils.normalize("Kuai Le Bi Qi III - Di Qiu Zhan Shi (Happy Biqi III - World Fighter)", "zip"));

        assertEquals("Hokuto_no_Ken_4-Shich-hisei_Haken_Den-HokutoShinkennoKanatae.zip", StringUtils.normalize("Hokuto_no_Ken_4-Shich-hisei_Haken_Den-Hokuto_Shinken_no_Kanata_e", "zip"));
        assertEquals("Family_Trainer_10-Rai-aiKyonshis-BabyKyonshinoAmidaDaibouken.zip", StringUtils.normalize("Family_Trainer_10-Rai-Rai_Kyonshis-Baby_Kyonshi_noAmidaDaibouken", "zip"));

        assertEquals("SD_Gundam_Gaiden-Knight_Gundam_Monogatari3-DensetsunoKishiDan.7z", StringUtils.normalize("SD Gundam Gaiden - Knight Gundam Monogatari 3 - Densetsu no Kishi Dan", "7z"));
        assertEquals("Yamamura_Misa_Suspense-Kyouto_Hana_no_Misshitsu_SatsujinJiken.7z", StringUtils.normalize("Yamamura Misa Suspense - Kyouto Hana no Misshitsu Satsujin Jiken", "7z"));
        assertEquals("Zui_Zhong_Huan_XiangIVGuangYuAnShuiJingFenZhengFinalFantasyIV.7z", StringUtils.normalize("Zui Zhong Huan Xiang IV Guang Yu An Shui Jing Fen Zheng (Final Fantasy IV)", "7z"));

        assertEquals("2-in-1-Mortal_Kombat_3_Extra_60_People-SuperShinobiKing005Ch.zip", StringUtils.normalize("2-in-1 - Mortal Kombat 3 Extra 60 People + Super Shinobi (King005) (Ch)", "zip"));
        assertEquals("180-in-1_15-in-1_18-i--in-158-in-1160-in-1288-in-1SJ-0027p1U.zip", StringUtils.normalize("180-in-1 (15-in-1, 18-in-1, 30-in-1, 52-in-1, 58-in-1, 160-in-1, 288-in-1) (SJ-0027) [p1][U][!]", "zip"));
        assertEquals("SD_Gundam_Gaiden-Knight_GundamMonogatari3-DensetsunoKishiDan.zip", StringUtils.normalize("SD Gundam Gaiden - Knight Gundam Monogatari 3 - Densetsu no Kishi Dan", "zip"));
        assertEquals("Yamamura_Misa_Suspense-Kyouto_Hana_no_MisshitsuSatsujinJiken.zip", StringUtils.normalize("Yamamura Misa Suspense - Kyouto Hana no Misshitsu Satsujin Jiken", "zip"));

        assertEquals("Phantasy_Star-Hordes_of_Nei-NoahVersionbyKomradeV144PS12Hack.zip", StringUtils.normalize("Phantasy Star - Hordes of Nei - Noah Version by Komrade V1.44 (PS 1.2 Hack)", "zip"));
        assertEquals("Rodrigo_2_em-Mate_O_Papai_Noel-Versao_de_Natal_Teddy_BoyHack.zip", StringUtils.normalize("Rodrigo 2 em - Mate O Papai Noel - Versao de Natal (Teddy Boy Hack)", "zip"));

        assertEquals("DCEvolutionnet_Intro_by_Ventzislav_Tzvetkov_MazeApathyV100PD.zip", StringUtils.normalize("DCEvolution.net Intro by Ventzislav Tzvetkov, Maze & Apathy V1.00 (PD)", "zip"));
        assertEquals("Sega_Genesis_6-Button-rollerv2byCharlesMacDonaldpooraussiePD.zip", StringUtils.normalize("Sega Genesis 6-Button Controller v2 by Charles MacDonald & pooraussie (PD)", "zip"));
        assertEquals("Sega_Mega_DriveGenesi-nControllerTestV10byCharlesMacDonaldPD.zip", StringUtils.normalize("Sega Mega Drive & Genesis 6 Button Controller Test V1.0 by Charles MacDonald (PD)", "zip"));
    }

    @Test
    void replaceFromTheEnd() {
        assertEquals("hot-dance-2000", StringUtils.replaceFromTail("-unl", "", "hot-dance-2000-unl"));
        assertEquals("hot-dance-2000-2", StringUtils.replaceFromTail("-ii", "-2", "hot-dance-2000-ii"));
    }

    @Test
    void stripExtension() {
        assertEquals("Schnurmanator 2011-08-05 SMW1 Hack", StringUtils.stripExtension("Schnurmanator 2011-08-05 SMW1 Hack.zip"));
        assertEquals("Schnurmanator 2011-08-05 SMW1 Hack.zip2", StringUtils.stripExtension("Schnurmanator 2011-08-05 SMW1 Hack.zip2"));
        assertEquals("Schnurmanator 2011-08-05.gg SMW1 Hack", StringUtils.stripExtension("Schnurmanator 2011-08-05.gg SMW1 Hack.zip"));
    }

    @Test
    void removeTail() {
        assertEquals("Test", StringUtils.removeTail("Test, The", ", The"));
    }

    @Test
    void removeFront() {
        assertEquals("Test", StringUtils.removeFront("The Test", "The "));
    }
}
