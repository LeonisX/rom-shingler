package md.leonis.shingler.utils;

import md.leonis.shingler.model.ConfigHolder;
import md.leonis.shingler.model.Platform;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static md.leonis.shingler.model.ConfigHolder.platforms;
import static md.leonis.shingler.model.ConfigHolder.platformsByCpu;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StringUtilsTest {

    static {
        ConfigHolder.platform = "gg";
        platforms.put("Sega Game Gear", new Platform("Sega Game Gear", "gg", "(.*\\(Hack\\).*|.*\\(Hack .*|.* Hack\\).*)", "(.*\\[[bhot][0-9a-f]].*|.*\\[T[+\\-].*].*|.*\\[hM\\d{2}].*|.*\\[hFFE].*)", ".*\\(PD\\).*", Arrays.asList(".gg", ".sms", ".7z", ".zip")));
        platforms.values().forEach(p -> platformsByCpu.put(p.getCpu(), p));
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
        assertEquals("123456789 123456789 1~456789123456789123456789123456789123456789", StringUtils.force63("123456789 123456789 123456789 123456789 123456789 123456789 123456789"));
        assertEquals("123456789 123456789 1~456789123456789123456789123456789123456789", StringUtils.force63("123456789 123456789 123456789 123456789 123456789 123456789 123456789 "));
        assertEquals("123456789 123456789 1~6789-1234567891234567891234567891234567891", StringUtils.force63("123456789 123456789 123456789-123456789 123456789 123456789 123456789 1"));
        assertEquals("123456789 123456789 1~678912345678912345678912345678912345678912", StringUtils.force63("123456789 123456789 123456789 123456789 123456789 123456789 123456789 12"));
        assertEquals("123456789123456789 12~9 123456789123456789=123456789123456789123", StringUtils.force63("123456789'123456789 123456789 123456789 123456789=123456789 123456789 123"));
        assertEquals("123456789 123456789 1~912345678912345678912345678912345678912345", StringUtils.force63("123456789 123456789 123456789 123456789 123456789 123456789 123456789 12345"));
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
    void translit() {
        assertEquals("Zamechanie-po-baze-dannyh", StringUtils.translit("Замечание по базе данных"));
    }

    @Test
    void cpu() {
        assertEquals("89-dennou-kyuusei-uranai", StringUtils.cpu("&rsquo;89 Dennou Kyuusei Uranai"));
        assertEquals("2-in-1-family-kid-aladdin-4", StringUtils.cpu("2-in-1 - Family Kid &amp; Aladdin 4"));
        assertEquals("adventures-of-rocky-and-bullwinkle-and-friends-the", StringUtils.cpu("Adventures of Rocky and Bullwinkle and Friends, The"));
    }

    @Test
    void normalize() {
        assertEquals("Ariel-Little_Mermaid", StringUtils.normalize("Ariel - The Little Mermaid"));
    }

    @Test
    void replaceFromTheEnd() {
        assertEquals("hot-dance-2000", StringUtils.replaceFromTail("-unl", "hot-dance-2000-unl", ""));
        assertEquals("hot-dance-2000-2", StringUtils.replaceFromTail("-ii", "hot-dance-2000-ii", "-2"));
    }

}
