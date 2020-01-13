package md.leonis.shingler.utils;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ShingleUtilsTest {

    @Test
    void writeShinglesToFile() {
        File file = new File("tmp");
        int[] shingles = {1, 1, 1, 1, 2, 2, 3, 4, 4, 4, 5};
        ShingleUtils.save(shingles, file);

        int[] result = ShingleUtils.load(file);
        assertArrayEquals(shingles, result);
    }

    @Disabled
    @Test
    void writeShinglesToFile2() {
        File file = new File("tmp");

        int[] shingles = {0, 12, 45, 66, 7, 8, 89, 6575, 234343243, 333, 44, 33, 1, 1, 1, 1, 2, 2, 3, 4, 4, 4, 5};
        ShingleUtils.save(shingles, file);

        int[] result = ShingleUtils.load(file);
        assertArrayEquals(shingles, result);

        file.deleteOnExit();
    }
}
