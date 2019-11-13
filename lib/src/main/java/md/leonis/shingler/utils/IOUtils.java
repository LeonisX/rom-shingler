package md.leonis.shingler.utils;

import md.leonis.shingler.model.Family;
import md.leonis.shingler.model.GID;
import md.leonis.shingler.model.RomsCollection;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static md.leonis.shingler.utils.BinaryUtils.*;

public class IOUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(IOUtils.class);

    @SuppressWarnings("all")
    @Measured
    public static List<File> listFiles(final File folder) {
        return Arrays.stream(folder.listFiles()).filter(File::isFile).collect(Collectors.toList());
    }

    @Measured
    public static List<Path> listFiles(final Path folder) {
        LOGGER.info("Getting a list of files...");
        List<Path> fileList = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) {
                    fileList.add(path);
                }
            }
        } catch (IOException e) {
            LOGGER.error("Can't get list of files from dir: {}", folder.toString(), e);
        }
        return fileList;
    }

    @Measured
    public static void serialize(File file, Object object) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Measured
    public static byte[] loadBytes(File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }

    @Measured
    public static byte[] loadBytes(Path file) throws IOException {
        return Files.readAllBytes(file);
    }

    @SuppressWarnings("unchecked")
    @Measured
    public static Map<String, Family> loadFamilies(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Map<String, Family> result = (Map<String, Family>) ois.readObject();

            ois.close();
            fis.close();
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    @Measured
    static Map<String, GID> loadGIDs(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Map<String, GID> result = (Map<String, GID>) ois.readObject();

            ois.close();
            fis.close();
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Measured
    public static RomsCollection loadCollection(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            RomsCollection result = (RomsCollection) ois.readObject();

            ois.close();
            fis.close();
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Measured
    public static byte[] loadBytesFromArchive(Path file, String fileName) {

        try (SevenZFile sevenZFile = new SevenZFile(file.toFile())) {
            SevenZArchiveEntry entry = sevenZFile.getNextEntry();
            while (entry != null) {
                if (entry.getName().equals(fileName)) {
                    byte[] content = new byte[(int) entry.getSize()];
                    sevenZFile.read(content, 0, content.length);
                    return content;
                }
                entry = sevenZFile.getNextEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException(String.format("File %s not found in %s archive", fileName, file));
    }

    public static Stream<GID> loadGIDsFromArchive(Path file) {
        try (SevenZFile archiveFile = new SevenZFile(file.toFile())) {
            return StreamSupport.stream(archiveFile.getEntries().spliterator(), false)
                    .map(e -> new GID(e.getName(), e.getSize(), e.getCrcValue(), null, null, null, null, null, file.getFileName().toString()));
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    public static List<GID> loadHashedGIDsFromArchive(Path file) {
        List<GID> result = new ArrayList<>();

        try (SevenZFile sevenZFile = new SevenZFile(file.toFile())) {
            SevenZArchiveEntry entry = sevenZFile.getNextEntry();
            while (entry != null) {
                byte[] bytes = new byte[(int) entry.getSize()];
                sevenZFile.read(bytes, 0, bytes.length);
                byte[] bytestwh = Arrays.copyOfRange(bytes, 16, bytes.length);
                result.add(new GID(entry.getName(), entry.getSize(), entry.getCrcValue(), md5(bytes), sha1(bytes), crc32(bytestwh), md5(bytestwh), sha1(bytestwh), file.getFileName().toString()));
                entry = sevenZFile.getNextEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    @Measured
    public static void deleteFile(Path path) {
        try {
            Files.delete(path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void createDirectories(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            LOGGER.error("Can't create directory: {}", path.toString(), e);
        }
    }
}
