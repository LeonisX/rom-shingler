package md.leonis.shingler;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

public class Main2 {

    private static final int SHINGLE_LENGTH = 8;
    private static final int SAMPLE_PROBE = 64;

    @SuppressWarnings("all")
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        final File folder = new File("D:\\Downloads\\games");
        final File shdFolder = new File("D:\\Downloads\\games\\shd");
        final File sampleFolder = new File("D:\\Downloads\\games\\sample" + SAMPLE_PROBE);
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
                double relative = s1intersect.size() * 1.0 / s1Set.size();
                double jakkard = s1intersect.size() * 1.0 / s1union.size();

                Result result = new Result(file1.getName(), file2.getName(), relative, jakkard);
                out.println(result.toString());
                out.flush();
                k++;
            }
        }*/

        // sample

        System.out.println("\nGetting samples...");

        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);

            File sampleFile = new File(sampleFolder.getAbsolutePath() + File.separator + file.getName() + ".shg");

            if (sampleFile.exists() && sampleFile.length() > 0) {
                System.out.println(String.format(Locale.US, "Skipping: %s: %2.2f", file.getName(), ((i + 1) * 100.0 / files.size())));
                continue;
            }

            System.out.println(String.format(Locale.US, "%s: %2.2f%%", file.getName(), ((i + 1) * 100.0 / files.size())));

            Set<Long> shingles = readShdFromFile(new File(shdFolder.getAbsolutePath() + File.separator + file.getName() + ".shg"));

            FileOutputStream fout = new FileOutputStream(sampleFile);
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(shingles.stream().filter(s -> s % SAMPLE_PROBE == 0).collect(Collectors.toSet()));
            oos.close();
            fout.close();
        }


        int[] kx = {0};

        files.parallelStream().forEach(file1 -> {
            try {
                Set<Long> s1Set = readShdFromFile(new File(sampleFolder.getAbsolutePath() + File.separator + file1.getName() + ".shg"));

                int i = files.indexOf(file1);

                if (i != files.size() - 1) {
                    //System.out.println(i + ": " + file1.getName());

                    for (int j = i + 1; j < files.size(); j++) {

                        File file2 = files.get(j);
                        System.out.println(String.format(Locale.US, "%s: %2.4f%%", file1.getName() + " -- " + file2.getName(), (kx[0] * 100.0 / (files.size() * files.size() / 2))));

                        Set<Long> s2Set = readShdFromFile(new File(sampleFolder.getAbsolutePath() + File.separator + file2.getName() + ".shg"));

                        Set<Long> s1intersect = new HashSet<>(s1Set);
                        Set<Long> s1union = new HashSet<>(s1Set);

                        s1intersect.retainAll(s2Set);
                        s1union.addAll(s2Set);
                        double relative = s1intersect.size() * 1.0 / s1Set.size();
                        double jakkard = s1intersect.size() * 1.0 / s1union.size();

                        Result result = new Result(file1.getName(), file2.getName(), relative, jakkard);
                        out.println(result.toString());
                        out.flush();
                        kx[0]++;
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        out.close();

        // 10-Yard Fight (J) (PRG0) [p1][o1].nes:
        //0.9975749661894324
        //0.9950227928179366

        //10-Yard Fight (J) (PRG1) [!].nes:
        //0.8302009979946836
        //0.7096954233774517

        //10-Yard Fight (U) [!].nes:
        //0.670195401762813
        //0.4609635617141391

        //1919 by Me_Dave (Hard version) (1942 Hack).nes:
        //0.007741454087581028
        //0.0028800444151427878
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
