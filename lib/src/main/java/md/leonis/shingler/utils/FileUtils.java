package md.leonis.shingler.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private static final String JSON = ".json";

    public static Map<String, String> loadJsonMap(Path path, String fileName) {
        try {
            return MAPPER.readValue(path.resolve(fileName + JSON).toFile(), new TypeReference<Map<String, String>>() {
            });
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public static <T> Map<String, List<T>> loadJsonMapWithList(Path path, String fileName, Class<T> clazz) {
        try {
            return MAPPER.readValue(path.resolve(fileName + JSON).toFile(), new TypeReference<Map<String, List<T>>>() {
            });
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public static List<String> loadJsonList(Path path, String fileName) {
        return loadJsonList(path, fileName, String.class);
    }

    public static <T> List<T> loadJsonList(Path path, String fileName, Class<T> clazz) {
        try {
            JavaType type = MAPPER.getTypeFactory().constructCollectionType(List.class, clazz);
            return MAPPER.readValue(path.resolve(fileName + JSON).normalize().toAbsolutePath().toFile(), type);
        } catch (Exception e) {
            if (!(e instanceof FileNotFoundException)) {
                e.printStackTrace();
            }
            return new ArrayList<>();
        }
    }

    public static <T> T loadAsJson(Path path, String fileName, Class<T> clazz) {
        try{
            return MAPPER.readValue(path.resolve(fileName + JSON).normalize().toAbsolutePath().toFile(), clazz);
        } catch (Exception e) {
            LOGGER.debug(e.getMessage());
            return null;
        }
    }

    public static void saveAsJson(Path path, String fileName, Object object) throws IOException {
        saveAsJson(path.resolve(fileName + JSON), object);
    }

    public static void saveAsJson(Path path, Object object) throws IOException {

        backupFile(path);

        String result = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        result = result.replace("&nbsp;", " ");

        Files.createDirectories(path.getParent());
        Files.write(path, result.getBytes());
        LOGGER.debug("Saved: " + path.getFileName().toString());
    }

    public static void backupFile(File file) throws IOException {
        backupFile(file.toPath());
    }

    public static void backupFile(Path path) throws IOException {
        if (Files.exists(path)) {
            Path backupFile = Paths.get(path.normalize().toAbsolutePath().toString() + ".bak");
            if (Files.exists(backupFile)) {
                Files.delete(backupFile);
            }
            Files.move(path, backupFile);
        }
    }

    public static void createDirectories(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            LOGGER.error("Can't create directory: {}", path.toString(), e);
        }
    }

    public static void deleteJsonFile(Path path, String fileName) {
        try {
            Files.delete(path.resolve(fileName + JSON));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
