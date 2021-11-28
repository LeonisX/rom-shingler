package md.leonis.shingler.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import md.leonis.shingler.model.*;
import md.leonis.shingler.model.dto.FamiliesDto;
import md.leonis.shingler.model.dto.FamilyDto;
import md.leonis.shingler.model.dto.ResultDto;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.nio.file.FileVisitResult.CONTINUE;
import static md.leonis.shingler.model.ConfigHolder.families;
import static md.leonis.shingler.utils.BinaryUtils.*;
import static md.leonis.shingler.utils.FileUtils.backupFile;

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

    public static void downloadFromUrl(URL url, Path path) throws IOException {

        FileUtils.createDirectories(path.getParent());

        ReadableByteChannel channel = null;
        FileOutputStream fileOutputStream = null;

        try {
            channel = Channels.newChannel(url.openStream());
            fileOutputStream = new FileOutputStream(path.toAbsolutePath().toString());
            fileOutputStream.getChannel().transferFrom(channel, 0, Long.MAX_VALUE);

        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
                if (channel != null) {
                    channel.close();
                }
            } catch (IOException ioExObj) {
                System.out.println("Problem Occurred While Closing The Object= " + ioExObj.getMessage());
            }
        }
    }

    public static class Finder extends SimpleFileVisitor<Path> {

        private final PathMatcher matcher;
        private final List<Path> results = new ArrayList<>();

        Finder(String pattern) {
            matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        }

        // Compares the glob pattern against
        // the file or directory name.
        void find(Path file) {
            Path name = file.getFileName();
            if (name != null && matcher.matches(name)) {
                results.add(file);
                //System.out.println(file);
            }
        }

        // Prints the total number of
        // matches to standard out.
        void done() {
            //System.out.println("Matched: " + numMatches);
        }

        // Invoke the pattern matching
        // method on each file.
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
            find(file);
            return CONTINUE;
        }

        // Invoke the pattern matching
        // method on each directory.
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            find(dir);
            return CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            System.err.println(exc);
            return CONTINUE;
        }
    }

    public static List<Path> findFiles(Path startingDir, String pattern) throws IOException {
        //String pattern = "*.zip";

        Finder finder = new Finder(pattern);
        Files.walkFileTree(startingDir, finder);
        finder.done();
        return finder.results;
    }

    @Measured
    public static void serialize(File file, Object object) {

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            backupFile(file);
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

    //TODO other platforms
    //TODO SMS, GG https://github.com/maxim-zhao/sega8bitheaderreader
    public static byte[] getBytesWOHeader(byte[] bytes) {

        switch (ConfigHolder.platform) {
            case "nes":
                //TODO get all NES length, may be use the same function as for SNES
                //TODO NES, UNIF
                return Arrays.copyOfRange(bytes, 16, bytes.length);
            case "snes":
                if ((bytes.length % 0x400) == 0x200) {
                    return Arrays.copyOfRange(bytes, 0x200, bytes.length);
                } else {
                    return bytes;
                }
        }

        return bytes;
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
    public static Map<Family, Map<Family, Double>> loadFamilyRelations(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);
            Map<Family, Map<Family, Double>> result = (Map<Family, Map<Family, Double>>) ois.readObject();

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
    public static RomsCollection loadCollectionAsJson(File file) {
        try {
            return new ObjectMapper().readValue(file, RomsCollection.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Measured
    public static void serializeAsJson(File file, Object object) {

        try {
            backupFile(file);
            new ObjectMapper().writeValue(file, object);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Measured
    public static void serializeFamiliesAsJson(File file, Map<String, Family> families) {

        try {
            backupFile(file);
            Map<Integer, Name> names = new HashMap<>();

            families.values().forEach(f -> f.getMembers().forEach(m -> {
                names.putIfAbsent(m.hashCode(), m);
            }));

            List<FamilyDto> familyDtos = new ArrayList<>();

            families.values().forEach(f -> {
                List<Integer> members = f.getMembers().stream().map(Object::hashCode).collect(Collectors.toList());
                List<ResultDto> relations = f.getRelations().stream().map(r -> new ResultDto(r.getName1().hashCode(), r.getName2().hashCode(), r.getJakkard())).collect(Collectors.toList());
                int hashCode = (f.getMother() == null) ? f.getMembers().get(0).hashCode() : f.getMother().hashCode();
                familyDtos.add(new FamilyDto(f.getName(), f.getTribe(), members, hashCode, relations, f.isSkip(), f.getType()));
            });

            FamiliesDto familiesDto = new FamiliesDto(
                    familyDtos,
                    names.values()
            );
            new ObjectMapper().writeValue(file, familiesDto);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Measured
    public static void serializeFamilyRelationsAsJson(File file, Map<Family, Map<Family, Double>> familyRelations) {

        try {
            backupFile(file);
            Map<String, Map<String, Double>> model = new LinkedHashMap<>();

            familyRelations.forEach((key1, value1) -> {
                Map<String, Double> children = new LinkedHashMap<>();
                value1.forEach((key, value) -> children.put(key.getName(), value));
                model.put(key1.getName(), children);
            });

            new ObjectMapper().writeValue(file, model);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Measured
    public static Map<String, Family> loadFamiliesAsJson(File file) {

        try {
            FamiliesDto familiesDto = new ObjectMapper().readValue(file, FamiliesDto.class);
            Map<Integer, Name> names = familiesDto.getNames().stream().collect(Collectors.toMap(Name::hashCode, Function.identity()));
            return familiesDto.getFamilies().stream().map(f -> {
                List<Name> members = f.getMembers().stream().map(names::get).collect(Collectors.toList());
                List<Result> relations = f.getRelations().stream().map(r -> new Result(names.get(r.getName1()), names.get(r.getName2()), r.getJakkard())).collect(Collectors.toList());
                return new Family(f.getName(), f.getTribe(), members, names.get(f.getMother()), relations, new HashMap<>(), new HashSet<>(), f.isSkip(), f.getType());

            }).collect(Collectors.toMap(Family::getName, Function.identity()));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Measured
    public static Map<Family, Map<Family, Double>> loadFamilyRelationsAsJson(File file) {

        try {
            TypeReference<LinkedHashMap<String, LinkedHashMap<String, Double>>> typeRef = new TypeReference<LinkedHashMap<String, LinkedHashMap<String, Double>>>() {
            };
            LinkedHashMap<String, LinkedHashMap<String, Double>> model = new ObjectMapper().readValue(file, typeRef);

            Map<Family, Map<Family, Double>> result = new LinkedHashMap<>();

            model.forEach((key1, value1) -> {
                LinkedHashMap<Family, Double> children = new LinkedHashMap<>();
                value1.forEach((key, value) -> {
                    Family family = families.get(key);
                    if (family != null) {
                        children.put(families.get(key), value);
                    } else {
                        LOGGER.warn("Unknown family: {}", key1);
                    }
                });
                result.put(families.get(key1), children);
            });

            return result;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Measured
    public static Path extractFromArchive(Path file, String fileName) {
        try {
            Path tempFile = Files.createTempFile("shingler", fileName);
            Files.write(tempFile, loadBytesFromArchive(file, fileName));
            return tempFile;
        } catch (IOException e) {
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
                    .map(e -> new GID(e.getName(), e.getSize(), (int) e.getCrcValue(), null, null, null, null, null, file.getFileName().toString()));
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
                byte[] bytestwh = IOUtils.getBytesWOHeader(bytes);
                result.add(new GID(entry.getName(), entry.getSize(), (int) entry.getCrcValue(), md5(bytes), sha1(bytes), crc32(bytestwh), md5(bytestwh), sha1(bytestwh), file.getFileName().toString()));
                entry = sevenZFile.getNextEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public static void copyFile(Path src, Path dest) {
        try {
            Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Measured
    public static void deleteFile(Path path) {
        try {
            Files.delete(path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static long fileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> loadTextFile(Path path) {
        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveToFile(Path path, List<String> list) {
        saveToFile(path, String.join("\n", list));
    }

    public static void saveToFile(Path path, String text) {
        try (PrintWriter out = new PrintWriter(path.toFile())) {
            out.println(text);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
