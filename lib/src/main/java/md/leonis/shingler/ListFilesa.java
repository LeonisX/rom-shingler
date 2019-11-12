package md.leonis.shingler;

import md.leonis.shingler.model.GID;
import md.leonis.shingler.model.Name;
import md.leonis.shingler.utils.MeasureMethodTest;
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

public class ListFilesa {

    public static void main(String[] args) throws IOException {

        MeasureMethodTest.premain();

        System.out.println("Reading list of games...");
        List<File> files = Arrays.asList(Objects.requireNonNull(new File("D:\\Downloads\\Nintendo Famicom - GoodNES ROMS v3.23b Merged").listFiles()));

        System.out.println("Get file names from archives...");
        Map<File, List<String>> map = files.stream().collect(Collectors.toMap(Function.identity(), ListFilesa::listFiles));

        System.out.println("Preparing cache...");
        Main1024a.loadSamplesInCache(map.values().stream().flatMap(v -> v.stream().map(File::new)).collect(Collectors.toList()));

        System.out.println("Creating list of unique games...");
        Map<File, List<String>> filtereMap = new HashMap<>();

        // really unique
        map.forEach((key, value) -> {
            List<String> names = value.stream().filter(ListFilesa::nonHack).map(s -> s.substring(0, s.lastIndexOf('.'))).map(ListFilesa::normalize).distinct().filter(v -> !v.equals(normalize(key.getName().replace(".7z", "")))).collect(Collectors.toList());
            filtereMap.put(key, names);
        });

        List<String> lines = new ArrayList<>();

        filtereMap.entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().getName())).forEach(e -> {
            lines.add(e.getKey().getName().replace(".7z", ""));
            if (e.getValue().size() >= 1) {
                e.getValue().forEach(v -> lines.add("    " + v));
            }
            lines.add("");
        });

        Files.write(Paths.get("unique.txt"), lines);

        //generateFamilies(map, 1, 30);

        System.out.println("Generating families...");
        Main1024a.SAMPLES.forEach(index -> {
            if (!new File("low-jakkard" + index + ".csv").exists())
                generateFamilies(map, index, 30);
        });

        System.out.println("Calculating Jakkard deviations for families...");
        Map<String, Main1024a.Family> families1 = Main1024a.readFamiliesFromFile(new File("list-family" + 1));

        for (int i = 1; i < Main1024a.SAMPLES.size(); i++) {
            measureJakkard(Main1024a.SAMPLES.get(i), families1);
        }
    }

    private static void measureJakkard(Integer index, Map<String, Main1024a.Family> fams1) {

        List<Main1024a.Family> families1 = new ArrayList<>(fams1.values());

        Map<String, Main1024a.Family> families = Main1024a.readFamiliesFromFile(new File("list-family" + index));
        List<Main1024a.Family> familiesX = new ArrayList<>(families.values());

        double maxDeviation = 0;
        double medDeviation = 0;

        for (int i = 0; i < families1.size(); i++) {
            Main1024a.Family family1 = families1.get(i);
            Main1024a.Family familyX = familiesX.get(i);

            double sumDeviation = 0;

            for (int j = 0; j < family1.size(); j++) {

                double j1 = family1.getJakkardStatus(j);
                double jX = familyX.getJakkardStatus(j);
                double deviation = deviation(j1, jX);
                if (Double.valueOf(deviation).isNaN()) {
                    deviation = 0;
                }
                if (deviation > 10 && j1 > 3) {
                    System.out.println(String.format("%s:%s %2.2f->%2.2f(%2.4f%%)", family1.getName(), family1.get(j).getName(), j1, jX, deviation));
                }
                maxDeviation = Math.max(maxDeviation, deviation);
                sumDeviation += deviation;
            }
            medDeviation += sumDeviation / family1.size();
        }
        System.out.println(String.format("%s. Max: %2.4f%%; Med: %2.4f%%", index, maxDeviation, medDeviation / families.size()));
    }

    // a < b = ((b-a)/a) * 100
    static double deviation(double d1, double d2) {
        return Math.abs(d2 - d1) / d1 * 100;
    }

    private static void generateFamilies(Map<File, List<String>> map, int index, int jakkardIndex) {

        Main1024a.cache.fullCleanup();

        File familyFile = new File("list-family" + index);

        Map<String, Main1024a.Family> families;
        if (familyFile.exists()) {
            System.out.println(String.format("%nReading families from file %s...", familyFile));
            families = Main1024a.readFamiliesFromFile(familyFile);
        } else {
            System.out.println("\nGenerating families...");
            Map<String, List<Name>> namesList = new HashMap<>();
            map.forEach((key, value) -> namesList.put(key.getName(), value.stream().map(v -> new Name(new File(v), false)).collect(Collectors.toList())));

            families = namesList.entrySet().stream()/*.filter(e -> e.getValue().size() != 1)*/
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> new Main1024a.Family(e.getKey(), e.getValue())));
        }

        Main1024a.calculateRelations(families, index, familyFile);

        System.out.println("Saving family...");
        Main1024a.serialize(familyFile, families);

        int total = (int) map.values().stream().mapToLong(Collection::size).sum();
        int inFamily = families.values().stream().map(Main1024a.Family::size).mapToInt(Integer::intValue).sum();

        System.out.println("Total: " + total);
        System.out.println("In family: " + inFamily);
        System.out.println("Not in family: " + (total - inFamily));

        Main1024a.saveDropCsv(families, index, 50);

        //TODO jakkardIndex
        /*Main1024a.drop(families, 50);
        inFamily = families.values().stream().map(Main1024a.Family::size).mapToInt(Integer::intValue).sum();

        System.out.println("Total: " + total);
        System.out.println("In family: " + inFamily);
        System.out.println("Not in family: " + (total - inFamily));*/

        System.out.println("==");
        //TODO merge jakkardIndex 20
        //TODO index 1 or 16. Better 1
        //families = mergeFamilies(families, 30, 64);

        //TODO проходиться сиротами и смотреть куда пристроить.
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

    public static List<String> listFiles(File file) {
        try (SevenZFile archiveFile = new SevenZFile(file)) {
            return StreamSupport.stream(archiveFile.getEntries().spliterator(), false).map(SevenZArchiveEntry::getName).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    public static Map<String, GID> listFiles2(File file) {
        Map<String, GID> result = new LinkedHashMap<>();

        try (SevenZFile sevenZFile = new SevenZFile(file)) {
            SevenZArchiveEntry entry = sevenZFile.getNextEntry();
            while (entry != null) {
                byte[] content = new byte[(int) entry.getSize()];
                byte[] contentwh = Arrays.copyOfRange(content,16, content.length);
                sevenZFile.read(content, 0, content.length);
                result.put(entry.getName(), new GID(entry.getName(), entry.getSize(), entry.getCrcValue(), Main1024a.md5(content), Main1024a.sha1(content), Main1024a.crc32(contentwh), Main1024a.md5(contentwh), Main1024a.sha1(contentwh), null));
                entry = sevenZFile.getNextEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }
}
