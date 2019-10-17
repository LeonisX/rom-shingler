package md.leonis.shingler;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

public class Main1024 {

    private static final int SHINGLE_LENGTH = 8;
    private static final int SAMPLE_PROBE = 256;

    @SuppressWarnings("all")
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        final File folder = new File("D:\\Downloads\\games");
        final File shdFolder = new File("D:\\Downloads\\games\\shd");
        final File sampleFolder256 = new File("D:\\Downloads\\games\\sample256");
        final File sampleFolder = new File("D:\\Downloads\\games\\sample" + SAMPLE_PROBE);

        final File shinglesMapFile = new File("shinglesMap" + SAMPLE_PROBE);

        shdFolder.mkdir();
        sampleFolder.mkdir();
        List<File> files = listFilesForFolder(folder);

        System.out.println("Generating shingles...");

        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);

            File shdFile = new File(shdFolder.getAbsolutePath() + File.separator + file.getName() + ".shg");

            if (shdFile.exists() && shdFile.length() > 0) {
                System.out.println(String.format(Locale.US, "Skipping: %s: %2.2f", file.getName(), ((i + 1) * 100.0 / files.size())));
                continue;
            }

            byte[] bytes = readFromFile(file);
            Set<Long> shingles = toShingles(bytes);

            System.out.println(String.format(Locale.US, "%s: %2.2f%%", file.getName(), ((i + 1) * 100.0 / files.size())));
            FileOutputStream fout = new FileOutputStream(shdFile);
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(shingles);
            oos.close();
            fout.close();
        }

        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("result.csv", true)));

        System.out.println("\nCalculating diff...");

        /*int k = 0;
        for (int i = 0; i < files.size() - 1; i++) {
            File file1 = files.get(i);
            Set<Long> s1Set = readShdFromFile(new File(shdFolder.getAbsolutePath() + File.separator + file1.getName() + ".shg"));

            for (int j = i + 1; j < files.size(); j++) {

                File file2 = files.get(j);
                System.out.println(String.format(Locale.US, "%s: %2.4f%%", file1.getName() + " -- " + file2.getName(), (k * 100.0 / (files.size() * files.size() / 2))));

                Set<Long> s2Set = readShdFromFile(new File(shdFolder.getAbsolutePath() + File.separator + file2.getName() + ".shg"));

                Set<Long> s1intersect = new HashSet<>(s1Set);
                Set<Long> s1union = new HashSet<>(s1Set);

                s1intersect.retainAll(s2Set);
                s1union.addAll(s2Set);
                double relative = s1intersect.size() * 100.0 / s1Set.size();
                double jakkard = s1intersect.size() * 100.0 / s1union.size();

                Result result = new Result(file1.getName(), file2.getName(), relative, jakkard);
                out.println(result.toString());
                out.flush();
                k++;
            }
        }*/

        // sample

        System.out.println("\nGetting samples...");

        Map<String, Set<Long>> shinglesMap = (!shinglesMapFile.exists()) ? new HashMap<>() : readShingleMapFromFile(shinglesMapFile);

        if (!shinglesMapFile.exists()) {

            for (int i = 0; i < files.size(); i++) {
                File file = files.get(i);

                File sampleFile = new File(sampleFolder.getAbsolutePath() + File.separator + file.getName() + ".shg");

                /*if (sampleFile.exists() && sampleFile.length() > 0) {
                    System.out.println(String.format(Locale.US, "Skipping: %s: %2.2f", file.getName(), ((i + 1) * 100.0 / files.size())));
                    continue;
                }*/

                System.out.println(String.format(Locale.US, "%s: %2.2f%%", file.getName(), ((i + 1) * 100.0 / files.size())));

                //TODO revert
                Set<Long> shingles = readShdFromFile(new File(sampleFolder256.getAbsolutePath() + File.separator + file.getName() + ".shg"));
                //Set<Long> shingles = readShdFromFile(new File(shdFolder.getAbsolutePath() + File.separator + file.getName() + ".shg"));
                Set<Long> filteredShingles = shingles.stream().filter(s -> s % SAMPLE_PROBE == 0).collect(Collectors.toSet());

                shinglesMap.put(file.getName(), filteredShingles);

                FileOutputStream fout = new FileOutputStream(sampleFile);
                ObjectOutputStream oos = new ObjectOutputStream(fout);
                oos.writeObject(shingles.stream().filter(s -> s % SAMPLE_PROBE == 0).collect(Collectors.toSet()));
                oos.close();
                fout.close();
            }

            FileOutputStream fout = new FileOutputStream(shinglesMapFile);
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(shinglesMap);
            oos.close();
            fout.close();
        }


        System.out.println("\nComparing...");

        int[] kx = {0};

        List<Name> names = files.stream().map(f -> new Name(f, false)).collect(Collectors.toList());

        //List<Result> results = files.parallelStream().flatMap(file1 -> {
        /*List<Result> results = */names.stream().forEach(name1 -> {
            List<Result> res = new ArrayList<>();

            int i = names.indexOf(name1);

            if (!name1.isDone()) {

                try {
                    List<Name> toCopy = new ArrayList<>();
                    toCopy.add(name1);

                    Set<Long> s1Set = shinglesMap.get(name1.getName());
                    //Set<Long> s1Set = readShdFromFile(new File(sampleFolder.getAbsolutePath() + File.separator + file1.getName() + ".shg"));

                    if (i != names.size() - 1) {
                        //System.out.println(i + ": " + file1.getName());

                        for (int j = i + 1; j < names.size(); j++) {

                            Name name2 = names.get(j);

                            if (name2.isDone()) {
                                kx[0]++;
                                continue;
                            }

                            //System.out.println(String.format(Locale.US, "%s: %2.4f%%", name1.getName() + " -- " + name2.getName(), (kx[0] * 100.0 / (names.size() * names.size() / 2))));

                            //Set<Long> s2Set = readShdFromFile(new File(sampleFolder.getAbsolutePath() + File.separator + file2.getName() + ".shg"));
                            Set<Long> s2Set = shinglesMap.get(name2.getName());

                            Set<Long> s1intersect = new HashSet<>(s1Set);
                            Set<Long> s1union = new HashSet<>(s1Set);

                            s1intersect.retainAll(s2Set);
                            s1union.addAll(s2Set);
                            double relative = s1intersect.size() * 100.0 / s1Set.size();
                            double jakkard = s1intersect.size() * 100.0 / s1union.size();

                            Result result = new Result(name1.getName(), name2.getName(), relative, jakkard);

                            if (jakkard > 35) {
                                name2.setDone(true);
                                System.out.println(result);
                                toCopy.add(name2);
                            }


                            res.add(result);
                            out.println(result.toString());
                            out.flush();
                            kx[0]++;
                        }
                    }
                    name1.setDone(true);

                    String catName = getCleanTitle(toCopy);

                    File cat = Paths.get("D:\\Downloads\\games\\gata", catName).toFile();

                    if (cat.exists()) {
                        catName = getTitle(toCopy);
                        cat = Paths.get("D:\\Downloads\\games\\gata", catName).toFile();
                    }

                    cat.mkdir();
                    for (Name name : toCopy) {
                        Files.move(name.getFile().toPath(), Paths.get("D:\\Downloads\\games\\gata", catName, name.getFile().getName()));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                System.out.println("Skipping: " + name1.getName());
                for (int j = i + 1; j < names.size(); j++) {
                    kx[0]++;
                }
            }
            //return res.stream();
        });/*.collect(Collectors.toList());*/

        out.close();
    }

    private static String getTitle(List<Name> names) {
        return names.stream().max(Comparator.comparing(Name::getIndex)).orElse(null).getName();
    }

    private static String getCleanTitle(List<Name> names) {
        return names.stream().max(Comparator.comparing(Name::getIndex)).orElse(null).getCleanName();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Set<Long>> readShingleMapFromFile(File file) throws IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream(file);
        ObjectInputStream ois = new ObjectInputStream(fis);
        Map<String, Set<Long>> shinglesMap = (Map<String, Set<Long>>) ois.readObject();

        ois.close();
        fis.close();
        return shinglesMap;
    }

    @SuppressWarnings("unchecked")
    private static Set<Long> readShdFromFile(File file) throws IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream(file);
        ObjectInputStream ois = new ObjectInputStream(fis);
        Set<Long> result = (Set<Long>) ois.readObject();

        ois.close();
        fis.close();
        return result;
    }

    @SuppressWarnings("all")
    private static Set<Long> toShingles(byte[] bytes) {
        Set<Long> hashes = new HashSet<>();

        for (int i = 0; i < bytes.length - SHINGLE_LENGTH + 1; i++) {
            CRC32 crc = new CRC32();
            crc.update(Arrays.copyOfRange(bytes, i, i + SHINGLE_LENGTH));
            long value = crc.getValue();
            if (value % SAMPLE_PROBE == 0) {
                hashes.add(crc.getValue());
            }
        }

        return hashes;
    }

    @SuppressWarnings("all")
    private static List<File> listFilesForFolder(final File folder) {
        return Arrays.stream(folder.listFiles()).filter(File::isFile).collect(Collectors.toList());
    }

    private static byte[] readFromFile(File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }


    private static class Name {

        private File file;
        private boolean done;
        private int index = 100;

        Name(File file, boolean done) {
            this.file = file;
            this.done = done;

            if (file.getName().contains("(U)")) {
                index += 100;
            }
            if (file.getName().contains("(W)")) {
                index += 99;
            }
            if (file.getName().contains("(E)")) {
                index += 80;
            }
            if (file.getName().contains("(F)")) {
                index += 70;
            }
            if (file.getName().contains("(G)")) {
                index += 70;
            }
            if (file.getName().contains("(J)")) {
                index += 60;
            }
            if (file.getName().contains("[b")) {
                index -= 50;
            }
            if (file.getName().contains("[a")) {
                index -= 5;
            }
            if (file.getName().contains("[h")) {
                index -= 20;
            }
            if (file.getName().contains("[t")) {
                index -= 10;
            }
            if (file.getName().contains("[p")) {
                index -= 10;
            }
            if (file.getName().contains("[f")) {
                index -= 10;
            }
            if (file.getName().contains("[T")) {
                index -= 10;
            }
            if (file.getName().contains("[!]")) {
                index += 10;
            }
            if (file.getName().contains("(PD)")) {
                index -= 45;
            }
            if (file.getName().contains("(Hack") || file.getName().contains("Hack)")) {
                index -= 45;
            }
            if (file.getName().contains("+")) {
                index -= 2;
            }
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }

        public String getName() {
            return file.getName();
        }

        public String getCleanName() {
            String result = file.getName();
            int braceIndex = result.indexOf("(");
            if (braceIndex > 0) {
                result = result.substring(0, braceIndex);
            }
            braceIndex = result.indexOf("[");
            if (braceIndex > 0) {
                result = result.substring(0, braceIndex);
            }
            return result.trim();
        }

        public boolean isDone() {
            return done;
        }

        public void setDone(boolean done) {
            this.done = done;
        }
    }

    private static class Result {

        private String file1;
        private String file2;
        private double relative;
        private double jakkard;

        Result(String file1, String file2, double relative, double jakkard) {
            this.file1 = file1;
            this.file2 = file2;
            this.relative = relative;
            this.jakkard = jakkard;
        }

        public String getFile1() {
            return file1;
        }

        public String getFile2() {
            return file2;
        }

        public double getRelative() {
            return relative;
        }

        public double getJakkard() {
            return jakkard;
        }

        @Override
        public String toString() {
            return "\"" + file1 + "\",\"" + file2 + "\",\"" + relative + "\",\"" + jakkard + "\"";
        }
    }
}
