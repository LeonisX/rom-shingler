package md.leonis.shingler;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.util.List;

public class CSV {

    private static final String BASE_NAME = "base_nes";

    public static void main(String[] args) throws Exception {

        List<MySqlStructure> records = readFile(new File("E:\\" + BASE_NAME + ".csv"));

        records.forEach(r -> {
            System.out.println(String.format("UPDATE `%s` SET `cpu`='%s', `game`='%s', `rom`='%s' WHERE `name`='%s'",
                    BASE_NAME, r.getCpu(), r.getGame(), r.getRom(), r.getName()));
        });
    }

    private static List<MySqlStructure> readFile(File csvFile) throws Exception {

        CsvSchema schema = new CsvMapper().schemaFor(MySqlStructure.class).withColumnSeparator(';').withHeader().withQuoteChar('"');


        MappingIterator<MySqlStructure> personIter = new CsvMapper().readerFor(MySqlStructure.class).with(schema).readValues(csvFile);
        return personIter.readAll();
    }

    @Data
    @NoArgsConstructor
    @JsonPropertyOrder({"name", "cpu", "game", "rom"})
    static class MySqlStructure {

        private String name;
        private String cpu;
        private String game;
        private String rom;
    }
}