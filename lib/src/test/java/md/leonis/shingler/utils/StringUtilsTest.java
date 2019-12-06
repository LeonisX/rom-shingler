package md.leonis.shingler.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StringUtilsTest {

    @Test
    void force63() {
        assertEquals("", StringUtils.force63(""));
        assertEquals("123456789 123456789 123456789 123456789 123456789 123456789 123", StringUtils.force63("123456789 123456789 123456789 123456789 123456789 123456789 123"));
        assertEquals("123456789 123456789 123456789 123456789 123456789 123456789 1234", StringUtils.force63("123456789 123456789 123456789 123456789 123456789 123456789 1234"));
        assertEquals("123456789 123456789 123456789 123456789 123456789 12345678912345", StringUtils.force63("123456789 123456789 123456789 123456789 123456789 123456789 12345"));
        assertEquals("123456789 123456789 123456789 123456789 123456789123456789123456", StringUtils.force63("123456789 123456789 123456789 123456789 123456789 123456789 123456"));
        assertEquals("123456789 123456789 123456789 1234567891234567891234567891234567", StringUtils.force63("123456789 123456789 123456789 123456789 123456789 123456789 1234567"));
        assertEquals("123456789 123456789 12345678912345678912345678912345678912345678", StringUtils.force63("123456789 123456789 123456789 123456789 123456789 123456789 12345678"));
        assertEquals("123456789 123456789 1234567891234567~123456789123456789123456789", StringUtils.force63("123456789 123456789 123456789 123456789 123456789 123456789 123456789"));
        assertEquals("123456789 123456789 1234567891234567~123456789123456789123456789", StringUtils.force63("123456789 123456789 123456789 123456789 123456789 123456789 123456789 "));
        assertEquals("123456789 123456789 123456789-123456~234567891234567891234567891", StringUtils.force63("123456789 123456789 123456789-123456789 123456789 123456789 123456789 1"));
        assertEquals("123456789 123456789 1234567891234567~345678912345678912345678912", StringUtils.force63("123456789 123456789 123456789 123456789 123456789 123456789 123456789 12"));
        assertEquals("123456789123456789 123456789 1234567~56789=123456789123456789123", StringUtils.force63("123456789'123456789 123456789 123456789 123456789=123456789 123456789 123"));
        assertEquals("123456789 123456789 1234567891234567~678912345678912345678912345", StringUtils.force63("123456789 123456789 123456789 123456789 123456789 123456789 123456789 12345"));
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
}