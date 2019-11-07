package md.leonis.shingler.gui.domain.test;

import lombok.*;

import javax.persistence.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Dictionary {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    private String langFrom;

    private String langTo;

    private String format;

    private String revision;

    private String fullName;

    private Long size;

    private Integer recordsCount;

    private String path;

}
