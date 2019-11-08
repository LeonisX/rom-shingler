package md.leonis.shingler;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ListFilesaTest {

    @Test
    void deviation2() { //a < b = ((b-a)/a) * 100
        double d1 = 99.97283387917113;
        double d2 = 99.97291685474407;

        assertEquals(8.299812030666449E-5, ListFilesa.deviation(d1, d2));

        d1 = 100;
        d2 = 100;

        assertEquals(0, ListFilesa.deviation(d1, d2));

        d1 = 100;
        d2 = 1;

        assertEquals(99.0, ListFilesa.deviation(d1, d2));
    }

    //@Disabled
    @Test
    void listFiles2() {
        Map<String, Main1024a.GID> gids = ListFilesa.listFiles2(new File("D:\\Downloads\\text.7z"));
        Main1024a.GID gid = new ArrayList<>(gids.values()).get(0);

        assertEquals("text.txt", gid.getName());
        assertEquals(4, gid.getSize());
        assertEquals(3_632_233_996L, gid.getCrc32());
        assertEquals("098F6BCD4621D373CADE4E832627B4F6", Main1024a.bytesToHex(gid.getMd5()));
        assertEquals("A94A8FE5CCB19BA61C4C0873D391E987982FBBD3", Main1024a.bytesToHex(gid.getSha1()));
    }
}
