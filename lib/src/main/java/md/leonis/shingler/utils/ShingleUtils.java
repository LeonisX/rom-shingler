package md.leonis.shingler.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.Map;

public class ShingleUtils {

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
}
