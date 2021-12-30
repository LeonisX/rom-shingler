package md.leonis.shingler.utils;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import md.leonis.shingler.model.dto.TableName;
import md.leonis.shingler.model.dto.TiviStructure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static md.leonis.shingler.model.ConfigHolder.*;
import static md.leonis.shingler.utils.FileUtils.loadJsonList;
import static md.leonis.shingler.utils.FileUtils.saveAsJson;

public class TiviApiUtils {

    public static final String TIVI = "tivi";

    //TODO
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    public static List<String> readTables(String apiPath, String serverSecret) {
        List<TableName> tables = new ArrayList<>();
        String requestURL = apiPath + "shingler-api.php?to=tables";
        try {
            String jsonString = WebUtils.readFromUrl(requestURL, serverSecret);
            JavaType type = MAPPER.getTypeFactory().constructCollectionType(List.class, TableName.class);
            tables = MAPPER.readValue(jsonString, type);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error in readTables");
        }
        return tables.stream().map(t -> t.getTableName().replace("base_", "")).collect(Collectors.toList());
    }

    public static List<TiviStructure> loadTiviGames(String platformId, boolean force) throws IOException {
        List<TiviStructure> data = loadTiviGames(platformId);
        if (data.isEmpty() || force) {
            data = TiviApiUtils.readTable(apiPath, platformId, serverSecret);
            saveTiviGames(platformId, data);
        }
        return data;
    }


    public static List<TiviStructure> loadTiviGames(String platformId) {
        return loadJsonList(getGamesDir(TIVI), platformId, TiviStructure.class);
    }

    public static void saveTiviGames(String platformId, List<TiviStructure> list) throws IOException {
            saveAsJson(getGamesDir(TIVI), platformId, list);
    }

    public static List<TiviStructure> readTable(String apiPath, String platform, String serverSecret) {
        List<TiviStructure> tables = new ArrayList<>();

        int offset = 0;
        int count = 2000;

        try {
            while (true) {
                String requestURL = String.format("%sshingler-api.php?to=games&platform=%s&offset=%s&count=%s", apiPath, platform, offset, count);
                String jsonString = WebUtils.readFromUrl(requestURL, serverSecret);
                JavaType type = MAPPER.getTypeFactory().constructCollectionType(List.class, TiviStructure.class);
                List<TiviStructure> result = MAPPER.readValue(jsonString, type);
                tables.addAll(result);
                if (result.size() < count) {
                    break;
                }
                offset += count;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error in readTable");
        }
        return tables;
    }
}
