package md.leonis.shingler.utils;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

import static md.leonis.shingler.utils.BinaryUtils.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BinaryUtilsTest {

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

        long[] result = unionArrays(a, b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new long[]{1, 2, 3, 4, 5}, result);
    }

    @Test
    void mergeArrays2() {
        long[] a = {1, 2, 6};
        long[] b = {2, 3, 4, 5};

        long[] result = unionArrays(a, b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new long[]{1, 2, 3, 4, 5, 6}, result);
    }

    @Test
    void mergeArrays3() {
        long[] a = {1, 2, 2};
        long[] b = {2, 3, 4, 5};

        long[] result = unionArrays(a, b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new long[]{1, 2, 3, 4, 5}, result);
    }

    @Test
    void mergeArrays4() {
        long[] a = {1, 2, 3};
        long[] b = {4, 5};

        long[] result = unionArrays(a, b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new long[]{1, 2, 3, 4, 5}, result);
    }

    @Test
    void mergeArrays5() {
        long[] a = {6, 7, 8};
        long[] b = {2, 3};

        long[] result = unionArrays(a, b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new long[]{2, 3, 6, 7, 8}, result);
    }

    @Test
    void testIntersectArrays() {
        long[] a = {1, 2, 3, 4};
        long[] b = {2, 3, 4, 5};

        long[] result = intersectArrays(a, b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new long[]{2, 3, 4}, result);
    }

    @Test
    void testIntersectArrays2() {
        long[] a = {1, 2, 3, 4};
        long[] b = {5, 6, 7};

        long[] result = intersectArrays(a, b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new long[]{}, result);
    }

    @Test
    void testIntersectArrays3() {
        long[] a = {1, 2, 3, 7, 8, 9};
        long[] b = {1, 7};

        long[] result = intersectArrays(a, b);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new long[]{1, 7}, result);
    }

    @Test
    void testFilterArrays() {
        long[] a = {1, 2, 3, 4, 5};

        long[] result = filterArrays(a, 1);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new long[]{1, 2, 3, 4, 5}, result);
    }

    @Test
    void testFilterArrays2() {
        long[] a = {1, 2, 3, 4, 5};

        long[] result = filterArrays(a, 2);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new long[]{2, 4}, result);
    }

    @Test
    void testFilterArrays3() {
        long[] a = {1, 2, 3, 4, 5};

        long[] result = filterArrays(a, 3);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new long[]{3}, result);
    }

    @Test
    void testRemoveDuplicates() {
        long[] a = {1, 2, 3, 4, 4, 5};

        long[] result = removeDuplicates(a);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new long[]{1, 2, 3, 4, 5}, result);
    }

    @Test
    void testRemoveDuplicates2() {
        long[] a = {1, 1, 1, 1, 2, 2, 3, 4, 4, 4, 5};

        long[] result = removeDuplicates(a);
        System.out.println(Arrays.stream(result).boxed().collect(Collectors.toList()));
        assertArrayEquals(new long[]{1, 2, 3, 4, 5}, result);
    }

    @Test
    void testCrc32() {
        assertEquals(3_632_233_996L, crc32("test".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void testMd5() {
        assertEquals("098F6BCD4621D373CADE4E832627B4F6", bytesToHex(md5("test".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void testSha1() {
        assertEquals("A94A8FE5CCB19BA61C4C0873D391E987982FBBD3", bytesToHex(sha1("test".getBytes(StandardCharsets.UTF_8))));
    }
}
