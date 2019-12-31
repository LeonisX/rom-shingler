package md.leonis.shingler.model.nes;

import lombok.Data;
import lombok.NoArgsConstructor;

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

    private String header;
    private byte[] chunk = new byte[4];

    private byte[] raw = new byte[1024];
    private String rawString;

    private String mapper;
    private String name;
    private int version;

    private boolean isUNIF() {
        return header.equals(HEADER);
    }

    public UnifHeader(File file) {
        try {
            RandomAccessFile f = new RandomAccessFile(file, "r");
            header = readString(f, 4);

            if (!isUNIF()) {
                throw new RuntimeException("Not UNIF header");
            }

            version = readLEInt(f);

            readBytes(f, 24);

            boolean read = true;

            /*if (file.getName().equals("Tin Choi Ma Li (Genius Merio Bros) (JMH MTV Corp) (Ch) [U].unf")) {
                System.out.println(file);
            }*/

            while (read) {
                Chunk chunk = readChunk(f);
                if (chunk == null) {
                    read = false;
                } else {
                    if (chunk.title.equals("MAPR")) {
                        mapper = chunk.value;
                    }
                    if (chunk.title.equals("NAME")) {
                        name = chunk.value;
                    }
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

    private static String readTag(RandomAccessFile f) {
        return readString(f, 4);
    }

    private static String readString(RandomAccessFile f, int length) {
        return new String(readBytes(f, length), StandardCharsets.UTF_8);
    }

    private static String readNullTerminatedString(RandomAccessFile f, int length) {
        byte[] bytes = new byte[length];
        int i = 0;
        while (i < length) {
            bytes[i] = readByte(f);
            if (bytes[i] == 0) {
                break;
            }
            i++;
        }
        return new String(Arrays.copyOfRange(bytes, 0, i), StandardCharsets.UTF_8);
    }

    private static byte[] readBytes(RandomAccessFile f, int length) {
        try {
            byte[] bytes = new byte[length];
            f.readFully(bytes);
            return bytes;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte readByte(RandomAccessFile f) {
        try {
            return f.readByte();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Set<String> VAR_TITLES = new HashSet<>(Arrays.asList("MAPR", "NAME", "WRTR", "READ"));
    private static Set<String> VAR_TITLESS = new HashSet<>(Arrays.asList("PRG", "CHR"));

    private static Chunk readChunk(RandomAccessFile f) {
        try {
            if (f.getFilePointer() + 8 > f.length()) {
                return null;
            }

            String tag = readTag(f);
            int length = readLEInt(f);

            if (f.getFilePointer() + length > f.length()) {
                return null;
            }

            Chunk chunk = new Chunk();
            chunk.setTitle(tag);

            if (VAR_TITLES.contains(tag)) {
                chunk.setValue(readNullTerminatedString(f, length));
            } else if (tag.startsWith("PRG")) {
                readBytes(f, length);
            } else if (tag.startsWith("CHR")) {
                readBytes(f, length);
            } else if (tag.equals("TVCI")) {
                readBytes(f, 1);
            } else if (tag.equals("BATR")) {
                readBytes(f, 1);
            } else if (tag.equals("MIRR")) {
                readBytes(f, 1);
            } else {
                readBytes(f, length);
            }
            return chunk;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Data
    @NoArgsConstructor
    private static class Chunk {
        String title;
        String value;
    }
}
