package md.leonis.shingler.gui;

import liquibase.util.file.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class FormatsConverter {

    public static void main(String[] args) throws IOException {

        //bin2s19();

        st2ToBin("C:\\Users\\user\\Downloads\\_bysys\\studio2\\RCA Studio II files [ST2]");

        /*bin2ToSt2(Paths.get("C:\\Users\\user\\Downloads\\_bysys\\studio2\\RCA Studio II files [BIN] [ST2]\\TV School House I (USA).bin"),
                "??", "PT", "18V500", "TV SCHOOL HOUSE");*/

        /*bin2ToSt2(Paths.get("C:\\Users\\user\\Downloads\\_bysys\\studio2\\RCA Studio II files [BIN] [ST2]\\Sports Fan (Baseball & Sumo Wrestling) (CAS-130).bin"),
                "??", "PT", "CAS130", "SPORTS FAN");*/

        /*bin2ToSt2(Paths.get("C:\\Users\\user\\Downloads\\_bysys\\studio2\\RCA Studio II files [BIN] [ST2]\\Concentration Match (Europe).bin"),
                "??", "??", "MG-202", "Concentration Match".toUpperCase());

        bin2ToSt2(Paths.get("C:\\Users\\user\\Downloads\\_bysys\\studio2\\RCA Studio II files [BIN] [ST2]\\Pinball (Europe).bin"),
                "??", "??", "MG-205", "Pinball".toUpperCase());

        bin2ToSt2(Paths.get("C:\\Users\\user\\Downloads\\_bysys\\studio2\\RCA Studio II files [BIN] [ST2]\\Speedway + Tag (Europe).bin"),
                "??", "??", "MG-211", "Speedway/Tag".toUpperCase());

        bin2ToSt2(Paths.get("C:\\Users\\user\\Downloads\\_bysys\\studio2\\RCA Studio II files [BIN] [ST2]\\Star Wars (Europe).bin"),
                "??", "??", "MG-203", "Star Wars".toUpperCase());

        bin2ToSt2(Paths.get("C:\\Users\\user\\Downloads\\_bysys\\studio2\\RCA Studio II files [BIN] [ST2]\\TV Arcade II - Fun with Numbers (USA).bin"),
                "??", "??", "18V401", "TV Arcade II : Fun with Numbers".toUpperCase());

        bin2ToSt2(Paths.get("C:\\Users\\user\\Downloads\\_bysys\\studio2\\RCA Studio II files [BIN] [ST2]\\TV Casino Series - TV Bingo (USA, Europe).bin"),
                "??", "??", "18V601", "TV Casino Series : TV Bingo".toUpperCase());*/

        // TV Casino Series - TV Bingo (USA, Europe)

        //    MG-200 Grand Pack (Doodle, Patterns, Blackjack and Bowling)
        //    MG-201 Bingo
        //    MG-204 Math Fun (School House II)
        //    MG-206 Biorythm
        //    MG-207 Tennis/Squash
        //    MG-208 Fun with Numbers
        //    MG-209 Computer Quiz (School House I)
        //    MG-210 Baseball
        //    MG-212 Spacewar Intercept
        //    MG-213 Gun Fight/Moon ship

    }

    // https://archive.kontek.net/studio2.classicgaming.gamespy.com/cart.htm
    private static void bin2ToSt2(Path path, String author, String dumper, String catalogue, String title) throws IOException {

        byte[] header = new byte[256];
        byte[] body = Files.readAllBytes(path);

        //TODO
        /*
         * Offset Contents 	Reqd 	Notes
         * 0-3 	   Header 	 Y 	RCA2 in ASCII code
         * 4 	   Blocks 	 Y 	Total number of 256 byte blocks in file (including this one)
         * 5 	   Format 	 Y 	Format Code (this is format number 1)
         * 6 	   Video 	 Y 	If non-zero uses a special video driver, and programs cannot assume that it uses the standard Studio 2 one (top of screen at $0900+RB.0).
         *                      A value of '1' here indicates the RAM is used normally, but scrolling is not (e.g. the top of the page is always at $900).
         * 7 	    -
         * 8,9 	   Author 	 N 	2 byte ASCII code indicating the identity of the program coder.
         * 10,11   Dumper 	 N 	2 byte ASCII code indicating the identity of the ROM Source.
         * 12-15 	-
         * 16-25   Catalogue N 	RCA Catalogue Code as ASCIIZ string. If a homebrew ROM, may contain any identifying code you wish.
         * 26-31 	-
         * 32-63   Title 	 N 	Cartridge Program Title as ASCIIZ string.
         * 64-127  Block Pages Y Contain the page addresses for each 256 byte block. The first byte at 64, contains the target address of the
         *                      data at offset 256, the second byte contains the target address of the data at offset 512, and so on. Unused block bytes should be filled
         *                      with $00 (an invalid page address). So, if byte 64 contains $1C, the ROM is paged into memory from $1C00-$1CFF
         * 128-255 	-
         * 256 	Block 1 		(Page address at 64)
         * 512 	Block 2 		(Page address at 65)
         * 			and so on
         */
        int blocksCount = body.length / 256;

        header[0] = 'R';
        header[1] = 'C';
        header[2] = 'A';
        header[3] = '2';
        header[4] = (byte) (blocksCount + 1);
        header[5] = 1;
        header[6] = 0; // ??
        header[8] = (byte) author.charAt(0);
        header[9] = (byte) author.charAt(1);
        header[10] = (byte) dumper.charAt(0);
        header[11] = (byte) dumper.charAt(1);

        for (int i = 0; i < catalogue.length(); i++) {
            header[16 + i] = (byte) catalogue.charAt(i);
        }

        for (int i = 0; i < title.length(); i++) {
            header[32 + i] = (byte) title.charAt(i);
        }

        int offset = body.length >= 2048 ? 8 : 4;

        for (int i = 0; i < blocksCount; i++) {
            header[64 + i] = (byte) (offset + i);
        }

        String fileName = FilenameUtils.removeExtension(path.toString());
        Files.write(Paths.get(fileName + "-from-bin.st2"), ArrayUtils.addAll(header, body));
    }

    private static void st2ToBin(String root) throws IOException {

        for (Path path : listFilesUsingFilesList(root)) {
            if (path.toString().toLowerCase().endsWith(".st2")) {
                byte[] bytes = Files.readAllBytes(path);
                String fileName = FilenameUtils.removeExtension(path.toString());
                Files.write(Paths.get(fileName + "-from-st2.bin"), Arrays.copyOfRange(bytes, 256, bytes.length));
            }
        }
    }

    // http://web.archive.org/web/20071002020002/https://linux.die.net/man/5/srec
    private static void bin2s19() throws IOException {

        String root = "D:\\Emulsex\\dosbox\\";

        for (Path path : listFilesUsingFilesList(root)) {
            if (path.toString().toLowerCase().endsWith(".bin")) {
                byte[] bytes = Files.readAllBytes(path);
                String fileName = FilenameUtils.removeExtension(path.toString());
                Files.write(Paths.get(fileName + ".s19"), toS19(bytes).getBytes());
            }
        }
    }

    private static String toS19(byte[] bytes) {

        int offset = 0x8000;
        int size = 0x20;

        StringBuilder sb = new StringBuilder();

        for (int y = 0; y < Math.floor(bytes.length * 1.0 / size); y++) {
            sb.append("S1");
            int count = size + 2 + 1;
            sb.append(String.format("%02x", count).toUpperCase());
            byte[] byt = ByteBuffer.allocate(4).putInt(offset + y * size).array();
            //System.out.println(byt[3]);
            //System.out.println(byt[2]);
            int sum = ((byte) count + 128) + (byt[3] + 128) + (byt[2] + 128);
            sb.append(String.format("%02x", offset + y * size).toUpperCase());
            for (int x = 0; x < size; x++) {
                if (y * size + x == bytes.length) {
                    break;
                }
                sb.append(String.format("%02x", bytes[y * size + x]).toUpperCase());
                sum += ((byte) bytes[y * size + x] + 128);
                // 11000010
                // 00111101
                // 01000010

            }
            String hex = String.format("%02x%n", ~(byte) sum - 128);
            sb.append(hex.substring(hex.length() - 4).toUpperCase());
        }

        // S1238000BB87B8330096004A260CCE81A6DF23CE819DDF2520164A260CCE81A6DF23CE823D
        // S123802005DF252007CE8243DF2320F2CE002EBD87AECE0010BD87AECE0008BD87AE863C1D

        return sb.toString();
    }

    private static Set<Path> listFilesUsingFilesList(String dir) throws IOException {
        try (Stream<Path> stream = Files.list(Paths.get(dir))) {
            return stream.filter(file -> !Files.isDirectory(file)).collect(Collectors.toSet());
        }
    }
}
