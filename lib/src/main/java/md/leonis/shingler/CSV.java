package md.leonis.shingler;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import md.leonis.shingler.utils.StringUtils;

import java.io.File;
import java.util.List;

public class CSV {

    private static final String BASE_NAME = "base_nes";

    public static void main(String[] args) throws Exception {

        List<MySqlStructure> records = readFile(new File("E:\\" + BASE_NAME + ".csv"));

        records.forEach(r -> System.out.println(String.format("UPDATE `%s` SET `cpu`='%s', `game`='%s', `rom`='%s' WHERE `name`='%s'",
                BASE_NAME, r.getCpu(), r.getGame(), r.getRom(), r.getName())));
    }

    private static List<MySqlStructure> readFile(File csvFile) throws Exception {

        CsvSchema schema = new CsvMapper().schemaFor(MySqlStructure.class).withColumnSeparator(';').withHeader().withQuoteChar('"');


        MappingIterator<MySqlStructure> personIter = new CsvMapper().readerFor(MySqlStructure.class).with(schema).readValues(csvFile);
        return personIter.readAll();
    }

    @Data
    @NoArgsConstructor
    @JsonPropertyOrder({"sid", "name", "cpu", "game", "rom"})
    public static class MySqlStructure implements Cloneable {

        private String sid;
        private String name;
        //@JsonIgnore
        private String oldName;
        private String cpu;
        private String game;
        private String rom;

        public MySqlStructure(AddedStructure addedStructure) {
            this.name = addedStructure.getName();
            this.oldName = name;
            this.cpu = StringUtils.cpu(name);
            this.sid = addedStructure.getSid();
        }

        public String getGame() {
            return game == null ? "" : game.trim();
        }

        public String getRom() {
            return rom == null ? "" : rom.trim();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonPropertyOrder({"sid", "newName", "oldName"})
    public static class RenamedStructure {

        private String sid;
        private String newName;
        private String oldName;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonPropertyOrder({"sid", "name"})
    public static class AddedStructure {

        private String sid;
        private String name;
    }
}
