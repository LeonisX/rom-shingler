package md.leonis.shingler;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ListFilesTest {

    @Test
    void toChunksTest1() {
        List<String> chunks = ListFiles.toChunks("test");
        assertArrayEquals(new String[]{"test"}, chunks.toArray(new String[0]));
    }

    @Test
    void toChunksTest2() {
        List<String> chunks = ListFiles.toChunks("test 2");
        assertArrayEquals(new String[]{"test 2"}, chunks.toArray(new String[0]));
    }

    @Test
    void toChunksTest3() {
        List<String> chunks = ListFiles.toChunks("test (U)");
        assertArrayEquals(new String[]{"test", "(U)"}, chunks.toArray(new String[0]));
    }

    @Test
    void toChunksTest4() {
        List<String> chunks = ListFiles.toChunks("test [p1]");
        assertArrayEquals(new String[]{"test", "[p1]"}, chunks.toArray(new String[0]));
    }

    @Test
    void toChunksTest5() {
        List<String> chunks = ListFiles.toChunks("test (A) [p1] (Unl)");
        assertArrayEquals(new String[]{"test", "(A)", "[p1]", "(Unl)"}, chunks.toArray(new String[0]));
    }

    @Test
    void toChunksTest6() {
        List<String> chunks = ListFiles.toChunks("test [T+Rus Leonis (test)]");
        assertArrayEquals(new String[]{"test", "[T+Rus Leonis (test)]"}, chunks.toArray(new String[0]));
    }

    @Test
    void toChunksTest7() {
        List<String> chunks = ListFiles.toChunks("test [T+Rus Leonis (test)] (Unl)");
        assertArrayEquals(new String[]{"test", "[T+Rus Leonis (test)]", "(Unl)"}, chunks.toArray(new String[0]));
    }

    @Test
    void toChunksTest8() {
        List<String> chunks = ListFiles.toChunks("test [T+Rus Leonis (test[1])] (Unl [a[s]])");
        assertArrayEquals(new String[]{"test", "[T+Rus Leonis (test[1])]", "(Unl [a[s]])"}, chunks.toArray(new String[0]));
    }
}