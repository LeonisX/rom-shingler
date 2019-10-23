package md.leonis.shingler;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ListFiles {

    public static void main(String[] args) throws IOException {

        List<File> files = Arrays.asList(Objects.requireNonNull(new File("D:\\Downloads\\Nintendo Famicom - GoodNES ROMS v3.23b Merged").listFiles()));

        Map<File, List<String>> map = files.stream().collect(Collectors.toMap(Function.identity(), ListFiles::listFiles));

        List<String> lines = new ArrayList<>();

        map.forEach((key, value) -> {
            List<String> names = value.stream().filter(ListFiles::nonHack).map(ListFiles::normalize).distinct().filter(v -> !v.equals(normalize(key.getName().replace(".7z", "")))).collect(Collectors.toList());
            map.replace(key, names);
        });


        map.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().getName())).forEach(e -> {
            lines.add(e.getKey().getName().replace(".7z", ""));
            if (e.getValue().size() > 1) {
                e.getValue().forEach(v -> lines.add("    " + v));
            }
            lines.add("");
        });

        Files.write(Paths.get("s"), lines);
    }

    private static boolean nonHack(String s) {
        return !s.contains("(Hack)") && !s.contains("(Hack ") && !s.contains(" Hack)");
    }

    private static String normalize(String s) {
        List<String> chunks = new ArrayList<>();

        toChunks(s).forEach(c -> {
            if (
                    !c.matches("^\\[.+\\]$") &&
                            !c.matches("^\\(.+\\)$")


                    /*!c.matches("^\\[[abhfopt]\\d+]$") &&
                            !c.matches("^\\[[b][abc]]$") &&
                            !c.matches("^\\[[a][abcdef]]$") &&
                            !c.matches("^\\[!p]$") &&
                            !c.matches("^\\[T[\\-+].+]$") &&
                            !c.matches("^\\(PRG[0123]\\)$") &&
                            !c.matches("^\\(CHR[0123]\\)$") &&
                            !c.matches("^\\(REV[012ABC]\\)$") &&
                            !c.matches("^\\(REV1\\.[123x]\\)$") &&
                            !c.matches("^\\[hFFE]$") &&
                            !c.matches("^\\[hM\\d\\d\\d]$") &&

                            !c.matches("^\\[hM\\d{2}]$") &&

                            !c.matches("^\\((JU|UE|PC10|VC|VS|GC|Ch|M3|M4|M5|Chi|ChS|ChT|Eng|KC|[WJUERKGFICS]|Tw|Sw|Beta|Prototype|Prototype1|Prototype-1|Prototype2|Prototype-2|Sample|Decrypted|PAL|NTSC|Unl|Wxn|Gamepad|GBA e-Reader|Player 1 Mode|Player 2 Mode|GBA-Capcom Classics|FDS Conversion|FDS Conversion,Kaiser)\\)$") &&

                            !c.matches("^\\((Eternal Life|No Timer|Direction|2 Joys|E-GC|J-GC|J-AC|E-VC|J-VC|No-Bug|Dev Version|FDS Conversion, Kaiser Hacked|FDS Conversion, CHR-ROM version|Arabic|Maxi 15 Pack Version|With +4 Menu|START|Sound|Defaced by NA|NTSC by Ian Bell & David Braben)\\)$") &&

                            !c.matches("^\\((Newer Prototype|Older Prototype|Test Version|Wisdom Tree|SRAM Saving|Title & Weapon-Magic Exploit by Parasyte|Title by Parasyte v1.0|Weapon-Magic Exploit by Parasyte|Earlier|Open Bus|Cutscene Extra Animation|Ladies)\\)$") &&

                            !c.matches("^\\((7 Lasers and Double Bullets|Gun-Nac|Heavy Barrel|SRAM|UBI Soft|Jackal|SELECT\\+START|Jackie Chan|No Time|V5.0 CHR 6.0|V5.0 CHR 1.3|by dragon2snow|Push START|SOMARI-W|Demo Cart|Coolboy Version|NTSC by nfzxyd|Dragon)\\)$") &&

                            !c.matches("^\\((CH88|6 Lifebars|Sachen-HES|Hwang Shinwei,RCM|Sachen-NTSC|Sachen-PAL|Sachen-Hacker|Sachen-FC|MGC-008|Beta1|Panesian|Color Dreams|YOKO-Y1|Final|V1995|VT-482|ZR006|Blue Version|REV.B)\\)$") &&

                            !c.matches("^\\((JY-011|NT-851|YH2001|MD102|NT-328|YY-030|M1274|16 Player|8 Player|Time|Menu|9 Lives|50 Lives|100 Lives|Namco|AbabSoft|Dung Dung No 1)\\)$") &&


                            !c.matches("^\\((Sachen|Sachen-Chinese|Sachen-English|Bunch|Hacker|Hwang Shinwei|RCM Group|SuperGame|Gluk Video|Gluk Video, NTDEC|Camerica|Aladdin|Shin Data|Rex-Soft|Rex Soft|Subor|Tengen|Sugar Softec|Sachen-JAP|Sachen-USA|AGCI|HES|Joy Van|AVE|KaSing)\\)$") &&

                            !c.matches("^\\((GameTec)\\)$") &&


                            !c.matches("^\\(1[98]\\d\\d\\)$") &&
                            !c.matches("^\\([vV][123456]\\.\\da?\\)$") &&

                            !c.matches("^\\(Mapper \\d+\\)$") &&
                            !c.matches("^\\(NJ0\\d\\d\\)$") &&
                            !c.matches("^\\(ES-\\d+\\)$") &&

                            !c.matches("^\\(Hack\\)$") &&
                            !c.matches("^\\(Hack .+\\)$") &&
                            !c.matches("^\\(.+ Hack\\)$") &&

                            !c.matches("^\\[[!U]]$")*/
            ) {
                chunks.add(c);
            }
        });

        return String.join(" ", chunks);
    }

    static List<String> toChunks(String string) {
        List<String> chunks = new ArrayList<>();

        int openSB = 0;
        int openBR = 0;

        StringBuilder chunk = new StringBuilder();

        char[] chars = string.toCharArray();
        for (char c : chars) {
            switch (c) {
                case '[':
                    if (openSB == 0 && openBR == 0 && chunk.length() > 0) {
                        chunks.add(chunk.toString().trim());
                        chunk = new StringBuilder();
                    }
                    openSB++;
                    break;
                case ']':
                    if (openSB == 0 && openBR == 0 && chunk.length() > 0) {
                        chunks.add(chunk.append(']').toString().trim());
                        chunk = new StringBuilder();
                    }
                    openSB--;
                    break;
                case '(':
                    if (openSB == 0 && openBR == 0 && chunk.length() > 0) {
                        chunks.add(chunk.toString().trim());
                        chunk = new StringBuilder();
                    }
                    openBR++;
                    break;
                case ')':
                    if (openSB == 0 && openBR == 0 && chunk.length() > 0) {
                        chunks.add(chunk.append(')').toString().trim());
                        chunk = new StringBuilder();
                    }
                    openBR--;
                    break;
            }
            chunk.append(c);
        }
        if (chunk.length() > 0) {
            chunks.add(chunk.toString());
        }
        return chunks;
    }

    private static String getCleanName(String s) {
        int braceIndex = s.indexOf("(");
        if (braceIndex > 0) {
            s = s.substring(0, braceIndex);
        }
        braceIndex = s.indexOf("[");
        if (braceIndex > 0) {
            s = s.substring(0, braceIndex);
        }
        return s.trim();
    }

    private static List<String> listFiles(File file) {
        try (SevenZFile archiveFile = new SevenZFile(file)) {
            return StreamSupport.stream(archiveFile.getEntries().spliterator(), false).map(SevenZArchiveEntry::getName).map(s -> s.substring(0, s.lastIndexOf('.'))).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }
}
