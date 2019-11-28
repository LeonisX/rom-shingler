package md.leonis.shingler.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultDto implements Serializable {

    private Integer name1;
    private Integer name2;
    private double jakkard;
}
