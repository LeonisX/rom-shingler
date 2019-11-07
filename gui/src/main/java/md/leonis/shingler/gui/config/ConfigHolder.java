package md.leonis.shingler.gui.config;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
// A shared object that stores general application settings
public class ConfigHolder {

    private int wordsToLearnCount = 20;
}
