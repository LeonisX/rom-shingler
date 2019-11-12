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
    public static long[] toShingles(byte[] bytes) {
        Set<Long> hashes = new HashSet<>();

        for (int i = 0; i < bytes.length - SHINGLE_LENGTH + 1; i++) {
            hashes.add(crc32(Arrays.copyOfRange(bytes, i, i + SHINGLE_LENGTH)));
        }

        return hashes.stream().sorted().mapToLong(l -> l).toArray();
    }

    @SuppressWarnings("unchecked")
    @Measured
    public static Map<String, long[]> loadMap(File file) {

        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Map<String, long[]> shinglesMap = (Map<String, long[]>) ois.readObject();

            ois.close();
            fis.close();
            return shinglesMap;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Measured
    public static long[] load(File file) {

        int count = (int) file.length() / 8;
        long[] shingles = new long[count];
        try (FileChannel fc = FileChannel.open(file.toPath())) {
            MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            for (int i = 0; i < count; i++) {
                shingles[i] = mbb.getLong();
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return shingles;
    }

    @Measured
    public static long[] load(Path file) {

        try {
            long size = Files.size(file);
            int count = (int) size / 8;
            long[] shingles = new long[count];
            try (FileChannel fc = FileChannel.open(file)) {
                MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY, 0, size);
                for (int i = 0; i < count; i++) {
                    shingles[i] = mbb.getLong();
                }
            }
            return shingles;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Measured
    public static long[] loadFromCache(Cache<File, long[]> cache, File file) {
        long[] cachedValue = cache.get(file);
        if (cachedValue != null) {
            return cachedValue;
        }

        long[] result = ShingleUtils.load(file);
        if (getFreeMemory() < 250) {
            System.out.println(String.format("We have only %s Mb of RAM", getFreeMemory()));
            System.out.println("Cleaning Cache...");
            cache.cleanup();
            System.gc();
            System.out.println(String.format("Now we have %s Mb of RAM", getFreeMemory()));
        }
        cache.put(file, result);

        return result;
    }

    @Measured
    public static void save(long[] shingles, File file) {

        int count = shingles.length;
        try (FileChannel fc = FileChannel.open(file.toPath(), EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING))) {
            MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_WRITE, 0, count * 8);
            for (long shingle : shingles) {
                mbb.putLong(shingle);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Measured
    public static void save(long[] shingles, Path file) {

        int count = shingles.length;
        try (FileChannel fc = FileChannel.open(file, EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING))) {
            MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_WRITE, 0, count * 8);
            for (long shingle : shingles) {
                mbb.putLong(shingle);
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
