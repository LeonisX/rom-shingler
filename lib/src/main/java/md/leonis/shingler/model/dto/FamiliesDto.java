package md.leonis.shingler.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import md.leonis.shingler.model.Name;

import java.util.Collection;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamiliesDto {

    private Collection<FamilyDto> families;
    private Collection<Name> names;
}
