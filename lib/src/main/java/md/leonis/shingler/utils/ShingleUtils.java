package md.leonis.shingler.utils;

import md.leonis.shingler.Cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static md.leonis.shingler.utils.BinaryUtils.crc32;

public class ShingleUtils {

    private static final int SHINGLE_LENGTH = 8;

    @Measured
    public static int[] toShingles(byte[] bytes) {
        Set<Integer> hashes = new HashSet<>();

        for (int i = 0; i < bytes.length - SHINGLE_LENGTH + 1; i++) {
            hashes.add(crc32(Arrays.copyOfRange(bytes, i, i + SHINGLE_LENGTH)));
        }

        return hashes.stream().sorted().mapToInt(l -> l).toArray();
    }

    @SuppressWarnings("unchecked")
    @Measured
    public static Map<String, int[]> loadMap(File file) {

        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Map<String, int[]> shinglesMap = (Map<String, int[]>) ois.readObject();

            ois.close();
            fis.close();
            return shinglesMap;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Measured
    public static int[] load(File file) {

        int count = (int) file.length() / 4;
        int[] shingles = new int[count];
        try (FileChannel fc = FileChannel.open(file.toPath())) {
            MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            for (int i = 0; i < count; i++) {
                shingles[i] = mbb.getInt();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return shingles;
    }

    @Measured
    public static int[] load(Path file) {

        try {
            long size = Files.size(file);
            int count = (int) size / 4;
            int[] shingles = new int[count];
            try (FileChannel fc = FileChannel.open(file)) {
                MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY, 0, size);
                for (int i = 0; i < count; i++) {
                    shingles[i] = mbb.getInt();
                }
            }
            return shingles;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Measured
    public static int[] loadFromCache(Cache<String, int[]> cache, File file) {
        String key = file.getAbsolutePath();
        int[] cachedValue = cache.get(key);
        if (cachedValue != null) {
            return cachedValue;
        }

        int[] result = ShingleUtils.load(file);
        if (getFreeMemory() < 250) {
            System.out.println(String.format("We have only %s Mb of RAM", getFreeMemory()));
            System.out.println("Cleaning Cache...");
            cache.cleanup();
            System.gc();
            System.out.println(String.format("Now we have %s Mb of RAM", getFreeMemory()));
        }
        cache.put(key, result);

        return result;
    }

    @Measured
    public static int[] loadFromCache(Cache<String, int[]> cache, Path file) {
        String key = file.toAbsolutePath().toString();
        int[] cachedValue = cache.get(key);
        if (cachedValue != null) {
            return cachedValue;
        }

        int[] result = ShingleUtils.load(file);
        if (getFreeMemory() < 250) {
            System.out.println(String.format("We have only %s Mb of RAM", getFreeMemory()));
            System.out.println("Cleaning Cache...");
            cache.cleanup();
            System.gc();
            System.out.println(String.format("Now we have %s Mb of RAM", getFreeMemory()));
        }
        cache.put(key, result);

        return result;
    }

    @Measured
    public static void save(int[] shingles, File file) {

        int count = shingles.length;
        try (FileChannel fc = FileChannel.open(file.toPath(), EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING))) {
            MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_WRITE, 0, count * 4);
            for (int shingle : shingles) {
                mbb.putInt(shingle);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Measured
    public static void save(int[] shingles, Path file) {

        int count = shingles.length;
        try (FileChannel fc = FileChannel.open(file, EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING))) {
            MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_WRITE, 0, count * 4);
            for (int shingle : shingles) {
                mbb.putInt(shingle);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static long getFreeMemory() {
        long allocatedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long presumableFreeMemory = Runtime.getRuntime().maxMemory() - allocatedMemory;
        return presumableFreeMemory / 1024 / 1024;
    }
}
