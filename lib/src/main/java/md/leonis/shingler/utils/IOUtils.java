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
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static md.leonis.shingler.model.ConfigHolder.families;
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

        backupFile(file);

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

        backupFile(file);

        try {
            new ObjectMapper().writeValue(file, object);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Measured
    public static void serializeFamiliesAsJson(File file, Map<String, Family> families) {

        backupFile(file);

        try {
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

        backupFile(file);

        try {
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

    public static void backupFile(File file) {
        if (file.exists()) {
            File backupFile = new File(file.getAbsolutePath() + ".bak");
            if (backupFile.exists()) {
                backupFile.delete();
            }
            file.renameTo(backupFile);
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
        saveToFile(path.toAbsolutePath().toString(), list);
    }

    public static void saveToFile(String fileName, List<String> list) {
        saveToFile(fileName, String.join("\n", list));
    }

    public static void saveToFile(String fileName, String text) {
        try (PrintWriter out = new PrintWriter(fileName)) {
            out.println(text);
        } catch (FileNotFoundException e) {
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
