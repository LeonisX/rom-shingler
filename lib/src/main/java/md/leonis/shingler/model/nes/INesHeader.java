package md.leonis.shingler.model.nes;

import lombok.Data;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Arrays;

// Partial iNES coverage
// https://wiki.nesdev.com/w/index.php/INES
// https://github.com/eteran/libunif/blob/master/load_ines.c
@Data
public class INesHeader {

    // f000
    // 1111 0000 0000 0000

    private static final byte[] HEADER = {0x4E, 0x45, 0x53, 0x1A}; // 0-3: Constant $4E $45 $53 $1A ("NES" followed by MS-DOS end-of-file)

    private byte[] raw = new byte[16]; // 16 bytes

    private int prgSize;
    private int chrSize;
    private int trainerSize;
    private int prgAllocSize;
    private int chrAllocSize;

    private int mapper;
    private int submapper = 0;
    private int version;

    private boolean isNES() {
        return Arrays.equals(Arrays.copyOfRange(raw, 0, 4), HEADER);
    }

    public INesHeader(File file) {
        try {
            RandomAccessFile f = new RandomAccessFile(file, "r");
            f.readFully(raw);
            if (!isNES()) {
                throw new RuntimeException("Not NES header");
            }

            //    If byte 7 AND $0C = $08, and the size taking into account byte 9 does not exceed the actual size of the ROM image, then NES 2.0.
            //    If byte 7 AND $0C = $00, and bytes 12-15 are all 0, then iNES.
            //    Otherwise, archaic iNES.
            version = (raw[7] & 0x0C) == 0x08 ? 2 : 1;

            /*if (file.getName().startsWith("100-in-1 Contra Function 16 [p1][!]")) {
                System.out.println(raw);
            }*/
            mapper = (Byte.toUnsignedInt(raw[6]) >> 4) | (Byte.toUnsignedInt(raw[7]) & 0xF0);

            if (version == 2) {
                mapper = Byte.toUnsignedInt(raw[8]) & 0x0F << 8 | mapper;
                submapper = Byte.toUnsignedInt(raw[8]) & 0x0F >> 4;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
