package md.leonis.shingler;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Main1024aTest {

    Logger LOGGER = LoggerFactory.getLogger(Main1024aTest.class);
    /*@Test
    void unionArrays() {
        long[] a = {1,2,3,4};
        long[] b = {2,3,4,5};

        long[] result = Main1024a.unionArrays(a,b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new long[]{1,2,3,4,5}, result);
    }

    @Test
    void unionArrays2() {
        long[] a = {1,2,3,4};
        long[] b = {};

        long[] result = Main1024a.unionArrays(a,b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new long[]{1,2,3,4}, result);
    }

    @Test
    void unionArrays3() {
        long[] b = {1,2,3,4};
        long[] a = {};

        long[] result = Main1024a.unionArrays(a,b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new long[]{1,2,3,4}, result);
    }

    @Test
    void unionArrays4() {
        long[] a = {1,2,3,4};
        long[] b = {5,6,7};

        long[] result = Main1024a.unionArrays(a,b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new long[]{1,2,3,4,5,6,7}, result);
    }*/

    @Test
    void mergeArrays() {
        long[] a = {1, 2, 3, 4};
        long[] b = {2, 3, 4, 5};

        long[] result = Main1024a.unionArrays(a, b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new long[]{1, 2, 3, 4, 5}, result);
    }

    @Test
    void mergeArrays2() {
        long[] a = {1, 2, 6};
        long[] b = {2, 3, 4, 5};

        long[] result = Main1024a.unionArrays(a, b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new long[]{1, 2, 3, 4, 5, 6}, result);
    }

    @Test
    void mergeArrays3() {
        long[] a = {1, 2, 2};
        long[] b = {2, 3, 4, 5};

        long[] result = Main1024a.unionArrays(a, b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new long[]{1, 2, 3, 4, 5}, result);
    }

    @Test
    void mergeArrays4() {
        long[] a = {1, 2, 3};
        long[] b = {4, 5};

        long[] result = Main1024a.unionArrays(a, b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new long[]{1, 2, 3, 4, 5}, result);
    }

    @Test
    void mergeArrays5() {
        long[] a = {6, 7, 8};
        long[] b = {2, 3};

        long[] result = Main1024a.unionArrays(a, b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new long[]{2, 3, 6, 7, 8}, result);
    }

    @Test
    void intersectArrays() {
        long[] a = {1, 2, 3, 4};
        long[] b = {2, 3, 4, 5};

        long[] result = Main1024a.intersectArrays(a, b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new long[]{2, 3, 4}, result);
    }

    @Test
    void intersectArrays2() {
        long[] a = {1, 2, 3, 4};
        long[] b = {5, 6, 7};

        long[] result = Main1024a.intersectArrays(a, b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new long[]{}, result);
    }

    @Test
    void intersectArrays3() {
        long[] a = {1, 2, 3, 7, 8, 9};
        long[] b = {1, 7};

        long[] result = Main1024a.intersectArrays(a, b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new long[]{1, 7}, result);
    }

    @Test
    void filterArrays() {
        long[] a = {1, 2, 3, 4, 5};

        long[] result = Main1024a.filterArrays(a, 1);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new long[]{1, 2, 3, 4, 5}, result);
    }

    @Test
    void filterArrays2() {
        long[] a = {1, 2, 3, 4, 5};

        long[] result = Main1024a.filterArrays(a, 2);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new long[]{2, 4}, result);
    }

    @Test
    void filterArrays3() {
        long[] a = {1, 2, 3, 4, 5};

        long[] result = Main1024a.filterArrays(a, 3);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new long[]{3}, result);
    }

    @Test
    void removeDuplicates() {
        long[] a = {1, 2, 3, 4, 4, 5};

        long[] result = Main1024a.removeDuplicates(a);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new long[]{1, 2, 3, 4, 5}, result);
    }

    @Test
    void removeDuplicates2() {
        long[] a = {1, 1, 1, 1, 2, 2, 3, 4, 4, 4, 5};

        long[] result = Main1024a.removeDuplicates(a);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new long[]{1, 2, 3, 4, 5}, result);
    }

    @Test
    void writeShinglesToFile() {
        File file = new File("tmp");
        long[] shingles = {1, 1, 1, 1, 2, 2, 3, 4, 4, 4, 5};
        Main1024a.writeShinglesToFile(shingles, file);

        long[] result = Main1024a.loadShinglesFromFile(file);
        assertArrayEquals(shingles, result);
    }

    @Disabled
    @Test
    void writeShinglesToFile2() {
        File file = new File("tmp");

        long[] shingles = {0, 12, 45, 66, 7, 8, 89, 6575, 234343243, 333, 44, 33, 1, 1, 1, 1, 2, 2, 3, 4, 4, 4, 5};
        Main1024a.writeShinglesToFile(shingles, file);

        long[] result = Main1024a.loadShinglesFromFile(file);
        assertArrayEquals(shingles, result);

        file.deleteOnExit();
    }

    @Test
    void crc32() {
        assertEquals(3_632_233_996L, Main1024a.crc32("test".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void md5() {
        assertEquals("098F6BCD4621D373CADE4E832627B4F6", Main1024a.bytesToHex(Main1024a.md5("test".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void sha1() {
        assertEquals("A94A8FE5CCB19BA61C4C0873D391E987982FBBD3", Main1024a.bytesToHex(Main1024a.sha1("test".getBytes(StandardCharsets.UTF_8))));
    }
}
