package md.leonis.shingler.model.nes;

import lombok.Data;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

// Partial UNIF coverage
// https://wiki.nesdev.com/w/index.php/UNIF
// https://github.com/eteran/libunif/blob/master/lib_unif.c
@Data
public class UnifHeader {

    private static final String HEADER = "UNIF";

    private byte[] header = new byte[4];
    private byte[] chunk = new byte[4];

    private byte[] raw = new byte[1024];
    private String rawString;

    private String mapper;
    private String name;
    private int version;

    private boolean isUNIF() {
        return new String(header, StandardCharsets.UTF_8).equals(HEADER);
    }

    public UnifHeader(File file) {
        try {
            RandomAccessFile f = new RandomAccessFile(file, "r");
            f.readFully(header);

            if (!isUNIF()) {
                throw new RuntimeException("Not UNIF header");
            }

            version = readLEInt(f);

            f.readFully(new byte[24]);

            boolean read = true;

            while (read) {
                Chunk chunk = new Chunk(f);
                read = !chunk.title.isEmpty();
                if (chunk.title.equals("MAPR")) {
                    mapper = chunk.value;
                }
                if (chunk.title.equals("NAME")) {
                    name = chunk.value;
                }
            }

        } catch (Exception e) {
            //throw new RuntimeException(e);
            System.out.println(e.getMessage() + ":" + file);
        }
    }

    private static int readLEInt(RandomAccessFile f) {
        try {
            return (f.readUnsignedByte() | f.readUnsignedByte() << 8 | f.readUnsignedByte() << 16 | f.readUnsignedByte() << 24);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class Chunk {

        private static Set<String> VAR_TITLES = new HashSet<>(Arrays.asList(
                "MAPR", "NAME", "WRTR", "READ"
        ));

        private static Set<String> VAR_TITLESS = new HashSet<>(Arrays.asList(
                "PRG", "CHR"
        ));

        private static Set<String> BYTE_TITLES = new HashSet<>(Arrays.asList(
                "TVCI", "CTRL", "BATR", "VROR", "MIRR"
        ));

        String title;
        String value;

        public Chunk(RandomAccessFile f) {
            try {
                byte[] bytes = new byte[4];
                f.readFully(bytes);
                title = new String(bytes, StandardCharsets.UTF_8);

                if (VAR_TITLES.contains(title) || VAR_TITLESS.contains(title.substring(0, 3))) {
                    int length = readLEInt(f);

                    bytes = new byte[length - 1];
                    f.readFully(bytes);
                    value = new String(bytes, StandardCharsets.UTF_8);
                    assert (f.readByte() == 0);
                    f.readByte();
                } else if (title.startsWith("PCK") || title.startsWith("CCK")) {
                    f.readFully(new byte[4]);
                } else if (BYTE_TITLES.contains(title)) {
                    f.readByte();
                } else if (title.equals("DINF")) {
                    f.readFully(new byte[204]);
                }/* else if (title.equals("TVCI")) {
                    int length = readLEInt(f);
                    bytes = new byte[length];
                    f.readFully(bytes);
                }*/ else title = "";

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
