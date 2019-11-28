package md.leonis.shingler.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamilyDto {

    private String name;
    private List<Integer> members;
    private Integer mother;
    private List<ResultDto> relations;
    //private Map<String, Integer> relationsCount;
    //private Set<String> individualRelations;

    private boolean skip = false;

}
