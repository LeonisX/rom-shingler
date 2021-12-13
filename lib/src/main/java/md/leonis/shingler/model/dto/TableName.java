package md.leonis.shingler.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TableName implements Cloneable {

    @JsonProperty("TABLE_NAME")
    private String tableName;
}
